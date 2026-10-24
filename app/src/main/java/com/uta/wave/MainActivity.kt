package com.uta.wave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.ui.WaveApp
import com.uta.wave.ui.theme.UtaWaveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userPrefs = remember { UserPreferences.getInstance(applicationContext) }
            val settings by userPrefs.settingsFlow.collectAsState()

            UtaWaveTheme(
                darkMode = settings.darkMode,
                pureBlack = settings.pureBlack,
                accentColorHex = settings.accentColorHex,
                isDynamicPalette = settings.isDynamicPalette,
                selectedFont = settings.selectedFont,
                customFontPath = settings.customFontPath
            ) {
                WaveApp()
            }
        }
    }
}
