package com.uta.wave.desktop.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

data class DesktopAppSettings(
    val audioQuality: String = "Hi-Fi Lossless (24-bit 96kHz)",
    val bufferSizeMs: Int = 500,
    val volumeNormalization: Boolean = false,
    val isAutomixEnabled: Boolean = true,
    val isSmartShuffleSpread: Boolean = true,
    val autoResumeOnStartup: Boolean = true,
    val dynamicAlbumGlow: Boolean = true,
    val karaokeLyricsBloom: Boolean = true,
    val reducedMotion: Boolean = false,
    val crossfadeEnabled: Boolean = true,
    val crossfadeDurationSeconds: Int = 5,
    val crossfadeGapless: Boolean = true
)

class DesktopPreferences private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: DesktopPreferences? = null

        fun getInstance(): DesktopPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DesktopPreferences().also { INSTANCE = it }
            }
        }

        private fun getPrefsFile(): File {
            val localAppData = System.getenv("LOCALAPPDATA")
                ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local")
            val dir = File(localAppData, "UTA-wave")
            if (!dir.exists()) dir.mkdirs()
            return File(dir, "preferences.json")
        }
    }

    private val file = getPrefsFile()
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<DesktopAppSettings> = _settings.asStateFlow()

    private fun loadSettings(): DesktopAppSettings {
        try {
            if (!file.exists()) return DesktopAppSettings()
            val text = file.readText(Charsets.UTF_8).trim()
            if (text.isBlank()) return DesktopAppSettings()
            val json = JSONObject(text)
            return DesktopAppSettings(
                audioQuality = json.optString("audioQuality", "Hi-Fi Lossless (24-bit 96kHz)"),
                bufferSizeMs = json.optInt("bufferSizeMs", 500),
                volumeNormalization = json.optBoolean("volumeNormalization", false),
                isAutomixEnabled = json.optBoolean("isAutomixEnabled", true),
                isSmartShuffleSpread = json.optBoolean("isSmartShuffleSpread", true),
                autoResumeOnStartup = json.optBoolean("autoResumeOnStartup", true),
                dynamicAlbumGlow = json.optBoolean("dynamicAlbumGlow", true),
                karaokeLyricsBloom = json.optBoolean("karaokeLyricsBloom", true),
                reducedMotion = json.optBoolean("reducedMotion", false),
                crossfadeEnabled = json.optBoolean("crossfadeEnabled", true),
                crossfadeDurationSeconds = json.optInt("crossfadeDurationSeconds", 5),
                crossfadeGapless = json.optBoolean("crossfadeGapless", true)
            )
        } catch (_: Exception) {
            return DesktopAppSettings()
        }
    }

    fun updateSettings(newSettings: DesktopAppSettings) {
        _settings.value = newSettings
        try {
            val json = JSONObject().apply {
                put("audioQuality", newSettings.audioQuality)
                put("bufferSizeMs", newSettings.bufferSizeMs)
                put("volumeNormalization", newSettings.volumeNormalization)
                put("isAutomixEnabled", newSettings.isAutomixEnabled)
                put("isSmartShuffleSpread", newSettings.isSmartShuffleSpread)
                put("autoResumeOnStartup", newSettings.autoResumeOnStartup)
                put("dynamicAlbumGlow", newSettings.dynamicAlbumGlow)
                put("karaokeLyricsBloom", newSettings.karaokeLyricsBloom)
                put("reducedMotion", newSettings.reducedMotion)
                put("crossfadeEnabled", newSettings.crossfadeEnabled)
                put("crossfadeDurationSeconds", newSettings.crossfadeDurationSeconds)
                put("crossfadeGapless", newSettings.crossfadeGapless)
            }
            file.writeText(json.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            System.err.println("[DesktopPreferences] Failed to save preferences.json: ${e.message}")
        }
    }

    fun calculateCacheSizeBytes(): Long {
        return try {
            val localAppData = System.getenv("LOCALAPPDATA")
                ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local")
            val dir = File(localAppData, "UTA-wave")
            if (!dir.exists()) return 0L
            var total = 0L
            dir.walkTopDown().forEach { f ->
                if (f.isFile && f.name != "library.json" && f.name != "preferences.json") {
                    total += f.length()
                }
            }
            total
        } catch (_: Exception) {
            0L
        }
    }

    fun clearCache(): Boolean {
        return try {
            val localAppData = System.getenv("LOCALAPPDATA")
                ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local")
            val dir = File(localAppData, "UTA-wave")
            if (dir.exists()) {
                dir.walkTopDown().forEach { f ->
                    if (f.isFile && f.name != "library.json" && f.name != "preferences.json") {
                        f.delete()
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
