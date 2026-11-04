package com.uta.wave.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.desktop.account.DesktopUserProfile
import com.uta.wave.desktop.data.DesktopAppSettings
import com.uta.wave.desktop.data.DesktopDatabase
import com.uta.wave.desktop.data.DesktopPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CardBackground = Color(0xFF141622)
private val CardBorder = Color(0xFF222638)
private val SectionTitleColor = Color(0xFFC084FC)

@Composable
fun DesktopSettingsScreen(
    userProfile: DesktopUserProfile,
    database: DesktopDatabase,
    onSyncNow: () -> Unit,
    onOpenAccountDialog: () -> Unit,
    onSignOut: () -> Unit
) {
    val prefs = remember { DesktopPreferences.getInstance() }
    val settings by prefs.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var cacheSizeBytes by remember { mutableStateOf(prefs.calculateCacheSizeBytes()) }
    var actionNotification by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionNotification) {
        if (actionNotification != null) {
            kotlinx.coroutines.delay(3500)
            actionNotification = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Settings & Preferences",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize audio fidelity, playback behavior, cloud sync, and application storage.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Notification Banner
            item {
                AnimatedVisibility(
                    visible = actionNotification != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = actionNotification ?: "", color = Color(0xFFD1FAE5), fontSize = 12.5.sp)
                        }
                    }
                }
            }

            // 1. Audio & Streaming Engine Card
            item {
                SettingsCard(title = "Audio Engine & Streaming Quality", icon = Icons.Default.GraphicEq) {
                    // Quality Selection
                    Column {
                        Text("Streaming Audio Fidelity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Determines audio bitrate and decode engine priority.", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.45f))
                        Spacer(modifier = Modifier.height(10.dp))

                        val qualities = listOf(
                            "Hi-Fi Lossless (24-bit 96kHz)",
                            "High Quality (160kbps AAC)",
                            "Data Saver (96kbps Opus)"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            qualities.forEach { q ->
                                val selected = settings.audioQuality == q
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { prefs.updateSettings(settings.copy(audioQuality = q)) }
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    color = if (selected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF1A1D2D),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = q,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color(0xFFE9D5FF) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.06f))

                    // Buffer Duration
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Audio Buffer Size", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Prevents stuttering during network fluctuations.", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.45f))
                            }
                            Text(
                                text = "${settings.bufferSizeMs} ms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC084FC)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(250, 500, 1000).forEach { ms ->
                                val sel = settings.bufferSizeMs == ms
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { prefs.updateSettings(settings.copy(bufferSizeMs = ms)) }
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    color = if (sel) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF1A1D2D),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (sel) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.08f))
                                ) {
                                    Text(
                                        text = if (ms == 250) "250ms (Low Latency)" else if (ms == 500) "500ms (Recommended)" else "1000ms (High Reliability)",
                                        fontSize = 11.sp,
                                        color = if (sel) Color.White else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.06f))

                    // Volume Normalization Toggle
                    SettingsToggleRow(
                        title = "Volume Normalization (ReplayGain)",
                        subtitle = "Equalizes track loudness across different music sources.",
                        checked = settings.volumeNormalization,
                        onCheckedChange = { prefs.updateSettings(settings.copy(volumeNormalization = it)) }
                    )
                }
            }

            // 2. Playback & Queue Engine Card
            item {
                SettingsCard(title = "Playback & Queue Behavior", icon = Icons.Default.QueueMusic) {
                    SettingsToggleRow(
                        title = "Automix Infinite Station",
                        subtitle = "Automatically load similar recommended tracks when reaching the end of the queue.",
                        checked = settings.isAutomixEnabled,
                        onCheckedChange = { prefs.updateSettings(settings.copy(isAutomixEnabled = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.06f))

                    SettingsToggleRow(
                        title = "Smart Shuffle Artist Dispersion",
                        subtitle = "Evenly spreads tracks to avoid grouping consecutive songs by the same artist.",
                        checked = settings.isSmartShuffleSpread,
                        onCheckedChange = { prefs.updateSettings(settings.copy(isSmartShuffleSpread = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.06f))

                    SettingsToggleRow(
                        title = "Auto-Resume on Startup",
                        subtitle = "Restores the last active track and queue when opening UTA-WAVE Desktop.",
                        checked = settings.autoResumeOnStartup,
                        onCheckedChange = { prefs.updateSettings(settings.copy(autoResumeOnStartup = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.06f))

                    // Crossfade Audio Transitions
                    SettingsToggleRow(
                        title = "Crossfade Audio Transitions",
                        subtitle = "Smoothly blends outgoing and incoming songs with an equal-power acoustic curve.",
                        checked = settings.crossfadeEnabled,
                        onCheckedChange = { prefs.updateSettings(settings.copy(crossfadeEnabled = it)) }
                    )

                    AnimatedVisibility(visible = settings.crossfadeEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Crossfade Duration", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Text("Length of the overlapping audio blend between tracks.", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.45f))
                                }
                                Text(
                                    text = "${settings.crossfadeDurationSeconds} seconds",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC084FC)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(2, 4, 6, 8, 12).forEach { sec ->
                                    val sel = settings.crossfadeDurationSeconds == sec
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { prefs.updateSettings(settings.copy(crossfadeDurationSeconds = sec)) }
                                            .pointerHoverIcon(PointerIcon.Hand),
                                        color = if (sel) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF1A1D2D),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (sel) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.08f))
                                    ) {
                                        Text(
                                            text = "${sec}s",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sel) Color.White else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            SettingsToggleRow(
                                title = "Disable for Gapless Albums",
                                subtitle = "Don't crossfade if consecutive songs belong to the same album.",
                                checked = settings.crossfadeGapless,
                                onCheckedChange = { prefs.updateSettings(settings.copy(crossfadeGapless = it)) }
                            )
                        }
                    }
                }
            }

            // 3. Atmosphere & Visuals Card
            item {
                SettingsCard(title = "Atmosphere & Visual Effects", icon = Icons.Default.Palette) {
                    SettingsToggleRow(
                        title = "Dynamic Atmospheric Glow Background",
                        subtitle = "Generates a deep blurred cinematic background derived from the playing song's artwork.",
                        checked = settings.dynamicAlbumGlow,
                        onCheckedChange = { prefs.updateSettings(settings.copy(dynamicAlbumGlow = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.06f))

                    SettingsToggleRow(
                        title = "Synchronized Lyrics Word Bloom",
                        subtitle = "Subtle glowing bloom on active vocal lines in the Fullscreen Karaoke Player.",
                        checked = settings.karaokeLyricsBloom,
                        onCheckedChange = { prefs.updateSettings(settings.copy(karaokeLyricsBloom = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.06f))

                    SettingsToggleRow(
                        title = "Reduced Motion & Low GPU Mode",
                        subtitle = "Disables intensive blur layers and background visualizer loops to conserve battery and GPU.",
                        checked = settings.reducedMotion,
                        onCheckedChange = { prefs.updateSettings(settings.copy(reducedMotion = it)) }
                    )
                }
            }

            // 4. Account & Cloud Sync Card
            item {
                SettingsCard(title = "Account & Cloud Synchronization", icon = Icons.Default.CloudSync) {
                    // Profile Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userProfile.name.take(1).uppercase().ifBlank { "U" },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC084FC)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (userProfile.isLoggedIn) userProfile.name else "Guest Session",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (userProfile.isLoggedIn) userProfile.email else "Not signed in to mobile account",
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Button(
                            onClick = onOpenAccountDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = if (userProfile.isLoggedIn) Icons.Default.ManageAccounts else Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (userProfile.isLoggedIn) "Account Details" else "Connect Account",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Sync Now Button
                        OutlinedButton(
                            onClick = {
                                onSyncNow()
                                actionNotification = "Cloud library sync initiated..."
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Mobile Library Now", fontSize = 11.5.sp)
                        }

                        // Sign Out Button
                        if (userProfile.isLoggedIn) {
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }

            // 5. Storage, Cache & Data Management Card
            item {
                SettingsCard(title = "Storage & Cache Management", icon = Icons.Default.Storage) {
                    val formattedMb = "%.1f MB".format(cacheSizeBytes / (1024.0 * 1024.0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Application Storage Cache", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Artwork images, temporary stream buffers, and lyrics data.", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.45f))
                        }
                        Text(
                            text = formattedMb,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val ok = prefs.clearCache()
                                cacheSizeBytes = prefs.calculateCacheSizeBytes()
                                actionNotification = if (ok) "Cache flushed successfully." else "Could not clear cache."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222638)),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Cached Data", fontSize = 11.5.sp, color = Color.White)
                        }
                    }
                }
            }

            // 6. About UTA-WAVE Desktop
            item {
                SettingsCard(title = "About UTA-WAVE", icon = Icons.Default.Info) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Application Version", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                            Text("v1.0.0 (Windows Native)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Display & Rendering Pipeline", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                            Text("DirectX 12 Skiko + Compose Multiplatform 1.7.0", fontSize = 12.sp, color = Color(0xFFC084FC))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Audio Playback Engine", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                            Text("OpenJFX 21 + Low-Latency Local Proxy", fontSize = 12.sp, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Platform Architecture", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                            Text("Microsoft Windows 10/11 (x86_64)", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SectionTitleColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.45f))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color(0xFF222638)
            ),
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
        )
    }
}
