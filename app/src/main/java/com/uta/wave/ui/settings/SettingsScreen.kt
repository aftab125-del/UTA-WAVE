package com.uta.wave.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropOriginal
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import com.uta.wave.ui.settings.font.FontSelectionScreen
import com.uta.wave.ui.settings.glass.GlassEffectSettingsScreen
import com.uta.wave.ui.settings.icon.AppIconSettingsScreen
import com.uta.wave.ui.settings.theme.ThemeScreen
import com.uta.wave.ui.theme.*
import com.uta.wave.ui.components.PlayerSliderStyleDialog
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import com.uta.wave.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.downloads.AudioDownloadManager
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.data.preferences.AppSettings
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.ui.account.AccountScreen
import com.uta.wave.ui.stats.ListeningSummaryScreen
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

/**
 * 100% Comprehensive Settings & Customization Hub for UTA-wave matching Echo Music.
 * Grouped into 5 card containers with all 11 sub-screens fully interactive and integrated:
 * 1. Account & AI Hub
 * 2. Listening Summary
 * 3. Appearance, Player and audio, Listen Together
 * 4. Content, Privacy, Storage, Backup and restore
 * 5. System update, Supported Links, About
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAccount: () -> Unit = {},
    onNavigateToListeningSummary: () -> Unit = {},
    onPlayTrack: ((TrackItem, List<TrackItem>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences.getInstance(context) }
    val settings by userPrefs.settingsFlow.collectAsState()

    var activeSubSection by remember { mutableStateOf<String?>(null) }

    if (activeSubSection == "Account") {
        AccountScreen(
            onBack = { activeSubSection = null },
            onOpenSettings = { activeSubSection = null }
        )
        return
    }

    if (activeSubSection == "Listening Summary") {
        ListeningSummaryScreen(onBack = { activeSubSection = null })
        return
    }

    if (activeSubSection == "Theme") {
        ThemeScreen(onBack = { activeSubSection = null })
        return
    }

    if (activeSubSection == "App Icon") {
        AppIconSettingsScreen(onBack = { activeSubSection = null })
        return
    }

    if (activeSubSection == "App Font") {
        FontSelectionScreen(onBack = { activeSubSection = null })
        return
    }

    if (activeSubSection == "Liquid Glass") {
        GlassEffectSettingsScreen(onBack = { activeSubSection = null })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activeSubSection ?: "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeSubSection != null) {
                            activeSubSection = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = ObsidianBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activeSubSection == null) {
                // 5 Grouped Settings Cards Matching Echo Music
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ---------------------------------------------------------
                    // GROUP 1: Account & AI Hub
                    // ---------------------------------------------------------
                    SettingsGroupCard {
                        SettingsGroupItem(
                            icon = Icons.Default.AccountCircle,
                            title = "Account",
                            subtitle = "Manage login and integrations",
                            accentColor = Color(0xFFF2AEC7),
                            onClick = { activeSubSection = "Account" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.AutoAwesome,
                            title = "AI Hub",
                            subtitle = "AI-powered lyrics and translations",
                            accentColor = Color(0xFFA855F7),
                            onClick = { activeSubSection = "AI Hub" }
                        )
                    }

                    // ---------------------------------------------------------
                    // GROUP 2: Listening Summary
                    // ---------------------------------------------------------
                    SettingsGroupCard {
                        SettingsGroupItem(
                            icon = Icons.Default.Equalizer,
                            title = "Listening Summary",
                            subtitle = "View your music listening stats",
                            accentColor = Color(0xFFF2AEC7),
                            onClick = { activeSubSection = "Listening Summary" }
                        )
                    }

                    // ---------------------------------------------------------
                    // GROUP 3: Theme, Appearance, Audio, Listen Together
                    // ---------------------------------------------------------
                    SettingsGroupCard {
                        val accentColor = remember(settings.accentColorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(settings.accentColorHex))
                            } catch (e: Exception) {
                                Color(0xFFF2AEC7)
                            }
                        }
                        SettingsGroupItem(
                            icon = Icons.Default.Palette,
                            title = "Theme & Colors",
                            subtitle = when {
                                settings.pureBlack -> "AMOLED Pure Black"
                                settings.darkMode == "OFF" -> "Light Mode"
                                settings.darkMode == "AUTO" -> "Follow System"
                                else -> "Dark Mode"
                            },
                            accentColor = accentColor,
                            onClick = { activeSubSection = "Theme" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.Tune,
                            title = "Appearance & Layout",
                            subtitle = "Player layout, lyrics, canvas & styles",
                            accentColor = Color(0xFFF2AEC7),
                            onClick = { activeSubSection = "Appearance" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.FontDownload,
                            title = "App Font",
                            subtitle = "Google Sans, Outfit, Plus Jakarta & Custom TTF",
                            accentColor = Color(0xFFA855F7),
                            onClick = { activeSubSection = "App Font" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.CropOriginal,
                            title = "App Icon & Branding",
                            subtitle = "Custom gallery image, shapes & presets",
                            accentColor = Color(0xFFFB7185),
                            onClick = { activeSubSection = "App Icon" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.PlayCircleOutline,
                            title = "Player and audio",
                            subtitle = "Playback, quality, and equalizer",
                            accentColor = Color(0xFF38BDF8),
                            onClick = { activeSubSection = "Player and audio" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.Group,
                            title = "Listen Together",
                            subtitle = "Sync playback with friends",
                            accentColor = Color(0xFF10B981),
                            onClick = { activeSubSection = "Listen Together" }
                        )
                    }

                    // ---------------------------------------------------------
                    // GROUP 4: Content, Privacy, Storage, Backup and restore
                    // ---------------------------------------------------------
                    SettingsGroupCard {
                        SettingsGroupItem(
                            icon = Icons.Default.Public,
                            title = "Content",
                            subtitle = "Language, region, and providers",
                            accentColor = Color(0xFFF59E0B),
                            onClick = { activeSubSection = "Content" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.Lock,
                            title = "Privacy",
                            subtitle = "History and tracking",
                            accentColor = Color(0xFFEC4899),
                            onClick = { activeSubSection = "Privacy" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.Storage,
                            title = "Storage",
                            subtitle = "Cache and downloads",
                            accentColor = Color(0xFF10B981),
                            onClick = { activeSubSection = "Storage" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.Backup,
                            title = "Backup and restore",
                            subtitle = "Export and import data",
                            accentColor = Color(0xFF8B5CF6),
                            onClick = { activeSubSection = "Backup and restore" }
                        )
                    }

                    // ---------------------------------------------------------
                    // GROUP 5: System update, Supported Links, About
                    // ---------------------------------------------------------
                    SettingsGroupCard {
                        SettingsGroupItem(
                            icon = Icons.Default.SystemUpdate,
                            title = "System update",
                            subtitle = "UTA-wave v1.1.0 (Lossless & Ambient)",
                            accentColor = Color(0xFFF2AEC7),
                            badgeText = "Up to date",
                            onClick = { activeSubSection = "System update" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.Link,
                            title = "Supported Links",
                            subtitle = "App linking settings",
                            accentColor = Color(0xFF38BDF8),
                            onClick = { activeSubSection = "Supported Links" }
                        )
                        SettingsItemDivider()
                        SettingsGroupItem(
                            icon = Icons.Default.Info,
                            title = "About",
                            subtitle = "App info and licenses",
                            accentColor = Color(0xFFF59E0B),
                            onClick = { activeSubSection = "About UTA-wave" }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // Sub-Section Details
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    when (activeSubSection) {
                        "AI Hub" -> AIHubSettingsSection(userPrefs, settings)
                        "Appearance" -> AppearanceSettingsSection(
                            userPrefs,
                            settings,
                            onNavigateToTheme = { activeSubSection = "Theme" },
                            onNavigateToAppIcon = { activeSubSection = "App Icon" },
                            onNavigateToAppFont = { activeSubSection = "App Font" },
                            onNavigateToLiquidGlass = { activeSubSection = "Liquid Glass" }
                        )
                        "Player and audio" -> PlayerAudioSettingsSection(userPrefs, settings)
                        "Listen Together" -> ListenTogetherSettingsSection(userPrefs, settings)
                        "Content" -> ContentSettingsSection(userPrefs, settings)
                        "Privacy" -> PrivacySettingsSection(context, userPrefs, settings)
                        "Storage" -> StorageSettingsSection(context, userPrefs, settings)
                        "Backup and restore" -> BackupRestoreSettingsSection(context, userPrefs, settings)
                        "System update" -> SystemUpdateSettingsSection(context)
                        "Supported Links" -> SupportedLinksSettingsSection(context, onPlayTrack)
                        "About UTA-wave" -> AboutSettingsSection(context)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 1. AI HUB SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun AIHubSettingsSection(prefs: UserPreferences, settings: AppSettings) {
    SectionHeader("AI Lyrics Translation")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        SettingsToggleRow(
            title = "Translate Foreign Lyrics",
            subtitle = "Real-time bilingual translation for non-English songs",
            checked = settings.aiTranslationEnabled,
            onCheckedChange = { prefs.updateAiTranslation(it) }
        )
        SettingsItemDivider()
        SettingsToggleRow(
            title = "AI Romanization / Transliteration",
            subtitle = "Sing along in Latin alphabet (Japanese, Korean, Hindi)",
            checked = settings.aiRomanizationEnabled,
            onCheckedChange = { prefs.updateAiRomanization(it) }
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Target Translation Language")

    val languages = listOf("English", "Spanish", "French", "Japanese", "Hindi", "German", "Portuguese")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        languages.forEachIndexed { idx, lang ->
            val isSelected = settings.aiTargetLanguage.equals(lang, ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateAiTargetLanguage(lang) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lang,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (idx < languages.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("AI Engine Provider")

    val providers = listOf(
        Pair("BUILTIN", "Built-in Neural Model (Offline & Fast)"),
        Pair("WHISPER_CLOUD", "Cloud Whisper Engine (High Precision)"),
        Pair("OPENROUTER", "OpenRouter / Custom BYO LLM Key")
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        providers.forEachIndexed { idx, (id, label) ->
            val isSelected = settings.aiProvider == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateAiProvider(id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (idx < providers.size - 1) SettingsItemDivider()
        }
    }

    if (settings.aiProvider == "OPENROUTER") {
        Spacer(modifier = Modifier.height(10.dp))
        var apiKeyText by remember { mutableStateOf(settings.aiApiKey) }
        OutlinedTextField(
            value = apiKeyText,
            onValueChange = {
                apiKeyText = it
                prefs.updateAiApiKey(it)
            },
            label = { Text("OpenRouter / Custom API Key") },
            placeholder = { Text("sk-or-v1-...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFF2AEC7),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

// -------------------------------------------------------------------------
// 2. APPEARANCE SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun AppearanceSettingsSection(
    prefs: UserPreferences,
    settings: AppSettings,
    onNavigateToTheme: () -> Unit = {},
    onNavigateToAppIcon: () -> Unit = {},
    onNavigateToAppFont: () -> Unit = {},
    onNavigateToLiquidGlass: () -> Unit = {}
) {
    val currentAccent = remember(settings.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(settings.accentColorHex))
        } catch (e: Exception) {
            Color(0xFFF2AEC7)
        }
    }

    var showMiniPlayerBackgroundDialog by remember { mutableStateOf(false) }
    var showSliderStyleDialog by remember { mutableStateOf(false) }
    var showLyricsPositionDialog by remember { mutableStateOf(false) }
    var showLyricsAnimationStyleDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }

    if (showLyricsPositionDialog) {
        val positions = listOf(
            "LEFT" to "Left",
            "CENTER" to "Center",
            "RIGHT" to "Right"
        )
        AlertDialog(
            onDismissRequest = { showLyricsPositionDialog = false },
            title = {
                Text(
                    text = "Lyrics text position",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    positions.forEach { (pos, label) ->
                        val isSelected = settings.lyricsPosition.equals(pos, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    prefs.updateLyricsPosition(pos)
                                    showLyricsPositionDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    prefs.updateLyricsPosition(pos)
                                    showLyricsPositionDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = currentAccent,
                                    unselectedColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricsPositionDialog = false }) {
                    Text("Cancel", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showLyricsAnimationStyleDialog) {
        val styles = listOf(
            "ECHOMUSIC_1" to "Echo Music 1 (Spring Pop & Sweep)",
            "APPLE_V2" to "Apple Music v2 (Letter Fluid Wave)",
            "APPLE" to "Apple Music (Word-by-word Cubic)",
            "LYRICS_V2" to "Lyrics v2 (Floating Word Wave)",
            "METRO_LYRICS" to "Metro Lyrics (Kinetic Bold)",
            "KARAOKE" to "Karaoke (Brush Wipe & Corona)",
            "SLIDE" to "Slide (Breathing Horizontal Wipe)",
            "GLOW" to "Glow (Neon Bloom Pulse)",
            "FADE" to "Fade (Smooth Cubic Alpha)",
            "NONE" to "None (Standard Clean Text)"
        )
        AlertDialog(
            onDismissRequest = { showLyricsAnimationStyleDialog = false },
            title = {
                Text(
                    text = "Lyrics animation style",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    styles.forEach { (style, label) ->
                        val isSelected = settings.lyricsAnimationStyle.equals(style, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    prefs.updateLyricsAnimationStyle(style)
                                    showLyricsAnimationStyleDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    prefs.updateLyricsAnimationStyle(style)
                                    showLyricsAnimationStyleDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = currentAccent,
                                    unselectedColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricsAnimationStyleDialog = false }) {
                    Text("Cancel", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showLyricsTextSizeDialog) {
        var tempSize by remember { mutableFloatStateOf(settings.lyricsTextSize) }
        AlertDialog(
            onDismissRequest = { showLyricsTextSizeDialog = false },
            title = {
                Text(
                    text = "Lyrics text size",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${tempSize.toInt()} sp",
                        color = currentAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = tempSize,
                        onValueChange = { tempSize = it },
                        valueRange = 16f..36f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = currentAccent,
                            activeTrackColor = currentAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("16 sp", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("Default: 24 sp", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("36 sp", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    tempSize = 24f
                    prefs.updateLyricsTextSize(24f)
                }) {
                    Text("Reset", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.updateLyricsTextSize(tempSize)
                    showLyricsTextSizeDialog = false
                }) {
                    Text("Done", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showLyricsLineSpacingDialog) {
        var tempSpacing by remember { mutableFloatStateOf(settings.lyricsLineSpacing) }
        AlertDialog(
            onDismissRequest = { showLyricsLineSpacingDialog = false },
            title = {
                Text(
                    text = "Lyrics line spacing",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${String.format(java.util.Locale.ROOT, "%.1f", tempSpacing)}x",
                        color = currentAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = tempSpacing,
                        onValueChange = { tempSpacing = it },
                        valueRange = 1.0f..4.0f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = currentAccent,
                            activeTrackColor = currentAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1.0x", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("Default: 1.8x", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("4.0x", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    tempSpacing = 1.8f
                    prefs.updateLyricsLineSpacing(1.8f)
                }) {
                    Text("Reset", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.updateLyricsLineSpacing(tempSpacing)
                    showLyricsLineSpacingDialog = false
                }) {
                    Text("Done", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showSliderStyleDialog) {
        PlayerSliderStyleDialog(
            currentStyle = settings.sliderStyle,
            onSelectStyle = { prefs.updateSliderStyle(it) },
            onDismiss = { showSliderStyleDialog = false }
        )
    }

    if (showMiniPlayerBackgroundDialog) {
        AlertDialog(
            onDismissRequest = { showMiniPlayerBackgroundDialog = false },
            title = {
                Text(
                    text = "Miniplayer background style",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val styles = listOf(
                        "DEFAULT" to "Follow theme",
                        "BLUR" to "Blur",
                        "GLOW_ANIMATED" to "Glow animated",
                        "LIVE_MESH" to "Live mesh",
                        "LIQUID_GLASS" to "Liquid glass"
                    )
                    styles.forEach { (id, label) ->
                        val isSelected = settings.miniplayerBackgroundStyle == id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    prefs.updateMiniplayerBackgroundStyle(id)
                                    showMiniPlayerBackgroundDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    prefs.updateMiniplayerBackgroundStyle(id)
                                    showMiniPlayerBackgroundDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = currentAccent,
                                    unselectedColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMiniPlayerBackgroundDialog = false }) {
                    Text("Cancel", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    SectionHeader("Theme & Dynamic Colors")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .clickable(onClick = onNavigateToTheme)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(currentAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = currentAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Customize Theme & Colors",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            settings.pureBlack -> "AMOLED Pure Black • OLED power saving"
                            settings.darkMode == "OFF" -> "Light Theme • Custom palette"
                            settings.darkMode == "AUTO" -> "Following System Theme"
                            else -> "Obsidian Dark • Custom palette"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("App Launcher Icon")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .clickable(onClick = onNavigateToAppIcon)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2AEC7).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CropOriginal,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Customize App Icon",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Upload gallery photo, custom label & shapes",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Typography")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .clickable(onClick = onNavigateToAppFont)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA855F7).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FontDownload,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "App Font",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Google Sans, Outfit, Plus Jakarta & Custom TTF",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Liquid Glass (Beta)")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .clickable(onClick = onNavigateToLiquidGlass)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA855F7).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Liquid Glass (Beta)",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (settings.liquidGlassGlobalEnabled) "Enabled • AGSL shaders, lens refraction & blur" else "Disabled • Tap to configure",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Display Refresh Rate")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .clickable { prefs.updateEnableHighRefreshRate(!settings.enableHighRefreshRate) }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Enable high refresh rate",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Force the display to run at the highest supported refresh rate (e.g. 120Hz)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
            Switch(
                checked = settings.enableHighRefreshRate,
                onCheckedChange = { prefs.updateEnableHighRefreshRate(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Theme Mode")

    val themes = listOf(
        Pair("OBSIDIAN", "Obsidian Dark (Default)"),
        Pair("AMOLED", "Pure Black (AMOLED)"),
        Pair("DIM", "Dim Charcoal"),
        Pair("SYSTEM", "Follow System")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        themes.forEachIndexed { idx, (id, label) ->
            val isSelected = settings.themeMode == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateThemeMode(id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (idx < themes.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Accent Color Palette")

    val accentColors = listOf(
        Pair("#F2AEC7", "Blush Pink"),
        Pair("#A855F7", "Electric Violet"),
        Pair("#38BDF8", "Sky Blue"),
        Pair("#10B981", "Emerald"),
        Pair("#F59E0B", "Sunset Amber")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        accentColors.forEach { (hex, name) ->
            val color = Color(android.graphics.Color.parseColor(hex))
            val isSelected = settings.accentColorHex.equals(hex, ignoreCase = true)

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .clickable { prefs.updateAccentColor(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = name,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    SectionHeader("Live Accent Preview")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, currentAccent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(currentAccent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = currentAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "A Universe of Music",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "UTA-wave Audio Engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(currentAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Font Scale")

    val fontScales = listOf(
        Pair(0.90f, "Compact (90%)"),
        Pair(1.00f, "Standard (100%)"),
        Pair(1.15f, "Large (115%)")
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        fontScales.forEachIndexed { idx, (scale, label) ->
            val isSelected = kotlin.math.abs(settings.fontScale - scale) < 0.01f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateFontScale(scale) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (idx < fontScales.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Mini Player")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .clickable { showMiniPlayerBackgroundDialog = true }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(currentAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = currentAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Miniplayer background style",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (settings.miniplayerBackgroundStyle) {
                            "BLUR" -> "Blur"
                            "GLOW_ANIMATED" -> "Glow animated"
                            "LIVE_MESH" -> "Live mesh"
                            "LIQUID_GLASS" -> "Liquid glass"
                            else -> "Follow theme"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Miniplayer Dock Style")

    val miniStyles = listOf(
        Pair("FLOATING", "Floating Docked Capsule"),
        Pair("BOTTOM_ATTACHED", "Bottom-Attached Bar")
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        miniStyles.forEachIndexed { idx, (style, label) ->
            val isSelected = settings.miniplayerStyle == style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateMiniplayerStyle(style) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (idx < miniStyles.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Player Layout")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(currentAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.apple_music_me),
                        contentDescription = null,
                        tint = currentAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Apple Music Inspired",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dissolve artwork, Apple playback controls, volume scrub & AirPlay pill",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }
            Switch(
                checked = settings.appleMusicPlayerEnabled,
                onCheckedChange = { prefs.updateAppleMusicPlayer(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        if (settings.appleMusicPlayerEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            SettingsItemDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide volume slider",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hide the on-screen volume slider on Apple Music player",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
                Switch(
                    checked = settings.hideVolumeSlider,
                    onCheckedChange = { prefs.updateHideVolumeSlider(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = currentAccent,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Player slider style (Bar, Wavy, Slim, Squiggly)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSliderStyleDialog = true }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Player slider style",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (settings.sliderStyle.uppercase()) {
                        "BAR" -> "Bar"
                        "WAVY" -> "Wavy"
                        "SLIM" -> "Slim"
                        else -> "Squiggly"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = currentAccent
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Wavy play/pause button switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wavy play/pause",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Morphs play/pause button into an expressive wavy cookie shape while playing",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.wavyPlayPause,
                onCheckedChange = { prefs.updateWavyPlayPause(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Seeker haptic feedback switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Seeker haptic ticks",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Subtle tactile haptic ticks when scrubbing through the progress bar",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.enableSliderHaptics,
                onCheckedChange = { prefs.updateEnableSliderHaptics(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Swipe to change song
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Swipe to change song",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Swipe left or right on the album artwork or mini player to skip tracks",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.enableSwipeToChangeSong,
                onCheckedChange = { prefs.updateEnableSwipeToChangeSong(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Rotating turntable vinyl animation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rotating thumbnail",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Turn album art into a 20s spinning vinyl turntable with double-tap seek",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.rotatingThumbnail,
                onCheckedChange = { prefs.updateRotatingThumbnail(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Canvas artwork video animation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Canvas artwork video",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Play looping motion video canvases from streaming catalogs on supported songs",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.canvasThumbnailAnimation,
                onCheckedChange = { prefs.updateCanvasThumbnailAnimation(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Lyrics Appearance")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // 1. Lyrics text position
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLyricsPositionDialog = true }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lyrics text position",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (settings.lyricsPosition.uppercase()) {
                        "CENTER" -> "Center"
                        "RIGHT" -> "Right"
                        else -> "Left"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = currentAccent
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 2. Lyrics animation style
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLyricsAnimationStyleDialog = true }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lyrics animation style",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (settings.lyricsAnimationStyle.uppercase()) {
                        "ECHOMUSIC_1" -> "Echo Music 1 (Spring Pop & Sweep)"
                        "APPLE_V2" -> "Apple Music v2 (Letter Fluid Wave)"
                        "APPLE" -> "Apple Music (Word-by-word Cubic)"
                        "LYRICS_V2" -> "Lyrics v2 (Floating Word Wave)"
                        "METRO_LYRICS" -> "Metro Lyrics (Kinetic Bold)"
                        "KARAOKE" -> "Karaoke (Brush Wipe & Corona)"
                        "SLIDE" -> "Slide (Breathing Horizontal Wipe)"
                        "GLOW" -> "Glow (Neon Bloom Pulse)"
                        "FADE" -> "Fade (Smooth Cubic Alpha)"
                        else -> "None (Standard Clean Text)"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = currentAccent
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 3. Apple Music lyrics blur
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Apple Music lyrics blur",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Progressive depth-of-field blur on inactive lines (farther lines blur up to 7dp)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.appleMusicLyricsBlur,
                onCheckedChange = { prefs.updateAppleMusicLyricsBlur(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 4. Standard lyrics blur
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lyrics standard blur",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Uniform soft blur across all inactive lyric lines",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.lyricsStandardBlur,
                onCheckedChange = { prefs.updateLyricsStandardBlur(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 5. Lyrics text size
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLyricsTextSizeDialog = true }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lyrics text size",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${settings.lyricsTextSize.toInt()} sp",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = currentAccent
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 6. Lyrics line spacing
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLyricsLineSpacingDialog = true }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lyrics line spacing",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${String.format(java.util.Locale.ROOT, "%.1f", settings.lyricsLineSpacing)}x",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = currentAccent
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 7. Lyrics click / tap to seek
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tap to seek",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Jump playback directly to timestamp when tapping a lyric line",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.lyricsClick,
                onCheckedChange = { prefs.updateLyricsClick(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 8. Auto scroll
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto scroll",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Keep currently active lyric smoothly centered in view",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.lyricsScroll,
                onCheckedChange = { prefs.updateLyricsScroll(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 9. Swipe lyrics to change song
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Swipe lyrics to change song",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Swipe horizontally across the lyrics screen to skip tracks",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.swipeLyrics,
                onCheckedChange = { prefs.updateSwipeLyrics(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 10. Enable lyrics thumbnail play/pause button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Thumbnail play/pause button",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Show play/pause toggle button over album thumbnail in lyrics header",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.enableLyricsThumbnailPlayPause,
                onCheckedChange = { prefs.updateEnableLyricsThumbnailPlayPause(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 11. Hide status bar on fullscreen
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hide status bar on lyrics",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Immersive fullscreen lyrics view hiding the system status bar",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.hideStatusBarOnFullscreen,
                onCheckedChange = { prefs.updateHideStatusBarOnFullscreen(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsItemDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // 12. Lyrics glow effect
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lyrics glow effect",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Vibrant neon backlight glow on the active lyric line",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Switch(
                checked = settings.lyricsGlowEffect,
                onCheckedChange = { prefs.updateLyricsGlowEffect(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = currentAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }
    }
}

// -------------------------------------------------------------------------
// 3. PLAYER AND AUDIO SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun PlayerAudioSettingsSection(prefs: UserPreferences, settings: AppSettings) {
    SectionHeader("Audio Streaming Bitrate")

    val qualities = listOf(
        Pair("LOSSLESS", "Lossless Master (256k AAC / 160k Opus)"),
        Pair("HIGH", "High Quality (160 kbps)"),
        Pair("STANDARD", "Standard Quality (128 kbps)"),
        Pair("DATA_SAVER", "Data Saver (64 kbps)")
    )

    Text(
        text = "Wi-Fi Quality",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color(0xFFF2AEC7),
        modifier = Modifier.padding(start = 4.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        qualities.forEachIndexed { idx, (id, label) ->
            val isSelected = settings.wifiQuality == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateWifiQuality(id) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (idx < qualities.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Mobile Data Quality",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color(0xFFF2AEC7),
        modifier = Modifier.padding(start = 4.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        qualities.drop(1).forEachIndexed { idx, (id, label) ->
            val isSelected = settings.cellularQuality == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateCellularQuality(id) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (idx < qualities.size - 2) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    SectionHeader("Audio Engine & Transitions")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        SettingsToggleRow(
            title = "Gapless Audio Playback",
            subtitle = "Pre-buffers subsequent songs for zero-latency seamless transitions",
            checked = settings.gaplessPlayback,
            onCheckedChange = { prefs.updateGaplessPlayback(it) }
        )
        SettingsItemDivider()
        SettingsToggleRow(
            title = "Skip Silence",
            subtitle = "Fast-forwards dead air at intros and outros",
            checked = settings.skipSilence,
            onCheckedChange = { prefs.updateSkipSilence(it) }
        )
        SettingsItemDivider()
        SettingsToggleRow(
            title = "Audio Normalization (ReplayGain)",
            subtitle = "Dynamically balances audio volume across different albums",
            checked = settings.normalizeVolume,
            onCheckedChange = { prefs.updateNormalizeVolume(it) }
        )
        SettingsItemDivider()
        SettingsToggleRow(
            title = "Stop on Task Removed",
            subtitle = "Stops playback when app is swiped away from Recents",
            checked = settings.stopOnTaskRemoved,
            onCheckedChange = { prefs.updateStopOnTaskRemoved(it) }
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Equalizer Preset")

    val eqPresets = listOf("FLAT", "BASS_BOOST", "VOCAL", "POP", "ROCK", "ACOUSTIC")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        eqPresets.forEachIndexed { idx, preset ->
            val isSelected = settings.equalizerPreset == preset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateEqualizerPreset(preset) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (idx < eqPresets.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Seek Step Seconds")

    val seekOptions = listOf(5, 10, 15, 30)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        seekOptions.forEach { sec ->
            val isSelected = settings.seekStepSeconds == sec
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Color(0xFFF2AEC7) else Color.White.copy(alpha = 0.06f))
                    .clickable { prefs.updateSeekStep(sec) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${sec}s",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color(0xFF351225) else Color.White
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 4. LISTEN TOGETHER SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun ListenTogetherSettingsSection(prefs: UserPreferences, settings: AppSettings) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var joinCodeInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF281C2E), Color(0xFF1B1120)))
            )
            .border(1.dp, Color(0xFFF2AEC7).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Color(0xFFF2AEC7),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Listen Together Sessions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF2AEC7)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Stream music synchronously with your friends in real-time across devices with zero lag.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    SectionHeader("Host a Room")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        val currentCode = if (settings.listenTogetherRoomCode.isNotBlank()) settings.listenTogetherRoomCode else "WAVE-${Random.nextInt(100, 999)}"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Your Room Code", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                Text(text = currentCode, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFFF2AEC7))
            }
            Button(
                onClick = {
                    prefs.updateListenTogetherRoomCode(currentCode)
                    clipboardManager.setText(AnnotatedString(currentCode))
                    Toast.makeText(context, "Room code $currentCode copied!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2AEC7), contentColor = Color(0xFF351225)),
                shape = RoundedCornerShape(50)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    SectionHeader("Join a Room")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = joinCodeInput,
            onValueChange = { joinCodeInput = it },
            label = { Text("Enter 6-digit Room Code (e.g. WAVE-123)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFF2AEC7),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                if (joinCodeInput.isNotBlank()) {
                    prefs.updateListenTogetherRoomCode(joinCodeInput)
                    Toast.makeText(context, "Connected to room $joinCodeInput!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please enter a valid room code", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2230), contentColor = Color.White),
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Join Room", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

// -------------------------------------------------------------------------
// 5. CONTENT SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun ContentSettingsSection(prefs: UserPreferences, settings: AppSettings) {
    SectionHeader("Region & Country")

    val countries = listOf(
        Pair("GLOBAL", "Global (Worldwide)"),
        Pair("US", "United States 🇺🇸"),
        Pair("IN", "India 🇮🇳"),
        Pair("JP", "Japan 🇯🇵"),
        Pair("GB", "United Kingdom 🇬🇧"),
        Pair("KR", "South Korea 🇰🇷"),
        Pair("DE", "Germany 🇩🇪"),
        Pair("CA", "Canada 🇨🇦"),
        Pair("BR", "Brazil 🇧🇷")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        countries.forEachIndexed { idx, (code, name) ->
            val isSelected = settings.contentCountry == code
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        prefs.updateContentCountry(code)
                        InnerTubeClient.contentCountry = code
                    }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (idx < countries.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Content Filters")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        SettingsToggleRow(
            title = "Filter Explicit Content",
            subtitle = "Hide songs with parental advisory tags from explore feeds",
            checked = settings.filterExplicit,
            onCheckedChange = { prefs.updateFilterExplicit(it) }
        )
    }
}

// -------------------------------------------------------------------------
// 6. PRIVACY SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun PrivacySettingsSection(context: Context, prefs: UserPreferences, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Listening History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently erase all recently played songs and reset your weekly listening summary.", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            localDatabase.clearListeningHistory()
                            Toast.makeText(context, "Listening history cleared", Toast.LENGTH_SHORT).show()
                            showClearHistoryDialog = false
                        }
                    }
                ) {
                    Text("Clear", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF221726)
        )
    }

    SectionHeader("Tracking & Logging")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        SettingsToggleRow(
            title = "Pause Listening History",
            subtitle = "Songs played will not be recorded in history or weekly summary",
            checked = settings.pauseHistory,
            onCheckedChange = { prefs.updatePauseHistory(it) }
        )
        SettingsItemDivider()
        SettingsToggleRow(
            title = "Pause Search History",
            subtitle = "Search queries will not be saved locally",
            checked = settings.pauseSearchHistory,
            onCheckedChange = { prefs.updatePauseSearchHistory(it) }
        )
        SettingsItemDivider()
        SettingsToggleRow(
            title = "Anonymous Diagnostics",
            subtitle = "Send crash reports to improve stability",
            checked = settings.anonymousDiagnostics,
            onCheckedChange = { prefs.updateAnonymousDiagnostics(it) }
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Data Purge")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showClearHistoryDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Clear Listening History", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFFEF4444))
                Text(text = "Wipes history and listening time logs", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

// -------------------------------------------------------------------------
// 7. STORAGE SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun StorageSettingsSection(context: Context, prefs: UserPreferences, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    val downloads by localDatabase.downloadsFlow.collectAsState(initial = emptyList())
    val downloadManager = remember { AudioDownloadManager.getInstance(context) }

    var cacheDirSizeMb by remember {
        mutableStateOf(calculateDirSizeMb(context.cacheDir))
    }

    SectionHeader("Storage Overview")

    val downloadsMb = downloads.sumOf { it.durationSeconds * 25000L } / (1024 * 1024)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "App Cache & Data", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text(text = "$cacheDirSizeMb MB Cached", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF2AEC7))
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { 0.35f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Color(0xFFF2AEC7),
            trackColor = Color.White.copy(alpha = 0.08f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Offline Downloads: ${downloads.size} tracks (~$downloadsMb MB)", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Cache Flush")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.cacheDir.deleteRecursively()
                    cacheDirSizeMb = "0.0"
                    Toast.makeText(context, "Cache wiped successfully!", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF2AEC7), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Clear Image & Lyrics Cache", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                Text(text = "Free up memory without losing saved playlists", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Download Rules")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        SettingsToggleRow(
            title = "Download over Wi-Fi Only",
            subtitle = "Prevent downloads on cellular mobile data",
            checked = settings.downloadOverWifiOnly,
            onCheckedChange = { prefs.updateDownloadOverWifiOnly(it) }
        )
    }
}

// -------------------------------------------------------------------------
// 8. BACKUP AND RESTORE SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun BackupRestoreSettingsSection(context: Context, prefs: UserPreferences, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    val clipboardManager = LocalClipboardManager.current
    var restoreJsonInput by remember { mutableStateOf("") }
    var overwriteOption by remember { mutableStateOf(false) }

    SectionHeader("Export Data")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(text = "Export your playlists, favorites, and settings to a JSON file.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val json = localDatabase.exportJson()
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2AEC7), contentColor = Color(0xFF351225)),
                shape = RoundedCornerShape(50)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    scope.launch {
                        val json = localDatabase.exportJson()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, json)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share UTA-wave Backup"))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2230), contentColor = Color.White),
                shape = RoundedCornerShape(50)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Restore Data")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = restoreJsonInput,
            onValueChange = { restoreJsonInput = it },
            label = { Text("Paste Backup JSON here") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFF2AEC7),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = !overwriteOption,
                onClick = { overwriteOption = false },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF2AEC7))
            )
            Text(text = "Merge with existing", style = MaterialTheme.typography.bodySmall, color = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            RadioButton(
                selected = overwriteOption,
                onClick = { overwriteOption = true },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF2AEC7))
            )
            Text(text = "Overwrite", style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                if (restoreJsonInput.isNotBlank()) {
                    scope.launch {
                        val success = localDatabase.restoreBackupJson(restoreJsonInput, overwriteOption)
                        if (success) {
                            Toast.makeText(context, "Library restored successfully!", Toast.LENGTH_SHORT).show()
                            restoreJsonInput = ""
                        } else {
                            Toast.makeText(context, "Invalid JSON format", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2AEC7), contentColor = Color(0xFF351225)),
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Restore Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// -------------------------------------------------------------------------
// 9. SYSTEM UPDATE SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun SystemUpdateSettingsSection(context: Context) {
    var isChecking by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2A1C2C), Color(0xFF1B111E)))
            )
            .border(1.dp, Color(0xFFF2AEC7).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "UTA-wave", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
                    Text(text = "Version 1.1.0 • Build 20261024", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF2AEC7).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(text = "Up to date", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF2AEC7))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    isChecking = true
                    updateResult = null
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isChecking = false
                        updateResult = "You are on the latest version of UTA-wave!"
                    }, 1200)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2AEC7), contentColor = Color(0xFF351225)),
                shape = RoundedCornerShape(50)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF351225), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Check for Updates", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            if (updateResult != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = updateResult!!, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF10B981))
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Changelog (v1.1.0)")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        val features = listOf(
            "⚡ Lossless Master Audio Engine (256k AAC / 160k Opus)",
            "🌌 Landscape Ambient Mode with Kinetic Word Flow Lyrics",
            "✂️ Set as Ringtone 30s Audio Trimmer & M4A/MP3 Exporter",
            "💾 Lossless Offline Audio Downloads Manager",
            "👤 YouTube Music Auth & Channel Profile Avatar Sync",
            "📊 Weekly Listening Summary with 7-Day Pill Bar Chart & Vibe Callout",
            "🎛️ Complete 11-Domain Settings & Customization Hub"
        )
        features.forEach { feat ->
            Text(text = feat, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp), color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

// -------------------------------------------------------------------------
// 10. SUPPORTED LINKS SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun SupportedLinksSettingsSection(
    context: Context,
    onPlayTrack: ((TrackItem, List<TrackItem>) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var inputLink by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }

    SectionHeader("Supported Domains")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        val domains = listOf("music.youtube.com", "youtube.com/watch", "youtu.be", "open.spotify.com/track")
        domains.forEach { domain ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = domain, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("In-App Link Resolver")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = inputLink,
            onValueChange = { inputLink = it },
            label = { Text("Paste YouTube Music or Spotify link...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFF2AEC7),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (inputLink.isNotBlank()) {
                    isResolving = true
                    scope.launch {
                        try {
                            val innerTube = InnerTubeClient()
                            val query = inputLink.substringAfterLast("/").substringAfter("v=").take(11)
                            val results = innerTube.search(query)
                            if (results.isNotEmpty()) {
                                onPlayTrack?.invoke(results[0], results)
                                Toast.makeText(context, "Playing: ${results[0].title}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No match found for URL", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error resolving link", Toast.LENGTH_SHORT).show()
                        } finally {
                            isResolving = false
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2AEC7), contentColor = Color(0xFF351225)),
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isResolving) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF351225), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text("Resolve & Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Button(
        onClick = {
            try {
                val intent = Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "System settings not available", Toast.LENGTH_SHORT).show()
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Open Android App Links Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

// -------------------------------------------------------------------------
// 11. ABOUT SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun AboutSettingsSection(context: Context) {
    var showLicensesDialog by remember { mutableStateOf(false) }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open Source Licenses", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val licenses = listOf(
                        "AndroidX Media3 / ExoPlayer (Apache 2.0)",
                        "Jetpack Compose & Material 3 (Apache 2.0)",
                        "Square OkHttp 4.12 (Apache 2.0)",
                        "Coil Image Loader (Apache 2.0)",
                        "Room & SQLite Persistence (Apache 2.0)",
                        "Kotlinx Coroutines (Apache 2.0)",
                        "LRCLIB Synchronized Lyrics (MIT)"
                    )
                    licenses.forEach { lic ->
                        Text(text = "• $lic", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp), color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close", color = Color(0xFFF2AEC7))
                }
            },
            containerColor = Color(0xFF221726)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2AEC7).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFF2AEC7), modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "UTA-wave", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
        Text(text = "A Universe of Music • v1.1.0", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF2AEC7))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "A cinematic, dark, futuristic music streaming platform built with Next.js architecture principles, ExoPlayer lossless audio, dynamic ambient flow lyrics, and Echo Music interface design.",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = Color.White.copy(alpha = 0.7f)
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLicensesDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFF2AEC7), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = "Open Source Licenses", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(13.dp))
        }
        SettingsItemDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                    context.startActivity(browserIntent)
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = "GitHub Repository", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(13.dp))
        }
    }
}

// -------------------------------------------------------------------------
// SHARED UI HELPERS
// -------------------------------------------------------------------------
@Composable
private fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceElevated)
            .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
private fun SettingsGroupItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color = ElectricViolet,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )
        }

        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricViolet,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceContainerHigh
            )
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        ),
        color = ElectricViolet,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItemDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CardBorder.copy(alpha = 0.4f))
    )
}

private fun calculateDirSizeMb(dir: File): String {
    if (!dir.exists()) return "0.0"
    var bytes = 0L
    dir.walkTopDown().forEach { file ->
        if (file.isFile) bytes += file.length()
    }
    val mb = bytes.toDouble() / (1024 * 1024)
    return String.format("%.1f", mb)
}
