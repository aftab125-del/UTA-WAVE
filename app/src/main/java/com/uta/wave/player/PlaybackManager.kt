package com.uta.wave.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
        private const val TAG = "PlaybackManager"
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

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(20000)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory))
        .build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }

    private val _state = MutableStateFlow(WavePlayerState())
    val state = _state.asStateFlow()

    private var progressJob: Job? = null
    private var resolveJob: Job? = null

    init {
        setupPlayerListener()
        val userPrefs = com.uta.wave.data.preferences.UserPreferences.getInstance(context)
        scope.launch {
            userPrefs.settingsFlow.collect { settings ->
                try {
                    player.skipSilenceEnabled = settings.skipSilence
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
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
                val errMsg = "Playback error (${error.errorCodeName}): ${error.message ?: error.localizedMessage}"
                AppLogger.e(errMsg, error)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                }
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        isResolving = false,
                        error = errMsg
                    )
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
                delay(100)
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
            AppLogger.i("▶ playTrack requested: title=\"${track.title}\", videoId=$videoId")

            val localDb = com.uta.wave.data.local.LocalDatabase.getInstance(context)
            val downloadedPath = localDb.getDownloadPathSync(track.id)
            val isLocalMedia = track.videoId.startsWith("content://") || track.videoId.startsWith("file://")

            val streamUrl = when {
                isLocalMedia -> track.videoId
                downloadedPath != null && java.io.File(downloadedPath).exists() -> {
                    AppLogger.i("▶ Playing offline downloaded file: $downloadedPath")
                    android.net.Uri.fromFile(java.io.File(downloadedPath)).toString()
                }
                else -> {
                    val cached = getCachedStreamUrl(videoId)
                    cached ?: innerTubeClient.resolveStreamUrl(videoId)?.also {
                        setCachedStreamUrl(videoId, it)
                    }
                }
            }

            if (streamUrl != null) {
                AppLogger.i("▶ Resolved stream URL (${streamUrl.take(65)}...), setting ExoPlayer...")
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
                AppLogger.i("▶ ExoPlayer.prepare() and play() called successfully")

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
                val errMsg = "Failed to resolve stream for ${track.title} ($videoId)"
                AppLogger.e(errMsg)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                }
                _state.update {
                    it.copy(
                        isResolving = false,
                        isBuffering = false,
                        error = errMsg
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
