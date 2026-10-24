package com.uta.wave.data.ringtone

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import com.uta.wave.data.downloads.AudioDownloadManager
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

sealed class RingtoneResult {
    data class Success(val uri: Uri, val isDefaultSet: Boolean) : RingtoneResult()
    data class PermissionRequired(val uri: Uri) : RingtoneResult()
    data class Error(val message: String) : RingtoneResult()
}

class RingtoneHelper(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val innerTubeClient = InnerTubeClient()
    private val downloadManager = AudioDownloadManager.getInstance(context)

    /**
     * Obtains the audio source file, trims the segment from startMs to endMs,
     * saves it to the Android Ringtones storage, and sets it as the default ringtone if permitted.
     */
    suspend fun trimAndSetRingtone(
        track: TrackItem,
        startMs: Long,
        endMs: Long
    ): RingtoneResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.i("Starting Ringtone trim for ${track.title} [${startMs}ms - ${endMs}ms]")

            // 1. Obtain local source file
            val sourceFile = getOrDownloadSourceFile(track)
                ?: return@withContext RingtoneResult.Error("Could not download audio stream for ringtone")

            // 2. Trim audio with MediaExtractor & MediaMuxer
            val cacheDir = File(context.cacheDir, "ringtones").apply { if (!exists()) mkdirs() }
            val cleanTitle = track.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val trimmedFile = File(cacheDir, "${cleanTitle}_ringtone_${System.currentTimeMillis()}.m4a")

            trimAudioFile(sourceFile, trimmedFile, startMs, endMs)

            if (!trimmedFile.exists() || trimmedFile.length() == 0L) {
                return@withContext RingtoneResult.Error("Audio trimming produced an empty file")
            }

            // 3. Save to MediaStore (Ringtones directory)
            val ringtoneUri = saveToMediaStore(trimmedFile, track.title, track.artist)
                ?: return@withContext RingtoneResult.Error("Failed to save ringtone to device storage")

            // 4. Set as default ringtone if write settings permission is available
            val canWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.System.canWrite(context)
            } else {
                true
            }

            if (canWrite) {
                try {
                    RingtoneManager.setActualDefaultRingtoneUri(
                        context,
                        RingtoneManager.TYPE_RINGTONE,
                        ringtoneUri
                    )
                    AppLogger.i("Default ringtone set successfully: $ringtoneUri")
                    RingtoneResult.Success(ringtoneUri, isDefaultSet = true)
                } catch (e: Exception) {
                    AppLogger.w("Could not set actual default ringtone: ${e.message}")
                    RingtoneResult.Success(ringtoneUri, isDefaultSet = false)
                }
            } else {
                AppLogger.i("Ringtone saved, but WRITE_SETTINGS permission is required to set as default")
                RingtoneResult.PermissionRequired(ringtoneUri)
            }
        } catch (e: Exception) {
            AppLogger.e("Ringtone trim failed", e)
            RingtoneResult.Error(e.message ?: "Failed to create ringtone")
        }
    }

    private suspend fun getOrDownloadSourceFile(track: TrackItem): File? {
        // If already downloaded in app, use that file directly
        val existing = downloadManager.getDownloadFile(track.id)
        if (existing != null && existing.exists() && existing.length() > 0L) {
            return existing
        }

        // Otherwise download temporarily to cache
        val streamUrl = innerTubeClient.resolveStreamUrl(track.videoId) ?: return null
        val cacheFile = File(context.cacheDir, "temp_ringtone_source_${track.id.replace(Regex("[^a-zA-Z0-9]"), "_")}.m4a")

        val request = Request.Builder().url(streamUrl).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            FileOutputStream(cacheFile).use { out ->
                body.byteStream().copyTo(out)
            }
        }

        return if (cacheFile.exists() && cacheFile.length() > 0L) cacheFile else null
    }

    private fun trimAudioFile(
        sourceFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ) {
        val extractor = MediaExtractor()
        extractor.setDataSource(sourceFile.absolutePath)

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }

        if (audioTrackIndex < 0 || audioFormat == null) {
            extractor.release()
            throw IllegalStateException("No valid audio track found in source")
        }

        extractor.selectTrack(audioTrackIndex)

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerTrackIndex = muxer.addTrack(audioFormat)
        muxer.start()

        val startUs = startMs * 1000L
        val endUs = endMs * 1000L
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val bufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            128 * 1024
        }

        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        try {
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                if (sampleTime >= startUs) {
                    bufferInfo.presentationTimeUs = sampleTime - startUs
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }
                extractor.advance()
            }
        } finally {
            try { muxer.stop() } catch (e: Exception) { /* ignore */ }
            muxer.release()
            extractor.release()
        }
    }

    private fun saveToMediaStore(file: File, title: String, artist: String): Uri? {
        val fileName = "$title - Ringtone.m4a"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.TITLE, title)
            put(MediaStore.MediaColumns.ARTIST, artist)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            put(MediaStore.Audio.Media.IS_RINGTONE, true)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
            put(MediaStore.Audio.Media.IS_ALARM, false)
            put(MediaStore.Audio.Media.IS_MUSIC, false)
        }

        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        return uri
    }
}
