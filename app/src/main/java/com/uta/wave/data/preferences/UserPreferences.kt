package com.uta.wave.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppSettings(
    // Appearance & Themes (EchoMusic Theme & Colors System)
    val themeMode: String = "OBSIDIAN",            // OBSIDIAN (Default), AMOLED, DIM, SYSTEM, LIGHT
    val darkMode: String = "ON",                   // AUTO (Follow System), ON (Dark), OFF (Light)
    val pureBlack: Boolean = false,                // True AMOLED pitch-black
    val pureBlackMiniPlayer: Boolean = false,      // OLED true black miniplayer
    val isDynamicPalette: Boolean = true,          // Dynamic color palette
    val accentColorHex: String = "#F2AEC7",        // Blush Pink (#F2AEC7), Violet (#A855F7), Sky Blue (#38BDF8), Emerald (#10B981), Amber (#F59E0B)
    val fontScale: Float = 1.0f,                   // 0.9f, 1.0f, 1.15f
    val miniplayerStyle: String = "FLOATING",      // FLOATING, BOTTOM_ATTACHED
    val playerStyle: String = "WAVE",              // WAVE (Squiggly), CLASSIC
    val lyricsAnimation: String = "ALPHABET_V2",   // ALPHABET_V2 (Smooth Letter flow), WORD_BY_WORD
    val ambientCanvasEnabled: Boolean = true,

    // Audio & Playback
    val wifiQuality: String = "LOSSLESS",          // LOSSLESS, HIGH, STANDARD, DATA_SAVER
    val cellularQuality: String = "HIGH",
    val defaultDownloadQuality: String = "LOSSLESS",
    val downloadOverWifiOnly: Boolean = false,
    val seekStepSeconds: Int = 10,                 // 5, 10, 15, 30
    val gaplessPlayback: Boolean = true,
    val crossfadeSeconds: Int = 0,                 // 0, 2, 4, 6, 8, 12
    val skipSilence: Boolean = false,
    val normalizeVolume: Boolean = false,
    val equalizerPreset: String = "FLAT",          // FLAT, BASS_BOOST, VOCAL, POP, ROCK, ACOUSTIC
    val stopOnTaskRemoved: Boolean = true,

    // AI Hub
    val aiTranslationEnabled: Boolean = false,
    val aiRomanizationEnabled: Boolean = false,
    val aiProvider: String = "BUILTIN",            // BUILTIN, WHISPER_CLOUD, OPENROUTER, CUSTOM
    val aiTargetLanguage: String = "English",      // English, Spanish, French, Japanese, Hindi, German
    val aiApiKey: String = "",

    // Listen Together
    val listenTogetherRoomCode: String = "",
    val listenTogetherSyncMode: String = "HOST_SYNC", // HOST_SYNC, COLLABORATIVE

    // Content & Language
    val contentCountry: String = "GLOBAL",         // GLOBAL, US, IN, JP, GB, KR, DE, CA, BR
    val contentLanguage: String = "en",            // en, hi, ja, ko, es, fr, de
    val filterExplicit: Boolean = false,
    val discoveryFeedSource: String = "MIXES",     // MIXES, CHARTS
    val lyricsProviderPriority: String = "LRCLIB", // LRCLIB, YOUTUBE, PLAIN

    // Privacy
    val pauseHistory: Boolean = false,
    val pauseSearchHistory: Boolean = false,
    val anonymousDiagnostics: Boolean = true,

    // Backup & Restore
    val autoBackupSchedule: String = "OFF",        // OFF, WEEKLY, MONTHLY

    // App Icon & Customization
    val customIconPath: String? = null,
    val customIconLabel: String = "UTA-wave",
    val customIconShape: String = "SQUIRCLE",      // SQUIRCLE, CIRCLE, ROUNDED
    val selectedIconPreset: String = "DEFAULT",    // DEFAULT, ECHO_PINK, CYBER_GOLD, NEON_BLUE, CRIMSON, SAKURA
    val useCustomIconInApp: Boolean = true,

    // Typography & Font
    val selectedFont: String = "GOOGLE_SANS",      // SYSTEM, GOOGLE_SANS, SANS_FLEX, OUTFIT, PLUS_JAKARTA_SANS, CUSTOM
    val customFontPath: String? = null
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
            darkMode = prefs.getString("dark_mode", "ON") ?: "ON",
            pureBlack = prefs.getBoolean("pure_black", false),
            pureBlackMiniPlayer = prefs.getBoolean("pure_black_mini_player", false),
            isDynamicPalette = prefs.getBoolean("is_dynamic_palette", true),
            accentColorHex = prefs.getString("accent_color_hex", "#F2AEC7") ?: "#F2AEC7",
            fontScale = prefs.getFloat("font_scale", 1.0f),
            miniplayerStyle = prefs.getString("miniplayer_style", "FLOATING") ?: "FLOATING",
            playerStyle = prefs.getString("player_style", "WAVE") ?: "WAVE",
            lyricsAnimation = prefs.getString("lyrics_animation", "ALPHABET_V2") ?: "ALPHABET_V2",
            ambientCanvasEnabled = prefs.getBoolean("ambient_canvas_enabled", true),

            wifiQuality = prefs.getString("wifi_quality", "LOSSLESS") ?: "LOSSLESS",
            cellularQuality = prefs.getString("cellular_quality", "HIGH") ?: "HIGH",
            defaultDownloadQuality = prefs.getString("default_download_quality", "LOSSLESS") ?: "LOSSLESS",
            downloadOverWifiOnly = prefs.getBoolean("download_over_wifi_only", false),
            seekStepSeconds = prefs.getInt("seek_step_seconds", 10),
            gaplessPlayback = prefs.getBoolean("gapless_playback", true),
            crossfadeSeconds = prefs.getInt("crossfade_seconds", 0),
            skipSilence = prefs.getBoolean("skip_silence", false),
            normalizeVolume = prefs.getBoolean("normalize_volume", false),
            equalizerPreset = prefs.getString("equalizer_preset", "FLAT") ?: "FLAT",
            stopOnTaskRemoved = prefs.getBoolean("stop_on_task_removed", true),

            aiTranslationEnabled = prefs.getBoolean("ai_translation_enabled", false),
            aiRomanizationEnabled = prefs.getBoolean("ai_romanization_enabled", false),
            aiProvider = prefs.getString("ai_provider", "BUILTIN") ?: "BUILTIN",
            aiTargetLanguage = prefs.getString("ai_target_language", "English") ?: "English",
            aiApiKey = prefs.getString("ai_api_key", "") ?: "",

            listenTogetherRoomCode = prefs.getString("listen_together_room_code", "") ?: "",
            listenTogetherSyncMode = prefs.getString("listen_together_sync_mode", "HOST_SYNC") ?: "HOST_SYNC",

            contentCountry = prefs.getString("content_country", "GLOBAL") ?: "GLOBAL",
            contentLanguage = prefs.getString("content_language", "en") ?: "en",
            filterExplicit = prefs.getBoolean("filter_explicit", false),
            discoveryFeedSource = prefs.getString("discovery_feed_source", "MIXES") ?: "MIXES",
            lyricsProviderPriority = prefs.getString("lyrics_provider_priority", "LRCLIB") ?: "LRCLIB",

            pauseHistory = prefs.getBoolean("pause_history", false),
            pauseSearchHistory = prefs.getBoolean("pause_search_history", false),
            anonymousDiagnostics = prefs.getBoolean("anonymous_diagnostics", true),

            autoBackupSchedule = prefs.getString("auto_backup_schedule", "OFF") ?: "OFF",

            customIconPath = prefs.getString("custom_icon_path", null),
            customIconLabel = prefs.getString("custom_icon_label", "UTA-wave") ?: "UTA-wave",
            customIconShape = prefs.getString("custom_icon_shape", "SQUIRCLE") ?: "SQUIRCLE",
            selectedIconPreset = prefs.getString("selected_icon_preset", "DEFAULT") ?: "DEFAULT",
            useCustomIconInApp = prefs.getBoolean("use_custom_icon_in_app", true),

            selectedFont = prefs.getString("selected_font", "GOOGLE_SANS") ?: "GOOGLE_SANS",
            customFontPath = prefs.getString("custom_font_path", null)
        )
    }

    // Appearance Updates (EchoMusic Theme & Colors)
    fun updateDarkMode(mode: String) {
        val calculatedTheme = when (mode) {
            "OFF" -> "LIGHT"
            "AUTO" -> "SYSTEM"
            else -> if (_settingsFlow.value.pureBlack) "AMOLED" else "OBSIDIAN"
        }
        prefs.edit()
            .putString("dark_mode", mode)
            .putString("theme_mode", calculatedTheme)
            .apply()
        _settingsFlow.update { it.copy(darkMode = mode, themeMode = calculatedTheme) }
    }

    fun updatePureBlack(enabled: Boolean) {
        val calculatedTheme = if (enabled) "AMOLED" else "OBSIDIAN"
        prefs.edit()
            .putBoolean("pure_black", enabled)
            .putString("theme_mode", calculatedTheme)
            .apply()
        _settingsFlow.update { it.copy(pureBlack = enabled, themeMode = calculatedTheme) }
    }

    fun updatePureBlackMiniPlayer(enabled: Boolean) {
        prefs.edit().putBoolean("pure_black_mini_player", enabled).apply()
        _settingsFlow.update { it.copy(pureBlackMiniPlayer = enabled) }
    }

    fun updateDynamicPalette(enabled: Boolean) {
        prefs.edit().putBoolean("is_dynamic_palette", enabled).apply()
        _settingsFlow.update { it.copy(isDynamicPalette = enabled) }
    }

    fun updateThemeMode(theme: String) {
        val (dark, amoled) = when (theme) {
            "LIGHT" -> Pair("OFF", false)
            "SYSTEM" -> Pair("AUTO", false)
            "AMOLED" -> Pair("ON", true)
            else -> Pair("ON", false)
        }
        prefs.edit()
            .putString("theme_mode", theme)
            .putString("dark_mode", dark)
            .putBoolean("pure_black", amoled)
            .apply()
        _settingsFlow.update { it.copy(themeMode = theme, darkMode = dark, pureBlack = amoled) }
    }

    fun updateAccentColor(hex: String) {
        prefs.edit()
            .putString("accent_color_hex", hex)
            .putBoolean("is_dynamic_palette", false)
            .apply()
        _settingsFlow.update { it.copy(accentColorHex = hex, isDynamicPalette = false) }
    }

    fun updateFontScale(scale: Float) {
        prefs.edit().putFloat("font_scale", scale).apply()
        _settingsFlow.update { it.copy(fontScale = scale) }
    }

    fun updateMiniplayerStyle(style: String) {
        prefs.edit().putString("miniplayer_style", style).apply()
        _settingsFlow.update { it.copy(miniplayerStyle = style) }
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

    // App Icon & Branding Updates
    fun updateCustomAppIcon(path: String?, label: String, shape: String) {
        prefs.edit()
            .putString("custom_icon_path", path)
            .putString("custom_icon_label", label)
            .putString("custom_icon_shape", shape)
            .apply()
        _settingsFlow.update { it.copy(customIconPath = path, customIconLabel = label, customIconShape = shape) }
    }

    fun updateSelectedIconPreset(preset: String) {
        prefs.edit().putString("selected_icon_preset", preset).apply()
        _settingsFlow.update { it.copy(selectedIconPreset = preset) }
    }

    fun updateUseCustomIconInApp(enabled: Boolean) {
        prefs.edit().putBoolean("use_custom_icon_in_app", enabled).apply()
        _settingsFlow.update { it.copy(useCustomIconInApp = enabled) }
    }

    // Font & Typography Updates
    fun updateSelectedFont(font: String) {
        prefs.edit().putString("selected_font", font).apply()
        _settingsFlow.update { it.copy(selectedFont = font) }
    }

    fun updateCustomFontPath(path: String?) {
        prefs.edit().putString("custom_font_path", path).apply()
        _settingsFlow.update { it.copy(customFontPath = path) }
    }

    // Audio & Playback Updates
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

    fun updateSeekStep(seconds: Int) {
        prefs.edit().putInt("seek_step_seconds", seconds).apply()
        _settingsFlow.update { it.copy(seekStepSeconds = seconds) }
    }

    fun updateGaplessPlayback(enabled: Boolean) {
        prefs.edit().putBoolean("gapless_playback", enabled).apply()
        _settingsFlow.update { it.copy(gaplessPlayback = enabled) }
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

    fun updateEqualizerPreset(preset: String) {
        prefs.edit().putString("equalizer_preset", preset).apply()
        _settingsFlow.update { it.copy(equalizerPreset = preset) }
    }

    fun updateStopOnTaskRemoved(enabled: Boolean) {
        prefs.edit().putBoolean("stop_on_task_removed", enabled).apply()
        _settingsFlow.update { it.copy(stopOnTaskRemoved = enabled) }
    }

    // AI Hub Updates
    fun updateAiTranslation(enabled: Boolean) {
        prefs.edit().putBoolean("ai_translation_enabled", enabled).apply()
        _settingsFlow.update { it.copy(aiTranslationEnabled = enabled) }
    }

    fun updateAiRomanization(enabled: Boolean) {
        prefs.edit().putBoolean("ai_romanization_enabled", enabled).apply()
        _settingsFlow.update { it.copy(aiRomanizationEnabled = enabled) }
    }

    fun updateAiProvider(provider: String) {
        prefs.edit().putString("ai_provider", provider).apply()
        _settingsFlow.update { it.copy(aiProvider = provider) }
    }

    fun updateAiTargetLanguage(lang: String) {
        prefs.edit().putString("ai_target_language", lang).apply()
        _settingsFlow.update { it.copy(aiTargetLanguage = lang) }
    }

    fun updateAiApiKey(key: String) {
        prefs.edit().putString("ai_api_key", key).apply()
        _settingsFlow.update { it.copy(aiApiKey = key) }
    }

    // Listen Together Updates
    fun updateListenTogetherRoomCode(code: String) {
        prefs.edit().putString("listen_together_room_code", code).apply()
        _settingsFlow.update { it.copy(listenTogetherRoomCode = code) }
    }

    fun updateListenTogetherSyncMode(mode: String) {
        prefs.edit().putString("listen_together_sync_mode", mode).apply()
        _settingsFlow.update { it.copy(listenTogetherSyncMode = mode) }
    }

    // Content Updates
    fun updateContentCountry(country: String) {
        prefs.edit().putString("content_country", country).apply()
        _settingsFlow.update { it.copy(contentCountry = country) }
    }

    fun updateContentLanguage(language: String) {
        prefs.edit().putString("content_language", language).apply()
        _settingsFlow.update { it.copy(contentLanguage = language) }
    }

    fun updateFilterExplicit(filter: Boolean) {
        prefs.edit().putBoolean("filter_explicit", filter).apply()
        _settingsFlow.update { it.copy(filterExplicit = filter) }
    }

    fun updateDiscoveryFeedSource(source: String) {
        prefs.edit().putString("discovery_feed_source", source).apply()
        _settingsFlow.update { it.copy(discoveryFeedSource = source) }
    }

    fun updateLyricsProviderPriority(provider: String) {
        prefs.edit().putString("lyrics_provider_priority", provider).apply()
        _settingsFlow.update { it.copy(lyricsProviderPriority = provider) }
    }

    // Privacy Updates
    fun updatePauseHistory(pause: Boolean) {
        prefs.edit().putBoolean("pause_history", pause).apply()
        _settingsFlow.update { it.copy(pauseHistory = pause) }
    }

    fun updatePauseSearchHistory(pause: Boolean) {
        prefs.edit().putBoolean("pause_search_history", pause).apply()
        _settingsFlow.update { it.copy(pauseSearchHistory = pause) }
    }

    fun updateAnonymousDiagnostics(enabled: Boolean) {
        prefs.edit().putBoolean("anonymous_diagnostics", enabled).apply()
        _settingsFlow.update { it.copy(anonymousDiagnostics = enabled) }
    }

    // Backup Updates
    fun updateAutoBackupSchedule(schedule: String) {
        prefs.edit().putString("auto_backup_schedule", schedule).apply()
        _settingsFlow.update { it.copy(autoBackupSchedule = schedule) }
    }
}
