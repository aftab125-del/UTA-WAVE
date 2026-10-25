package com.uta.wave

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.quicksettings.MusicRecognizerTileService
import com.uta.wave.ui.WaveApp
import com.uta.wave.ui.theme.UtaWaveTheme

class MainActivity : ComponentActivity() {

    private var requestedTab by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
