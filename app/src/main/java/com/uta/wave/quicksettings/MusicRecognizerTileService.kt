package com.uta.wave.quicksettings

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.uta.wave.MainActivity
import com.uta.wave.R

/**
 * Android Quick Settings Tile allowing 1-tap music recognition
 * directly from the system drop-down notification shade.
 */
class MusicRecognizerTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Recognize Music"
            try {
                icon = Icon.createWithResource(this@MusicRecognizerTileService, R.drawable.ic_quick_recognize)
            } catch (e: Exception) {
                // fallback
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_RECOGNIZE_MUSIC
            putExtra(EXTRA_OPEN_RECOGNIZE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launchIntent)
        }
    }

    companion object {
        const val ACTION_RECOGNIZE_MUSIC = "com.uta.wave.action.RECOGNIZE_MUSIC"
        const val EXTRA_OPEN_RECOGNIZE = "extra_open_recognize"
    }
}
