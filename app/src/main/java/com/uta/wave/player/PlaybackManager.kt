package com.uta.wave.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.data.preferences.AppSettings
import com.uta.wave.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class PlaybackManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "PlaybackManager"
        private const val PERSISTENT_QUEUE_PREFS = "uta_wave_persistent_queue"
        private const val KEY_QUEUE_JSON = "persistent_queue_json"
        private const val KEY_QUEUE_INDEX = "persistent_queue_index"
        private const val KEY_QUEUE_POSITION = "persistent_queue_position"

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
    private val userPrefs = UserPreferences.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val streamCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val STREAM_CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(20000)

    private fun createPlayerInstance(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory))
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
    }

    // Dual-Player Instances for Seamless Crossfade
    private var primaryPlayerInstance: ExoPlayer = createPlayerInstance()
    private var standbyPlayerInstance: ExoPlayer = createPlayerInstance()

    val player: ExoPlayer get() = primaryPlayerInstance

    var onPlayerSwapped: ((ExoPlayer) -> Unit)? = null

    private val _state = MutableStateFlow(WavePlayerState())
    val state = _state.asStateFlow()

    var waveSyncManager: com.uta.wave.sync.WaveSyncManager? = null
    var isRemoteSync: Boolean = false

    private var progressJob: Job? = null
    private var resolveJob: Job? = null
    private var crossfadeJob: Job? = null
    private var isCrossfading = false
    private var currentSpeed: Float = 1.0f

    init {
        WaveEqualizer.initialize(context)
        setupPlayerListener(primaryPlayerInstance)
        setupPlayerListener(standbyPlayerInstance)
        setupSettingsCollector()
        setupVolumeObserver()
        restorePersistentQueueIfEnabled()
        WaveEqualizer.attachAudioSession(primaryPlayerInstance.audioSessionId)
    }

    private fun setupSettingsCollector() {
        scope.launch {
            userPrefs.settingsFlow.collect { settings ->
                try {
                    // 1. Skip Silence
                    primaryPlayerInstance.skipSilenceEnabled = settings.skipSilence
                    standbyPlayerInstance.skipSilenceEnabled = settings.skipSilence

                    // 2. Audio Normalization & Loudness
                    if (!isCrossfading) {
                        val normVolume = getTargetNormalizationVolume(settings)
                        primaryPlayerInstance.volume = normVolume
                    }

                    // 3. Varispeed
                    updatePlaybackSpeedAndVarispeed(currentSpeed, settings.varispeed)
                } catch (e: Exception) {
                    AppLogger.e("Error applying audio settings to player", e)
                }
            }
        }
    }

    private fun getTargetNormalizationVolume(settings: AppSettings = userPrefs.settingsFlow.value): Float {
        if (!settings.audioNormalization) return 1.0f
        return when (settings.audioLoudnessPreset.uppercase()) {
            "QUIET" -> 0.70f       // -18 LUFS (audiophile dynamic range)
            "LOUD" -> 1.00f        // -11 LUFS
            "AGGRESSIVE" -> 1.00f  // -8 LUFS peak
            else -> 0.85f          // -14 LUFS (Spotify/Apple Music streaming standard)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        val settings = userPrefs.settingsFlow.value
        updatePlaybackSpeedAndVarispeed(speed, settings.varispeed)
    }

    private fun updatePlaybackSpeedAndVarispeed(speed: Float, isVarispeed: Boolean) {
        val pitch = if (isVarispeed) speed else 1.0f
        val params = PlaybackParameters(speed, pitch)
        primaryPlayerInstance.playbackParameters = params
        standbyPlayerInstance.playbackParameters = params
    }

    private fun setupVolumeObserver() {
        try {
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val settings = userPrefs.settingsFlow.value
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: -1
                    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
                    if (currentVolume >= 0 && maxVolume > 0) {
                        waveSyncManager?.onLocalVolumeChanged(currentVolume.toFloat() / maxVolume.toFloat())
                    }
                    if (settings.pauseOnMute && currentVolume == 0 && player.isPlaying) {
                        player.pause()
                    }
                }
            }, filter)
        } catch (e: Exception) {
            // ignore receiver registration issues on some devices
        }
    }

    private fun setupPlayerListener(exoPlayer: ExoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
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
                                durationMs = exoPlayer.duration.coerceAtLeast(0L),
                                error = null
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (!isCrossfading) {
                            handleTrackEnded()
                        }
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
                if (!isRemoteSync) {
                    waveSyncManager?.onPlayerIsPlayingChanged(isPlaying, primaryPlayerInstance.currentPosition)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val errMsg = "Playback error (${error.errorCodeName}): ${error.message ?: error.localizedMessage}"
                AppLogger.e(errMsg, error)

                val settings = userPrefs.settingsFlow.value
                if (settings.autoSkipNextOnError && _state.value.queue.size > 1) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(context, "Stream error, auto-skipping to next song...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    scope.launch {
                        delay(600)
                        next()
                    }
                } else {
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
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (exoPlayer == primaryPlayerInstance && audioSessionId > 0) {
                    WaveEqualizer.attachAudioSession(audioSessionId)
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (primaryPlayerInstance.isPlaying) {
                    val currentPos = primaryPlayerInstance.currentPosition.coerceAtLeast(0L)
                    val duration = primaryPlayerInstance.duration.coerceAtLeast(0L)

                    _state.update {
                        it.copy(
                            positionMs = currentPos,
                            durationMs = duration
                        )
                    }

                    // Save queue position periodically for persistence
                    if (currentPos > 0 && currentPos % 5000 < 150) {
                        savePersistentQueuePosition(currentPos)
                    }

                    // Check for Standby Crossfade Trigger
                    checkAndTriggerCrossfade(currentPos, duration)
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun checkAndTriggerCrossfade(currentPos: Long, duration: Long) {
        val settings = userPrefs.settingsFlow.value
        if (!settings.crossfadeEnabled || isCrossfading || duration <= 10_000L) return

        val crossfadeDurationMs = (settings.crossfadeDuration * 1000f).toLong().coerceIn(1000L, 15_000L)
        val remainingMs = duration - currentPos

        if (remainingMs in 1..crossfadeDurationMs) {
            val q = _state.value.queue
            val qIdx = _state.value.queueIndex
            val nextIdx = getNextIndex(qIdx, q.size, _state.value.repeatMode, _state.value.isShuffle)
            if (nextIdx != null && nextIdx != qIdx) {
                val currentTrack = _state.value.currentTrack
                val nextTrack = q[nextIdx]

                // Gapless album check: don't crossfade if same album
                if (settings.crossfadeGapless && currentTrack?.album != null && currentTrack.album.isNotBlank() && currentTrack.album == nextTrack.album) {
                    AppLogger.i("▶ Gapless album detected (${currentTrack.album}), bypassing crossfade.")
                    return
                }

                startCrossfade(nextTrack, nextIdx, crossfadeDurationMs)
            }
        }
    }

    private fun startCrossfade(nextTrack: TrackItem, nextIndex: Int, crossfadeDurationMs: Long) {
        isCrossfading = true
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            AppLogger.i("▶ Starting Equal-Power Crossfade to \"${nextTrack.title}\" (${crossfadeDurationMs}ms)")
            val nextVideoId = nextTrack.videoId
            val streamUrl = getCachedStreamUrl(nextVideoId) ?: innerTubeClient.resolveStreamUrl(nextVideoId)?.also {
                setCachedStreamUrl(nextVideoId, it)
            }

            if (streamUrl == null) {
                AppLogger.w("Crossfade pre-resolve failed for ${nextTrack.title}")
                isCrossfading = false
                return@launch
            }

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(nextTrack.title)
                .setArtist(nextTrack.artist)
                .setArtworkUri(android.net.Uri.parse(nextTrack.artworkUrl))
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaId(nextTrack.id)
                .setMediaMetadata(mediaMetadata)
                .build()

            standbyPlayerInstance.volume = 0f
            standbyPlayerInstance.setMediaItem(mediaItem)
            standbyPlayerInstance.prepare()
            standbyPlayerInstance.play()

            val baseNormVolume = getTargetNormalizationVolume()
            val steps = (crossfadeDurationMs / 25L).toInt().coerceAtLeast(10)

            for (i in 0..steps) {
                val progress = i.toFloat() / steps.toFloat()
                val gains = EqualPowerCurve.calculateGains(progress)

                try {
                    primaryPlayerInstance.volume = gains.outgoingGain * baseNormVolume
                    standbyPlayerInstance.volume = gains.incomingGain * baseNormVolume
                } catch (e: Exception) {
                    // ignore volume update exceptions
                }
                delay(25L)
            }

            // Crossfade complete: Swap players
            try {
                primaryPlayerInstance.stop()
                primaryPlayerInstance.clearMediaItems()
            } catch (e: Exception) {}

            val oldPrimary = primaryPlayerInstance
            primaryPlayerInstance = standbyPlayerInstance
            standbyPlayerInstance = oldPrimary

            primaryPlayerInstance.volume = baseNormVolume
            standbyPlayerInstance.volume = 0f

            setupPlayerListener(primaryPlayerInstance)
            WaveEqualizer.attachAudioSession(primaryPlayerInstance.audioSessionId)
            onPlayerSwapped?.invoke(primaryPlayerInstance)

            _state.update {
                it.copy(
                    currentTrack = nextTrack,
                    queueIndex = nextIndex,
                    positionMs = primaryPlayerInstance.currentPosition.coerceAtLeast(0L),
                    durationMs = primaryPlayerInstance.duration.coerceAtLeast(0L)
                )
            }

            savePersistentQueue()
            isCrossfading = false
            AppLogger.i("▶ Equal-Power Crossfade swapped successfully")
        }
    }

    fun playTrack(track: TrackItem, queue: List<TrackItem> = listOf(track)) {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        val settings = userPrefs.settingsFlow.value

        // Cancel any active crossfade
        if (isCrossfading) {
            crossfadeJob?.cancel()
            isCrossfading = false
            standbyPlayerInstance.stop()
            standbyPlayerInstance.clearMediaItems()
            primaryPlayerInstance.volume = getTargetNormalizationVolume(settings)
        }

        // Prevent duplicates in queue if enabled
        val processedQueue = if (settings.preventDuplicateTracksInQueue && queue.size > 1) {
            val seen = mutableSetOf<String>()
            queue.filter {
                if (it.id == track.id) true
                else seen.add(it.id)
            }
        } else {
            queue
        }

        val targetIndex = processedQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _state.update {
            it.copy(
                currentTrack = track,
                queue = processedQueue,
                queueIndex = targetIndex,
                isResolving = true,
                isBuffering = true,
                error = null,
                positionMs = 0L
            )
        }

        savePersistentQueue()

        resolveJob?.cancel()
        resolveJob = scope.launch {
            val videoId = track.videoId
            AppLogger.i("▶ playTrack requested: title=\"${track.title}\", videoId=$videoId")

            val localDb = LocalDatabase.getInstance(context)
            val downloadedPath = localDb.getDownloadPathSync(track.id)
            val isLocalMedia = track.videoId.startsWith("content://") || track.videoId.startsWith("file://")

            val streamUrl = when {
                isLocalMedia -> track.videoId
                downloadedPath != null && File(downloadedPath).exists() -> {
                    AppLogger.i("▶ Playing offline downloaded file: $downloadedPath")
                    android.net.Uri.fromFile(File(downloadedPath)).toString()
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

                primaryPlayerInstance.volume = getTargetNormalizationVolume(settings)
                primaryPlayerInstance.setMediaItem(mediaItem)
                primaryPlayerInstance.prepare()
                primaryPlayerInstance.play()
                AppLogger.i("▶ ExoPlayer.prepare() and play() called successfully")

                if (!isRemoteSync) {
                    sync?.onLocalTrackChange(track, processedQueue)
                }

                // Pre-resolve next track if available
                if (targetIndex + 1 < processedQueue.size) {
                    launch(Dispatchers.IO) {
                        val nextTrack = processedQueue[targetIndex + 1]
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
                if (settings.autoSkipNextOnError && processedQueue.size > 1) {
                    next()
                } else {
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
    }

    fun togglePlayPause() {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        if (player.isPlaying) {
            player.pause()
            if (isCrossfading) {
                standbyPlayerInstance.pause()
            }
            if (!isRemoteSync) {
                sync?.onLocalPause(player.currentPosition)
            }
        } else {
            if (player.playbackState == Player.STATE_IDLE && _state.value.currentTrack != null) {
                _state.value.currentTrack?.let { playTrack(it, _state.value.queue) }
            } else {
                player.play()
                if (isCrossfading) {
                    standbyPlayerInstance.play()
                }
                if (!isRemoteSync) {
                    sync?.onLocalPlay(player.currentPosition)
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        player.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = positionMs) }
        savePersistentQueuePosition(positionMs)
        if (!isRemoteSync) {
            sync?.onLocalSeek(positionMs)
        }
    }

    private fun getNextIndex(currentIndex: Int, queueSize: Int, repeatMode: RepeatMode, isShuffle: Boolean): Int? {
        if (queueSize == 0) return null
        return when (repeatMode) {
            RepeatMode.ONE -> currentIndex
            else -> {
                if (isShuffle) {
                    (0 until queueSize).random()
                } else {
                    if (currentIndex + 1 < queueSize) currentIndex + 1 else 0
                }
            }
        }
    }

    fun next() {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        val currentState = _state.value
        val queue = currentState.queue
        if (queue.isEmpty()) return

        val nextIndex = getNextIndex(currentState.queueIndex, queue.size, currentState.repeatMode, currentState.isShuffle) ?: 0

        // Auto load more similar content if reaching the end of queue
        val settings = userPrefs.settingsFlow.value
        if (settings.autoLoadMore && currentState.queueIndex >= queue.size - 2) {
            loadSimilarTracksAndAppend(currentState.currentTrack)
        }

        playTrack(queue[nextIndex], queue)
        if (!isRemoteSync) {
            sync?.onLocalSkipNext()
        }
    }

    fun previous() {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        val currentState = _state.value
        val queue = currentState.queue
        if (queue.isEmpty()) return

        if (player.currentPosition > 3000) {
            player.seekTo(0)
            if (!isRemoteSync) {
                sync?.onLocalSeek(0L)
            }
            return
        }

        val prevIndex = if (currentState.queueIndex - 1 >= 0) currentState.queueIndex - 1 else queue.size - 1
        playTrack(queue[prevIndex], queue)
        if (!isRemoteSync) {
            sync?.onLocalSkipPrev()
        }
    }

    // -------------------------------------------------------------------------
    // Remote Sync & Collaborative Playback Methods
    // -------------------------------------------------------------------------

    fun applyRemotePlay(positionMs: Long) {
        isRemoteSync = true
        primaryPlayerInstance.seekTo(positionMs.coerceAtLeast(0L))
        primaryPlayerInstance.play()
        scope.launch {
            delay(300)
            isRemoteSync = false
        }
    }

    fun applyRemotePause(positionMs: Long) {
        isRemoteSync = true
        primaryPlayerInstance.pause()
        primaryPlayerInstance.seekTo(positionMs.coerceAtLeast(0L))
        scope.launch {
            delay(300)
            isRemoteSync = false
        }
    }

    fun applyRemoteSeek(positionMs: Long) {
        isRemoteSync = true
        primaryPlayerInstance.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = positionMs) }
        scope.launch {
            delay(300)
            isRemoteSync = false
        }
    }

    fun applyRemoteTrackChange(
        track: TrackItem,
        queue: List<TrackItem>,
        positionMs: Long,
        shouldPlay: Boolean,
        onPrepared: (() -> Unit)? = null
    ) {
        isRemoteSync = true
        val targetIndex = queue.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
        _state.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                queueIndex = targetIndex,
                isResolving = true,
                isBuffering = true,
                error = null,
                positionMs = positionMs
            )
        }

        resolveJob?.cancel()
        resolveJob = scope.launch {
            val videoId = track.videoId
            val streamUrl = getCachedStreamUrl(videoId) ?: innerTubeClient.resolveStreamUrl(videoId)?.also {
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

                primaryPlayerInstance.setMediaItem(mediaItem)
                primaryPlayerInstance.prepare()
                primaryPlayerInstance.seekTo(positionMs.coerceAtLeast(0L))
                if (shouldPlay) {
                    primaryPlayerInstance.play()
                } else {
                    primaryPlayerInstance.pause()
                }
                onPrepared?.invoke()
            }
            delay(300)
            isRemoteSync = false
        }
    }

    fun applyRemoteQueueSync(queue: List<TrackItem>) {
        isRemoteSync = true
        _state.update { it.copy(queue = queue) }
        savePersistentQueue()
        scope.launch {
            delay(300)
            isRemoteSync = false
        }
    }

    fun applyRemoteQueueAdd(track: TrackItem, insertNext: Boolean) {
        isRemoteSync = true
        val cur = _state.value.queue.toMutableList()
        val curIdx = _state.value.queueIndex
        if (insertNext && curIdx in 0 until cur.size) {
            cur.add(curIdx + 1, track)
        } else {
            cur.add(track)
        }
        _state.update { it.copy(queue = cur) }
        savePersistentQueue()
        scope.launch {
            delay(300)
            isRemoteSync = false
        }
    }

    fun applyRemoteQueueRemove(trackId: String) {
        isRemoteSync = true
        val cleanId = trackId.removePrefix("youtube:")
        val cur = _state.value.queue.filter { it.videoId != cleanId && it.id != trackId }
        val newIdx = _state.value.queueIndex.coerceAtMost(kotlin.math.max(0, cur.size - 1))
        _state.update { it.copy(queue = cur, queueIndex = newIdx) }
        savePersistentQueue()
        scope.launch {
            delay(300)
            isRemoteSync = false
        }
    }

    fun applyRemoteQueueClear() {
        isRemoteSync = true
        val cur = _state.value.currentTrack?.let { listOf(it) } ?: emptyList()
        _state.update { it.copy(queue = cur, queueIndex = 0) }
        savePersistentQueue()
        scope.launch {
            delay(300)
            isRemoteSync = false
        }
    }

    fun applyRemoteVolume(volume: Float) {
        primaryPlayerInstance.volume = volume.coerceIn(0f, 1f)
    }

    fun addToQueue(track: TrackItem, insertNext: Boolean = false) {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        val cur = _state.value.queue.toMutableList()
        val curIdx = _state.value.queueIndex
        if (insertNext && curIdx in 0 until cur.size) {
            cur.add(curIdx + 1, track)
        } else {
            cur.add(track)
        }
        _state.update { it.copy(queue = cur) }
        savePersistentQueue()
        if (!isRemoteSync) {
            sync?.onLocalQueueAdd(track, insertNext)
        }
    }

    fun removeFromQueue(trackId: String) {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        val cleanId = trackId.removePrefix("youtube:")
        val cur = _state.value.queue.filter { it.videoId != cleanId && it.id != trackId }
        val newIdx = _state.value.queueIndex.coerceAtMost(kotlin.math.max(0, cur.size - 1))
        _state.update { it.copy(queue = cur, queueIndex = newIdx) }
        savePersistentQueue()
        if (!isRemoteSync) {
            sync?.onLocalQueueRemove(trackId)
        }
    }

    fun clearQueue() {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        val cur = _state.value.currentTrack?.let { listOf(it) } ?: emptyList()
        _state.update { it.copy(queue = cur, queueIndex = 0) }
        savePersistentQueue()
        if (!isRemoteSync) {
            sync?.onLocalQueueClear()
        }
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

    private fun loadSimilarTracksAndAppend(currentTrack: TrackItem?) {
        if (currentTrack == null) return
        scope.launch(Dispatchers.IO) {
            try {
                val trending = innerTubeClient.getTrending()
                if (trending.isNotEmpty()) {
                    val currentQueue = _state.value.queue
                    val existingIds = currentQueue.map { it.id }.toSet()
                    val newTracks = trending.filter { it.id !in existingIds }
                    if (newTracks.isNotEmpty()) {
                        val merged = currentQueue + newTracks
                        _state.update { it.copy(queue = merged) }
                        savePersistentQueue()
                        AppLogger.i("▶ Auto-loaded ${newTracks.size} more tracks into queue")
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("Auto-load more tracks failed: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Persistent Queue Save & Restore
    // -------------------------------------------------------------------------
    private fun savePersistentQueue() {
        val settings = userPrefs.settingsFlow.value
        if (!settings.persistentQueue) return

        scope.launch(Dispatchers.IO) {
            try {
                val q = _state.value.queue
                val qIdx = _state.value.queueIndex
                val pos = _state.value.positionMs

                val array = JSONArray()
                for (item in q) {
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("videoId", item.videoId)
                        put("title", item.title)
                        put("artist", item.artist)
                        put("album", item.album ?: "")
                        put("artworkUrl", item.artworkUrl)
                        put("durationSeconds", item.durationSeconds)
                        put("isSynced", item.isSynced)
                        put("artistId", item.artistId ?: "")
                        put("albumId", item.albumId ?: "")
                    }
                    array.put(obj)
                }

                val sp = context.getSharedPreferences(PERSISTENT_QUEUE_PREFS, Context.MODE_PRIVATE)
                sp.edit()
                    .putString(KEY_QUEUE_JSON, array.toString())
                    .putInt(KEY_QUEUE_INDEX, qIdx)
                    .putLong(KEY_QUEUE_POSITION, pos)
                    .apply()
            } catch (e: Exception) {
                AppLogger.e("Failed to save persistent queue", e)
            }
        }
    }

    private fun savePersistentQueuePosition(positionMs: Long) {
        val settings = userPrefs.settingsFlow.value
        if (!settings.persistentQueue) return
        val sp = context.getSharedPreferences(PERSISTENT_QUEUE_PREFS, Context.MODE_PRIVATE)
        sp.edit().putLong(KEY_QUEUE_POSITION, positionMs).apply()
    }

    private fun restorePersistentQueueIfEnabled() {
        val settings = userPrefs.settingsFlow.value
        if (!settings.persistentQueue) return

        scope.launch(Dispatchers.IO) {
            try {
                val sp = context.getSharedPreferences(PERSISTENT_QUEUE_PREFS, Context.MODE_PRIVATE)
                val rawJson = sp.getString(KEY_QUEUE_JSON, null) ?: return@launch
                val savedIndex = sp.getInt(KEY_QUEUE_INDEX, 0)
                val savedPos = sp.getLong(KEY_QUEUE_POSITION, 0L)

                val array = JSONArray(rawJson)
                val restoredList = mutableListOf<TrackItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    restoredList.add(
                        TrackItem(
                            id = obj.optString("id"),
                            videoId = obj.optString("videoId"),
                            title = obj.optString("title"),
                            artist = obj.optString("artist"),
                            album = obj.optString("album").takeIf { it.isNotBlank() },
                            artworkUrl = obj.optString("artworkUrl"),
                            durationSeconds = obj.optInt("durationSeconds", 0),
                            isSynced = obj.optBoolean("isSynced", false),
                            artistId = obj.optString("artistId").takeIf { it.isNotBlank() },
                            albumId = obj.optString("albumId").takeIf { it.isNotBlank() }
                        )
                    )
                }

                if (restoredList.isNotEmpty()) {
                    val safeIndex = savedIndex.coerceIn(0, restoredList.size - 1)
                    val activeTrack = restoredList[safeIndex]
                    _state.update {
                        it.copy(
                            queue = restoredList,
                            queueIndex = safeIndex,
                            currentTrack = activeTrack,
                            positionMs = savedPos,
                            durationMs = (activeTrack.durationSeconds * 1000L).coerceAtLeast(0L),
                            isPlaying = false,
                            isBuffering = false
                        )
                    }
                    AppLogger.i("▶ Restored persistent queue: ${restoredList.size} tracks, active=\"${activeTrack.title}\" at ${savedPos}ms")
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to restore persistent queue", e)
            }
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
        crossfadeJob?.cancel()
        WaveEqualizer.release()
        primaryPlayerInstance.release()
        standbyPlayerInstance.release()
    }
}
