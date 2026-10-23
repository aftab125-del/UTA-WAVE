package com.uta.wave.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class PlaybackManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaybackManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val innerTubeClient = InnerTubeClient()
    private val localDatabase = LocalDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val streamCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val STREAM_CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = true
    }

    private val _state = MutableStateFlow(WavePlayerState())
    val state = _state.asStateFlow()

    private var progressJob: Job? = null
    private var resolveJob: Job? = null

    init {
        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _state.update { it.copy(isBuffering = true) }
                    }
                    Player.STATE_READY -> {
                        _state.update {
                            it.copy(
                                isBuffering = false,
                                isResolving = false,
                                durationMs = player.duration.coerceAtLeast(0L),
                                error = null
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        handleTrackEnded()
                    }
                    Player.STATE_IDLE -> {
                        _state.update { it.copy(isBuffering = false) }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                    _state.value.currentTrack?.let { track ->
                        scope.launch(Dispatchers.IO) {
                            localDatabase.recordHistory(track)
                        }
                    }
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        isResolving = false,
                        error = error.localizedMessage ?: "Playback error encountered"
                    )
                }
                // Try advancing to next song if queue available
                val currentState = _state.value
                if (currentState.queue.isNotEmpty() && currentState.queueIndex + 1 < currentState.queue.size) {
                    next()
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _state.update {
                        it.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playTrack(track: TrackItem, queue: List<TrackItem> = listOf(track)) {
        val targetIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _state.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                queueIndex = targetIndex,
                isResolving = true,
                isBuffering = true,
                error = null,
                positionMs = 0L
            )
        }

        resolveJob?.cancel()
        resolveJob = scope.launch {
            val videoId = track.videoId
            val cached = getCachedStreamUrl(videoId)

            val streamUrl = cached ?: innerTubeClient.resolveStreamUrl(videoId)?.also {
                setCachedStreamUrl(videoId, it)
            }

            if (streamUrl != null) {
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(android.net.Uri.parse(track.artworkUrl))
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(track.id)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                // Pre-resolve next track if available
                if (targetIndex + 1 < queue.size) {
                    launch(Dispatchers.IO) {
                        val nextTrack = queue[targetIndex + 1]
                        if (getCachedStreamUrl(nextTrack.videoId) == null) {
                            innerTubeClient.resolveStreamUrl(nextTrack.videoId)?.let { url ->
                                setCachedStreamUrl(nextTrack.videoId, url)
                            }
                        }
                    }
                }
            } else {
                _state.update {
                    it.copy(
                        isResolving = false,
                        isBuffering = false,
                        error = "Unable to resolve stream for ${track.title}"
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && _state.value.currentTrack != null) {
                _state.value.currentTrack?.let { playTrack(it, _state.value.queue) }
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun next() {
        val currentState = _state.value
        val queue = currentState.queue
        if (queue.isEmpty()) return

        val nextIndex = when (currentState.repeatMode) {
            RepeatMode.ONE -> currentState.queueIndex
            else -> {
                if (currentState.isShuffle) {
                    (queue.indices).random()
                } else {
                    if (currentState.queueIndex + 1 < queue.size) currentState.queueIndex + 1 else 0
                }
            }
        }

        playTrack(queue[nextIndex], queue)
    }

    fun previous() {
        val currentState = _state.value
        val queue = currentState.queue
        if (queue.isEmpty()) return

        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }

        val prevIndex = if (currentState.queueIndex - 1 >= 0) currentState.queueIndex - 1 else queue.size - 1
        playTrack(queue[prevIndex], queue)
    }

    fun toggleShuffle() {
        _state.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeatMode = nextMode) }
    }

    private fun handleTrackEnded() {
        when (_state.value.repeatMode) {
            RepeatMode.ONE -> {
                player.seekTo(0)
                player.play()
            }
            else -> next()
        }
    }

    private fun getCachedStreamUrl(videoId: String): String? {
        val entry = streamCache[videoId] ?: return null
        if (System.currentTimeMillis() > entry.second) {
            streamCache.remove(videoId)
            return null
        }
        return entry.first
    }

    private fun setCachedStreamUrl(videoId: String, url: String) {
        if (streamCache.size > 200) {
            streamCache.clear()
        }
        streamCache[videoId] = Pair(url, System.currentTimeMillis() + STREAM_CACHE_TTL_MS)
    }

    fun release() {
        stopProgressTracker()
        resolveJob?.cancel()
        player.release()
    }
}
