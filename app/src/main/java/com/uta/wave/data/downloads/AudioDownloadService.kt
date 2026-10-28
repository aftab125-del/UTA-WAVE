package com.uta.wave.data.downloads

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.uta.wave.MainActivity
import com.uta.wave.R
import com.uta.wave.player.AppLogger

class AudioDownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "uta_wave_downloads"
        const val NOTIFICATION_ID = 4040
        const val ACTION_START_DOWNLOAD = "com.uta.wave.action.START_DOWNLOAD"
        const val ACTION_UPDATE_PROGRESS = "com.uta.wave.action.UPDATE_PROGRESS"
        const val ACTION_CANCEL_DOWNLOAD = "com.uta.wave.action.CANCEL_DOWNLOAD"
        const val ACTION_STOP_SERVICE = "com.uta.wave.action.STOP_SERVICE"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_TOTAL = "extra_total"
        const val EXTRA_INDEX = "extra_index"

        fun start(context: Context, title: String, artist: String = "") {
            val intent = Intent(context, AudioDownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                AppLogger.e("Failed to start AudioDownloadService: ${e.message}")
            }
        }

        fun updateProgress(
            context: Context,
            title: String,
            progressPercent: Int,
            index: Int = 1,
            total: Int = 1
        ) {
            val intent = Intent(context, AudioDownloadService::class.java).apply {
                action = ACTION_UPDATE_PROGRESS
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS, progressPercent)
                putExtra(EXTRA_INDEX, index)
                putExtra(EXTRA_TOTAL, total)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // service may have stopped
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioDownloadService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Audio Track"
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
                startForegroundNotification(title, artist, progress = 0, indeterminate = true)
            }
            ACTION_UPDATE_PROGRESS -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Audio Track"
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                val index = intent.getIntExtra(EXTRA_INDEX, 1)
                val total = intent.getIntExtra(EXTRA_TOTAL, 1)
                val subtitle = if (total > 1) "Downloading [$index/$total] ($progress%)" else "Downloading ($progress%)"
                updateProgressNotification(title, subtitle, progress)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                AppLogger.i("Download cancelled by user notification action")
                AudioDownloadManager.getInstance(this).cancelBatchDownload()
                stopForegroundSafely()
                stopSelf()
            }
            ACTION_STOP_SERVICE -> {
                stopForegroundSafely()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UTA-wave Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of offline audio downloads"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification(
        title: String,
        artist: String,
        progress: Int,
        indeterminate: Boolean
    ) {
        val notification = buildNotification(title, if (artist.isNotBlank()) "Downloading • $artist" else "Downloading...", progress, indeterminate)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateProgressNotification(title: String, subtitle: String, progress: Int) {
        val notification = buildNotification(title, subtitle, progress, indeterminate = false)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        title: String,
        contentText: String,
        progress: Int,
        indeterminate: Boolean
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, AudioDownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun stopForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            AppLogger.e("Error stopping download foreground: ${e.message}")
        }
    }
}
