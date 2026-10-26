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
    val miniplayerBackgroundStyle: String = "LIQUID_GLASS", // LIQUID_GLASS, GLOW_ANIMATED, ACRYLIC_BLUR, SOLID_ELEVATED, DEFAULT
    val miniplayerProgressStyle: String = "WAVE",  // WAVE, LINEAR, NEON_TRACK, HIDDEN
    val miniplayerGlowIntensity: Float = 0.8f,     // 0.0f .. 1.0f
    val navBarPillCompact: Boolean = false,        // false = Stadium Full Bar, true = Compact Dynamic Capsule
    val navBarGlassOpacity: Float = 0.85f,         // 0.40f .. 1.00f
    val navBarAutoHideOnScroll: Boolean = true,    // Auto-hide navigation on feed scroll
    val navBarBlurRadius: Float = 24.0f,           // 10dp .. 40dp
    val playerStyle: String = "APPLE_MUSIC",       // APPLE_MUSIC (Permanent)
    val appleMusicPlayerEnabled: Boolean = true,   // Apple Music inspired fullscreen player layout (Permanent)
    val hideVolumeSlider: Boolean = false,         // Hide volume slider on Apple Music player
    val sliderStyle: String = "SQUIGGLY",          // BAR, WAVY, SLIM, SQUIGGLY
    val wavyPlayPause: Boolean = true,             // 9-sided rotating wavy cookie play/pause button
    val enableSliderHaptics: Boolean = true,       // Haptic ticks on seek scrub
    val enableSwipeToChangeSong: Boolean = true,   // Swipe left/right on artwork or miniplayer to change song
    val rotatingThumbnail: Boolean = false,        // Rotating turntable vinyl animation on player artwork
    val canvasThumbnailAnimation: Boolean = true,  // Animated looping video canvases for songs
    val lyricsAnimation: String = "ALPHABET_V2",   // ALPHABET_V2 (Smooth Letter flow), WORD_BY_WORD
    val lyricsPosition: String = "LEFT",           // LEFT, CENTER, RIGHT
    val lyricsAnimationStyle: String = "ECHOMUSIC_1", // NONE, FADE, GLOW, SLIDE, KARAOKE, APPLE, APPLE_V2, ECHOMUSIC_1, LYRICS_V2, METRO_LYRICS
    val appleMusicLyricsBlur: Boolean = false,     // Apple Music depth-of-field progressive blur on inactive lines
    val lyricsStandardBlur: Boolean = false,       // Standard uniform blur on inactive lines
    val lyricsTextSize: Float = 24.0f,             // 16f .. 36f sp
    val lyricsLineSpacing: Float = 1.3f,           // 1.0f .. 4.0f
    val lyricsClick: Boolean = true,               // Tap line to seek
    val lyricsScroll: Boolean = true,              // Auto-scroll active line to optical center
    val swipeLyrics: Boolean = true,               // Swipe left/right on lyrics header to change track
    val enableLyricsThumbnailPlayPause: Boolean = true, // Play/pause overlay on mini thumbnail in lyrics header
    val hideStatusBarOnFullscreen: Boolean = false,// Hide status bar in full-screen lyrics
    val lyricsGlowEffect: Boolean = false,         // Neon ambient glow bloom around active lyric line
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
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: String = "FLAT",          // FLAT, BASS_BOOST, VOCAL, POP, ROCK, ACOUSTIC, etc.
    val equalizerBassBoost: Int = 0,               // 0 to 1000
    val equalizerVirtualizer: Int = 0,             // 0 to 1000
    val equalizerBands: String = "0,0,0,0,0,0,0,0,0,0", // 10 band gains in dB
    val equalizerMode: Int = 0,                    // 0 = Simple (3-band), 1 = Advanced (10-band)
    val stopOnTaskRemoved: Boolean = true,

    // EchoMusic Player and Audio Feature Suite
    // Group 1: Data Saver
    val dataSaverEnabled: Boolean = false,

    // Group 2: Player & Audio Engine
    val showSeekButtons: Boolean = false,
    val audioQualityWifi: String = "AUTO",          // AUTO, HIGH, MEDIUM, LOW
    val audioQualityCellular: String = "MEDIUM",    // AUTO, HIGH, MEDIUM, LOW
    val downloadQuality: String = "YOUTUBE",        // YOUTUBE
    val downloadWithMetadata: Boolean = true,
    val downloadOnWifiOnly: Boolean = false,
    val playbackEngine: String = "POTOKEN",         // POTOKEN, BRAVEPIPE, AUTO
    val crossfadeEnabled: Boolean = false,
    val crossfadeDuration: Float = 5.0f,            // 1f .. 15f
    val crossfadeGapless: Boolean = true,
    val automixCrossfade: Boolean = false,
    val automixDebugOverlay: Boolean = false,
    val enableBitPerfectUsbDac: Boolean = false,
    val usbDacVolumeMode: String = "PURE_BIT_PERFECT", // PURE_BIT_PERFECT, SOFTWARE_SCALED
    val historyDuration: Float = 30.0f,             // 10s .. 60s
    val skipSilenceInstant: Boolean = false,
    val audioNormalization: Boolean = true,
    val audioLoudnessPreset: String = "NORMAL",      // QUIET, NORMAL, LOUD, AGGRESSIVE
    val varispeed: Boolean = false,
    val spatialAudio: Boolean = false,
    val audioOffload: Boolean = false,
    val preloadNextSongEnabled: Boolean = true,
    val preloadNextSongLimit: Int = 10,             // 1 .. 20
    val preloadLyricsEnabled: Boolean = true,
    val enableGoogleCast: Boolean = true,
    val seekExtraSeconds: Boolean = false,          // Progressive seek (+5s incrementally)

    // Group 3: Queue Management
    val persistentQueue: Boolean = true,
    val autoLoadMore: Boolean = true,
    val autoDownloadOnLike: Boolean = false,
    val similarContent: Boolean = true,
    val persistentShuffleAcrossQueues: Boolean = false,
    val rememberShuffleAndRepeat: Boolean = true,
    val shufflePlaylistFirst: Boolean = false,
    val preventDuplicateTracksInQueue: Boolean = false,
    val autoSkipNextOnError: Boolean = false,

    // Group 4: Misc & System Integration
    val stopMusicOnTaskClear: Boolean = true,
    val pauseOnMute: Boolean = false,
    val resumeOnBluetoothConnect: Boolean = false,
    val autoplayOnAutomotiveConnect: Boolean = false,
    val keepScreenOn: Boolean = false,
    val enableExportAsMp3: Boolean = false,

    // AI Hub
    val aiTranslationEnabled: Boolean = false,
    val aiRomanizationEnabled: Boolean = false,
    val aiProvider: String = "GEMINI",             // GEMINI (Google 2.0 Flash), OPENROUTER, BUILTIN
    val aiTargetLanguage: String = "English",      // English, Spanish, French, Japanese, Hindi, German
    val aiApiKey: String = "",
    val geminiApiKey: String = "",

    // Listen Together
    val listenTogetherRoomCode: String = "",
    val listenTogetherSyncMode: String = "HOST_SYNC", // HOST_SYNC, COLLABORATIVE
    val listenTogetherServerUrl: String = "wss://metroserverx.meowery.eu/ws",
    val listenTogetherUsername: String = "",
    val listenTogetherAutoApproval: Boolean = false,
    val listenTogetherSyncVolume: Boolean = true,
    val listenTogetherSmartResync: Boolean = true,
    val listenTogetherSessionToken: String = "",
    val listenTogetherSessionTimestamp: Long = 0L,
    val listenTogetherBlockedUsers: String = "",

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
    val customFontPath: String? = null,

    // Liquid Glass & Optics (Permanent)
    val liquidGlassGlobalEnabled: Boolean = true,
    val liquidGlassVibrancy: Float = 1.0f,
    val liquidGlassBlurRadius: Float = 8.0f,
    val liquidGlassLensHeight: Float = 0.5f,
    val liquidGlassLensAmount: Float = 0.5f,
    val liquidGlassChromaticAberration: Boolean = true,
    val liquidGlassDepthEffect: Boolean = true,
    val liquidGlassSurfaceTintColorInt: Int = 0, // 0 = adaptive
    val liquidGlassSurfaceOpacity: Float = 0.4f,
    val liquidGlassTextColorInt: Int = 0, // 0 = adaptive
    val liquidGlassPlayerEnabled: Boolean = true,
    val liquidGlassMiniPlayerEnabled: Boolean = true,
    val liquidGlassNavBarEnabled: Boolean = true,
    val useFloatingNavBar: Boolean = true,
    val enableHighRefreshRate: Boolean = true
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
            miniplayerBackgroundStyle = prefs.getString("miniplayer_background_style", "LIQUID_GLASS") ?: "LIQUID_GLASS",
            miniplayerProgressStyle = prefs.getString("miniplayer_progress_style", "WAVE") ?: "WAVE",
            miniplayerGlowIntensity = prefs.getFloat("miniplayer_glow_intensity", 0.8f),
            navBarPillCompact = prefs.getBoolean("navbar_pill_compact", false),
            navBarGlassOpacity = prefs.getFloat("navbar_glass_opacity", 0.85f),
            navBarAutoHideOnScroll = prefs.getBoolean("navbar_autohide_on_scroll", true),
            navBarBlurRadius = prefs.getFloat("navbar_blur_radius", 24.0f),
            playerStyle = "APPLE_MUSIC",
            appleMusicPlayerEnabled = true,
            hideVolumeSlider = prefs.getBoolean("hide_volume_slider", false),
            sliderStyle = prefs.getString("slider_style", "SQUIGGLY") ?: "SQUIGGLY",
            wavyPlayPause = prefs.getBoolean("wavy_play_pause", true),
            enableSliderHaptics = prefs.getBoolean("enable_slider_haptics", true),
            enableSwipeToChangeSong = prefs.getBoolean("enable_swipe_to_change_song", true),
            rotatingThumbnail = prefs.getBoolean("rotating_thumbnail", false),
            canvasThumbnailAnimation = prefs.getBoolean("canvas_thumbnail_animation", true),
            lyricsAnimation = prefs.getString("lyrics_animation", "ALPHABET_V2") ?: "ALPHABET_V2",
            lyricsPosition = prefs.getString("lyrics_position", "LEFT") ?: "LEFT",
            lyricsAnimationStyle = prefs.getString("lyrics_animation_style", "ECHOMUSIC_1") ?: "ECHOMUSIC_1",
            appleMusicLyricsBlur = prefs.getBoolean("apple_music_lyrics_blur", false),
            lyricsStandardBlur = prefs.getBoolean("lyrics_standard_blur", false),
            lyricsTextSize = prefs.getFloat("lyrics_text_size", 24.0f),
            lyricsLineSpacing = prefs.getFloat("lyrics_line_spacing", 1.3f),
            lyricsClick = prefs.getBoolean("lyrics_click", true),
            lyricsScroll = prefs.getBoolean("lyrics_scroll", true),
            swipeLyrics = prefs.getBoolean("swipe_lyrics", true),
            enableLyricsThumbnailPlayPause = prefs.getBoolean("enable_lyrics_thumbnail_play_pause", true),
            hideStatusBarOnFullscreen = prefs.getBoolean("hide_status_bar_on_fullscreen", false),
            lyricsGlowEffect = prefs.getBoolean("lyrics_glow_effect", false),
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
            equalizerEnabled = prefs.getBoolean("equalizer_enabled", false),
            equalizerPreset = prefs.getString("equalizer_preset", "FLAT") ?: "FLAT",
            equalizerBassBoost = prefs.getInt("equalizer_bass_boost", 0),
            equalizerVirtualizer = prefs.getInt("equalizer_virtualizer", 0),
            equalizerBands = prefs.getString("equalizer_bands", "0,0,0,0,0,0,0,0,0,0") ?: "0,0,0,0,0,0,0,0,0,0",
            equalizerMode = prefs.getInt("equalizer_mode", 0),
            stopOnTaskRemoved = prefs.getBoolean("stop_on_task_removed", true),

            // EchoMusic Player and Audio Preferences Loading
            dataSaverEnabled = prefs.getBoolean("data_saver_enabled", false),
            showSeekButtons = prefs.getBoolean("show_seek_buttons", false),
            audioQualityWifi = prefs.getString("audio_quality_wifi", "AUTO") ?: "AUTO",
            audioQualityCellular = prefs.getString("audio_quality_cellular", "MEDIUM") ?: "MEDIUM",
            downloadQuality = prefs.getString("download_quality", "YOUTUBE") ?: "YOUTUBE",
            downloadWithMetadata = prefs.getBoolean("download_with_metadata", true),
            downloadOnWifiOnly = prefs.getBoolean("download_on_wifi_only", false),
            playbackEngine = prefs.getString("playback_engine", "POTOKEN") ?: "POTOKEN",
            crossfadeEnabled = prefs.getBoolean("crossfade_enabled", false),
            crossfadeDuration = prefs.getFloat("crossfade_duration", 5.0f),
            crossfadeGapless = prefs.getBoolean("crossfade_gapless", true),
            automixCrossfade = prefs.getBoolean("automix_crossfade", false),
            automixDebugOverlay = prefs.getBoolean("automix_debug_overlay", false),
            enableBitPerfectUsbDac = prefs.getBoolean("enable_bit_perfect_usb_dac", false),
            usbDacVolumeMode = prefs.getString("usb_dac_volume_mode", "PURE_BIT_PERFECT") ?: "PURE_BIT_PERFECT",
            historyDuration = prefs.getFloat("history_duration", 30.0f),
            skipSilenceInstant = prefs.getBoolean("skip_silence_instant", false),
            audioNormalization = prefs.getBoolean("audio_normalization", true),
            audioLoudnessPreset = prefs.getString("audio_loudness_preset", "NORMAL") ?: "NORMAL",
            varispeed = prefs.getBoolean("varispeed", false),
            spatialAudio = prefs.getBoolean("spatial_audio", false),
            audioOffload = prefs.getBoolean("audio_offload", false),
            preloadNextSongEnabled = prefs.getBoolean("preload_next_song_enabled", true),
            preloadNextSongLimit = prefs.getInt("preload_next_song_limit", 10),
            preloadLyricsEnabled = prefs.getBoolean("preload_lyrics_enabled", true),
            enableGoogleCast = prefs.getBoolean("enable_google_cast", true),
            seekExtraSeconds = prefs.getBoolean("seek_extra_seconds", false),

            persistentQueue = prefs.getBoolean("persistent_queue", true),
            autoLoadMore = prefs.getBoolean("auto_load_more", true),
            autoDownloadOnLike = prefs.getBoolean("auto_download_on_like", false),
            similarContent = prefs.getBoolean("similar_content", true),
            persistentShuffleAcrossQueues = prefs.getBoolean("persistent_shuffle_across_queues", false),
            rememberShuffleAndRepeat = prefs.getBoolean("remember_shuffle_and_repeat", true),
            shufflePlaylistFirst = prefs.getBoolean("shuffle_playlist_first", false),
            preventDuplicateTracksInQueue = prefs.getBoolean("prevent_duplicate_tracks_in_queue", false),
            autoSkipNextOnError = prefs.getBoolean("auto_skip_next_on_error", false),

            stopMusicOnTaskClear = prefs.getBoolean("stop_music_on_task_clear", true),
            pauseOnMute = prefs.getBoolean("pause_on_mute", false),
            resumeOnBluetoothConnect = prefs.getBoolean("resume_on_bluetooth_connect", false),
            autoplayOnAutomotiveConnect = prefs.getBoolean("autoplay_on_automotive_connect", false),
            keepScreenOn = prefs.getBoolean("keep_screen_on", false),
            enableExportAsMp3 = prefs.getBoolean("enable_export_as_mp3", false),

            aiTranslationEnabled = prefs.getBoolean("ai_translation_enabled", false),
            aiRomanizationEnabled = prefs.getBoolean("ai_romanization_enabled", false),
            aiProvider = prefs.getString("ai_provider", "GEMINI") ?: "GEMINI",
            aiTargetLanguage = prefs.getString("ai_target_language", "English") ?: "English",
            aiApiKey = prefs.getString("ai_api_key", "") ?: "",
            geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",

            listenTogetherRoomCode = prefs.getString("listen_together_room_code", "") ?: "",
            listenTogetherSyncMode = prefs.getString("listen_together_sync_mode", "HOST_SYNC") ?: "HOST_SYNC",
            listenTogetherServerUrl = prefs.getString("listen_together_server_url", "wss://metroserverx.meowery.eu/ws") ?: "wss://metroserverx.meowery.eu/ws",
            listenTogetherUsername = prefs.getString("listen_together_username", "") ?: "",
            listenTogetherAutoApproval = prefs.getBoolean("listen_together_auto_approval", false),
            listenTogetherSyncVolume = prefs.getBoolean("listen_together_sync_volume", true),
            listenTogetherSmartResync = prefs.getBoolean("listen_together_smart_resync", true),
            listenTogetherSessionToken = prefs.getString("listen_together_session_token", "") ?: "",
            listenTogetherSessionTimestamp = prefs.getLong("listen_together_session_timestamp", 0L),
            listenTogetherBlockedUsers = prefs.getString("listen_together_blocked_users", "") ?: "",

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
            customFontPath = prefs.getString("custom_font_path", null),

            liquidGlassGlobalEnabled = prefs.getBoolean("liquid_glass_global_enabled", true),
            liquidGlassVibrancy = prefs.getFloat("liquid_glass_vibrancy", 1.0f),
            liquidGlassBlurRadius = prefs.getFloat("liquid_glass_blur_radius", 8.0f),
            liquidGlassLensHeight = prefs.getFloat("liquid_glass_lens_height", 0.5f),
            liquidGlassLensAmount = prefs.getFloat("liquid_glass_lens_amount", 0.5f),
            liquidGlassChromaticAberration = prefs.getBoolean("liquid_glass_chromatic_aberration", true),
            liquidGlassDepthEffect = prefs.getBoolean("liquid_glass_depth_effect", true),
            liquidGlassSurfaceTintColorInt = prefs.getInt("liquid_glass_surface_tint_color", 0),
            liquidGlassSurfaceOpacity = prefs.getFloat("liquid_glass_surface_opacity", 0.4f),
            liquidGlassTextColorInt = prefs.getInt("liquid_glass_text_color", 0),
            liquidGlassPlayerEnabled = prefs.getBoolean("liquid_glass_player_enabled", true),
            liquidGlassMiniPlayerEnabled = prefs.getBoolean("liquid_glass_mini_player_enabled", true),
            liquidGlassNavBarEnabled = prefs.getBoolean("liquid_glass_nav_bar_enabled", true),
            useFloatingNavBar = prefs.getBoolean("use_floating_nav_bar", true),
            enableHighRefreshRate = prefs.getBoolean("enable_high_refresh_rate", true)
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

    fun updateMiniplayerBackgroundStyle(style: String) {
        prefs.edit().putString("miniplayer_background_style", style).apply()
        _settingsFlow.update { it.copy(miniplayerBackgroundStyle = style) }
    }

    fun updateMiniplayerProgressStyle(style: String) {
        prefs.edit().putString("miniplayer_progress_style", style).apply()
        _settingsFlow.update { it.copy(miniplayerProgressStyle = style) }
    }

    fun updateMiniplayerGlowIntensity(intensity: Float) {
        prefs.edit().putFloat("miniplayer_glow_intensity", intensity).apply()
        _settingsFlow.update { it.copy(miniplayerGlowIntensity = intensity) }
    }

    fun updateNavBarPillCompact(compact: Boolean) {
        prefs.edit().putBoolean("navbar_pill_compact", compact).apply()
        _settingsFlow.update { it.copy(navBarPillCompact = compact) }
    }

    fun updateNavBarGlassOpacity(opacity: Float) {
        prefs.edit().putFloat("navbar_glass_opacity", opacity).apply()
        _settingsFlow.update { it.copy(navBarGlassOpacity = opacity) }
    }

    fun updateNavBarAutoHideOnScroll(autoHide: Boolean) {
        prefs.edit().putBoolean("navbar_autohide_on_scroll", autoHide).apply()
        _settingsFlow.update { it.copy(navBarAutoHideOnScroll = autoHide) }
    }

    fun updateNavBarBlurRadius(radius: Float) {
        prefs.edit().putFloat("navbar_blur_radius", radius).apply()
        _settingsFlow.update { it.copy(navBarBlurRadius = radius) }
    }

    fun updatePlayerStyle(style: String) {
        prefs.edit().putString("player_style", "APPLE_MUSIC").apply()
        _settingsFlow.update { it.copy(playerStyle = "APPLE_MUSIC") }
    }

    fun updateAppleMusicPlayer(enabled: Boolean = true) {
        // Apple Music player is locked permanent
        prefs.edit().putBoolean("apple_music_player_enabled", true).putString("player_style", "APPLE_MUSIC").apply()
        _settingsFlow.update { it.copy(appleMusicPlayerEnabled = true, playerStyle = "APPLE_MUSIC") }
    }

    fun updateHideVolumeSlider(hidden: Boolean) {
        prefs.edit().putBoolean("hide_volume_slider", hidden).apply()
        _settingsFlow.update { it.copy(hideVolumeSlider = hidden) }
    }

    fun updateSliderStyle(style: String) {
        prefs.edit().putString("slider_style", style).apply()
        _settingsFlow.update { it.copy(sliderStyle = style) }
    }

    fun updateWavyPlayPause(enabled: Boolean) {
        prefs.edit().putBoolean("wavy_play_pause", enabled).apply()
        _settingsFlow.update { it.copy(wavyPlayPause = enabled) }
    }

    fun updateEnableSliderHaptics(enabled: Boolean) {
        prefs.edit().putBoolean("enable_slider_haptics", enabled).apply()
        _settingsFlow.update { it.copy(enableSliderHaptics = enabled) }
    }

    fun updateEnableSwipeToChangeSong(enabled: Boolean) {
        prefs.edit().putBoolean("enable_swipe_to_change_song", enabled).apply()
        _settingsFlow.update { it.copy(enableSwipeToChangeSong = enabled) }
    }

    fun updateRotatingThumbnail(enabled: Boolean) {
        prefs.edit().putBoolean("rotating_thumbnail", enabled).apply()
        _settingsFlow.update { it.copy(rotatingThumbnail = enabled) }
    }

    fun updateCanvasThumbnailAnimation(enabled: Boolean) {
        prefs.edit().putBoolean("canvas_thumbnail_animation", enabled).apply()
        _settingsFlow.update { it.copy(canvasThumbnailAnimation = enabled) }
    }

    fun updateLyricsAnimation(style: String) {
        prefs.edit().putString("lyrics_animation", style).apply()
        _settingsFlow.update { it.copy(lyricsAnimation = style) }
    }

    fun updateLyricsPosition(position: String) {
        prefs.edit().putString("lyrics_position", position).apply()
        _settingsFlow.update { it.copy(lyricsPosition = position) }
    }

    fun updateLyricsAnimationStyle(style: String) {
        prefs.edit().putString("lyrics_animation_style", style).apply()
        _settingsFlow.update { it.copy(lyricsAnimationStyle = style) }
    }

    fun updateAppleMusicLyricsBlur(enabled: Boolean) {
        prefs.edit().putBoolean("apple_music_lyrics_blur", enabled).apply()
        _settingsFlow.update { it.copy(appleMusicLyricsBlur = enabled) }
    }

    fun updateLyricsStandardBlur(enabled: Boolean) {
        prefs.edit().putBoolean("lyrics_standard_blur", enabled).apply()
        _settingsFlow.update { it.copy(lyricsStandardBlur = enabled) }
    }

    fun updateLyricsTextSize(size: Float) {
        prefs.edit().putFloat("lyrics_text_size", size).apply()
        _settingsFlow.update { it.copy(lyricsTextSize = size) }
    }

    fun updateLyricsLineSpacing(spacing: Float) {
        prefs.edit().putFloat("lyrics_line_spacing", spacing).apply()
        _settingsFlow.update { it.copy(lyricsLineSpacing = spacing) }
    }

    fun updateLyricsClick(enabled: Boolean) {
        prefs.edit().putBoolean("lyrics_click", enabled).apply()
        _settingsFlow.update { it.copy(lyricsClick = enabled) }
    }

    fun updateLyricsScroll(enabled: Boolean) {
        prefs.edit().putBoolean("lyrics_scroll", enabled).apply()
        _settingsFlow.update { it.copy(lyricsScroll = enabled) }
    }

    fun updateSwipeLyrics(enabled: Boolean) {
        prefs.edit().putBoolean("swipe_lyrics", enabled).apply()
        _settingsFlow.update { it.copy(swipeLyrics = enabled) }
    }

    fun updateEnableLyricsThumbnailPlayPause(enabled: Boolean) {
        prefs.edit().putBoolean("enable_lyrics_thumbnail_play_pause", enabled).apply()
        _settingsFlow.update { it.copy(enableLyricsThumbnailPlayPause = enabled) }
    }

    fun updateHideStatusBarOnFullscreen(enabled: Boolean) {
        prefs.edit().putBoolean("hide_status_bar_on_fullscreen", enabled).apply()
        _settingsFlow.update { it.copy(hideStatusBarOnFullscreen = enabled) }
    }

    fun updateLyricsGlowEffect(enabled: Boolean) {
        prefs.edit().putBoolean("lyrics_glow_effect", enabled).apply()
        _settingsFlow.update { it.copy(lyricsGlowEffect = enabled) }
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

    fun updateEqualizerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("equalizer_enabled", enabled).apply()
        _settingsFlow.update { it.copy(equalizerEnabled = enabled) }
    }

    fun updateEqualizerPreset(preset: String) {
        prefs.edit().putString("equalizer_preset", preset).apply()
        _settingsFlow.update { it.copy(equalizerPreset = preset) }
    }

    fun updateEqualizerBassBoost(boost: Int) {
        prefs.edit().putInt("equalizer_bass_boost", boost).apply()
        _settingsFlow.update { it.copy(equalizerBassBoost = boost) }
    }

    fun updateEqualizerVirtualizer(virtualizer: Int) {
        prefs.edit().putInt("equalizer_virtualizer", virtualizer).apply()
        _settingsFlow.update { it.copy(equalizerVirtualizer = virtualizer) }
    }

    fun updateEqualizerBands(bands: String) {
        prefs.edit().putString("equalizer_bands", bands).apply()
        _settingsFlow.update { it.copy(equalizerBands = bands) }
    }

    fun updateEqualizerMode(mode: Int) {
        prefs.edit().putInt("equalizer_mode", mode).apply()
        _settingsFlow.update { it.copy(equalizerMode = mode) }
    }

    fun updateStopOnTaskRemoved(enabled: Boolean) {
        prefs.edit().putBoolean("stop_on_task_removed", enabled).apply()
        _settingsFlow.update { it.copy(stopOnTaskRemoved = enabled) }
    }

    // EchoMusic Player and Audio Feature Suite Updaters
    // Group 1: Data Saver
    fun updateDataSaverEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("data_saver_enabled", enabled).apply()
        _settingsFlow.update { it.copy(dataSaverEnabled = enabled) }
    }

    // Group 2: Player & Audio Engine
    fun updateShowSeekButtons(enabled: Boolean) {
        prefs.edit().putBoolean("show_seek_buttons", enabled).apply()
        _settingsFlow.update { it.copy(showSeekButtons = enabled) }
    }

    fun updateAudioQualityWifi(quality: String) {
        prefs.edit().putString("audio_quality_wifi", quality).apply()
        _settingsFlow.update { it.copy(audioQualityWifi = quality) }
    }

    fun updateAudioQualityCellular(quality: String) {
        prefs.edit().putString("audio_quality_cellular", quality).apply()
        _settingsFlow.update { it.copy(audioQualityCellular = quality) }
    }

    fun updateDownloadQualityProfile(quality: String) {
        prefs.edit().putString("download_quality", quality).apply()
        _settingsFlow.update { it.copy(downloadQuality = quality) }
    }

    fun updateDownloadWithMetadata(enabled: Boolean) {
        prefs.edit().putBoolean("download_with_metadata", enabled).apply()
        _settingsFlow.update { it.copy(downloadWithMetadata = enabled) }
    }

    fun updateDownloadOnWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean("download_on_wifi_only", enabled).apply()
        _settingsFlow.update { it.copy(downloadOnWifiOnly = enabled) }
    }

    fun updatePlaybackEngine(engine: String) {
        prefs.edit().putString("playback_engine", engine).apply()
        _settingsFlow.update { it.copy(playbackEngine = engine) }
    }

    fun updateCrossfadeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("crossfade_enabled", enabled).apply()
        _settingsFlow.update { it.copy(crossfadeEnabled = enabled) }
    }

    fun updateCrossfadeDuration(duration: Float) {
        prefs.edit().putFloat("crossfade_duration", duration).apply()
        _settingsFlow.update { it.copy(crossfadeDuration = duration) }
    }

    fun updateCrossfadeGapless(enabled: Boolean) {
        prefs.edit().putBoolean("crossfade_gapless", enabled).apply()
        _settingsFlow.update { it.copy(crossfadeGapless = enabled) }
    }

    fun updateAutomixCrossfade(enabled: Boolean) {
        prefs.edit().putBoolean("automix_crossfade", enabled).apply()
        _settingsFlow.update { it.copy(automixCrossfade = enabled) }
    }

    fun updateAutomixDebugOverlay(enabled: Boolean) {
        prefs.edit().putBoolean("automix_debug_overlay", enabled).apply()
        _settingsFlow.update { it.copy(automixDebugOverlay = enabled) }
    }

    fun updateEnableBitPerfectUsbDac(enabled: Boolean) {
        prefs.edit().putBoolean("enable_bit_perfect_usb_dac", enabled).apply()
        _settingsFlow.update { it.copy(enableBitPerfectUsbDac = enabled) }
    }

    fun updateUsbDacVolumeMode(mode: String) {
        prefs.edit().putString("usb_dac_volume_mode", mode).apply()
        _settingsFlow.update { it.copy(usbDacVolumeMode = mode) }
    }

    fun updateHistoryDuration(duration: Float) {
        prefs.edit().putFloat("history_duration", duration).apply()
        _settingsFlow.update { it.copy(historyDuration = duration) }
    }

    fun updateSkipSilenceInstant(enabled: Boolean) {
        prefs.edit().putBoolean("skip_silence_instant", enabled).apply()
        _settingsFlow.update { it.copy(skipSilenceInstant = enabled) }
    }

    fun updateAudioNormalization(enabled: Boolean) {
        prefs.edit().putBoolean("audio_normalization", enabled).apply()
        _settingsFlow.update { it.copy(audioNormalization = enabled) }
    }

    fun updateAudioLoudnessPreset(preset: String) {
        prefs.edit().putString("audio_loudness_preset", preset).apply()
        _settingsFlow.update { it.copy(audioLoudnessPreset = preset) }
    }

    fun updateVarispeed(enabled: Boolean) {
        prefs.edit().putBoolean("varispeed", enabled).apply()
        _settingsFlow.update { it.copy(varispeed = enabled) }
    }

    fun updateSpatialAudio(enabled: Boolean) {
        prefs.edit().putBoolean("spatial_audio", enabled).apply()
        _settingsFlow.update { it.copy(spatialAudio = enabled) }
    }

    fun updateAudioOffload(enabled: Boolean) {
        prefs.edit().putBoolean("audio_offload", enabled).apply()
        _settingsFlow.update { it.copy(audioOffload = enabled) }
    }

    fun updatePreloadNextSongEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("preload_next_song_enabled", enabled).apply()
        _settingsFlow.update { it.copy(preloadNextSongEnabled = enabled) }
    }

    fun updatePreloadNextSongLimit(limit: Int) {
        prefs.edit().putInt("preload_next_song_limit", limit).apply()
        _settingsFlow.update { it.copy(preloadNextSongLimit = limit) }
    }

    fun updatePreloadLyricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("preload_lyrics_enabled", enabled).apply()
        _settingsFlow.update { it.copy(preloadLyricsEnabled = enabled) }
    }

    fun updateEnableGoogleCast(enabled: Boolean) {
        prefs.edit().putBoolean("enable_google_cast", enabled).apply()
        _settingsFlow.update { it.copy(enableGoogleCast = enabled) }
    }

    fun updateSeekExtraSeconds(enabled: Boolean) {
        prefs.edit().putBoolean("seek_extra_seconds", enabled).apply()
        _settingsFlow.update { it.copy(seekExtraSeconds = enabled) }
    }

    // Group 3: Queue Management
    fun updatePersistentQueue(enabled: Boolean) {
        prefs.edit().putBoolean("persistent_queue", enabled).apply()
        _settingsFlow.update { it.copy(persistentQueue = enabled) }
    }

    fun updateAutoLoadMore(enabled: Boolean) {
        prefs.edit().putBoolean("auto_load_more", enabled).apply()
        _settingsFlow.update { it.copy(autoLoadMore = enabled) }
    }

    fun updateAutoDownloadOnLike(enabled: Boolean) {
        prefs.edit().putBoolean("auto_download_on_like", enabled).apply()
        _settingsFlow.update { it.copy(autoDownloadOnLike = enabled) }
    }

    fun updateSimilarContent(enabled: Boolean) {
        prefs.edit().putBoolean("similar_content", enabled).apply()
        _settingsFlow.update { it.copy(similarContent = enabled) }
    }

    fun updatePersistentShuffleAcrossQueues(enabled: Boolean) {
        prefs.edit().putBoolean("persistent_shuffle_across_queues", enabled).apply()
        _settingsFlow.update { it.copy(persistentShuffleAcrossQueues = enabled) }
    }

    fun updateRememberShuffleAndRepeat(enabled: Boolean) {
        prefs.edit().putBoolean("remember_shuffle_and_repeat", enabled).apply()
        _settingsFlow.update { it.copy(rememberShuffleAndRepeat = enabled) }
    }

    fun updateShufflePlaylistFirst(enabled: Boolean) {
        prefs.edit().putBoolean("shuffle_playlist_first", enabled).apply()
        _settingsFlow.update { it.copy(shufflePlaylistFirst = enabled) }
    }

    fun updatePreventDuplicateTracksInQueue(enabled: Boolean) {
        prefs.edit().putBoolean("prevent_duplicate_tracks_in_queue", enabled).apply()
        _settingsFlow.update { it.copy(preventDuplicateTracksInQueue = enabled) }
    }

    fun updateAutoSkipNextOnError(enabled: Boolean) {
        prefs.edit().putBoolean("auto_skip_next_on_error", enabled).apply()
        _settingsFlow.update { it.copy(autoSkipNextOnError = enabled) }
    }

    // Group 4: Misc & System Integration
    fun updateStopMusicOnTaskClear(enabled: Boolean) {
        prefs.edit().putBoolean("stop_music_on_task_clear", enabled).apply()
        _settingsFlow.update { it.copy(stopMusicOnTaskClear = enabled) }
    }

    fun updatePauseOnMute(enabled: Boolean) {
        prefs.edit().putBoolean("pause_on_mute", enabled).apply()
        _settingsFlow.update { it.copy(pauseOnMute = enabled) }
    }

    fun updateResumeOnBluetoothConnect(enabled: Boolean) {
        prefs.edit().putBoolean("resume_on_bluetooth_connect", enabled).apply()
        _settingsFlow.update { it.copy(resumeOnBluetoothConnect = enabled) }
    }

    fun updateAutoplayOnAutomotiveConnect(enabled: Boolean) {
        prefs.edit().putBoolean("autoplay_on_automotive_connect", enabled).apply()
        _settingsFlow.update { it.copy(autoplayOnAutomotiveConnect = enabled) }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean("keep_screen_on", enabled).apply()
        _settingsFlow.update { it.copy(keepScreenOn = enabled) }
    }

    fun updateEnableExportAsMp3(enabled: Boolean) {
        prefs.edit().putBoolean("enable_export_as_mp3", enabled).apply()
        _settingsFlow.update { it.copy(enableExportAsMp3 = enabled) }
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

    fun updateGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
        _settingsFlow.update { it.copy(geminiApiKey = key) }
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

    fun updateListenTogetherServerUrl(url: String) {
        prefs.edit().putString("listen_together_server_url", url).apply()
        _settingsFlow.update { it.copy(listenTogetherServerUrl = url) }
    }

    fun updateListenTogetherUsername(username: String) {
        prefs.edit().putString("listen_together_username", username).apply()
        _settingsFlow.update { it.copy(listenTogetherUsername = username) }
    }

    fun updateListenTogetherAutoApproval(enabled: Boolean) {
        prefs.edit().putBoolean("listen_together_auto_approval", enabled).apply()
        _settingsFlow.update { it.copy(listenTogetherAutoApproval = enabled) }
    }

    fun updateListenTogetherSyncVolume(enabled: Boolean) {
        prefs.edit().putBoolean("listen_together_sync_volume", enabled).apply()
        _settingsFlow.update { it.copy(listenTogetherSyncVolume = enabled) }
    }

    fun updateListenTogetherSmartResync(enabled: Boolean) {
        prefs.edit().putBoolean("listen_together_smart_resync", enabled).apply()
        _settingsFlow.update { it.copy(listenTogetherSmartResync = enabled) }
    }

    fun saveListenTogetherSession(token: String, roomCode: String) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString("listen_together_session_token", token)
            .putString("listen_together_room_code", roomCode)
            .putLong("listen_together_session_timestamp", now)
            .apply()
        _settingsFlow.update {
            it.copy(
                listenTogetherSessionToken = token,
                listenTogetherRoomCode = roomCode,
                listenTogetherSessionTimestamp = now
            )
        }
    }

    fun clearListenTogetherSession() {
        prefs.edit()
            .remove("listen_together_session_token")
            .remove("listen_together_room_code")
            .remove("listen_together_session_timestamp")
            .apply()
        _settingsFlow.update {
            it.copy(
                listenTogetherSessionToken = "",
                listenTogetherRoomCode = "",
                listenTogetherSessionTimestamp = 0L
            )
        }
    }

    fun updateListenTogetherBlockedUsers(blocked: String) {
        prefs.edit().putString("listen_together_blocked_users", blocked).apply()
        _settingsFlow.update { it.copy(listenTogetherBlockedUsers = blocked) }
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

    // Liquid Glass & Optics Updates (Permanent)
    fun updateLiquidGlassGlobalEnabled(enabled: Boolean = true) {
        prefs.edit().putBoolean("liquid_glass_global_enabled", true).apply()
        _settingsFlow.update { it.copy(liquidGlassGlobalEnabled = true) }
    }

    fun updateLiquidGlassVibrancy(vibrancy: Float) {
        prefs.edit().putFloat("liquid_glass_vibrancy", vibrancy).apply()
        _settingsFlow.update { it.copy(liquidGlassVibrancy = vibrancy) }
    }

    fun updateLiquidGlassBlurRadius(radius: Float) {
        prefs.edit().putFloat("liquid_glass_blur_radius", radius).apply()
        _settingsFlow.update { it.copy(liquidGlassBlurRadius = radius) }
    }

    fun updateLiquidGlassLensHeight(height: Float) {
        prefs.edit().putFloat("liquid_glass_lens_height", height).apply()
        _settingsFlow.update { it.copy(liquidGlassLensHeight = height) }
    }

    fun updateLiquidGlassLensAmount(amount: Float) {
        prefs.edit().putFloat("liquid_glass_lens_amount", amount).apply()
        _settingsFlow.update { it.copy(liquidGlassLensAmount = amount) }
    }

    fun updateLiquidGlassChromaticAberration(enabled: Boolean) {
        prefs.edit().putBoolean("liquid_glass_chromatic_aberration", enabled).apply()
        _settingsFlow.update { it.copy(liquidGlassChromaticAberration = enabled) }
    }

    fun updateLiquidGlassDepthEffect(enabled: Boolean) {
        prefs.edit().putBoolean("liquid_glass_depth_effect", enabled).apply()
        _settingsFlow.update { it.copy(liquidGlassDepthEffect = enabled) }
    }

    fun updateLiquidGlassSurfaceTintColorInt(color: Int) {
        prefs.edit().putInt("liquid_glass_surface_tint_color", color).apply()
        _settingsFlow.update { it.copy(liquidGlassSurfaceTintColorInt = color) }
    }

    fun updateLiquidGlassSurfaceOpacity(opacity: Float) {
        prefs.edit().putFloat("liquid_glass_surface_opacity", opacity).apply()
        _settingsFlow.update { it.copy(liquidGlassSurfaceOpacity = opacity) }
    }

    fun updateLiquidGlassTextColorInt(color: Int) {
        prefs.edit().putInt("liquid_glass_text_color", color).apply()
        _settingsFlow.update { it.copy(liquidGlassTextColorInt = color) }
    }

    fun updateLiquidGlassPlayerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("liquid_glass_player_enabled", enabled).apply()
        _settingsFlow.update { it.copy(liquidGlassPlayerEnabled = enabled) }
    }

    fun updateLiquidGlassMiniPlayerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("liquid_glass_mini_player_enabled", enabled).apply()
        _settingsFlow.update { it.copy(liquidGlassMiniPlayerEnabled = enabled) }
    }

    fun updateLiquidGlassNavBarEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("liquid_glass_nav_bar_enabled", enabled).apply()
        _settingsFlow.update { it.copy(liquidGlassNavBarEnabled = enabled) }
    }

    fun updateUseFloatingNavBar(enabled: Boolean) {
        prefs.edit().putBoolean("use_floating_nav_bar", enabled).apply()
        _settingsFlow.update { it.copy(useFloatingNavBar = enabled) }
    }

    fun updateEnableHighRefreshRate(enabled: Boolean) {
        prefs.edit().putBoolean("enable_high_refresh_rate", enabled).apply()
        _settingsFlow.update { it.copy(enableHighRefreshRate = enabled) }
    }

    fun resetLiquidGlassToDefaults() {
        prefs.edit()
            .putFloat("liquid_glass_vibrancy", 1.0f)
            .putFloat("liquid_glass_blur_radius", 8.0f)
            .putFloat("liquid_glass_lens_height", 0.5f)
            .putFloat("liquid_glass_lens_amount", 0.5f)
            .putBoolean("liquid_glass_chromatic_aberration", true)
            .putBoolean("liquid_glass_depth_effect", true)
            .putInt("liquid_glass_surface_tint_color", 0)
            .putFloat("liquid_glass_surface_opacity", 0.4f)
            .putInt("liquid_glass_text_color", 0)
            .apply()
        _settingsFlow.update {
            it.copy(
                liquidGlassVibrancy = 1.0f,
                liquidGlassBlurRadius = 8.0f,
                liquidGlassLensHeight = 0.5f,
                liquidGlassLensAmount = 0.5f,
                liquidGlassChromaticAberration = true,
                liquidGlassDepthEffect = true,
                liquidGlassSurfaceTintColorInt = 0,
                liquidGlassSurfaceOpacity = 0.4f,
                liquidGlassTextColorInt = 0
            )
        }
    }
}
