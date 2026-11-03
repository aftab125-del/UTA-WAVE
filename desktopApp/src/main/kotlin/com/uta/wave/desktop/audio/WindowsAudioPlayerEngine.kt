package com.uta.wave.desktop.audio

import com.uta.wave.audio.AudioPlayerEngine
import com.uta.wave.audio.RepeatMode
import com.uta.wave.audio.UnifiedPlaybackState
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object JavaFxInitializer {
    private val started = AtomicBoolean(false)

    fun initialize() {
        if (started.compareAndSet(false, true)) {
            try {
                Platform.startup { }
            } catch (e: IllegalStateException) {
                // Platform already initialized
            } catch (e: Exception) {
                println("[JavaFxInitializer] Warning starting Platform: ${e.message}")
            }
        }
    }
}

/**
 * Windows Native Audio Engine implementation for Compose Desktop.
 * Powered by OpenJFX Media Foundation (WMF) and backed by an embedded localhost audio proxy.
 */
class WindowsAudioPlayerEngine(
    private val proxy: LocalAudioProxy = LocalAudioProxy()
) : AudioPlayerEngine {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _state = MutableStateFlow(UnifiedPlaybackState())
    override val state: StateFlow<UnifiedPlaybackState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var currentStreamUrl: String? = null
    var onTrackEnded: (() -> Unit)? = null

    init {
        JavaFxInitializer.initialize()
        proxy.start()
    }

    override fun load(
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String?,
        streamUrl: String,
        startPositionMs: Long
    ) {
        currentStreamUrl = streamUrl
        _state.value = _state.value.copy(
            videoId = videoId,
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            isLoading = true,
            isPlaying = false,
            positionMs = startPositionMs,
            errorMessage = null
        )

        // Route stream through local proxy to attach YouTube persona headers
        val playableUrl = if (streamUrl.startsWith("http://127.0.0.1") || streamUrl.startsWith("file:")) {
            streamUrl
        } else {
            proxy.getProxyUrl(streamUrl)
        }

        Platform.runLater {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.dispose()

                val media = Media(playableUrl)
                val player = MediaPlayer(media).apply {
                    volume = _state.value.volume.toDouble()
                    isMute = _state.value.isMuted

                    setOnReady {
                        val durationMs = media.duration.toMillis().toLong()
                        _state.value = _state.value.copy(
                            durationMs = if (durationMs > 0) durationMs else _state.value.durationMs,
                            isLoading = false
                        )
                        if (startPositionMs > 0) {
                            seek(Duration.millis(startPositionMs.toDouble()))
                        }
                        play()
                    }

                    setOnPlaying {
                        _state.value = _state.value.copy(isPlaying = true, isLoading = false)
                    }

                    setOnPaused {
                        _state.value = _state.value.copy(isPlaying = false)
                    }

                    setOnEndOfMedia {
                        if (_state.value.repeatMode == RepeatMode.ONE) {
                            seek(Duration.ZERO)
                            play()
                        } else {
                            _state.value = _state.value.copy(isPlaying = false, positionMs = 0L)
                            onTrackEnded?.invoke()
                        }
                    }

                    setOnError {
                        val err = media.error?.message ?: error?.message ?: "Windows playback decode error"
                        println("[WindowsAudioEngine] Playback error: $err")
                        _state.value = _state.value.copy(
                            isPlaying = false,
                            isLoading = false,
                            errorMessage = err
                        )
                    }

                    currentTimeProperty().addListener { _, _, newTime ->
                        val pos = newTime.toMillis().toLong()
                        if (pos >= 0) {
                            _state.value = _state.value.copy(positionMs = pos)
                        }
                    }

                    bufferProgressTimeProperty().addListener { _, _, newBuffer ->
                        val buf = newBuffer.toMillis().toLong()
                        if (buf >= 0) {
                            _state.value = _state.value.copy(bufferedMs = buf)
                        }
                    }
                }

                mediaPlayer = player
            } catch (e: Exception) {
                println("[WindowsAudioEngine] Failed to load media: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    isPlaying = false,
                    errorMessage = e.message ?: "Failed to load audio"
                )
            }
        }
    }

    override fun play() {
        Platform.runLater {
            mediaPlayer?.play()
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    override fun pause() {
        Platform.runLater {
            mediaPlayer?.pause()
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    override fun seekTo(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs)
        Platform.runLater {
            mediaPlayer?.seek(Duration.millis(positionMs.toDouble()))
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        Platform.runLater {
            mediaPlayer?.volume = clamped.toDouble()
        }
    }

    override fun setMuted(muted: Boolean) {
        _state.value = _state.value.copy(isMuted = muted)
        Platform.runLater {
            mediaPlayer?.isMute = muted
        }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        _state.value = _state.value.copy(repeatMode = mode)
    }

    override fun release() {
        proxy.stop()
        Platform.runLater {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.dispose()
                mediaPlayer = null
            } catch (_: Exception) {}
        }
    }
}
