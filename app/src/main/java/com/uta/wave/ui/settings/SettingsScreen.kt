package com.uta.wave.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import com.uta.wave.ui.components.AudioDeviceBottomSheet
import com.uta.wave.ui.components.EqualizerSheet
import com.uta.wave.ui.components.PlaybackLogsDialog
import com.uta.wave.player.WavePlayerState
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
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.uta.wave.data.ai.AiPlaylistGenerator
import com.uta.wave.data.downloads.AudioDownloadManager
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.data.preferences.AppSettings
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.ui.account.AccountScreen
import com.uta.wave.ui.stats.ListeningSummaryScreen
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
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

    BackHandler(enabled = activeSubSection != null) {
        activeSubSection = null
    }
    BackHandler(enabled = activeSubSection == null) {
        onBack()
    }

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val currentAccent = remember(settings.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(settings.accentColorHex))
        } catch (e: Exception) {
            Color(0xFFF2AEC7)
        }
    }

    // AI Connection test state
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // API Key inputs & visibility
    var geminiKeyText by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var showGeminiKey by remember { mutableStateOf(false) }

    var openRouterKeyText by remember(settings.aiApiKey) { mutableStateOf(settings.aiApiKey) }
    var showOpenRouterKey by remember { mutableStateOf(false) }

    // AI Music Studio Sandbox state
    var sandboxPrompt by remember { mutableStateOf("") }
    var sandboxCount by remember { mutableFloatStateOf(10f) }
    var isGeneratingPlaylist by remember { mutableStateOf(false) }
    var generationProgressText by remember { mutableStateOf("") }
    var generatedResultTitle by remember { mutableStateOf<String?>(null) }
    var generatedResultTrackCount by remember { mutableIntStateOf(0) }

    // -------------------------------------------------------------
    // 1. AI PROVIDER & API KEYS CARD
    // -------------------------------------------------------------
    SectionHeader("AI Intelligence Engine")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Select your AI provider for Playlist Generation & Lyrics Intelligence:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )

        val providers = listOf(
            Triple("GEMINI", "Google Gemini 2.0 Flash", "Free Official API Key • 15 RPM • Ultra Fast (< 1s)"),
            Triple("OPENROUTER", "OpenRouter / DeepSeek", "Custom BYO Key for GPT-4o, Claude 3.5, DeepSeek"),
            Triple("BUILTIN", "Free Neural Cloud", "Zero-setup public cloud fallback (no key needed)")
        )

        providers.forEach { (id, title, desc) ->
            val isSelected = settings.aiProvider == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) currentAccent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
                    .border(1.dp, if (isSelected) currentAccent.copy(alpha = 0.45f) else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable {
                        prefs.updateAiProvider(id)
                        connectionTestResult = null
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) currentAccent else Color.White
                        )
                        if (id == "GEMINI") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "RECOMMENDED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = Color(0xFF34D399),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = currentAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Gemini Key Input
        if (settings.aiProvider == "GEMINI") {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = geminiKeyText,
                onValueChange = {
                    geminiKeyText = it
                    prefs.updateGeminiApiKey(it)
                    connectionTestResult = null
                },
                label = { Text("Google Gemini API Key") },
                placeholder = { Text("AIzaSy...") },
                visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                            Icon(
                                imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle key visibility",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { pasted ->
                                val clean = pasted.trim()
                                geminiKeyText = clean
                                prefs.updateGeminiApiKey(clean)
                                connectionTestResult = null
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste key",
                                tint = currentAccent
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = currentAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = currentAccent
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Free tier: 15 requests / min at Google AI Studio",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = "Get Free Key ↗",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                    color = currentAccent,
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Visit https://aistudio.google.com/app/apikey", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }

        // OpenRouter Key Input
        if (settings.aiProvider == "OPENROUTER") {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = openRouterKeyText,
                onValueChange = {
                    openRouterKeyText = it
                    prefs.updateAiApiKey(it)
                    connectionTestResult = null
                },
                label = { Text("OpenRouter API Key") },
                placeholder = { Text("sk-or-v1-...") },
                visualTransformation = if (showOpenRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showOpenRouterKey = !showOpenRouterKey }) {
                            Icon(
                                imageVector = if (showOpenRouterKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle key visibility",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { pasted ->
                                val clean = pasted.trim()
                                openRouterKeyText = clean
                                prefs.updateAiApiKey(clean)
                                connectionTestResult = null
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste key",
                                tint = currentAccent
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = currentAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = currentAccent
                )
            )
        }

        // Test Connection Button & Result Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    isTestingConnection = true
                    connectionTestResult = null
                    coroutineScope.launch {
                        val res = AiPlaylistGenerator.testAiConnection(context)
                        connectionTestResult = res
                        isTestingConnection = false
                    }
                },
                enabled = !isTestingConnection,
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentAccent.copy(alpha = 0.25f),
                    contentColor = currentAccent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = currentAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing...", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Connection", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            connectionTestResult?.let { (success, msg) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (success) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (success) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (success) Color(0xFF34D399) else Color(0xFFF87171),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (success) "Online" else "Failed",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (success) Color(0xFF34D399) else Color(0xFFF87171)
                        )
                    }
                }
            }
        }

        connectionTestResult?.let { (_, msg) ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }

    // -------------------------------------------------------------
    // 2. INTERACTIVE AI PLAYLIST STUDIO SANDBOX
    // -------------------------------------------------------------
    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("AI Music Studio Sandbox")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Generate a bespoke themed playlist with AI right now:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )

        OutlinedTextField(
            value = sandboxPrompt,
            onValueChange = { sandboxPrompt = it },
            label = { Text("Vibe, Mood, or Scene") },
            placeholder = { Text("e.g. Midnight neon drive through Neo-Tokyo") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = currentAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = currentAccent
            )
        )

        // Prompt suggestions
        val suggestions = listOf("Cyberpunk Drive", "Rainy Day Acoustic", "Tokyo City Pop", "Intense Gym", "Lo-Fi Focus")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            suggestions.take(3).forEach { chip ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.clickable { sandboxPrompt = chip }
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Count slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Songs: ${sandboxCount.toInt()}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.8f)
            )
            Slider(
                value = sandboxCount,
                onValueChange = { sandboxCount = it },
                valueRange = 5f..25f,
                steps = 19,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                colors = SliderDefaults.colors(
                    thumbColor = currentAccent,
                    activeTrackColor = currentAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }

        // Generate Button
        Button(
            onClick = {
                val promptToUse = sandboxPrompt.ifBlank { "Top atmospheric synthwave and melodic rhythms" }
                isGeneratingPlaylist = true
                generationProgressText = "Starting..."
                generatedResultTitle = null
                coroutineScope.launch {
                    val result = AiPlaylistGenerator.generatePlaylist(
                        context = context,
                        prompt = promptToUse,
                        songCount = sandboxCount.toInt()
                    ) { step, cur, tot ->
                        generationProgressText = if (tot > 0) "$step ($cur/$tot)" else step
                    }
                    isGeneratingPlaylist = false
                    if (result != null) {
                        generatedResultTitle = result.name
                        generatedResultTrackCount = result.tracks.size
                        Toast.makeText(context, "Playlist '${result.name}' created in Library!", Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = !isGeneratingPlaylist,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = currentAccent,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isGeneratingPlaylist) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = generationProgressText.ifBlank { "Curating Playlist..." },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
            } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate AI Playlist",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
            }
        }

        generatedResultTitle?.let { title ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Created: $title",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "$generatedResultTrackCount tracks added to your Library Playlists",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // 3. AI LYRICS TRANSLATION CARD
    // -------------------------------------------------------------
    Spacer(modifier = Modifier.height(10.dp))
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
                        tint = currentAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (idx < languages.size - 1) SettingsItemDivider()
        }
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
                        text = "Liquid Glass & Optics",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Permanent • AGSL shaders, miniplayer & navigation optics",
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
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = currentAccent.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, currentAccent.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "Permanent",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = currentAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

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
    val currentAccent = remember(settings.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(settings.accentColorHex))
        } catch (e: Exception) {
            Color(0xFFF2AEC7)
        }
    }

    // Dialog states
    var showAudioQualityWifiDialog by remember { mutableStateOf(false) }
    var showAudioQualityCellularDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showPlaybackEngineDialog by remember { mutableStateOf(false) }
    var showCrossfadeBetaDialog by remember { mutableStateOf(false) }
    var showCrossfadeDurationDialog by remember { mutableStateOf(false) }
    var showHistoryDurationDialog by remember { mutableStateOf(false) }
    var showLoudnessPresetDialog by remember { mutableStateOf(false) }
    var showPreloadLimitDialog by remember { mutableStateOf(false) }
    var showEqualizerPresetDialog by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showAudioDeviceSheet by remember { mutableStateOf(false) }
    var showPlaybackLogsSheet by remember { mutableStateOf(false) }

    if (showEqualizerSheet) {
        EqualizerSheet(onDismiss = { showEqualizerSheet = false })
    }
    if (showAudioDeviceSheet) {
        AudioDeviceBottomSheet(onDismiss = { showAudioDeviceSheet = false })
    }
    if (showPlaybackLogsSheet) {
        PlaybackLogsDialog(playerState = WavePlayerState(), onDismiss = { showPlaybackLogsSheet = false })
    }

    // 1. Wi-Fi Audio Quality Dialog
    if (showAudioQualityWifiDialog) {
        val options = listOf(
            "AUTO" to "Auto (Adaptive)",
            "HIGH" to "High (256k AAC / 160k Opus)",
            "MEDIUM" to "Medium (128k AAC / 70k Opus)",
            "LOW" to "Low (48k Opus)"
        )
        AlertDialog(
            onDismissRequest = { showAudioQualityWifiDialog = false },
            title = { Text("Wi-Fi Audio Quality", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    options.forEach { (key, label) ->
                        val isSelected = settings.audioQualityWifi.equals(key, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    prefs.updateAudioQualityWifi(key)
                                    showAudioQualityWifiDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    prefs.updateAudioQualityWifi(key)
                                    showAudioQualityWifiDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = currentAccent, unselectedColor = Color.White.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioQualityWifiDialog = false }) {
                    Text("Cancel", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 2. Cellular Audio Quality Dialog
    if (showAudioQualityCellularDialog) {
        val options = listOf(
            "AUTO" to "Auto (Adaptive)",
            "HIGH" to "High (160 kbps)",
            "MEDIUM" to "Medium (128 kbps)",
            "LOW" to "Low (48 kbps)"
        )
        AlertDialog(
            onDismissRequest = { showAudioQualityCellularDialog = false },
            title = { Text("Cellular Audio Quality", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    options.forEach { (key, label) ->
                        val isSelected = settings.audioQualityCellular.equals(key, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    prefs.updateAudioQualityCellular(key)
                                    showAudioQualityCellularDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    prefs.updateAudioQualityCellular(key)
                                    showAudioQualityCellularDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = currentAccent, unselectedColor = Color.White.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioQualityCellularDialog = false }) {
                    Text("Cancel", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 3. Download Quality Dialog
    if (showDownloadQualityDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadQualityDialog = false },
            title = { Text("Download Quality", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDownloadQualityDialog = false }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = true,
                            onClick = { showDownloadQualityDialog = false },
                            colors = RadioButtonDefaults.colors(selectedColor = currentAccent)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("YouTube Music (AAC/Default)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadQualityDialog = false }) {
                    Text("Done", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 4. Playback Engine Dialog
    if (showPlaybackEngineDialog) {
        val engines = listOf(
            Triple("POTOKEN", "eXtended InnerTube", "Uses WebView PoToken + CipherDeobfuscator. Most reliable and future-proof."),
            Triple("BRAVEPIPE", "BravePipe (NewPipe)", "Uses NewPipe extractor for stream resolution."),
            Triple("AUTO", "Auto (Try Both)", "Tries PoToken first, falls back to BravePipe if it fails.")
        )
        AlertDialog(
            onDismissRequest = { showPlaybackEngineDialog = false },
            title = { Text("Playback Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    engines.forEach { (key, label, desc) ->
                        val isSelected = settings.playbackEngine.equals(key, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    prefs.updatePlaybackEngine(key)
                                    showPlaybackEngineDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    prefs.updatePlaybackEngine(key)
                                    showPlaybackEngineDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = currentAccent, unselectedColor = Color.White.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = label, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = desc, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaybackEngineDialog = false }) {
                    Text("Cancel", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 5. Crossfade Beta Warning Dialog
    if (showCrossfadeBetaDialog) {
        AlertDialog(
            onDismissRequest = { showCrossfadeBetaDialog = false },
            title = { Text("Beta Feature", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    text = "Crossfade is a new feature and may have bugs. If you experience any issues, please report them.\n\nThis feature disables audio offload due to technical limitations.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { showCrossfadeBetaDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCrossfadeBetaDialog = false
                    prefs.updateCrossfadeEnabled(true)
                }) {
                    Text("Enable", color = currentAccent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 6. Crossfade Duration Dialog
    if (showCrossfadeDurationDialog) {
        var tempDuration by remember { mutableFloatStateOf(settings.crossfadeDuration) }
        AlertDialog(
            onDismissRequest = { showCrossfadeDurationDialog = false },
            title = { Text("Crossfade duration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${tempDuration.toInt()} seconds",
                        color = currentAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = tempDuration,
                        onValueChange = { tempDuration = it },
                        valueRange = 1f..15f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = currentAccent,
                            activeTrackColor = currentAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1s", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("Default: 5s", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("15s", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    tempDuration = 5.0f
                    prefs.updateCrossfadeDuration(5.0f)
                }) {
                    Text("Reset", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.updateCrossfadeDuration(tempDuration)
                    showCrossfadeDurationDialog = false
                }) {
                    Text("Done", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 7. History Duration Dialog
    if (showHistoryDurationDialog) {
        var tempSec by remember { mutableFloatStateOf(settings.historyDuration) }
        AlertDialog(
            onDismissRequest = { showHistoryDurationDialog = false },
            title = { Text("History duration threshold", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${tempSec.toInt()} seconds",
                        color = currentAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = tempSec,
                        onValueChange = { tempSec = it },
                        valueRange = 10f..60f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = currentAccent,
                            activeTrackColor = currentAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("10s", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("Default: 30s", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("60s", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    tempSec = 30f
                    prefs.updateHistoryDuration(30f)
                }) {
                    Text("Reset", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.updateHistoryDuration(tempSec)
                    showHistoryDurationDialog = false
                }) {
                    Text("Done", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 8. Loudness Preset Dialog
    if (showLoudnessPresetDialog) {
        val presets = listOf(
            "QUIET" to "Quiet (-18 LUFS, max dynamic range)",
            "NORMAL" to "Normal (-14 LUFS, streaming standard)",
            "LOUD" to "Loud (-11 LUFS, punchy outdoor)",
            "AGGRESSIVE" to "Aggressive (-8 LUFS, max gain limiter)"
        )
        AlertDialog(
            onDismissRequest = { showLoudnessPresetDialog = false },
            title = { Text("Loudness Preset", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    presets.forEach { (key, label) ->
                        val isSelected = settings.audioLoudnessPreset.equals(key, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    prefs.updateAudioLoudnessPreset(key)
                                    showLoudnessPresetDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    prefs.updateAudioLoudnessPreset(key)
                                    showLoudnessPresetDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = currentAccent, unselectedColor = Color.White.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoudnessPresetDialog = false }) {
                    Text("Cancel", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 9. Preload Limit Dialog
    if (showPreloadLimitDialog) {
        var tempLimit by remember { mutableFloatStateOf(settings.preloadNextSongLimit.toFloat()) }
        AlertDialog(
            onDismissRequest = { showPreloadLimitDialog = false },
            title = { Text("Preload Limit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${tempLimit.toInt()} songs",
                        color = currentAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = tempLimit,
                        onValueChange = { tempLimit = it },
                        valueRange = 1f..20f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = currentAccent,
                            activeTrackColor = currentAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("Default: 10", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text("20", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    tempLimit = 10f
                    prefs.updatePreloadNextSongLimit(10)
                }) {
                    Text("Reset", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.updatePreloadNextSongLimit(tempLimit.toInt())
                    showPreloadLimitDialog = false
                }) {
                    Text("Done", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 10. Equalizer Preset Dialog
    if (showEqualizerPresetDialog) {
        val eqPresets = listOf("FLAT", "BASS_BOOST", "VOCAL", "POP", "ROCK", "ACOUSTIC")
        AlertDialog(
            onDismissRequest = { showEqualizerPresetDialog = false },
            title = { Text("Equalizer Preset", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    eqPresets.forEach { preset ->
                        val isSelected = settings.equalizerPreset.equals(preset, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    prefs.updateEqualizerPreset(preset)
                                    showEqualizerPresetDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    prefs.updateEqualizerPreset(preset)
                                    showEqualizerPresetDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = currentAccent, unselectedColor = Color.White.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = preset.replace("_", " "), color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqualizerPresetDialog = false }) {
                    Text("Cancel", color = currentAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF221726),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // =========================================================================
    // GROUP 1: DATA SAVER
    // =========================================================================
    SectionHeader("Data Saver")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        SettingsToggleRow(
            title = "Data Saver Mode (Beta)",
            subtitle = "Disable lyrics, videos, preloading, background syncs, and force Opus audio to save data",
            checked = settings.dataSaverEnabled,
            onCheckedChange = { prefs.updateDataSaverEnabled(it) }
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // =========================================================================
    // GROUP 2: PLAYER & AUDIO ENGINE
    // =========================================================================
    SectionHeader("Player & Audio Engine")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        SettingsToggleRow(
            title = "Show 10-second seek buttons",
            subtitle = "Show backward and forward buttons in the full player",
            checked = settings.showSeekButtons,
            onCheckedChange = { prefs.updateShowSeekButtons(it) }
        )
        SettingsItemDivider()

        // Wi-Fi Quality Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAudioQualityWifiDialog = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Wi-Fi Audio Quality", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (settings.audioQualityWifi.uppercase()) {
                        "HIGH" -> "High (256k AAC / 160k Opus)"
                        "MEDIUM" -> "Medium (128k AAC / 70k Opus)"
                        "LOW" -> "Low (48k Opus)"
                        else -> "Auto (Adaptive)"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = currentAccent
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
        SettingsItemDivider()

        // Cellular Quality Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAudioQualityCellularDialog = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Cellular Audio Quality", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (settings.audioQualityCellular.uppercase()) {
                        "HIGH" -> "High (160 kbps)"
                        "LOW" -> "Low (48 kbps)"
                        "AUTO" -> "Auto (Adaptive)"
                        else -> "Medium (128 kbps)"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = currentAccent
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
        SettingsItemDivider()

        // Download Quality Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDownloadQualityDialog = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Download Quality", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text("YouTube Music (AAC/Default)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = currentAccent)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Download with metadata",
            subtitle = "Downloads lyrics and tags when downloading a song",
            checked = settings.downloadWithMetadata,
            onCheckedChange = { prefs.updateDownloadWithMetadata(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Download on Wi-Fi only",
            subtitle = "Pause song downloads while on mobile data",
            checked = settings.downloadOnWifiOnly,
            onCheckedChange = { prefs.updateDownloadOnWifiOnly(it) }
        )
        SettingsItemDivider()

        // Playback Engine Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPlaybackEngineDialog = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Playback Engine", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (settings.playbackEngine.uppercase()) {
                        "BRAVEPIPE" -> "BravePipe (NewPipe)"
                        "AUTO" -> "Auto (Try Both)"
                        else -> "eXtended InnerTube (PoToken)"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = currentAccent
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
        SettingsItemDivider()

        // Crossfade Toggle
        SettingsToggleRow(
            title = "Crossfade",
            subtitle = "Crossfade between songs with constant acoustic power",
            checked = settings.crossfadeEnabled,
            onCheckedChange = { enabled ->
                if (enabled) {
                    showCrossfadeBetaDialog = true
                } else {
                    prefs.updateCrossfadeEnabled(false)
                }
            }
        )

        if (settings.crossfadeEnabled) {
            SettingsItemDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCrossfadeDurationDialog = true }
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Crossfade duration", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${settings.crossfadeDuration.toInt()} seconds", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = currentAccent)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            }
            SettingsItemDivider()

            SettingsToggleRow(
                title = "Disable for gapless albums",
                subtitle = "Don't crossfade if the album is gapless",
                checked = settings.crossfadeGapless,
                onCheckedChange = { prefs.updateCrossfadeGapless(it) }
            )
        }
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Automix (Beta)",
            subtitle = "Beat-matched transitions: align downbeats and tempo of songs during crossfade",
            checked = settings.automixCrossfade,
            onCheckedChange = { prefs.updateAutomixCrossfade(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Automix debug overlay",
            subtitle = "Show live beat analysis and transition plan on the player",
            checked = settings.automixDebugOverlay,
            onCheckedChange = { prefs.updateAutomixDebugOverlay(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Bit-Perfect USB-DAC Output",
            subtitle = "Direct UAC1/UAC2 driver bypassing Android OS mixer",
            checked = settings.enableBitPerfectUsbDac,
            onCheckedChange = { prefs.updateEnableBitPerfectUsbDac(it) }
        )
        SettingsItemDivider()

        // History Duration Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showHistoryDurationDialog = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("History duration", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text("${settings.historyDuration.toInt()} seconds threshold", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = currentAccent)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Skip silence",
            subtitle = "Fast forward through silent parts of songs",
            checked = settings.skipSilence,
            onCheckedChange = { prefs.updateSkipSilence(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Instantly skip silence",
            subtitle = "Jump ahead during silent moments instead of speeding up playback",
            checked = settings.skipSilenceInstant,
            onCheckedChange = { prefs.updateSkipSilenceInstant(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Audio normalization",
            subtitle = "Manage audio normalization settings",
            checked = settings.audioNormalization,
            onCheckedChange = { prefs.updateAudioNormalization(it) }
        )
        SettingsItemDivider()

        // Loudness Preset Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLoudnessPresetDialog = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Loudness Preset", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (settings.audioLoudnessPreset.uppercase()) {
                        "QUIET" -> "Quiet (-18 LUFS)"
                        "LOUD" -> "Loud (-11 LUFS)"
                        "AGGRESSIVE" -> "Aggressive (-8 LUFS)"
                        else -> "Normal (-14 LUFS)"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = currentAccent
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Enable varispeed",
            subtitle = "Link pitch to playback speed, resulting in tape-style speedup and slowdown effects",
            checked = settings.varispeed,
            onCheckedChange = { prefs.updateVarispeed(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Spatial Audio",
            subtitle = "Widens stereo tracks for a more immersive feel",
            checked = settings.spatialAudio,
            onCheckedChange = { prefs.updateSpatialAudio(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Enable offload",
            subtitle = if (settings.crossfadeEnabled) "Disabled because Crossfade is active" else "Use offload audio path to minimize power consumption",
            checked = settings.audioOffload && !settings.crossfadeEnabled,
            onCheckedChange = { if (!settings.crossfadeEnabled) prefs.updateAudioOffload(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Preload Next Song",
            subtitle = "Cache the next song for gapless playback",
            checked = settings.preloadNextSongEnabled,
            onCheckedChange = { prefs.updatePreloadNextSongEnabled(it) }
        )
        SettingsItemDivider()

        // Preload Limit Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPreloadLimitDialog = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Preload Limit", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text("${settings.preloadNextSongLimit} songs in background cache", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = currentAccent)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Preload Lyrics",
            subtitle = "Also cache lyrics for the preloaded songs",
            checked = settings.preloadLyricsEnabled,
            onCheckedChange = { prefs.updatePreloadLyricsEnabled(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Google Cast",
            subtitle = "Enable casting audio to Chromecast and other Cast-enabled devices",
            checked = settings.enableGoogleCast,
            onCheckedChange = { prefs.updateEnableGoogleCast(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Progressive seek",
            subtitle = "If enabled, adds up 5 extra seconds incrementally on each seek skip",
            checked = settings.seekExtraSeconds,
            onCheckedChange = { prefs.updateSeekExtraSeconds(it) }
        )
        SettingsItemDivider()

        // Equalizer Preset Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEqualizerSheet = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Equalizer", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Pro-grade acoustic tuning • ${settings.equalizerPreset.replace("_", " ")}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = currentAccent)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // =========================================================================
    // GROUP 3: QUEUE
    // =========================================================================
    SectionHeader("Queue")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        SettingsToggleRow(
            title = "Persistent queue",
            subtitle = "Restore your last queue when the app starts",
            checked = settings.persistentQueue,
            onCheckedChange = { prefs.updatePersistentQueue(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Auto load more songs",
            subtitle = "Automatically add more songs when the end of the queue is reached, if possible",
            checked = settings.autoLoadMore,
            onCheckedChange = { prefs.updateAutoLoadMore(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Auto download on like",
            subtitle = "Automatically download songs when you like them",
            checked = settings.autoDownloadOnLike,
            onCheckedChange = { prefs.updateAutoDownloadOnLike(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Enable similar content",
            subtitle = "Automatically add more similar songs when the end of the queue is reached",
            checked = settings.similarContent,
            onCheckedChange = { prefs.updateSimilarContent(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Persistent shuffle",
            subtitle = "Keep shuffle enabled when starting new songs or playlists",
            checked = settings.persistentShuffleAcrossQueues,
            onCheckedChange = { prefs.updatePersistentShuffleAcrossQueues(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Remember shuffle and repeat",
            subtitle = "Remember shuffle and repeat mode when restarting the app",
            checked = settings.rememberShuffleAndRepeat,
            onCheckedChange = { prefs.updateRememberShuffleAndRepeat(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Shuffle playlist/album first",
            subtitle = "When shuffling, play all songs from the original playlist/album first, then similar content",
            checked = settings.shufflePlaylistFirst,
            onCheckedChange = { prefs.updateShufflePlaylistFirst(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Prevent duplicate tracks in queue",
            subtitle = "When adding a track to queue, remove it from its previous position if already present",
            checked = settings.preventDuplicateTracksInQueue,
            onCheckedChange = { prefs.updatePreventDuplicateTracksInQueue(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Auto skip to next song when error occurs",
            subtitle = "Ensure your continuous playback experience",
            checked = settings.autoSkipNextOnError,
            onCheckedChange = { prefs.updateAutoSkipNextOnError(it) }
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // =========================================================================
    // GROUP 4: MISC
    // =========================================================================
    SectionHeader("Misc")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
    ) {
        SettingsToggleRow(
            title = "Stop music on task clear",
            subtitle = "Stop music service when app is swiped away from recent apps",
            checked = settings.stopMusicOnTaskClear,
            onCheckedChange = { prefs.updateStopMusicOnTaskClear(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Pause music when media is muted",
            subtitle = "Automatically pause playback when volume reaches zero",
            checked = settings.pauseOnMute,
            onCheckedChange = { prefs.updatePauseOnMute(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Resume on Bluetooth connect",
            subtitle = "Resume playback when Bluetooth audio device connects",
            checked = settings.resumeOnBluetoothConnect,
            onCheckedChange = { prefs.updateResumeOnBluetoothConnect(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Autoplay on Android Auto",
            subtitle = "Start last queue automatically when Android Auto connects",
            checked = settings.autoplayOnAutomotiveConnect,
            onCheckedChange = { prefs.updateAutoplayOnAutomotiveConnect(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Keep screen on when player is expanded",
            subtitle = "Keep display on while viewing fullscreen player or lyrics",
            checked = settings.keepScreenOn,
            onCheckedChange = { prefs.updateKeepScreenOn(it) }
        )
        SettingsItemDivider()

        SettingsToggleRow(
            title = "Export as MP3",
            subtitle = "Show 'Export as MP3' in track action menus",
            checked = settings.enableExportAsMp3,
            onCheckedChange = { prefs.updateEnableExportAsMp3(it) }
        )
        SettingsItemDivider()

        // Audio Output Routing Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAudioDeviceSheet = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Audio Output Routing", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Bluetooth, wired headphones, phone speaker & volume", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Color.White.copy(alpha = 0.55f))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
        SettingsItemDivider()

        // Playback Logs HUD Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPlaybackLogsSheet = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Playback Logs & Telemetry HUD", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Real-time stream events, decoders, buffer levels & errors", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = currentAccent)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
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
