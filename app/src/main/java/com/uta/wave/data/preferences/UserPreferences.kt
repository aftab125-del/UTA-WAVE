package com.uta.wave.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppSettings(
    val themeMode: String = "OBSIDIAN",            // OBSIDIAN (Default), AMOLED, DIM, SYSTEM
    val accentColorHex: String = "#F2AEC7",        // Default Blush Pink (#F2AEC7), Violet (#A855F7), Sky Blue (#38BDF8), Emerald (#10B981), Amber (#F59E0B)
    val wifiQuality: String = "LOSSLESS",          // LOSSLESS, HIGH, STANDARD, DATA_SAVER
    val cellularQuality: String = "HIGH",
    val defaultDownloadQuality: String = "LOSSLESS",
    val downloadOverWifiOnly: Boolean = false,
    val playerStyle: String = "WAVE",              // WAVE (Squiggly), CLASSIC
    val lyricsAnimation: String = "ALPHABET_V2",   // ALPHABET_V2 (Smooth Letter flow), WORD_BY_WORD
    val ambientCanvasEnabled: Boolean = true,
    val seekStepSeconds: Int = 10,                 // 5, 10, 15, 30
    val crossfadeSeconds: Int = 0,                 // 0, 2, 4, 6, 8, 12
    val skipSilence: Boolean = false,
    val normalizeVolume: Boolean = false,
    val stopOnTaskRemoved: Boolean = true
)

class UserPreferences private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "uta_wave_preferences"

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    val currentSettings: AppSettings
        get() = _settingsFlow.value

    private fun loadSettings(): AppSettings {
        return AppSettings(
            themeMode = prefs.getString("theme_mode", "OBSIDIAN") ?: "OBSIDIAN",
            accentColorHex = prefs.getString("accent_color_hex", "#F2AEC7") ?: "#F2AEC7",
            wifiQuality = prefs.getString("wifi_quality", "LOSSLESS") ?: "LOSSLESS",
            cellularQuality = prefs.getString("cellular_quality", "HIGH") ?: "HIGH",
            defaultDownloadQuality = prefs.getString("default_download_quality", "LOSSLESS") ?: "LOSSLESS",
            downloadOverWifiOnly = prefs.getBoolean("download_over_wifi_only", false),
            playerStyle = prefs.getString("player_style", "WAVE") ?: "WAVE",
            lyricsAnimation = prefs.getString("lyrics_animation", "ALPHABET_V2") ?: "ALPHABET_V2",
            ambientCanvasEnabled = prefs.getBoolean("ambient_canvas_enabled", true),
            seekStepSeconds = prefs.getInt("seek_step_seconds", 10),
            crossfadeSeconds = prefs.getInt("crossfade_seconds", 0),
            skipSilence = prefs.getBoolean("skip_silence", false),
            normalizeVolume = prefs.getBoolean("normalize_volume", false),
            stopOnTaskRemoved = prefs.getBoolean("stop_on_task_removed", true)
        )
    }

    fun updateThemeMode(theme: String) {
        prefs.edit().putString("theme_mode", theme).apply()
        _settingsFlow.update { it.copy(themeMode = theme) }
    }

    fun updateAccentColor(hex: String) {
        prefs.edit().putString("accent_color_hex", hex).apply()
        _settingsFlow.update { it.copy(accentColorHex = hex) }
    }

    fun updateWifiQuality(quality: String) {
        prefs.edit().putString("wifi_quality", quality).apply()
        _settingsFlow.update { it.copy(wifiQuality = quality) }
    }

    fun updateCellularQuality(quality: String) {
        prefs.edit().putString("cellular_quality", quality).apply()
        _settingsFlow.update { it.copy(cellularQuality = quality) }
    }

    fun updateDownloadQuality(quality: String) {
        prefs.edit().putString("default_download_quality", quality).apply()
        _settingsFlow.update { it.copy(defaultDownloadQuality = quality) }
    }

    fun updateDownloadOverWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean("download_over_wifi_only", enabled).apply()
        _settingsFlow.update { it.copy(downloadOverWifiOnly = enabled) }
    }

    fun updatePlayerStyle(style: String) {
        prefs.edit().putString("player_style", style).apply()
        _settingsFlow.update { it.copy(playerStyle = style) }
    }

    fun updateLyricsAnimation(style: String) {
        prefs.edit().putString("lyrics_animation", style).apply()
        _settingsFlow.update { it.copy(lyricsAnimation = style) }
    }

    fun updateAmbientCanvas(enabled: Boolean) {
        prefs.edit().putBoolean("ambient_canvas_enabled", enabled).apply()
        _settingsFlow.update { it.copy(ambientCanvasEnabled = enabled) }
    }

    fun updateSeekStep(seconds: Int) {
        prefs.edit().putInt("seek_step_seconds", seconds).apply()
        _settingsFlow.update { it.copy(seekStepSeconds = seconds) }
    }

    fun updateCrossfade(seconds: Int) {
        prefs.edit().putInt("crossfade_seconds", seconds).apply()
        _settingsFlow.update { it.copy(crossfadeSeconds = seconds) }
    }

    fun updateSkipSilence(enabled: Boolean) {
        prefs.edit().putBoolean("skip_silence", enabled).apply()
        _settingsFlow.update { it.copy(skipSilence = enabled) }
    }

    fun updateNormalizeVolume(enabled: Boolean) {
        prefs.edit().putBoolean("normalize_volume", enabled).apply()
        _settingsFlow.update { it.copy(normalizeVolume = enabled) }
    }

    fun updateStopOnTaskRemoved(enabled: Boolean) {
        prefs.edit().putBoolean("stop_on_task_removed", enabled).apply()
        _settingsFlow.update { it.copy(stopOnTaskRemoved = enabled) }
    }
}
