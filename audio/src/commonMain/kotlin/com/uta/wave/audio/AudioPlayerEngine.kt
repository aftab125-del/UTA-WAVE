package com.uta.wave.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * Repeat mode definitions across platforms.
 */
enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

/**
 * Unified playback state observed by Compose Multiplatform UI on both Mobile & Windows.
 */
data class UnifiedPlaybackState(
    val videoId: String? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffled: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Common Audio Player Engine Interface.
 *
 * Implementations:
 * - Android: AndroidX Media3 ExoPlayer with WaveMediaService and Foreground Wake-Lock.
 * - Windows Desktop: Miniaudio WASAPI Low-Latency Engine with System Media Transport Controls.
 */
interface AudioPlayerEngine {
    val state: StateFlow<UnifiedPlaybackState>

    /**
     * Loads a track stream URL and prepares the audio pipeline.
     */
    fun load(
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String?,
        streamUrl: String,
        startPositionMs: Long = 0L
    )

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setMuted(muted: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun release()
}
