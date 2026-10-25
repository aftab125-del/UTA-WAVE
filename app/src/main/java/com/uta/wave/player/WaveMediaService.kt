package com.uta.wave.player

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class WaveMediaService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val playbackManager = PlaybackManager.getInstance(this)
        mediaSession = MediaSession.Builder(this, playbackManager.player).build()
        playbackManager.onPlayerSwapped = { newPlayer ->
            try {
                mediaSession?.player = newPlayer
            } catch (e: Exception) {
                AppLogger.e("Failed to swap player in MediaSession", e)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val userPrefs = com.uta.wave.data.preferences.UserPreferences.getInstance(this)
        val stopOnTaskClear = userPrefs.settingsFlow.value.stopMusicOnTaskClear
        if (stopOnTaskClear) {
            val playbackManager = PlaybackManager.getInstance(this)
            playbackManager.player.pause()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
