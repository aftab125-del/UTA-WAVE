package com.uta.wave.data.downloads

import android.content.Context
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class AudioQuality(val label: String, val badge: String, val description: String) {
    LOSSLESS("Lossless / Master", "LOSSLESS", "256 kbps AAC / 160 kbps Opus • Studio Fidelity"),
    HIGH("High Quality", "HIGH", "160 kbps • Crystal clear audio"),
    STANDARD("Standard Quality", "STANDARD", "128 kbps M4A • Compact size")
}

class AudioDownloadManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AudioDownloadManager? = null

        fun getInstance(context: Context): AudioDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val localDatabase = LocalDatabase.getInstance(context)
    private val innerTubeClient = InnerTubeClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds = _downloadingIds.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    private val downloadsDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "downloads")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun isDownloaded(trackId: String): Boolean {
        return localDatabase.isDownloadedSync(trackId)
    }

    fun getDownloadFile(trackId: String): File? {
        val path = localDatabase.getDownloadPathSync(trackId) ?: return null
        val file = File(path)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun downloadTrack(
        track: TrackItem,
        quality: AudioQuality = AudioQuality.LOSSLESS,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (_downloadingIds.value.contains(track.id) || isDownloaded(track.id)) {
            onComplete(true)
            return
        }

        _downloadingIds.update { it + track.id }
        _downloadProgress.update { it + (track.id to 0f) }

        scope.launch {
            try {
                AppLogger.i("Starting download (${quality.badge}) for: ${track.title}")
                val streamUrl = innerTubeClient.resolveStreamUrl(track.videoId, quality.badge)
                if (streamUrl.isNullOrBlank()) {
                    AppLogger.e("Failed to resolve stream URL for download: ${track.title}")
                    _downloadingIds.update { it - track.id }
                    _downloadProgress.update { it - track.id }
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@launch
                }

                val safeId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val outputFile = File(downloadsDir, "$safeId.m4a")

                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        AppLogger.e("Download HTTP failed: ${response.code} ${response.message}")
                        if (outputFile.exists()) outputFile.delete()
                        _downloadingIds.update { it - track.id }
                        _downloadProgress.update { it - track.id }
                        withContext(Dispatchers.Main) { onComplete(false) }
                        return@launch
                    }

                    val body = response.body
                    if (body == null) {
                        if (outputFile.exists()) outputFile.delete()
                        _downloadingIds.update { it - track.id }
                        _downloadProgress.update { it - track.id }
                        withContext(Dispatchers.Main) { onComplete(false) }
                        return@launch
                    }

                    val contentLength = body.contentLength()
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(outputFile)

                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    var totalRead = 0L

                    try {
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                                _downloadProgress.update { it + (track.id to progress) }
                            }
                        }
                        outputStream.flush()
                    } finally {
                        try { outputStream.close() } catch (e: Exception) { /* ignore */ }
                        try { inputStream.close() } catch (e: Exception) { /* ignore */ }
                    }

                    if (outputFile.exists() && outputFile.length() > 0L) {
                        localDatabase.insertDownload(track, outputFile.absolutePath, outputFile.length(), quality.badge)
                        AppLogger.i("Download complete (${quality.badge}) for: ${track.title} (${outputFile.length() / 1024} KB)")

                        _downloadingIds.update { it - track.id }
                        _downloadProgress.update { it - track.id }
                        withContext(Dispatchers.Main) { onComplete(true) }
                    } else {
                        if (outputFile.exists()) outputFile.delete()
                        AppLogger.e("Downloaded file is empty or missing: ${outputFile.absolutePath}")
                        _downloadingIds.update { it - track.id }
                        _downloadProgress.update { it - track.id }
                        withContext(Dispatchers.Main) { onComplete(false) }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Download error for: ${track.title}: ${e.message}", e)
                try {
                    val safeId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val outputFile = File(downloadsDir, "$safeId.m4a")
                    if (outputFile.exists()) outputFile.delete()
                } catch (ex: Exception) { /* ignore */ }
                _downloadingIds.update { it - track.id }
                _downloadProgress.update { it - track.id }
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun deleteDownload(trackId: String) {
        scope.launch {
            val path = localDatabase.getDownloadPathSync(trackId)
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) file.delete()
            }
            localDatabase.deleteDownload(trackId)
            AppLogger.i("Deleted download for trackId: $trackId")
        }
    }

    fun downloadTracks(
        tracks: List<TrackItem>,
        quality: AudioQuality = AudioQuality.LOSSLESS
    ) {
        val pending = tracks.filter { !isDownloaded(it.id) && !_downloadingIds.value.contains(it.id) }
        pending.forEach { track ->
            downloadTrack(track, quality)
        }
    }

    fun deleteTracks(tracks: List<TrackItem>) {
        tracks.forEach { track ->
            deleteDownload(track.id)
        }
    }

    fun areAllTracksDownloaded(tracks: List<TrackItem>): Boolean {
        if (tracks.isEmpty()) return false
        return tracks.all { isDownloaded(it.id) }
    }

    fun isAnyTrackDownloading(tracks: List<TrackItem>): Boolean {
        val downloading = _downloadingIds.value
        return tracks.any { downloading.contains(it.id) }
    }

    fun countDownloadedTracks(tracks: List<TrackItem>): Int {
        return tracks.count { isDownloaded(it.id) }
    }
}
