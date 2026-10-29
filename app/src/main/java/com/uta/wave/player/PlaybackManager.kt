package com.uta.wave.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.data.preferences.AppSettings
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.player.queue.ListQueue
import com.uta.wave.player.queue.OccurrenceAwareFilter
import com.uta.wave.player.queue.WaveQueue
import com.uta.wave.player.queue.YouTubeRadioQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        private const val KEY_QUEUE_TITLE = "persistent_queue_title"
        private const val KEY_QUEUE_IS_RADIO = "persistent_queue_is_radio"

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

    var currentQueueProvider: WaveQueue? = null

    private val streamCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val STREAM_CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(20000)

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    private fun createPlayerInstance(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false // handleAudioFocus = false (Critical: dual ExoPlayer instances must never compete for audio focus)
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
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
    private val preArmManager = com.uta.wave.player.crossfade.AdaptivePreArmManager()
    private var preArmedTrackId: String? = null
    private var isPreArming = false
    private var isPreArmed = false
    private var preArmJob: Job? = null

    // App-level Audio Focus (EchoMusic standard: managed once for the entire app)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                if (_state.value.isPlaying) {
                    player.pause()
                    if (isCrossfading) {
                        standbyPlayerInstance.pause()
                    }
                    _state.update { it.copy(isPlaying = false) }
                    stopProgressTracker()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
            }
        }
    }

    private fun requestAppAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonAppAudioFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }
    private var currentSpeed: Float = 1.0f
    private var lastListeningSamplePosMs: Long = 0L
    private var lastListeningTrackId: String? = null

    fun ensureServiceStarted() {
        try {
            val intent = Intent(context, WaveMediaService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            AppLogger.e("Failed to ensure WaveMediaService is started", e)
        }
    }

    init {
        ensureServiceStarted()
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
                if (exoPlayer != primaryPlayerInstance) return
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
                if (exoPlayer != primaryPlayerInstance) return
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                    _state.value.currentTrack?.let { track ->
                        if (lastListeningTrackId != track.id) {
                            lastListeningTrackId = track.id
                            lastListeningSamplePosMs = primaryPlayerInstance.currentPosition.coerceAtLeast(0L)
                            scope.launch(Dispatchers.IO) {
                                localDatabase.recordHistory(track)
                            }
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

                // Fallback: If playing a local/downloaded file failed, fall back to streaming online
                val currentTrack = _state.value.currentTrack
                val currentMediaUri = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
                if (currentTrack != null && currentMediaUri?.startsWith("file:") == true &&
                    !currentTrack.videoId.startsWith("content://") && !currentTrack.videoId.startsWith("file:")
                ) {
                    AppLogger.w("Offline file playback failed, falling back to online streaming for \"${currentTrack.title}\"")
                    scope.launch {
                        val onlineUrl = innerTubeClient.resolveStreamUrl(currentTrack.videoId)
                        if (onlineUrl != null) {
                            setCachedStreamUrl(currentTrack.videoId, onlineUrl)
                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(currentTrack.title)
                                .setArtist(currentTrack.artist)
                                .setAlbumTitle(currentTrack.album ?: "")
                                .setArtworkUri(android.net.Uri.parse(currentTrack.artworkUrl))
                                .setIsPlayable(true)
                                .build()
                            val mediaItem = MediaItem.Builder()
                                .setUri(onlineUrl)
                                .setMediaId(currentTrack.id)
                                .setMediaMetadata(mediaMetadata)
                                .build()
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                            exoPlayer.play()
                            return@launch
                        }
                    }
                }

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

                    // Real-time accurate listening duration accumulation
                    val currentTrack = _state.value.currentTrack
                    if (currentTrack != null) {
                        if (lastListeningTrackId != currentTrack.id) {
                            lastListeningTrackId = currentTrack.id
                            lastListeningSamplePosMs = currentPos
                        } else {
                            val deltaMs = currentPos - lastListeningSamplePosMs
                            if (deltaMs >= 15_000L && deltaMs < 60_000L) {
                                lastListeningSamplePosMs = currentPos
                                val deltaSec = (deltaMs / 1000L).toInt()
                                scope.launch(Dispatchers.IO) {
                                    localDatabase.recordListeningTime(currentTrack, deltaSec)
                                }
                            } else if (deltaMs < 0 || deltaMs >= 60_000L) {
                                // Seek occurred
                                lastListeningSamplePosMs = currentPos
                            }
                        }
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

    fun cancelCrossfade() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        preArmJob?.cancel()
        preArmJob = null
        isCrossfading = false
        isPreArming = false
        isPreArmed = false
        preArmedTrackId = null
        try {
            standbyPlayerInstance.stop()
            standbyPlayerInstance.clearMediaItems()
            standbyPlayerInstance.volume = 0f
        } catch (_: Exception) {}
        primaryPlayerInstance.volume = getTargetNormalizationVolume()
    }

    private fun checkAndTriggerCrossfade(currentPos: Long, duration: Long) {
        val settings = userPrefs.settingsFlow.value
        if (!settings.crossfadeEnabled || isCrossfading || duration <= 10_000L) return

        val crossfadeDurationMs = (settings.crossfadeDuration * 1000f).toLong().coerceIn(1000L, 15_000L)
        val remainingMs = duration - currentPos

        val q = _state.value.queue
        val qIdx = _state.value.queueIndex
        val nextIdx = getNextIndex(qIdx, q.size, _state.value.repeatMode, _state.value.isShuffle) ?: return
        if (nextIdx == qIdx && _state.value.repeatMode != RepeatMode.ONE) return
        val currentTrack = _state.value.currentTrack
        val nextTrack = q.getOrNull(nextIdx) ?: return

        // Gapless album check: don't crossfade if same album (EchoMusic standard)
        if (settings.crossfadeGapless && currentTrack?.album != null && currentTrack.album.isNotBlank() && currentTrack.album.equals(nextTrack.album, ignoreCase = true)) {
            return
        }

        // 1. Adaptive Pre-Arm Phase: Pre-buffer next track on standbyPlayerInstance ahead of time
        val leadTimeMs = preArmManager.currentLeadTimeMs
        if (remainingMs in (crossfadeDurationMs + 100L)..leadTimeMs) {
            if (!isPreArming && (!isPreArmed || preArmedTrackId != nextTrack.id)) {
                preArmStandbyPlayer(nextTrack, nextIdx)
            }
        }

        // 2. Trigger Crossfade Phase: Start equal-power ramp when remaining playback reaches duration
        if (remainingMs in 1..crossfadeDurationMs) {
            startCrossfade(nextTrack, nextIdx, crossfadeDurationMs)
        }
    }

    private fun preArmStandbyPlayer(nextTrack: TrackItem, nextIndex: Int) {
        if (isPreArming || isCrossfading) return
        isPreArming = true
        preArmJob?.cancel()
        preArmJob = scope.launch(Dispatchers.IO) {
            try {
                AppLogger.i("▶ Pre-arming standby player for \"${nextTrack.title}\" (lead time: ${preArmManager.currentLeadTimeMs}ms)")
                val startPrepMs = System.currentTimeMillis()
                val nextVideoId = nextTrack.videoId
                val localDb = LocalDatabase.getInstance(context)
                val downloadedPath = localDb.getDownloadPathSync(nextTrack.id) ?: localDb.getDownloadPathSync(nextTrack.videoId)
                val isLocalMedia = nextTrack.videoId.startsWith("content://") || nextTrack.videoId.startsWith("file://")
                val isDownloadedValid = downloadedPath != null && File(downloadedPath).let { it.exists() && it.length() > 0L }

                val streamUrl = when {
                    isLocalMedia -> nextTrack.videoId
                    isDownloadedValid -> {
                        AppLogger.i("Pre-arm: Using downloaded file: $downloadedPath")
                        android.net.Uri.fromFile(File(downloadedPath)).toString()
                    }
                    else -> getCachedStreamUrl(nextVideoId) ?: innerTubeClient.resolveStreamUrl(nextVideoId)?.also {
                        setCachedStreamUrl(nextVideoId, it)
                    }
                }

                if (streamUrl == null) {
                    AppLogger.w("Pre-arm: Failed to resolve streamUrl for ${nextTrack.title}")
                    isPreArming = false
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(nextTrack.title)
                        .setArtist(nextTrack.artist)
                        .setAlbumTitle(nextTrack.album ?: "")
                        .setArtworkUri(android.net.Uri.parse(nextTrack.artworkUrl))
                        .setIsPlayable(true)
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setUri(streamUrl)
                        .setMediaId(nextTrack.id)
                        .setMediaMetadata(mediaMetadata)
                        .build()

                    standbyPlayerInstance.volume = 0f
                    standbyPlayerInstance.playWhenReady = false
                    standbyPlayerInstance.setMediaItem(mediaItem)

                    var readyListener: Player.Listener? = null
                    readyListener = object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                val prepDuration = System.currentTimeMillis() - startPrepMs
                                if (prepDuration > 0) {
                                    preArmManager.recordPreparationDuration(prepDuration)
                                }
                                isPreArmed = true
                                isPreArming = false
                                preArmedTrackId = nextTrack.id
                                AppLogger.i("▶ Standby player pre-armed and ready for \"${nextTrack.title}\" in ${prepDuration}ms")
                                readyListener?.let { standbyPlayerInstance.removeListener(it) }
                            } else if (state == Player.STATE_IDLE) {
                                isPreArming = false
                                readyListener?.let { standbyPlayerInstance.removeListener(it) }
                            }
                        }
                    }
                    standbyPlayerInstance.addListener(readyListener)
                    standbyPlayerInstance.prepare()
                }
            } catch (e: Exception) {
                AppLogger.e("Pre-arm failed for ${nextTrack.title}", e)
                isPreArming = false
            }
        }
    }

    private fun startCrossfade(nextTrack: TrackItem, nextIndex: Int, crossfadeDurationMs: Long) {
        if (isCrossfading) return
        isCrossfading = true
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch(Dispatchers.Main) {
            AppLogger.i("▶ Starting Equal-Power Crossfade to \"${nextTrack.title}\" (${crossfadeDurationMs}ms)")

            // 1. If not yet pre-armed (e.g. user scrubbed near track end), resolve stream immediately
            if (!isPreArmed || preArmedTrackId != nextTrack.id) {
                AppLogger.i("Crossfade: Standby not pre-armed, resolving stream immediately...")
                val nextVideoId = nextTrack.videoId
                val localDb = LocalDatabase.getInstance(context)
                val downloadedPath = localDb.getDownloadPathSync(nextTrack.id) ?: localDb.getDownloadPathSync(nextTrack.videoId)
                val isLocalMedia = nextTrack.videoId.startsWith("content://") || nextTrack.videoId.startsWith("file://")
                val isDownloadedValid = downloadedPath != null && File(downloadedPath).let { it.exists() && it.length() > 0L }

                val streamUrl = withContext(Dispatchers.IO) {
                    when {
                        isLocalMedia -> nextTrack.videoId
                        isDownloadedValid -> android.net.Uri.fromFile(File(downloadedPath)).toString()
                        else -> getCachedStreamUrl(nextVideoId) ?: innerTubeClient.resolveStreamUrl(nextVideoId)?.also {
                            setCachedStreamUrl(nextVideoId, it)
                        }
                    }
                }

                if (streamUrl == null) {
                    AppLogger.w("Crossfade pre-resolve failed for ${nextTrack.title}, letting primary track finish")
                    isCrossfading = false
                    return@launch
                }

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(nextTrack.title)
                    .setArtist(nextTrack.artist)
                    .setAlbumTitle(nextTrack.album ?: "")
                    .setArtworkUri(android.net.Uri.parse(nextTrack.artworkUrl))
                    .setIsPlayable(true)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(nextTrack.id)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                standbyPlayerInstance.volume = 0f
                standbyPlayerInstance.playWhenReady = false
                standbyPlayerInstance.setMediaItem(mediaItem)
                standbyPlayerInstance.prepare()
            }

            // 2. Ready Gate: Wait for standby player to reach STATE_READY before fading down primary!
            if (standbyPlayerInstance.playbackState != Player.STATE_READY) {
                AppLogger.i("Crossfade: Waiting for standby player STATE_READY before beginning fade...")
                var waitedMs = 0L
                while (standbyPlayerInstance.playbackState != Player.STATE_READY &&
                       waitedMs < 3500L &&
                       primaryPlayerInstance.playbackState != Player.STATE_ENDED &&
                       isActive && isCrossfading) {
                    delay(50L)
                    waitedMs += 50L
                }
            }

            // If standby player failed to buffer in time, don't cut primary off! Let it finish naturally.
            if (standbyPlayerInstance.playbackState != Player.STATE_READY &&
                standbyPlayerInstance.playbackState != Player.STATE_BUFFERING) {
                AppLogger.w("Crossfade: Standby failed to ready in time (${standbyPlayerInstance.playbackState}), aborting crossfade")
                isCrossfading = false
                return@launch
            }

            // 3. Start playing the incoming track concurrently
            standbyPlayerInstance.volume = 0f
            standbyPlayerInstance.playWhenReady = true
            standbyPlayerInstance.play()

            val baseNormVolume = getTargetNormalizationVolume()
            // 15ms step granularity (1:1 with EchoMusic) to eliminate audible clicks
            val steps = (crossfadeDurationMs / 15L).toInt().coerceIn(40, 800)
            val stepTime = crossfadeDurationMs / steps

            try {
                for (i in 0..steps) {
                    if (!isActive || !isCrossfading) break

                    // If outgoing player ended or reached idle, snap incoming player to full volume and finish
                    if (primaryPlayerInstance.playbackState == Player.STATE_ENDED ||
                        primaryPlayerInstance.playbackState == Player.STATE_IDLE) {
                        standbyPlayerInstance.volume = baseNormVolume
                        break
                    }

                    val progress = (i / steps.toFloat()).coerceIn(0f, 1f)
                    val gains = EqualPowerCurve.calculateGains(progress)

                    try {
                        primaryPlayerInstance.volume = gains.outgoingGain * baseNormVolume
                        standbyPlayerInstance.volume = gains.incomingGain * baseNormVolume
                    } catch (_: Exception) {
                        break
                    }

                    delay(stepTime)
                }
            } finally {
                try {
                    primaryPlayerInstance.volume = 0f
                    standbyPlayerInstance.volume = baseNormVolume
                } catch (_: Exception) {}
            }

            // 4. Crossfade complete: Atomically swap players
            val oldPrimary = primaryPlayerInstance
            primaryPlayerInstance = standbyPlayerInstance
            standbyPlayerInstance = oldPrimary

            try {
                standbyPlayerInstance.stop()
                standbyPlayerInstance.clearMediaItems()
                standbyPlayerInstance.volume = 0f
            } catch (_: Exception) {}

            primaryPlayerInstance.volume = baseNormVolume

            // Re-attach hardware audio session & notify MediaLibraryService
            WaveEqualizer.attachAudioSession(primaryPlayerInstance.audioSessionId)
            onPlayerSwapped?.invoke(primaryPlayerInstance)

            _state.update {
                it.copy(
                    currentTrack = nextTrack,
                    queueIndex = nextIndex,
                    positionMs = primaryPlayerInstance.currentPosition.coerceAtLeast(0L),
                    durationMs = primaryPlayerInstance.duration.coerceAtLeast(0L),
                    isPlaying = primaryPlayerInstance.isPlaying,
                    isBuffering = false,
                    isResolving = false
                )
            }

            if (primaryPlayerInstance.isPlaying) {
                startProgressTracker()
            }

            savePersistentQueue()
            isCrossfading = false
            isPreArmed = false
            isPreArming = false
            preArmedTrackId = null
            AppLogger.i("▶ Equal-Power Crossfade swapped successfully to \"${nextTrack.title}\"")
        }
    }

    fun playRadio(track: TrackItem) {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        val radioQueue = YouTubeRadioQueue(videoId = track.videoId, initialTrack = track, innerTubeClient = innerTubeClient)
        currentQueueProvider = radioQueue

        val isAlreadyPlayingThisTrack = _state.value.currentTrack?.id == track.id &&
                primaryPlayerInstance.playbackState != Player.STATE_IDLE &&
                primaryPlayerInstance.playbackState != Player.STATE_ENDED

        _state.update {
            it.copy(
                queueTitle = "${track.title} Radio",
                isRadioActive = true,
                activePlaylistId = null,
                activePlaylistTitle = null
            )
        }

        if (!isAlreadyPlayingThisTrack) {
            playTrack(track, listOf(track))
        }

        scope.launch(Dispatchers.IO) {
            try {
                val radioTracks = radioQueue.getInitialTracks()
                if (radioTracks.isNotEmpty()) {
                    val targetIndex = radioTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                    _state.update { state ->
                        state.copy(
                            queue = radioTracks,
                            queueIndex = targetIndex,
                            queueTitle = "${track.title} Radio",
                            isRadioActive = true
                        )
                    }
                    savePersistentQueue()
                    AppLogger.i("▶ Algorithmic radio ready with ${radioTracks.size} tracks for \"${track.title}\"")
                }
            } catch (e: Exception) {
                AppLogger.w("Failed to populate initial radio tracks: ${e.message}")
            }
        }
    }

    fun startRadioFromCurrentTrack() {
        val current = _state.value.currentTrack ?: return
        playRadio(current)
    }

    fun playQueue(
        queueProvider: WaveQueue,
        startTrack: TrackItem? = null,
        playlistId: String? = null,
        playlistTitle: String? = null
    ) {
        currentQueueProvider = queueProvider
        scope.launch(Dispatchers.IO) {
            try {
                val tracks = queueProvider.getInitialTracks()
                if (tracks.isNotEmpty()) {
                    val target = startTrack ?: tracks.first()
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                queueTitle = playlistTitle ?: queueProvider.title,
                                isRadioActive = queueProvider is YouTubeRadioQueue,
                                activePlaylistId = playlistId,
                                activePlaylistTitle = playlistTitle ?: queueProvider.title
                            )
                        }
                        playTrack(target, tracks, playlistId, playlistTitle ?: queueProvider.title)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to play queue: ${e.message}", e)
            }
        }
    }

    fun playTrack(
        track: TrackItem,
        queue: List<TrackItem> = listOf(track),
        playlistId: String? = null,
        playlistTitle: String? = null
    ) {
        ensureServiceStarted()
        if (queue.size > 1 && currentQueueProvider !is YouTubeRadioQueue) {
            currentQueueProvider = ListQueue(queue, playlistTitle ?: _state.value.queueTitle ?: "Queue")
        }
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        val settings = userPrefs.settingsFlow.value

        // Cancel any active crossfade or pre-arm
        cancelCrossfade()
        requestAppAudioFocus()

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
                queueTitle = playlistTitle ?: if (queue.size > 1) it.queueTitle else null,
                activePlaylistId = playlistId ?: if (queue.size > 1) it.activePlaylistId else null,
                activePlaylistTitle = playlistTitle ?: if (queue.size > 1) it.activePlaylistTitle else null,
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
            val downloadedPath = localDb.getDownloadPathSync(track.id) ?: localDb.getDownloadPathSync(track.videoId)
            val isLocalMedia = track.videoId.startsWith("content://") || track.videoId.startsWith("file://")
            val isDownloadedValid = downloadedPath != null && File(downloadedPath).let { it.exists() && it.length() > 0L }

            val streamUrl = when {
                isLocalMedia -> track.videoId
                isDownloadedValid -> {
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
                    .setAlbumTitle(track.album ?: "")
                    .setArtworkUri(android.net.Uri.parse(track.artworkUrl))
                    .setIsPlayable(true)
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

        val isActuallyPlaying = player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_READY)

        if (isActuallyPlaying) {
            player.pause()
            if (isCrossfading) {
                standbyPlayerInstance.pause()
            }
            _state.update { it.copy(isPlaying = false) }
            stopProgressTracker()
            if (!isRemoteSync) {
                sync?.onLocalPause(player.currentPosition)
            }
        } else {
            ensureServiceStarted()
            if (player.playbackState == Player.STATE_IDLE && _state.value.currentTrack != null) {
                _state.value.currentTrack?.let { playTrack(it, _state.value.queue) }
            } else {
                player.play()
                if (isCrossfading) {
                    standbyPlayerInstance.play()
                }
                _state.update { it.copy(isPlaying = true) }
                startProgressTracker()
                if (!isRemoteSync) {
                    sync?.onLocalPlay(player.currentPosition)
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        cancelCrossfade()
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

    fun hasNextTrack(): Boolean {
        val state = _state.value
        val queue = state.queue
        if (queue.isEmpty()) return false
        if (state.repeatMode != RepeatMode.OFF || state.isShuffle) return true
        if (state.isRadioActive) return true
        return state.queueIndex + 1 < queue.size
    }

    fun hasPreviousTrack(): Boolean {
        return _state.value.queue.isNotEmpty()
    }

    fun next() {
        cancelCrossfade()
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
        cancelCrossfade()
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
        ensureServiceStarted()
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
                    .setAlbumTitle(track.album ?: "")
                    .setArtworkUri(android.net.Uri.parse(track.artworkUrl))
                    .setIsPlayable(true)
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

    fun toggleFavoriteCurrent() {
        val track = _state.value.currentTrack ?: return
        scope.launch {
            localDatabase.toggleFavorite(track)
        }
    }

    fun isCurrentTrackFavorite(): Boolean {
        val track = _state.value.currentTrack ?: return false
        return localDatabase.isFavoriteSync(track.id)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val sync = waveSyncManager
        if (sync != null && sync.isInRoom && !sync.canControlMusic && !isRemoteSync) {
            sync.notifyGuestRestricted()
            return
        }

        val currentQueue = _state.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return

        val currentTrack = _state.value.currentTrack
        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)

        val newIndex = if (currentTrack != null) {
            val matchedIndex = currentQueue.indexOfFirst { it.id == currentTrack.id }
            if (matchedIndex >= 0) matchedIndex else _state.value.queueIndex
        } else {
            _state.value.queueIndex
        }

        _state.update {
            it.copy(
                queue = currentQueue,
                queueIndex = newIndex
            )
        }
        savePersistentQueue()
        if (!isRemoteSync) {
            sync?.onLocalQueueSync(currentQueue)
        }
    }

    fun filterExplicitInQueue(enabled: Boolean = true) {
        if (!enabled) return
        val cur = _state.value.queue
        val idx = _state.value.queueIndex
        val (filtered, newIdx) = OccurrenceAwareFilter.filterTracks(cur, idx) {
            !it.title.contains("(Explicit)", ignoreCase = true)
        }
        _state.update { it.copy(queue = filtered, queueIndex = newIdx) }
        savePersistentQueue()
    }

    private fun loadSimilarTracksAndAppend(currentTrack: TrackItem?) {
        if (currentTrack == null) return
        scope.launch(Dispatchers.IO) {
            try {
                val provider = currentQueueProvider
                val newTracks = if (provider != null && provider.hasNextPage()) {
                    AppLogger.i("▶ Paging next batch from active queue provider (${provider.title})")
                    provider.nextPage()
                } else {
                    AppLogger.i("▶ Seamlessly initiating YouTube Radio Queue for \"${currentTrack.title}\"")
                    val radioQueue = YouTubeRadioQueue(
                        videoId = currentTrack.videoId,
                        initialTrack = currentTrack,
                        innerTubeClient = innerTubeClient
                    )
                    currentQueueProvider = radioQueue
                    val fetched = radioQueue.getInitialTracks()
                    fetched.filter { it.videoId != currentTrack.videoId && it.id != currentTrack.id }
                }

                if (newTracks.isNotEmpty()) {
                    val currentQueue = _state.value.queue
                    val existingIds = currentQueue.map { it.id }.toSet()
                    val existingVideoIds = currentQueue.map { it.videoId }.toSet()
                    val filtered = newTracks.filter { it.id !in existingIds && it.videoId !in existingVideoIds }

                    if (filtered.isNotEmpty()) {
                        val merged = currentQueue + filtered
                        _state.update {
                            it.copy(
                                queue = merged,
                                isRadioActive = true
                            )
                        }
                        savePersistentQueue()
                        AppLogger.i("▶ Auto-appended ${filtered.size} radio tracks to queue. Total: ${merged.size}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("Auto-load radio continuation failed: ${e.message}")
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
                val qTitle = _state.value.queueTitle ?: ""
                val isRadio = _state.value.isRadioActive

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
                    .putString(KEY_QUEUE_TITLE, qTitle)
                    .putBoolean(KEY_QUEUE_IS_RADIO, isRadio)
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
                val savedTitle = sp.getString(KEY_QUEUE_TITLE, null)
                val savedIsRadio = sp.getBoolean(KEY_QUEUE_IS_RADIO, false)

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
                    if (savedIsRadio) {
                        currentQueueProvider = YouTubeRadioQueue(
                            videoId = activeTrack.videoId,
                            initialTrack = activeTrack,
                            innerTubeClient = innerTubeClient
                        )
                    } else {
                        currentQueueProvider = ListQueue(restoredList, savedTitle ?: "Queue")
                    }
                    _state.update {
                        it.copy(
                            queue = restoredList,
                            queueIndex = safeIndex,
                            currentTrack = activeTrack,
                            positionMs = savedPos,
                            durationMs = (activeTrack.durationSeconds * 1000L).coerceAtLeast(0L),
                            isPlaying = false,
                            isBuffering = false,
                            queueTitle = savedTitle,
                            isRadioActive = savedIsRadio
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
        cancelCrossfade()
        abandonAppAudioFocus()
        WaveEqualizer.release()
        primaryPlayerInstance.release()
        standbyPlayerInstance.release()
    }
}
