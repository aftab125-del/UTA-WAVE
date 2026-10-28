package com.uta.wave.data.local

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LocalMediaScanner {

    fun hasAudioPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    suspend fun scanAndPersistLocalTracks(context: Context): List<TrackItem> = withContext(Dispatchers.IO) {
        val tracks = scanLocalTracks(context)
        if (tracks.isNotEmpty()) {
            val db = LocalDatabase.getInstance(context)
            tracks.forEach { track ->
                try {
                    db.insertTrack(track)
                } catch (e: Exception) {
                    AppLogger.w("Could not persist local track ${track.title}: ${e.message}")
                }
            }
            AppLogger.i("Scanned and persisted ${tracks.size} local audio files into database")
        }
        tracks
    }

    suspend fun scanLocalTracks(context: Context): List<TrackItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackItem>()
        val seenPaths = mutableSetOf<String>()

        if (!hasAudioPermission(context)) {
            AppLogger.w("Cannot scan local tracks: Audio read permission not granted")
            return@withContext emptyList()
        }

        // 1. Primary: Query MediaStore Audio
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
                "${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.mp3' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.m4a' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.flac' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.wav' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.aac' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.opus') AND " +
                "(${MediaStore.Audio.Media.DURATION} >= 3000 OR ${MediaStore.Audio.Media.DURATION} IS NULL)"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = it.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val rawTitle = it.getString(titleColumn) ?: "Unknown Track"
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn) ?: ""
                    val durationMs = it.getInt(durationColumn)
                    val albumId = it.getLong(albumIdColumn)
                    val filePath = if (dataColumn >= 0) it.getString(dataColumn) else null

                    if (!filePath.isNullOrBlank()) {
                        seenPaths.add(filePath)
                    }

                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val cleanTitle = if (rawTitle.isBlank() && !filePath.isNullOrBlank()) {
                        File(filePath).nameWithoutExtension
                    } else {
                        rawTitle
                    }

                    tracks.add(
                        TrackItem(
                            id = "local:$id",
                            videoId = contentUri, // Content URI for native ExoPlayer playback
                            title = cleanTitle,
                            artist = if (artist == "<unknown>" || artist.isBlank()) "Local Music" else artist,
                            album = album,
                            artworkUrl = artworkUri,
                            durationSeconds = (durationMs / 1000).coerceAtLeast(0),
                            isSynced = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.e("MediaStore scan error: ${e.message}", e)
        }

        // 2. Secondary Fallback: Scan public Music and Download directories for unindexed MP3s
        try {
            val publicDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            )

            for (dir in publicDirs) {
                if (dir.exists() && dir.isDirectory) {
                    scanDirectoryRecursive(dir, seenPaths, tracks)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Direct directory scan fallback error: ${e.message}", e)
        }

        tracks
    }

    private fun scanDirectoryRecursive(
        dir: File,
        seenPaths: MutableSet<String>,
        tracks: MutableList<TrackItem>,
        maxDepth: Int = 3
    ) {
        if (maxDepth <= 0) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectoryRecursive(file, seenPaths, tracks, maxDepth - 1)
            } else if (file.isFile && !seenPaths.contains(file.absolutePath)) {
                val nameLower = file.name.lowercase()
                if (nameLower.endsWith(".mp3") || nameLower.endsWith(".m4a") ||
                    nameLower.endsWith(".flac") || nameLower.endsWith(".wav") ||
                    nameLower.endsWith(".aac") || nameLower.endsWith(".opus")
                ) {
                    seenPaths.add(file.absolutePath)
                    tracks.add(extractMetadataFromFile(file))
                }
            }
        }
    }

    private fun extractMetadataFromFile(file: File): TrackItem {
        var title = file.nameWithoutExtension
        var artist = "Local Music"
        var album = ""
        var durationSec = 0

        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(file.absolutePath)
            val extractedTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val extractedArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val extractedAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

            if (!extractedTitle.isNullOrBlank()) title = extractedTitle
            if (!extractedArtist.isNullOrBlank()) artist = extractedArtist
            if (!extractedAlbum.isNullOrBlank()) album = extractedAlbum
            durationSec = (durationMs / 1000).toInt()
            mmr.release()
        } catch (e: Exception) {
            // fallback to filename
        }

        val safeId = "local_file_" + file.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return TrackItem(
            id = safeId,
            videoId = Uri.fromFile(file).toString(),
            title = title,
            artist = artist,
            album = album,
            artworkUrl = "",
            durationSeconds = durationSec,
            isSynced = false
        )
    }
}
