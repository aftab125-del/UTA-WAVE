package com.uta.wave.data.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.uta.wave.R
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class AppUpdateManager private constructor(private val context: Context) {

    companion object {
        const val VERSION_URL = "https://raw.githubusercontent.com/aftab125-del/UTA-WAVE/main/version.json"
        const val CHANNEL_ID = "uta_wave_app_updates"
        const val NOTIFICATION_ID = 8888
        const val PREFS_NAME = "uta_wave_updater_prefs"
        const val KEY_LAST_DISMISSED_VERSION = "last_dismissed_version"
        const val KEY_ACTIVE_DOWNLOAD_ID = "active_download_id"
        const val KEY_PENDING_INSTALL_PATH = "pending_install_path"

        const val ACTION_DOWNLOAD_UPDATE = "com.uta.wave.ACTION_DOWNLOAD_UPDATE"
        const val ACTION_DISMISS_UPDATE = "com.uta.wave.ACTION_DISMISS_UPDATE"

        @Volatile
        private var INSTANCE: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppUpdateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new UTA-wave APK releases and updates"
                enableLights(true)
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun checkForUpdate(silent: Boolean = false, onResult: (AppUpdateInfo?) -> Unit = {}) {
        scope.launch {
            try {
                AppLogger.i("Checking for app update at: $VERSION_URL")
                val request = Request.Builder()
                    .url(VERSION_URL)
                    .header("Cache-Control", "no-cache")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    AppLogger.w("Update check HTTP failed: ${response.code}")
                    withContext(Dispatchers.Main) { onResult(null) }
                    return@launch
                }

                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)

                val remoteVersionCode = json.optInt("versionCode", 0)
                val remoteVersionName = json.optString("versionName", "")
                val title = json.optString("title", "New UTA-wave Update")
                val changelog = json.optString("changelog", "Bug fixes and performance improvements.")
                val downloadUrl = json.optString("downloadUrl", "")
                val mandatory = json.optBoolean("mandatory", false)

                val currentCode = getCurrentVersionCode()
                AppLogger.i("Version check: Current=$currentCode, Remote=$remoteVersionCode ($remoteVersionName)")

                if (remoteVersionCode > currentCode && downloadUrl.isNotBlank()) {
                    val updateInfo = AppUpdateInfo(
                        versionCode = remoteVersionCode,
                        versionName = remoteVersionName,
                        title = title,
                        changelog = changelog,
                        downloadUrl = downloadUrl,
                        isMandatory = mandatory
                    )

                    _availableUpdate.value = updateInfo

                    val lastDismissed = prefs.getInt(KEY_LAST_DISMISSED_VERSION, 0)
                    if (mandatory || lastDismissed < remoteVersionCode || !silent) {
                        showUpdateNotification(updateInfo)
                    }

                    withContext(Dispatchers.Main) { onResult(updateInfo) }
                } else {
                    _availableUpdate.value = null
                    withContext(Dispatchers.Main) { onResult(null) }
                }
            } catch (e: Exception) {
                AppLogger.e("Update check error: ${e.message}", e)
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    fun showUpdateNotification(info: AppUpdateInfo) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent for "Later"
        val dismissIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = ACTION_DISMISS_UPDATE
            putExtra("version_code", info.versionCode)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Download now"
        val downloadIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = ACTION_DOWNLOAD_UPDATE
            putExtra("download_url", info.downloadUrl)
            putExtra("version_name", info.versionName)
            putExtra("version_code", info.versionCode)
        }
        val downloadPendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content intent: Opens app
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            103,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("UTA-wave Update Available")
            .setContentText("Version ${info.versionName} is ready. Tap to download.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Version ${info.versionName} is available with new features & improvements:\n\n${info.changelog}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Later", dismissPendingIntent)
            .addAction(0, "Download now", downloadPendingIntent)
            .setColor(0xFFF2AEC7.toInt()) // BlushRose accent
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    fun dismissUpdate(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_DISMISSED_VERSION, versionCode).apply()
        _availableUpdate.value = null
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    fun startDownload(info: AppUpdateInfo) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)

        val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "updates").also { if (!it.exists()) it.mkdirs() }

        val apkFileName = "UTA-wave-v${info.versionName}.apk"
        val destinationFile = File(destinationDir, apkFileName)

        // If file already exists and valid, skip download and prompt install immediately
        if (destinationFile.exists() && destinationFile.length() > 1024 * 1024) {
            AppLogger.i("Update APK already downloaded: ${destinationFile.absolutePath}, launching installer...")
            installApk(destinationFile)
            return
        }

        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
                .setTitle("Downloading UTA-wave v${info.versionName}")
                .setDescription("Downloading latest APK update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setMimeType("application/vnd.android.package-archive")

            val downloadId = dm.enqueue(request)
            prefs.edit()
                .putLong(KEY_ACTIVE_DOWNLOAD_ID, downloadId)
                .putString(KEY_PENDING_INSTALL_PATH, destinationFile.absolutePath)
                .apply()

            Toast.makeText(context, "Downloading UTA-wave v${info.versionName}...", Toast.LENGTH_SHORT).show()
            AppLogger.i("Enqueued update download ID=$downloadId to ${destinationFile.absolutePath}")
        } catch (e: Exception) {
            AppLogger.e("Failed to enqueue download: ${e.message}", e)
            Toast.makeText(context, "Could not start download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun onDownloadComplete(downloadId: Long) {
        val savedId = prefs.getLong(KEY_ACTIVE_DOWNLOAD_ID, -1L)
        val savedPath = prefs.getString(KEY_PENDING_INSTALL_PATH, null)

        if (downloadId == savedId && !savedPath.isNullOrBlank()) {
            val file = File(savedPath)
            if (file.exists() && file.length() > 0) {
                AppLogger.i("Download complete for ${file.absolutePath} (${file.length()} bytes)")
                installApk(file)
            } else {
                AppLogger.e("Downloaded APK file missing or empty: $savedPath")
            }
        }
    }

    fun installApk(apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    Toast.makeText(context, "Please allow UTA-wave to install updates", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
            AppLogger.i("Launched APK Package Installer for: ${apkFile.absolutePath}")
        } catch (e: Exception) {
            AppLogger.e("Error opening installer: ${e.message}", e)
            Toast.makeText(context, "Unable to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
