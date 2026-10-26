package com.uta.wave

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.player.WaveMediaService
import com.uta.wave.player.AppLogger
import com.uta.wave.quicksettings.MusicRecognizerTileService
import com.uta.wave.ui.WaveApp
import com.uta.wave.ui.theme.UtaWaveTheme

class MainActivity : ComponentActivity() {

    private var requestedTab by mutableStateOf<Int?>(null)
    private var isServiceBound = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        AppLogger.i("POST_NOTIFICATIONS permission result: $isGranted")
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isServiceBound = true
            AppLogger.i("WaveMediaService bound successfully")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            AppLogger.i("WaveMediaService unbound")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Pre-start WaveMediaService for immediate session warmup (1:1 with EchoMusic)
        try {
            startService(Intent(this, WaveMediaService::class.java))
        } catch (e: Exception) {
            AppLogger.e("Failed to pre-start WaveMediaService", e)
        }

        if (intent?.getBooleanExtra(MusicRecognizerTileService.EXTRA_OPEN_RECOGNIZE, false) == true ||
            intent?.action == MusicRecognizerTileService.ACTION_RECOGNIZE_MUSIC
        ) {
            requestedTab = 1
        }

        setContent {
            val userPrefs = remember { UserPreferences.getInstance(applicationContext) }
            val settings by userPrefs.settingsFlow.collectAsState()

            // High Refresh Rate Display Controller (1:1 with EchoMusic)
            LaunchedEffect(settings.enableHighRefreshRate) {
                val window = this@MainActivity.window
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val layoutParams = window.attributes
                    if (settings.enableHighRefreshRate) {
                        // 0 allows the window to run at peak device refresh rate (90Hz / 120Hz / 144Hz)
                        layoutParams.preferredDisplayModeId = 0
                    } else {
                        // Lock display mode to 60Hz to save battery
                        val display = this@MainActivity.display
                        val modes = display?.supportedModes ?: emptyArray()
                        val mode60 = modes.firstOrNull { kotlin.math.abs(it.refreshRate - 60f) < 1f }
                            ?: modes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }

                        if (mode60 != null) {
                            layoutParams.preferredDisplayModeId = mode60.modeId
                        }
                    }
                    window.attributes = layoutParams
                } else {
                    val params = window.attributes
                    if (settings.enableHighRefreshRate) {
                        params.preferredRefreshRate = 0f
                    } else {
                        params.preferredRefreshRate = 60f
                    }
                    window.attributes = params
                }
            }

            UtaWaveTheme(
                darkMode = settings.darkMode,
                pureBlack = settings.pureBlack,
                accentColorHex = settings.accentColorHex,
                isDynamicPalette = settings.isDynamicPalette,
                selectedFont = settings.selectedFont,
                customFontPath = settings.customFontPath
            ) {
                WaveApp(initialTab = requestedTab ?: 0)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            bindService(
                Intent(this, WaveMediaService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (e: Exception) {
            AppLogger.e("Failed to bind WaveMediaService", e)
        }
    }

    override fun onStop() {
        if (isServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: Exception) {
                AppLogger.e("Failed to unbind WaveMediaService", e)
            }
            isServiceBound = false
        }
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(MusicRecognizerTileService.EXTRA_OPEN_RECOGNIZE, false) ||
            intent.action == MusicRecognizerTileService.ACTION_RECOGNIZE_MUSIC
        ) {
            requestedTab = 1
        }
    }
}
