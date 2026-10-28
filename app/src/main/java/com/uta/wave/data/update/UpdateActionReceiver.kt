package com.uta.wave.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.uta.wave.player.AppLogger

class UpdateActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val manager = AppUpdateManager.getInstance(context)

        when (intent.action) {
            AppUpdateManager.ACTION_DISMISS_UPDATE -> {
                val versionCode = intent.getIntExtra("version_code", 0)
                AppLogger.i("User tapped 'Later' for update versionCode=$versionCode")
                manager.dismissUpdate(versionCode)
            }

            AppUpdateManager.ACTION_DOWNLOAD_UPDATE -> {
                val downloadUrl = intent.getStringExtra("download_url") ?: ""
                val versionName = intent.getStringExtra("version_name") ?: "latest"
                val versionCode = intent.getIntExtra("version_code", 0)

                AppLogger.i("User tapped 'Download now' for update v$versionName ($versionCode)")
                if (downloadUrl.isNotBlank()) {
                    val info = AppUpdateInfo(
                        versionCode = versionCode,
                        versionName = versionName,
                        title = "UTA-wave Update",
                        changelog = "",
                        downloadUrl = downloadUrl
                    )
                    manager.startDownload(info)
                }
            }

            DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    AppLogger.i("Received ACTION_DOWNLOAD_COMPLETE for downloadId=$downloadId")
                    manager.onDownloadComplete(downloadId)
                }
            }
        }
    }
}
