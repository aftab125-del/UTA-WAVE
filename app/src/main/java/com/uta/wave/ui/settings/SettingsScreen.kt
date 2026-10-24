package com.uta.wave.ui.settings

import android.content.Context
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.preferences.AppSettings
import com.uta.wave.data.preferences.UserPreferences
import java.io.File

/**
 * 100% Comprehensive Settings & Customization Hub for UTA-wave matching Echo Music.
 * Categories:
 * 1. Appearance (Theme, Palette Accent, Live Color Preview)
 * 2. Player & Playback (Player Style, Apple V2 Flow Lyrics, Ambient Glow, Crossfade, Seek Step)
 * 3. Audio Quality & Engine (Wi-Fi/Cellular Bitrate Limits, ReplayGain, Skip Silence)
 * 4. Downloads & Storage (Default Quality, Wi-Fi Only, Cache Size Counter & Flush)
 * 5. About (Version, Open Source Licenses, GitHub)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences.getInstance(context) }
    val settings by userPrefs.settingsFlow.collectAsState()

    var activeSubSection by remember { mutableStateOf<String?>(null) }

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
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF140E18)
                )
            )
        },
        containerColor = Color(0xFF100A14),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activeSubSection == null) {
                // Main Settings Category Directory
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsNavCard(
                        icon = Icons.Default.Palette,
                        title = "Appearance",
                        subtitle = "Theme, custom accent palette, live preview",
                        accentColor = Color(0xFFF2AEC7),
                        onClick = { activeSubSection = "Appearance" }
                    )

                    SettingsNavCard(
                        icon = Icons.Default.Tune,
                        title = "Player & Playback",
                        subtitle = "Wave style, Apple V2 lyrics, crossfade, seek step",
                        accentColor = Color(0xFFA855F7),
                        onClick = { activeSubSection = "Player & Playback" }
                    )

                    SettingsNavCard(
                        icon = Icons.Default.GraphicEq,
                        title = "Audio Quality & Engine",
                        subtitle = "Lossless stream, Wi-Fi/Cellular limits, ReplayGain",
                        accentColor = Color(0xFF38BDF8),
                        onClick = { activeSubSection = "Audio Quality & Engine" }
                    )

                    SettingsNavCard(
                        icon = Icons.Default.Storage,
                        title = "Downloads & Storage",
                        subtitle = "Offline quality, cache management, storage usage",
                        accentColor = Color(0xFF10B981),
                        onClick = { activeSubSection = "Downloads & Storage" }
                    )

                    SettingsNavCard(
                        icon = Icons.Default.Info,
                        title = "About UTA-wave",
                        subtitle = "Version, open-source credits, GitHub repository",
                        accentColor = Color(0xFFF59E0B),
                        onClick = { activeSubSection = "About UTA-wave" }
                    )
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
                        "Appearance" -> AppearanceSettingsSection(userPrefs, settings)
                        "Player & Playback" -> PlayerSettingsSection(userPrefs, settings)
                        "Audio Quality & Engine" -> AudioSettingsSection(userPrefs, settings)
                        "Downloads & Storage" -> DownloadSettingsSection(context, userPrefs, settings)
                        "About UTA-wave" -> AboutSettingsSection(context)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 1. APPEARANCE SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun AppearanceSettingsSection(prefs: UserPreferences, settings: AppSettings) {
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
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
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

    // Mini preview card
    val previewColor = Color(android.graphics.Color.parseColor(settings.accentColorHex))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, previewColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(previewColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF221124),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "Previewing Accent Theme",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Dynamic colors applied across player & Ambient Mode",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.60f)
            )
        }
    }
}

// -------------------------------------------------------------------------
// 2. PLAYER & PLAYBACK SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun PlayerSettingsSection(prefs: UserPreferences, settings: AppSettings) {
    SectionHeader("Lyrics Flow Animation")

    val lyricStyles = listOf(
        Pair("ALPHABET_V2", "Apple Music V2 (Smooth Alphabet Sweep)"),
        Pair("WORD_BY_WORD", "Word-by-Word Highlight")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        lyricStyles.forEachIndexed { idx, (id, label) ->
            val isSelected = settings.lyricsAnimation == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateLyricsAnimation(id) }
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
            if (idx < lyricStyles.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Player Features")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        SettingsToggleRow(
            title = "Ambient Canvas Glow",
            subtitle = "Extracts dynamic radial bloom from album artwork",
            checked = settings.ambientCanvasEnabled,
            onCheckedChange = { prefs.updateAmbientCanvas(it) }
        )
        SettingsItemDivider()
        SettingsToggleRow(
            title = "Skip Silence",
            subtitle = "Automatically fast-forward past silent intros/outros",
            checked = settings.skipSilence,
            onCheckedChange = { prefs.updateSkipSilence(it) }
        )
        SettingsItemDivider()
        SettingsToggleRow(
            title = "Normalize Volume (ReplayGain)",
            subtitle = "Equalize loudness levels across different songs",
            checked = settings.normalizeVolume,
            onCheckedChange = { prefs.updateNormalizeVolume(it) }
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Skip / Seek Duration")

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
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Color(0xFFF2AEC7) else Color.White.copy(alpha = 0.08f))
                    .clickable { prefs.updateSeekStep(sec) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${sec}s",
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF3B1528) else Color.White
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 3. AUDIO QUALITY SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun AudioSettingsSection(prefs: UserPreferences, settings: AppSettings) {
    SectionHeader("Wi-Fi Streaming Quality")

    val qualities = listOf(
        Pair("LOSSLESS", "Lossless / Studio (256k AAC / 160k Opus)"),
        Pair("HIGH", "High Quality (160 kbps)"),
        Pair("STANDARD", "Standard Quality (128 kbps)"),
        Pair("DATA_SAVER", "Data Saver (64 kbps)")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        qualities.forEachIndexed { idx, (id, label) ->
            val isSelected = settings.wifiQuality == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateWifiQuality(id) }
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
            if (idx < qualities.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Mobile Cellular Streaming Quality")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        qualities.forEachIndexed { idx, (id, label) ->
            val isSelected = settings.cellularQuality == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateCellularQuality(id) }
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
            if (idx < qualities.size - 1) SettingsItemDivider()
        }
    }
}

// -------------------------------------------------------------------------
// 4. DOWNLOADS & STORAGE SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun DownloadSettingsSection(context: Context, prefs: UserPreferences, settings: AppSettings) {
    var cacheSizeMb by remember {
        mutableStateOf(calculateDirSizeMb(context.cacheDir))
    }

    SectionHeader("Default Download Quality")

    val downloadQualities = listOf(
        Pair("LOSSLESS", "Lossless Master (256k AAC / 160k Opus)"),
        Pair("HIGH", "High Quality (160 kbps)"),
        Pair("STANDARD", "Standard Quality (128 kbps M4A)")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        downloadQualities.forEachIndexed { idx, (id, label) ->
            val isSelected = settings.defaultDownloadQuality == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.updateDownloadQuality(id) }
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
            if (idx < downloadQualities.size - 1) SettingsItemDivider()
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Download Rules")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        SettingsToggleRow(
            title = "Download Over Wi-Fi Only",
            subtitle = "Prevent downloading files over mobile data",
            checked = settings.downloadOverWifiOnly,
            onCheckedChange = { prefs.updateDownloadOverWifiOnly(it) }
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Offline Cache Management")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Temporary Cache: $cacheSizeMb MB",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Album covers, temporary stream snippets, and indexes",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.60f)
            )
        }

        Button(
            onClick = {
                try {
                    context.cacheDir.deleteRecursively()
                    context.cacheDir.mkdirs()
                    cacheSizeMb = calculateDirSizeMb(context.cacheDir)
                    Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to clear cache", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.12f),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Clear", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------------------
// 5. ABOUT SETTINGS SECTION
// -------------------------------------------------------------------------
@Composable
private fun AboutSettingsSection(context: Context) {
    SectionHeader("Application Details")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        Text(
            text = "UTA-wave",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Version 1.1.0 • A Universe of Music",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFF2AEC7)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "An ultra-premium, ad-free streaming client featuring real-time synchronized lyrics, landscape Ambient Mode, lossless offline downloads, and YouTube Music synchronization.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = Color.White.copy(alpha = 0.70f)
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    SectionHeader("Open Source Acknowledgements")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        Text(
            text = "• Inspired by Echo Music (EchoMusicApp/Echo-Music)\n• Powered by YouTube InnerTube Engine\n• Media3 ExoPlayer Audio Engine\n• LRCLIB Synchronized Lyrics Provider\n• Coil Image Loading & Jetpack Compose",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
            color = Color.White.copy(alpha = 0.80f)
        )
    }
}

// -------------------------------------------------------------------------
// REUSABLE HELPER COMPONENTS
// -------------------------------------------------------------------------
@Composable
private fun SettingsNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.60f)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(14.dp)
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.60f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFF2AEC7),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
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
        color = Color(0xFFF2AEC7),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItemDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.05f))
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
