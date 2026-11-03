package com.uta.wave.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import java.awt.Dimension

// UTA-VERSE Cinematic Dark Theme Colors
val BackgroundDeep = Color(0xFF090A0F)
val SurfaceDark = Color(0xFF12141D)
val SurfaceBorder = Color(0xFF1E2230)
val AccentPurple = Color(0xFF8B5CF6)
val AccentPink = Color(0xFFEC4899)
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)

enum class DesktopNavTab(val label: String, val icon: @Composable () -> Unit) {
    EXPLORE("Explore", { Icon(Icons.Filled.Explore, contentDescription = "Explore") }),
    SEARCH("Search", { Icon(Icons.Filled.Search, contentDescription = "Search") }),
    LIBRARY("Library", { Icon(Icons.Filled.LibraryMusic, contentDescription = "Library") }),
    CONNECT("UTA Connect", { Icon(Icons.Filled.Devices, contentDescription = "UTA Connect") }),
    AI_STUDIO("AI Studio", { Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Studio") }),
    SETTINGS("Settings", { Icon(Icons.Filled.Settings, contentDescription = "Settings") })
}

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1200.dp, 800.dp),
        position = WindowPosition(Alignment.Center)
    )

    var currentTab by remember { mutableStateOf(DesktopNavTab.EXPLORE) }
    var playbackState by remember {
        mutableStateOf(
            UnifiedPlaybackState(
                title = "Ajj Din Chadheya",
                artist = "Pritam, Rahat Fateh Ali Khan",
                durationMs = 314000L,
                positionMs = 78000L,
                isPlaying = false,
                volume = 0.85f
            )
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "UTA-WAVE — A Universe of Music",
        undecorated = false
    ) {
        window.minimumSize = Dimension(900, 600)

        MaterialTheme(
            colorScheme = darkColorScheme(
                background = BackgroundDeep,
                surface = SurfaceDark,
                primary = AccentPurple,
                onBackground = TextPrimary,
                onSurface = TextPrimary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDeep)
            ) {
                // Top Custom Header / Branding Bar
                DesktopTopHeader(
                    activeSessionDevice = "Windows PC",
                    isCloudSynced = true
                )

                // Main Content Workspace (Sidebar + View Area)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Left Navigation Sidebar
                    DesktopSidebar(
                        selectedTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(SurfaceBorder)
                    )

                    // Tab View Workspace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp)
                    ) {
                        when (currentTab) {
                            DesktopNavTab.EXPLORE -> ExploreWorkspace()
                            DesktopNavTab.SEARCH -> SearchWorkspace()
                            DesktopNavTab.LIBRARY -> LibraryWorkspace()
                            DesktopNavTab.CONNECT -> ConnectWorkspace()
                            DesktopNavTab.AI_STUDIO -> AiStudioWorkspace()
                            DesktopNavTab.SETTINGS -> SettingsWorkspace()
                        }
                    }
                }

                // Bottom Persistent Playback Deck
                DesktopPlayerDock(
                    playbackState = playbackState,
                    onPlayPause = {
                        playbackState = playbackState.copy(isPlaying = !playbackState.isPlaying)
                    },
                    onSeek = { newPos ->
                        playbackState = playbackState.copy(positionMs = newPos)
                    },
                    onVolumeChange = { newVol ->
                        playbackState = playbackState.copy(volume = newVol)
                    }
                )
            }
        }
    }
}

@Composable
fun DesktopTopHeader(activeSessionDevice: String, isCloudSynced: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SurfaceDark)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🌊 UTA-WAVE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPurple
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Desktop Edition (Windows)",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // UTA Connect Indicator
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Device: $activeSessionDevice",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Cloud Sync Status Pill
            Surface(
                color = if (isCloudSynced) Color(0xFF064E3B) else Color(0xFF78350F),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCloudSynced) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
                        contentDescription = "Cloud Status",
                        modifier = Modifier.size(14.dp),
                        tint = if (isCloudSynced) Color(0xFF34D399) else Color(0xFFFBBF24)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCloudSynced) "Synced with Mobile" else "Syncing...",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopSidebar(
    selectedTab: DesktopNavTab,
    onTabSelected: (DesktopNavTab) -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(SurfaceDark)
            .padding(12.dp)
    ) {
        DesktopNavTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(tab) },
                color = if (isSelected) AccentPurple.copy(alpha = 0.2f) else Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        tintColor = if (isSelected) AccentPurple else TextSecondary
                    ) {
                        tab.icon()
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tab.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopPlayerDock(
    playbackState: UnifiedPlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Current Track Info
            Row(
                modifier = Modifier.width(260.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF262A3B)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = "Track Artwork",
                            tint = AccentPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = playbackState.title.ifEmpty { "No track playing" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = playbackState.artist.ifEmpty { "Select a song to start" },
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Center: Playback Controls & Progress Bar
            Column(
                modifier = Modifier.width(480.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = TextSecondary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(42.dp)
                            .background(AccentPurple, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = TextPrimary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Repeat, contentDescription = "Repeat", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Bar
                val progress = if (playbackState.durationMs > 0) {
                    (playbackState.positionMs.toFloat() / playbackState.durationMs).coerceIn(0f, 1f)
                } else 0f

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatTime(playbackState.positionMs),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentPurple,
                        trackColor = Color(0xFF262A3B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTime(playbackState.durationMs),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Right: Volume & Options
            Row(
                modifier = Modifier.width(200.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = "Volume",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = playbackState.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.width(100.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurple,
                        activeTrackColor = AccentPurple,
                        inactiveTrackColor = Color(0xFF262A3B)
                    )
                )
            }
        }
    }
}

// Workspace Placeholders
@Composable fun ExploreWorkspace() { WorkspaceCard("🔥 Explore & Trending Charts", "Discover trending songs, top charts, and curated playlists seamlessly streamable.") }
@Composable fun SearchWorkspace() { WorkspaceCard("🔍 Search YouTube Music", "Direct, quota-free YouTube Music autocomplete, artists, albums, and tracks search.") }
@Composable fun LibraryWorkspace() { WorkspaceCard("📚 Your Cloud Library", "Synced playlists, Liked Songs, and custom collections mirrored between Mobile & Windows.") }
@Composable fun ConnectWorkspace() { WorkspaceCard("📱 UTA Connect (Remote Control)", "Control music playing on your phone or PC. Handoff playback between devices in real-time.") }
@Composable fun AiStudioWorkspace() { WorkspaceCard("✨ AI Playlist Studio", "Generate prompt-curated playlists in seconds powered by Google Gemini 2.0 Flash.") }
@Composable fun SettingsWorkspace() { WorkspaceCard("⚙️ Settings & Audio Engine", "Configure WASAPI bit-perfect output, equalizer, cloud account sync, and theme.") }

@Composable
fun WorkspaceCard(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(subtitle, fontSize = 14.sp, color = TextSecondary)
        }
    }
}

@Composable
fun Box(tintColor: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides tintColor) {
        content()
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
