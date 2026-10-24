package com.uta.wave.data.ringtone

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
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

            // 1. Obtain local source file (prefer AAC stream to guarantee native MP4 muxer compatibility)
            val sourceFile = getOrDownloadSourceFile(track)
                ?: return@withContext RingtoneResult.Error("Could not download audio stream for ringtone")

            // 2. Trim audio using direct remux (AAC) or MediaCodec transcode fallback (Opus/WebM/OEM quirks)
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
            AppLogger.i("Using existing offline download file for ringtone: ${existing.absolutePath}")
            return existing
        }

        // Request AAC stream from InnerTube for maximum Android muxer compatibility
        val streamUrl = innerTubeClient.resolveStreamUrl(track.videoId, qualityTag = "AAC") ?: return null
        val cacheDir = File(context.cacheDir, "ringtones").apply { if (!exists()) mkdirs() }
        val cacheFile = File(cacheDir, "temp_source_${track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.m4a")

        if (cacheFile.exists()) {
            cacheFile.delete()
        }

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

        val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
        val isAac = mime.contains("mp4a") || mime.contains("aac")

        if (isAac) {
            try {
                directRemuxAac(extractor, audioTrackIndex, audioFormat, outputFile, startMs, endMs)
                extractor.release()
                return
            } catch (e: Exception) {
                AppLogger.w("Direct AAC remux failed (${e.message}), falling back to MediaCodec transcode...")
                if (outputFile.exists()) outputFile.delete()
            }
        }

        extractor.release()

        // Universal MediaCodec transcoding fallback (handles Opus/WebM and OEM muxer strictness)
        transcodeAudioToAac(sourceFile, outputFile, startMs, endMs)
    }

    private fun directRemuxAac(
        extractor: MediaExtractor,
        audioTrackIndex: Int,
        audioFormat: MediaFormat,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ) {
        extractor.selectTrack(audioTrackIndex)

        // Sanitize MediaFormat: copy only required keys to prevent native MediaMuxer rejection on ColorOS/MIUI
        val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else 44100
        val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else 2

        val cleanFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
        if (audioFormat.containsKey("csd-0")) {
            cleanFormat.setByteBuffer("csd-0", audioFormat.getByteBuffer("csd-0"))
        }
        if (audioFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            cleanFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioFormat.getInteger(MediaFormat.KEY_BIT_RATE))
        }

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        try {
            val muxerTrackIndex = muxer.addTrack(cleanFormat)
            muxer.start()
            muxerStarted = true

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val bufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024)
            } else {
                128 * 1024
            }

            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var sampleCount = 0
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                if (sampleTime >= startUs) {
                    bufferInfo.presentationTimeUs = (sampleTime - startUs).coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    sampleCount++
                }
                extractor.advance()
            }
            AppLogger.i("Direct remux complete: wrote $sampleCount AAC samples")
            if (sampleCount == 0) {
                throw IllegalStateException("Direct remux yielded 0 samples in selected range")
            }
        } finally {
            if (muxerStarted) {
                try { muxer.stop() } catch (e: Exception) { AppLogger.w("Muxer stop warning: ${e.message}") }
            }
            try { muxer.release() } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun transcodeAudioToAac(
        sourceFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ) {
        AppLogger.i("Starting MediaCodec transcode to AAC: ${sourceFile.name} [${startMs}ms - ${endMs}ms]")
        if (outputFile.exists()) outputFile.delete()

        val extractor = MediaExtractor()
        extractor.setDataSource(sourceFile.absolutePath)

        var audioTrackIndex = -1
        var inputFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                inputFormat = format
                break
            }
        }

        if (audioTrackIndex < 0 || inputFormat == null) {
            extractor.release()
            throw IllegalStateException("No valid audio track found for transcoding")
        }

        extractor.selectTrack(audioTrackIndex)
        val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: ""

        val decoder = MediaCodec.createDecoderByType(inputMime)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerTrackIndex = -1
        var muxerStarted = false

        val startUs = startMs * 1000L
        val endUs = endMs * 1000L
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        var extractorDone = false
        var decoderDone = false
        var encoderDone = false

        val bufferInfo = MediaCodec.BufferInfo()
        val timeoutUs = 5000L

        fun drainEncoderOutput(): Boolean {
            val enc = encoder ?: return false
            val mux = muxer ?: return false
            var eos = false

            while (true) {
                val encOutIndex = enc.dequeueOutputBuffer(bufferInfo, 0L)
                if (encOutIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
                if (encOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        val newFormat = enc.outputFormat
                        AppLogger.i("AAC Encoder output format configured: $newFormat")
                        muxerTrackIndex = mux.addTrack(newFormat)
                        mux.start()
                        muxerStarted = true
                    }
                } else if (encOutIndex >= 0) {
                    val encOutBuf = enc.getOutputBuffer(encOutIndex)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && muxerStarted && encOutBuf != null) {
                        encOutBuf.position(bufferInfo.offset)
                        encOutBuf.limit(bufferInfo.offset + bufferInfo.size)
                        mux.writeSampleData(muxerTrackIndex, encOutBuf, bufferInfo)
                    }
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                    enc.releaseOutputBuffer(encOutIndex, false)
                }
            }
            return eos
        }

        try {
            var loopCount = 0
            val maxLoops = 250000

            while (!encoderDone && loopCount++ < maxLoops) {
                // 1. Feed Extractor -> Decoder
                if (!extractorDone) {
                    val inIndex = decoder.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val inBuf = decoder.getInputBuffer(inIndex)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            val sampleTime = extractor.sampleTime
                            if (sampleSize < 0 || sampleTime > endUs) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                extractorDone = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, extractor.sampleFlags)
                                extractor.advance()
                            }
                        }
                    }
                }

                // 2. Drain Decoder -> Feed into Encoder
                if (!decoderDone) {
                    val outIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED || (outIndex >= 0 && encoder == null)) {
                        val pcmFormat = decoder.outputFormat
                        val sampleRate = if (pcmFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            pcmFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        } else 44100
                        val channelCount = if (pcmFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            pcmFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 2)
                        } else 2

                        AppLogger.i("Decoder PCM format ready: ${sampleRate}Hz, $channelCount channels")

                        val aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                            setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 32768)
                        }
                        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                            configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                            start()
                        }
                        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    }

                    if (outIndex >= 0) {
                        val decodedBuf = decoder.getOutputBuffer(outIndex)
                        val isEos = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                        val enc = encoder
                        if (enc != null) {
                            if (bufferInfo.size > 0 && decodedBuf != null && bufferInfo.presentationTimeUs >= startUs) {
                                var queued = false
                                var attempts = 0
                                while (!queued && attempts++ < 20) {
                                    val encInIndex = enc.dequeueInputBuffer(timeoutUs)
                                    if (encInIndex >= 0) {
                                        val encInBuf = enc.getInputBuffer(encInIndex)
                                        if (encInBuf != null) {
                                            encInBuf.clear()
                                            decodedBuf.position(bufferInfo.offset)
                                            decodedBuf.limit(bufferInfo.offset + bufferInfo.size)
                                            encInBuf.put(decodedBuf)

                                            val adjTimeUs = (bufferInfo.presentationTimeUs - startUs).coerceAtLeast(0L)
                                            val flags = if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                                            enc.queueInputBuffer(encInIndex, 0, bufferInfo.size, adjTimeUs, flags)
                                            queued = true
                                        }
                                    } else {
                                        if (drainEncoderOutput()) encoderDone = true
                                    }
                                }
                            } else if (isEos) {
                                var queued = false
                                var attempts = 0
                                while (!queued && attempts++ < 20) {
                                    val encInIndex = enc.dequeueInputBuffer(timeoutUs)
                                    if (encInIndex >= 0) {
                                        enc.queueInputBuffer(encInIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                        queued = true
                                    } else {
                                        if (drainEncoderOutput()) encoderDone = true
                                    }
                                }
                            }
                        }

                        decoder.releaseOutputBuffer(outIndex, false)
                        if (isEos) {
                            decoderDone = true
                        }
                    }
                }

                // 3. Encoder -> Muxer
                if (drainEncoderOutput()) {
                    encoderDone = true
                }
            }
            AppLogger.i("MediaCodec transcode finished. loopCount: $loopCount, encoderDone: $encoderDone")
        } finally {
            try { decoder.stop() } catch (ignored: Exception) {}
            try { decoder.release() } catch (ignored: Exception) {}
            try { encoder?.stop() } catch (ignored: Exception) {}
            try { encoder?.release() } catch (ignored: Exception) {}
            if (muxerStarted) {
                try { muxer?.stop() } catch (e: Exception) { AppLogger.w("Muxer stop warning: ${e.message}") }
            }
            try { muxer?.release() } catch (ignored: Exception) {}
            try { extractor.release() } catch (ignored: Exception) {}
        }
    }

    private fun saveToMediaStore(file: File, title: String, artist: String): Uri? {
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9 _-]"), "").trim().take(40)
        val fileName = "$sanitizedTitle - Ringtone.m4a"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.TITLE, sanitizedTitle)
                put(MediaStore.MediaColumns.ARTIST, artist)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
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
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } else {
            // Android 8.0 - 9.0 (API 26-28)
            val ringtonesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
            if (!ringtonesDir.exists()) ringtonesDir.mkdirs()
            val destFile = File(ringtonesDir, fileName)
            file.copyTo(destFile, overwrite = true)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
                put(MediaStore.MediaColumns.TITLE, sanitizedTitle)
                put(MediaStore.MediaColumns.ARTIST, artist)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                put(MediaStore.MediaColumns.SIZE, destFile.length())
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }
            context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: Uri.fromFile(destFile)
        }
    }
}
