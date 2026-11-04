package com.uta.wave.desktop.audio

import com.uta.wave.audio.AudioPlayerEngine
import com.uta.wave.audio.RepeatMode
import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.data.DesktopPreferences
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Candidate info for next track passed from queue orchestrator.
 */
data class NextTrackCandidate(
    val track: TrackItem,
    val queueIndex: Int,
    val isSameAlbum: Boolean
)

/**
 * Windows Native Audio Engine implementation for Compose Desktop.
 *
 * Direct parity with UTA-WAVE Mobile's dual-player architecture:
 * - Dual OpenJFX MediaPlayer instances (primaryPlayer + standbyPlayer).
 * - EqualPowerCurve (quarter-sine / quarter-cosine constant acoustic power).
 * - AdaptivePreArmManager (20s-35s dynamic lead time pre-buffering).
 * - Ready-Gate safety protection.
 * - Gapless album exemption.
 * - Atomic player swap on fade completion.
 */
class WindowsAudioPlayerEngine(
    private val proxy: LocalAudioProxy = LocalAudioProxy()
) : AudioPlayerEngine {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _state = MutableStateFlow(UnifiedPlaybackState())
    override val state: StateFlow<UnifiedPlaybackState> = _state.asStateFlow()

    // Dual-Player Instances for Seamless Crossfade
    private var primaryPlayer: MediaPlayer? = null
    private var standbyPlayer: MediaPlayer? = null

    // Seeking synchronization latch (prevents stale currentTime ticks from snapping the timeline back)
    @Volatile
    private var isSeeking = false
    private var targetSeekPosMs: Long = -1L
    private var lastSeekTimestampMs: Long = 0L

    // Pre-arm & Crossfade management
    private val preArmManager = AdaptivePreArmManager()
    private var preArmedTrackId: String? = null
    private var isPreArming = false
    private var isPreArmed = false
    private var isCrossfading = false

    private var preArmJob: Job? = null
    private var crossfadeJob: Job? = null

    // Orchestrator callbacks
    var nextTrackProvider: (() -> NextTrackCandidate?)? = null
    var streamUrlResolver: (suspend (String) -> String?)? = null
    var onTrackSwapped: ((TrackItem, Int) -> Unit)? = null
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
        cancelCrossfade()
        isSeeking = false

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

        val playableUrl = if (streamUrl.startsWith("http://127.0.0.1") || streamUrl.startsWith("file:")) {
            streamUrl
        } else {
            proxy.getProxyUrl(streamUrl)
        }

        Platform.runLater {
            try {
                primaryPlayer?.stop()
                primaryPlayer?.dispose()

                val media = Media(playableUrl)
                val player = MediaPlayer(media).apply {
                    volume = _state.value.volume.toDouble()
                    isMute = _state.value.isMuted
                }

                attachPrimaryListeners(player, media, startPositionMs)
                primaryPlayer = player
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

    private fun attachPrimaryListeners(player: MediaPlayer, media: Media, startPositionMs: Long = 0L) {
        player.setOnReady {
            val durationMs = media.duration.toMillis().toLong()
            _state.value = _state.value.copy(
                durationMs = if (durationMs > 0) durationMs else _state.value.durationMs,
                isLoading = false
            )
            if (startPositionMs > 0) {
                player.seek(Duration.millis(startPositionMs.toDouble()))
            }
            player.play()
        }

        player.setOnPlaying {
            _state.value = _state.value.copy(isPlaying = true, isLoading = false)
        }

        player.setOnPaused {
            if (!isCrossfading) {
                _state.value = _state.value.copy(isPlaying = false)
            }
        }

        player.setOnEndOfMedia {
            if (_state.value.repeatMode == RepeatMode.ONE) {
                player.seek(Duration.ZERO)
                player.play()
            } else if (!isCrossfading) {
                _state.value = _state.value.copy(isPlaying = false, positionMs = 0L)
                onTrackEnded?.invoke()
            }
        }

        player.setOnError {
            val err = media.error?.message ?: player.error?.message ?: "Windows playback decode error"
            println("[WindowsAudioEngine] Primary player error: $err")
            if (!isCrossfading) {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isLoading = false,
                    errorMessage = err
                )
            }
        }

        media.durationProperty().addListener { _, _, newDur ->
            val durMs = newDur?.toMillis()?.toLong() ?: 0L
            if (durMs > 0) {
                _state.value = _state.value.copy(durationMs = durMs)
            }
        }

        player.currentTimeProperty().addListener { _, _, newTime ->
            val pos = newTime.toMillis().toLong()
            if (isSeeking) {
                if (kotlin.math.abs(pos - targetSeekPosMs) < 1500L || System.currentTimeMillis() - lastSeekTimestampMs > 1500L) {
                    isSeeking = false
                } else {
                    return@addListener
                }
            }
            if (pos >= 0) {
                _state.value = _state.value.copy(positionMs = pos)
                val dur = _state.value.durationMs
                if (dur > 0) {
                    checkAndTriggerCrossfade(pos, dur)
                }
            }
        }

        player.bufferProgressTimeProperty().addListener { _, _, newBuffer ->
            val buf = newBuffer.toMillis().toLong()
            if (buf >= 0) {
                _state.value = _state.value.copy(bufferedMs = buf)
            }
        }
    }

    /**
     * Checks whether to pre-arm standby player or initiate equal-power crossfade ramp.
     */
    private fun checkAndTriggerCrossfade(currentPos: Long, duration: Long) {
        val prefs = DesktopPreferences.getInstance().settings.value
        if (!prefs.crossfadeEnabled || isCrossfading || duration <= 10_000L) return
        if (_state.value.repeatMode == RepeatMode.ONE) return

        val crossfadeDurationMs = (prefs.crossfadeDurationSeconds * 1000L).coerceIn(1000L, 15_000L)
        val remainingMs = duration - currentPos

        val candidate = nextTrackProvider?.invoke() ?: return

        // Gapless album check (EchoMusic / UTA-WAVE standard): don't crossfade if same album
        if (prefs.crossfadeGapless && candidate.isSameAlbum) {
            return
        }

        // 1. Adaptive Pre-Arm Phase: Pre-buffer next track on standbyPlayer ahead of time
        val leadTimeMs = preArmManager.currentLeadTimeMs
        if (remainingMs in (crossfadeDurationMs + 100L)..leadTimeMs) {
            if (!isPreArming && (!isPreArmed || preArmedTrackId != candidate.track.id)) {
                preArmStandbyPlayer(candidate.track)
            }
        }

        // 2. Trigger Crossfade Phase: Start equal-power ramp when remaining playback reaches duration
        if (remainingMs in 1..crossfadeDurationMs) {
            startCrossfade(candidate.track, candidate.queueIndex, crossfadeDurationMs)
        }
    }

    private fun preArmStandbyPlayer(nextTrack: TrackItem) {
        if (isPreArming || isCrossfading) return
        isPreArming = true
        preArmJob?.cancel()

        preArmJob = scope.launch(Dispatchers.IO) {
            try {
                val startPrepMs = System.currentTimeMillis()
                val resolver = streamUrlResolver
                val streamUrl = resolver?.invoke(nextTrack.videoId)

                if (streamUrl.isNullOrBlank()) {
                    isPreArming = false
                    return@launch
                }

                val playableUrl = if (streamUrl.startsWith("http://127.0.0.1") || streamUrl.startsWith("file:")) {
                    streamUrl
                } else {
                    proxy.getProxyUrl(streamUrl)
                }

                Platform.runLater {
                    try {
                        standbyPlayer?.stop()
                        standbyPlayer?.dispose()

                        val media = Media(playableUrl)
                        val player = MediaPlayer(media).apply {
                            volume = 0.0
                            isMute = false
                        }

                        player.setOnReady {
                            val prepDuration = System.currentTimeMillis() - startPrepMs
                            if (prepDuration > 0) {
                                preArmManager.recordPreparationDuration(prepDuration)
                            }
                            isPreArmed = true
                            isPreArming = false
                            preArmedTrackId = nextTrack.id
                            println("[WindowsAudioEngine] Standby pre-armed & ready for \"${nextTrack.title}\" in ${prepDuration}ms")
                        }

                        player.setOnError {
                            println("[WindowsAudioEngine] Standby pre-arm error: ${player.error?.message}")
                            isPreArming = false
                            isPreArmed = false
                        }

                        standbyPlayer = player
                    } catch (e: Exception) {
                        println("[WindowsAudioEngine] Failed to initialize standby player: ${e.message}")
                        isPreArming = false
                        isPreArmed = false
                    }
                }
            } catch (e: Exception) {
                println("[WindowsAudioEngine] Pre-arm failed: ${e.message}")
                isPreArming = false
                isPreArmed = false
            }
        }
    }

    private fun startCrossfade(nextTrack: TrackItem, nextIndex: Int, crossfadeDurationMs: Long) {
        if (isCrossfading) return
        isCrossfading = true
        crossfadeJob?.cancel()

        crossfadeJob = scope.launch(Dispatchers.Default) {
            println("[WindowsAudioEngine] Starting Equal-Power Crossfade to \"${nextTrack.title}\" (${crossfadeDurationMs}ms)")

            // 1. Ready-Gate: Wait up to 3500ms for standby player to be ready before fading down primary!
            var waitedMs = 0L
            while (waitedMs < 3500L && isActive && isCrossfading) {
                val isReady = withContext(Dispatchers.Main) {
                    standbyPlayer?.status == MediaPlayer.Status.READY ||
                    standbyPlayer?.status == MediaPlayer.Status.PAUSED ||
                    standbyPlayer?.status == MediaPlayer.Status.PLAYING
                }
                if (isReady) break
                delay(50L)
                waitedMs += 50L
            }

            val standby = standbyPlayer
            if (standby == null) {
                println("[WindowsAudioEngine] Standby player not available, aborting crossfade")
                isCrossfading = false
                return@launch
            }

            // 2. Start standby player concurrently at 0 volume
            Platform.runLater {
                standby.volume = 0.0
                standby.play()
            }

            val masterVolume = _state.value.volume
            // 20ms step granularity for seamless analog-smooth volume crossfade
            val steps = (crossfadeDurationMs / 20L).toInt().coerceIn(25, 750)
            val stepTime = crossfadeDurationMs / steps

            try {
                for (i in 0..steps) {
                    if (!isActive || !isCrossfading) break

                    val progress = (i / steps.toFloat()).coerceIn(0f, 1f)
                    val gains = EqualPowerCurve.calculateGains(progress)

                    Platform.runLater {
                        try {
                            primaryPlayer?.volume = (gains.outgoingGain * masterVolume).toDouble()
                            standbyPlayer?.volume = (gains.incomingGain * masterVolume).toDouble()
                        } catch (_: Exception) {}
                    }

                    delay(stepTime)
                }
            } finally {
                Platform.runLater {
                    try {
                        primaryPlayer?.volume = 0.0
                        standbyPlayer?.volume = masterVolume.toDouble()
                    } catch (_: Exception) {}
                }
            }

            // 3. Crossfade complete: Atomically swap players
            Platform.runLater {
                val oldPrimary = primaryPlayer
                val newPrimary = standbyPlayer

                primaryPlayer = newPrimary
                standbyPlayer = null

                try {
                    oldPrimary?.stop()
                    oldPrimary?.dispose()
                } catch (_: Exception) {}

                if (newPrimary != null) {
                    newPrimary.volume = masterVolume.toDouble()
                    attachPrimaryListeners(newPrimary, newPrimary.media, 0L)

                    val durMs = newPrimary.media.duration.toMillis().toLong()
                    _state.value = _state.value.copy(
                        videoId = nextTrack.videoId,
                        title = nextTrack.title,
                        artist = nextTrack.artist,
                        artworkUrl = nextTrack.artworkUrl,
                        positionMs = 0L,
                        durationMs = if (durMs > 0) durMs else _state.value.durationMs,
                        isPlaying = true,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                onTrackSwapped?.invoke(nextTrack, nextIndex)

                isCrossfading = false
                isPreArmed = false
                isPreArming = false
                preArmedTrackId = null
                println("[WindowsAudioEngine] Crossfade swapped successfully to \"${nextTrack.title}\"")
            }
        }
    }

    fun cancelCrossfade() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        preArmJob?.cancel()
        preArmJob = null

        isCrossfading = false
        isPreArming = false
        isPreArmed = false
        preArmedTrackId = null

        val masterVolume = _state.value.volume
        Platform.runLater {
            try {
                standbyPlayer?.stop()
                standbyPlayer?.dispose()
                standbyPlayer = null
            } catch (_: Exception) {}

            try {
                primaryPlayer?.volume = masterVolume.toDouble()
            } catch (_: Exception) {}
        }
    }

    override fun play() {
        Platform.runLater {
            primaryPlayer?.play()
            if (isCrossfading) {
                standbyPlayer?.play()
            }
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    override fun pause() {
        Platform.runLater {
            primaryPlayer?.pause()
            if (isCrossfading) {
                standbyPlayer?.pause()
            }
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    override fun seekTo(positionMs: Long) {
        cancelCrossfade()
        isSeeking = true
        targetSeekPosMs = positionMs
        lastSeekTimestampMs = System.currentTimeMillis()
        _state.value = _state.value.copy(positionMs = positionMs)
        Platform.runLater {
            try {
                primaryPlayer?.seek(Duration.millis(positionMs.toDouble()))
            } catch (e: Exception) {
                isSeeking = false
                println("[WindowsAudioEngine] seekTo error: ${e.message}")
            }
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        Platform.runLater {
            if (!isCrossfading) {
                primaryPlayer?.volume = clamped.toDouble()
            }
        }
    }

    override fun setMuted(muted: Boolean) {
        _state.value = _state.value.copy(isMuted = muted)
        Platform.runLater {
            primaryPlayer?.isMute = muted
            standbyPlayer?.isMute = muted
        }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        _state.value = _state.value.copy(repeatMode = mode)
    }

    override fun release() {
        cancelCrossfade()
        proxy.stop()
        Platform.runLater {
            try {
                primaryPlayer?.stop()
                primaryPlayer?.dispose()
                primaryPlayer = null
            } catch (_: Exception) {}

            try {
                standbyPlayer?.stop()
                standbyPlayer?.dispose()
                standbyPlayer = null
            } catch (_: Exception) {}
        }
    }
}
