package com.uta.wave.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.uta.wave.MainActivity
import com.uta.wave.R
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Production-grade MediaLibraryService for UTA-WAVE.
 *
 * Implements:
 * - Silent NotificationChannel (IMPORTANCE_LOW, VISIBILITY_PUBLIC) for noiseless track updates.
 * - Media3 WaveNotificationProvider with dynamic swipe-to-dismiss behavior (ongoing when playing, dismissible when paused).
 * - High-performance CoilBitmapLoader for lockscreen/notification album artwork with queue prefetching.
 * - Dynamic custom layout actions: Favorite (Heart), Repeat Mode cycle, and Shuffle toggle.
 * - Android 13+ Lockscreen & Notification wavy seekbar compatibility.
 * - Full MediaLibraryService & MediaBrowserService compatibility for SystemUI lockscreen controls & Android Auto.
 * - Warmup MediaController connection to ensure immediate OS framework MediaSessionManager registration.
 */
@OptIn(UnstableApi::class)
class WaveMediaService : MediaLibraryService() {

    companion object {
        const val CHANNEL_ID = "wave_music_playback_channel"
        const val NOTIFICATION_ID = 1001
    }

    private var mediaSession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var playbackManager: PlaybackManager
    private lateinit var localDatabase: LocalDatabase
    private lateinit var bitmapLoader: CoilBitmapLoader
    private val binder = WaveMediaBinder()

    inner class WaveMediaBinder : Binder() {
        val service: WaveMediaService get() = this@WaveMediaService
    }

    private val sessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(MediaSessionConstants.CommandToggleLike)
                .add(MediaSessionConstants.CommandToggleRepeatMode)
                .add(MediaSessionConstants.CommandToggleShuffle)
                .build()

            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootExtras = Bundle().apply {
                putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
                putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
            }
            val rootItem = MediaItem.Builder()
                .setMediaId("[root]")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setTitle(getString(R.string.app_name))
                        .build()
                )
                .build()
            val libraryParams = LibraryParams.Builder().setExtras(rootExtras).build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                MediaSessionConstants.ACTION_TOGGLE_LIKE -> {
                    playbackManager.toggleFavoriteCurrent()
                    updateNotification()
                }
                MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> {
                    playbackManager.toggleRepeat()
                    updateNotification()
                }
                MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> {
                    playbackManager.toggleShuffle()
                    updateNotification()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Guard against ForegroundServiceStartNotAllowedException on Android 12+
        setListener(
            object : Listener {
                override fun onForegroundServiceStartNotAllowedException() {
                    AppLogger.e("ForegroundServiceStartNotAllowedException caught by WaveMediaService listener")
                }
            }
        )

        playbackManager = PlaybackManager.getInstance(this)
        localDatabase = LocalDatabase.getInstance(this)
        bitmapLoader = CoilBitmapLoader(this, serviceScope)

        createNotificationChannel()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        setMediaNotificationProvider(
            WaveNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            ).apply {
                setSmallIcon(R.drawable.ic_wave_notification)
            }
        )

        mediaSession = MediaLibrarySession.Builder(this, playbackManager.player, sessionCallback)
            .setSessionActivity(sessionActivityPendingIntent)
            .setBitmapLoader(bitmapLoader)
            .build()

        // Publish session immediately to Android framework MediaSessionManager (1:1 with EchoMusic)
        val sessionToken = SessionToken(this, ComponentName(this, WaveMediaService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                controllerFuture.get()
                AppLogger.i("Internal MediaController connected successfully")
            } catch (e: Exception) {
                AppLogger.e("Failed to connect internal MediaController", e)
            }
        }, ContextCompat.getMainExecutor(this))

        playbackManager.onPlayerSwapped = { newPlayer ->
            try {
                mediaSession?.player = newPlayer
                updateNotification()
            } catch (e: Exception) {
                AppLogger.e("Failed to swap player in MediaSession", e)
            }
        }

        // Reactively observe playback state and update notification custom layout & prefetch
        serviceScope.launch {
            playbackManager.state.collect { state ->
                updateNotification()

                // Prefetch upcoming song's artwork
                val queue = state.queue
                val nextIndex = state.queueIndex + 1
                if (nextIndex in queue.indices) {
                    val nextTrack = queue[nextIndex]
                    if (nextTrack.artworkUrl.isNotBlank()) {
                        bitmapLoader.prefetch(Uri.parse(nextTrack.artworkUrl))
                    }
                }
            }
        }

        // Reactively observe favorites to update Like heart icon in notification
        serviceScope.launch {
            localDatabase.favoritesFlow.collect {
                updateNotification()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.music_player),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.music_player_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val session = mediaSession ?: return
        val playbackState = playbackManager.state.value
        val isLiked = playbackManager.isCurrentTrackFavorite()
        val isShuffle = playbackState.isShuffle
        val repeatMode = playbackState.repeatMode

        val likeButton = CommandButton.Builder()
            .setDisplayName(getString(if (isLiked) R.string.action_remove_like else R.string.action_like))
            .setIconResId(if (isLiked) R.drawable.ic_heart else R.drawable.ic_heart_outline)
            .setSessionCommand(MediaSessionConstants.CommandToggleLike)
            .setEnabled(playbackState.currentTrack != null)
            .build()

        val repeatButton = CommandButton.Builder()
            .setDisplayName(
                getString(
                    when (repeatMode) {
                        RepeatMode.OFF -> R.string.repeat_mode_off
                        RepeatMode.ONE -> R.string.repeat_mode_one
                        RepeatMode.ALL -> R.string.repeat_mode_all
                    }
                )
            )
            .setIconResId(
                when (repeatMode) {
                    RepeatMode.OFF -> R.drawable.repeat
                    RepeatMode.ONE -> R.drawable.repeat_one_on
                    RepeatMode.ALL -> R.drawable.repeat_on
                }
            )
            .setSessionCommand(MediaSessionConstants.CommandToggleRepeatMode)
            .build()

        val shuffleButton = CommandButton.Builder()
            .setDisplayName(getString(if (isShuffle) R.string.action_shuffle_off else R.string.action_shuffle_on))
            .setIconResId(if (isShuffle) R.drawable.shuffle_on else R.drawable.shuffle)
            .setSessionCommand(MediaSessionConstants.CommandToggleShuffle)
            .build()

        session.setCustomLayout(listOf(likeButton, repeatButton, shuffleButton))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent) ?: binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val userPrefs = UserPreferences.getInstance(this)
        val stopOnTaskClear = userPrefs.settingsFlow.value.stopMusicOnTaskClear
        if (stopOnTaskClear) {
            playbackManager.player.pause()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
