package com.uta.wave.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class WaveMediaService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val playbackManager = PlaybackManager.getInstance(this)
        mediaSession = MediaSession.Builder(this, playbackManager.player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
