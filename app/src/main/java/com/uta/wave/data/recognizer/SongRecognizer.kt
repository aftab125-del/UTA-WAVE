package com.uta.wave.data.recognizer

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.sqrt

sealed class RecognitionState {
    object Idle : RecognitionState()
    data class Listening(val progress: Float) : RecognitionState()
    object Analyzing : RecognitionState()
    data class Success(val track: TrackItem) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

class SongRecognizer(
    private val context: Context,
    private val innerTubeClient: InnerTubeClient = InnerTubeClient(),
    private val localDatabase: LocalDatabase = LocalDatabase.getInstance(context)
) {
    companion object {
        private const val RECORDING_SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val RECORDING_DURATION_MS = 6500L
    }

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude = _amplitude.asStateFlow()

    private var activeJob: Job? = null

    @SuppressLint("MissingPermission")
    suspend fun startListening() = withContext(Dispatchers.IO) {
        if (_state.value is RecognitionState.Listening || _state.value is RecognitionState.Analyzing) {
            return@withContext
        }

        _state.value = RecognitionState.Listening(0f)
        _amplitude.value = 0f

        val minBufferSize = AudioRecord.getMinBufferSize(
            RECORDING_SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        val bufferSize = minBufferSize.coerceAtLeast(4096)

        var audioRecord: AudioRecord? = null
        val outputStream = ByteArrayOutputStream()

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RECORDING_SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                _state.value = RecognitionState.Error("Microphone not initialized. Check permissions.")
                return@withContext
            }

            audioRecord.startRecording()

            val readBuffer = ByteArray(bufferSize)
            val startTime = System.currentTimeMillis()
            var maxRmsDetected = 0f

            while (isActive && (System.currentTimeMillis() - startTime) < RECORDING_DURATION_MS) {
                val bytesRead = audioRecord.read(readBuffer, 0, readBuffer.size)
                if (bytesRead > 0) {
                    outputStream.write(readBuffer, 0, bytesRead)

                    // Calculate RMS amplitude from 16-bit PCM (Little-Endian)
                    var sum = 0.0
                    val sampleCount = bytesRead / 2
                    for (i in 0 until sampleCount) {
                        val sample = ((readBuffer[i * 2 + 1].toInt() shl 8) or (readBuffer[i * 2].toInt() and 0xFF)).toShort()
                        sum += sample * sample
                    }

                    if (sampleCount > 0) {
                        val rms = sqrt(sum / sampleCount).toFloat() / 32768f
                        val normalized = (rms * 4.5f).coerceIn(0f, 1f)
                        _amplitude.value = normalized
                        if (normalized > maxRmsDetected) {
                            maxRmsDetected = normalized
                        }
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / RECORDING_DURATION_MS).coerceIn(0f, 1f)
                _state.value = RecognitionState.Listening(progress)

                delay(40)
            }

            // Stop recording
            try {
                audioRecord.stop()
                audioRecord.release()
            } catch (ignored: Exception) {}
            audioRecord = null
            _amplitude.value = 0f

            // Check if silent
            if (maxRmsDetected < 0.015f) {
                _state.value = RecognitionState.Error("No sound detected. Please hold your phone closer to the music.")
                return@withContext
            }

            // Move to analyzing
            _state.value = RecognitionState.Analyzing

            val rawAudioBytes = outputStream.toByteArray()
            if (rawAudioBytes.isEmpty() || rawAudioBytes.size < 4096) {
                _state.value = RecognitionState.Error("Captured audio was too short. Try again.")
                return@withContext
            }

            // Resample from 44.1 kHz to 16.0 kHz using Media3 SonicAudioProcessor
            val decodedAudio = DecodedAudio(
                data = rawAudioBytes,
                channelCount = 1,
                sampleRate = RECORDING_SAMPLE_RATE,
                pcmEncoding = AUDIO_FORMAT
            )

            val resampledResult = AudioResampler.resample(
                decodedAudio,
                ShazamSignatureGenerator.REQUIRED_SAMPLE_RATE
            ).getOrElse { error ->
                AppLogger.e("Resampling error: ${error.message}", error)
                _state.value = RecognitionState.Error("Failed to resample audio: ${error.message}")
                return@withContext
            }

            require(
                resampledResult.channelCount == 1 &&
                        resampledResult.sampleRate == ShazamSignatureGenerator.REQUIRED_SAMPLE_RATE &&
                        resampledResult.pcmEncoding == AudioFormat.ENCODING_PCM_16BIT &&
                        ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN &&
                        resampledResult.data.isNotEmpty() &&
                        resampledResult.data.size % 2 == 0
            ) {
                "Invalid resampled audio format for Shazam fingerprinting"
            }

            // Generate Shazam DejaVu signature
            val signature = try {
                ShazamSignatureGenerator.fromI16(resampledResult.data)
            } catch (e: Exception) {
                AppLogger.e("Signature generation failed: ${e.message}", e)
                _state.value = RecognitionState.Error("Failed to generate acoustic fingerprint: ${e.message}")
                return@withContext
            }

            val sampleDurationMs = (resampledResult.data.size / 2) * 1000L / ShazamSignatureGenerator.REQUIRED_SAMPLE_RATE

            // Query Shazam Discovery Cloud API
            val shazamResponse = ShazamApiClient.recognize(signature, sampleDurationMs)

            shazamResponse.fold(
                onSuccess = { shazamMatch ->
                    AppLogger.d("Shazam recognized: '${shazamMatch.title}' by '${shazamMatch.artist}'")

                    // Resolve YouTube/InnerTube playback metadata
                    val trackItem = resolveStreamableTrack(shazamMatch)
                    localDatabase.insertRecognizedTrack(trackItem)
                    _state.value = RecognitionState.Success(trackItem)
                },
                onFailure = { error ->
                    val msg = error.message ?: "Recognition failed"
                    if (msg.contains("No match", ignoreCase = true)) {
                        _state.value = RecognitionState.Error("No match found. Try again closer to the speaker.")
                    } else {
                        _state.value = RecognitionState.Error(msg)
                    }
                }
            )

        } catch (e: SecurityException) {
            _state.value = RecognitionState.Error("Microphone permission required for song recognition.")
        } catch (e: Exception) {
            AppLogger.e("Recognition pipeline error: ${e.message}", e)
            _state.value = RecognitionState.Error("Recognition failed: ${e.message}")
        } finally {
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (ignored: Exception) {}
            _amplitude.value = 0f
        }
    }

    private suspend fun resolveStreamableTrack(shazam: ShazamResult): TrackItem {
        val highResArt = (shazam.coverArtHqUrl ?: shazam.coverArtUrl)
            ?.replace("400x400", "1000x1000")
            ?: ""

        // If Shazam gave us a direct YouTube video ID
        val directVideoId = shazam.youtubeVideoId?.takeIf { it.isNotBlank() }

        // Search InnerTube for best match stream
        try {
            val query = "${shazam.title} ${shazam.artist}".trim()
            val searchResults = innerTubeClient.search(query)
            if (searchResults.isNotEmpty()) {
                val best = searchResults.first()
                return TrackItem(
                    id = best.id.ifBlank { UUID.randomUUID().toString() },
                    videoId = if (!directVideoId.isNullOrBlank()) directVideoId else best.videoId,
                    title = shazam.title.ifBlank { best.title },
                    artist = shazam.artist.ifBlank { best.artist },
                    album = shazam.album ?: best.album,
                    artworkUrl = if (highResArt.isNotBlank()) highResArt else best.artworkUrl,
                    durationSeconds = if (best.durationSeconds > 0) best.durationSeconds else 180,
                    isSynced = best.isSynced
                )
            }
        } catch (e: Exception) {
            AppLogger.w("InnerTube search resolution failed: ${e.message}")
        }

        // Return direct TrackItem with Shazam details
        return TrackItem(
            id = UUID.randomUUID().toString(),
            videoId = directVideoId ?: "",
            title = shazam.title,
            artist = shazam.artist,
            album = shazam.album,
            artworkUrl = highResArt,
            durationSeconds = 180,
            isSynced = false
        )
    }

    fun reset() {
        activeJob?.cancel()
        _state.value = RecognitionState.Idle
        _amplitude.value = 0f
    }
}
