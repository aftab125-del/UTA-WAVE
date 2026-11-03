package com.uta.wave.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.account.DesktopAccountManager
import com.uta.wave.desktop.account.DesktopUserProfile
import com.uta.wave.desktop.audio.WindowsAudioPlayerEngine
import com.uta.wave.desktop.data.DesktopDatabase
import com.uta.wave.desktop.data.DesktopMusicService
import com.uta.wave.desktop.data.DesktopPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.io.ByteArrayInputStream
import java.net.URI

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
        size = DpSize(1240.dp, 840.dp),
        position = WindowPosition(Alignment.Center)
    )

    val audioEngine = remember { WindowsAudioPlayerEngine() }
    val musicService = remember { DesktopMusicService() }
    val accountManager = remember { DesktopAccountManager.getInstance() }
    val database = remember { DesktopDatabase.getInstance() }

    val playbackState by audioEngine.state.collectAsState()
    val userProfile by accountManager.profile.collectAsState()
    val libraryData by database.library.collectAsState()
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(DesktopNavTab.LIBRARY) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    // Playlist Queue management
    var activeQueue by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var currentQueueIndex by remember { mutableStateOf(-1) }

    fun playTrack(track: TrackItem) {
        statusMessage = "Resolving audio stream for \"${track.title}\"..."
        scope.launch {
            try {
                val streamUrl = musicService.resolveStreamUrl(track.videoId)
                if (!streamUrl.isNullOrBlank()) {
                    statusMessage = null
                    audioEngine.load(
                        videoId = track.videoId,
                        title = track.title,
                        artist = track.artist,
                        artworkUrl = track.artworkUrl,
                        streamUrl = streamUrl
                    )
                } else {
                    statusMessage = "Could not resolve audio stream for \"${track.title}\"."
                }
            } catch (e: Exception) {
                statusMessage = "Playback error: ${e.message}"
            }
        }
    }

    fun playQueueTrack(index: Int) {
        if (index in activeQueue.indices) {
            currentQueueIndex = index
            playTrack(activeQueue[index])
        }
    }

    fun playPlaylist(playlist: DesktopPlaylist, startIndex: Int = 0) {
        if (playlist.tracks.isNotEmpty()) {
            activeQueue = playlist.tracks
            playQueueTrack(startIndex)
        }
    }

    fun shufflePlaylist(playlist: DesktopPlaylist) {
        if (playlist.tracks.isNotEmpty()) {
            activeQueue = playlist.tracks.shuffled()
            playQueueTrack(0)
        }
    }

    // Auto-advance to next track in queue when current track ends
    LaunchedEffect(Unit) {
        audioEngine.onTrackEnded = {
            if (currentQueueIndex + 1 < activeQueue.size) {
                playQueueTrack(currentQueueIndex + 1)
            }
        }
    }

    fun triggerMobileSync() {
        if (isSyncing) return
        isSyncing = true
        scope.launch {
            try {
                val cookie = userProfile.cookie
                if (!cookie.isNullOrBlank()) {
                    statusMessage = "Syncing with UTA-WAVE Mobile account..."
                    musicService.syncFullLibrary(cookie, userProfile.sapisid)
                    statusMessage = null
                } else {
                    statusMessage = "Connected to UTA-WAVE Mobile account."
                }
            } catch (e: Exception) {
                statusMessage = "Sync notice: ${e.message}"
            } finally {
                isSyncing = false
            }
        }
    }

    Window(
        onCloseRequest = {
            try {
                audioEngine.release()
            } catch (_: Exception) {}
            exitApplication()
            kotlin.system.exitProcess(0)
        },
        state = windowState,
        title = "UTA-WAVE — A Universe of Music",
        undecorated = false
    ) {
        window.minimumSize = Dimension(960, 640)

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
                    isCloudSynced = !isSyncing,
                    userProfile = userProfile,
                    onAccountClick = { showAccountDialog = true }
                )

                // Status Banner (if resolving or error)
                if (statusMessage != null || playbackState.errorMessage != null) {
                    val msg = statusMessage ?: playbackState.errorMessage
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF3B181E)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = msg ?: "", fontSize = 12.sp, color = Color(0xFFFCA5A5))
                        }
                    }
                }

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
                            .padding(20.dp)
                    ) {
                        when (currentTab) {
                            DesktopNavTab.EXPLORE -> ExploreWorkspace(
                                onPlayTrack = ::playTrack
                            )
                            DesktopNavTab.SEARCH -> SearchWorkspace(
                                musicService = musicService,
                                onPlayTrack = ::playTrack
                            )
                            DesktopNavTab.LIBRARY -> LibraryWorkspace(
                                library = libraryData,
                                isSyncing = isSyncing,
                                currentlyPlayingVideoId = playbackState.videoId,
                                onSyncNow = ::triggerMobileSync,
                                onPlayPlaylist = ::playPlaylist,
                                onShufflePlaylist = ::shufflePlaylist,
                                onPlayTrackInPlaylist = { playlist, index ->
                                    activeQueue = playlist.tracks
                                    playQueueTrack(index)
                                }
                            )
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
                        if (playbackState.isPlaying) {
                            audioEngine.pause()
                        } else {
                            audioEngine.play()
                        }
                    },
                    onSeek = { newPos ->
                        audioEngine.seekTo(newPos)
                    },
                    onVolumeChange = { newVol ->
                        audioEngine.setVolume(newVol)
                    },
                    onNext = {
                        if (currentQueueIndex + 1 < activeQueue.size) {
                            playQueueTrack(currentQueueIndex + 1)
                        }
                    },
                    onPrev = {
                        if (currentQueueIndex > 0) {
                            playQueueTrack(currentQueueIndex - 1)
                        } else {
                            audioEngine.seekTo(0L)
                        }
                    }
                )
            }

            // Account & Sync Modal Dialog
            if (showAccountDialog) {
                AccountSyncDialog(
                    userProfile = userProfile,
                    isSyncing = isSyncing,
                    onDismiss = { showAccountDialog = false },
                    onSyncNow = {
                        triggerMobileSync()
                    },
                    onImportToken = { token ->
                        accountManager.importToken(token, "Namikaze aftab")
                        triggerMobileSync()
                    },
                    onImportBackup = { jsonStr ->
                        val ok = database.importMobileBackupJson(jsonStr)
                        if (ok) {
                            statusMessage = "Restored mobile library backup successfully!"
                        }
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Top Header with Account Pill & Sync Status
// -----------------------------------------------------------------------------

@Composable
fun DesktopTopHeader(
    activeSessionDevice: String,
    isCloudSynced: Boolean,
    userProfile: DesktopUserProfile,
    onAccountClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                        text = "Device: Windows PC • Paired with Mobile",
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                        text = if (isCloudSynced) "Synced with UTA-WAVE Mobile" else "Syncing...",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Profile Pill (Clickable)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onAccountClick),
                color = Color(0xFF1E2230),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = CircleShape,
                        color = AccentPurple.copy(alpha = 0.2f)
                    ) {
                        if (!userProfile.avatarUrl.isNullOrBlank()) {
                            NetworkThumbnail(url = userProfile.avatarUrl, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = userProfile.name.take(1).uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPurple
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = userProfile.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Account & Sync Modal Dialog
// -----------------------------------------------------------------------------

@Composable
fun AccountSyncDialog(
    userProfile: DesktopUserProfile,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onImportToken: (String) -> Unit,
    onImportBackup: (String) -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }
    var backupInput by remember { mutableStateOf("") }
    var showTokenField by remember { mutableStateOf(false) }
    var showBackupField by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(500.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header with title and close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("UTA-WAVE Mobile Account & Sync", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Profile Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181B26),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = AccentPurple.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(2.dp, AccentPurple)
                        ) {
                            if (!userProfile.avatarUrl.isNullOrBlank()) {
                                NetworkThumbnail(url = userProfile.avatarUrl, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = userProfile.name.take(1).uppercase(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPurple
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(userProfile.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(userProfile.email, fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Synced with UTA-WAVE Mobile", fontSize = 11.sp, color = Color(0xFF34D399))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mobile Pairing Instructions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF151926),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📱 To pair with your UTA-WAVE Mobile phone:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "1. Open UTA-WAVE on your phone\n" +
                            "2. Go to Account → tap 'LOGIN WITH TOKEN: Tap to show token'\n" +
                            "3. Tap 'Copy Token' and paste below to sync.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sync Library button
                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing with Mobile...", color = Color.White)
                    } else {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Mobile Library Now", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Paste Mobile Token Button
                TextButton(
                    onClick = { showTokenField = !showTokenField },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (showTokenField) "▲ Hide Mobile Token Input" else "▼ Paste Token from Mobile Account",
                        fontSize = 12.sp,
                        color = AccentPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showTokenField) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("Paste cookie / token from Mobile", fontSize = 11.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (tokenInput.isNotBlank()) {
                                onImportToken(tokenInput)
                                tokenInput = ""
                                showTokenField = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Connect Mobile Account & Sync", fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Import Mobile Backup JSON option
                TextButton(
                    onClick = { showBackupField = !showBackupField },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (showBackupField) "▲ Hide Backup Import" else "▼ Import uta_wave_library_backup.json",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (showBackupField) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = backupInput,
                        onValueChange = { backupInput = it },
                        placeholder = { Text("Paste uta_wave_library_backup.json content...", fontSize = 11.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (backupInput.isNotBlank()) {
                                onImportBackup(backupInput)
                                backupInput = ""
                                showBackupField = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262A3B))
                    ) {
                        Text("Restore Mobile Backup", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Sidebar Navigation
// -----------------------------------------------------------------------------

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
                    Box {
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

// -----------------------------------------------------------------------------
// Persistent Bottom Player Dock
// -----------------------------------------------------------------------------

@Composable
fun DesktopPlayerDock(
    playbackState: UnifiedPlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onNext: () -> Unit = {},
    onPrev: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
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
            // Left: Current Track Info with Artwork
            Row(
                modifier = Modifier.width(280.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF262A3B)
                ) {
                    val artUrl = playbackState.artworkUrl
                    if (!artUrl.isNullOrBlank()) {
                        NetworkThumbnail(
                            url = artUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = "Track Artwork",
                                tint = AccentPurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = playbackState.title.ifEmpty { "Ready to play" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (playbackState.isLoading) "Buffering stream..." else playbackState.artist.ifEmpty { "Select any track above" },
                        fontSize = 12.sp,
                        color = if (playbackState.isLoading) AccentPurple else TextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Center: Playback Controls & Interactive Scrubber Slider
            Column(
                modifier = Modifier.width(520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = TextSecondary)
                    }
                    IconButton(onClick = onPrev) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(44.dp)
                            .background(AccentPurple, CircleShape)
                    ) {
                        if (playbackState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = TextPrimary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Repeat, contentDescription = "Repeat", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Bar & Time Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(playbackState.positionMs),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = if (playbackState.durationMs > 0) {
                            (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f,
                        onValueChange = { frac ->
                            val target = (frac * playbackState.durationMs).toLong()
                            onSeek(target)
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPurple,
                            activeTrackColor = AccentPurple,
                            inactiveTrackColor = Color(0xFF262A3B)
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = formatTime(playbackState.durationMs),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Right: Volume Control & Device Output Icon
            Row(
                modifier = Modifier.width(180.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = if (playbackState.volume > 0f) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
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

// -----------------------------------------------------------------------------
// Interactive Library Workspace: Synced Playlists & Tracklist View
// -----------------------------------------------------------------------------

@Composable
fun LibraryWorkspace(
    library: com.uta.wave.desktop.data.DesktopLibrary,
    isSyncing: Boolean,
    currentlyPlayingVideoId: String?,
    onSyncNow: () -> Unit,
    onPlayPlaylist: (DesktopPlaylist, Int) -> Unit,
    onShufflePlaylist: (DesktopPlaylist) -> Unit,
    onPlayTrackInPlaylist: (DesktopPlaylist, Int) -> Unit
) {
    var selectedPlaylist by remember { mutableStateOf<DesktopPlaylist?>(null) }

    // If a playlist is selected, keep its reference updated if library re-syncs
    val activePlaylist = selectedPlaylist?.let { cur ->
        library.playlists.firstOrNull { it.id == cur.id } ?: cur
    }

    if (activePlaylist != null) {
        // Detailed Playlist View (matching Image 1)
        PlaylistDetailView(
            playlist = activePlaylist,
            currentlyPlayingVideoId = currentlyPlayingVideoId,
            onBack = { selectedPlaylist = null },
            onPlayAll = { onPlayPlaylist(activePlaylist, 0) },
            onShuffle = { onShufflePlaylist(activePlaylist) },
            onPlayTrack = { index -> onPlayTrackInPlaylist(activePlaylist, index) }
        )
    } else {
        // Main Library Overview (Grid of Playlists)
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("UTA-WAVE Mobile Library", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Playlists, collections, and liked songs synced with your Namikaze aftab mobile account.", fontSize = 13.sp, color = TextSecondary)
                }

                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2230)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentPurple, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...", fontSize = 13.sp, color = TextPrimary)
                    } else {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync", tint = AccentPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Now", fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (library.playlists.isEmpty() && isSyncing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentPurple)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Hydrating your playlists from Cloud...", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Liked Songs Card
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val likedPlaylist = DesktopPlaylist(
                                        id = "liked_songs",
                                        name = "Liked Songs",
                                        description = "Your favorite tracks",
                                        trackCount = library.likedSongs.size,
                                        tracks = library.likedSongs
                                    )
                                    selectedPlaylist = likedPlaylist
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF4C1D95), Color(0xFF831843))
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                    Icon(
                                        Icons.Filled.Favorite,
                                        contentDescription = "Liked",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Liked Songs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${library.likedSongs.size} tracks", fontSize = 12.sp, color = Color(0xFFE2E8F0))
                                }
                            }
                        }
                    }

                    // User Synced Playlists
                    items(library.playlists) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { selectedPlaylist = playlist },
                            onPlay = { onPlayPlaylist(playlist, 0) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: DesktopPlaylist,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Artwork Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2230))
            ) {
                if (!playlist.coverUrl.isNullOrBlank()) {
                    NetworkThumbnail(url = playlist.coverUrl, modifier = Modifier.fillMaxSize())
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(40.dp))
                    }
                }

                // Quick Play Floating Button
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(38.dp)
                        .background(AccentPurple, CircleShape)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = playlist.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.trackCount} tracks",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Playlist Detail View (Inspired by Image 1)
// -----------------------------------------------------------------------------

@Composable
fun PlaylistDetailView(
    playlist: DesktopPlaylist,
    currentlyPlayingVideoId: String?,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onPlayTrack: (Int) -> Unit
) {
    val totalSeconds = playlist.tracks.sumOf { it.durationSeconds }
    val formattedDuration = formatPlaylistDuration(totalSeconds)

    Column(modifier = Modifier.fillMaxSize()) {
        // Back Navigation Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Back to Playlists", fontSize = 13.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Playlist Header Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            Surface(
                modifier = Modifier.size(180.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E2230),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                if (!playlist.coverUrl.isNullOrBlank()) {
                    NetworkThumbnail(url = playlist.coverUrl, modifier = Modifier.fillMaxSize())
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(60.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Playlist Info & Actions
            Column {
                Text("PLAYLIST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                if (!playlist.description.isNullOrBlank() && playlist.description != playlist.name) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(playlist.description, fontSize = 18.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(playlist.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("${playlist.tracks.size} tracks · $formattedDuration", fontSize = 13.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play All", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = onShuffle,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", color = TextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tracks List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(playlist.tracks) { index, track ->
                val isPlaying = track.videoId == currentlyPlayingVideoId
                PlaylistTrackRow(
                    index = index + 1,
                    track = track,
                    isPlaying = isPlaying,
                    onClick = { onPlayTrack(index) }
                )
            }
        }
    }
}

@Composable
fun PlaylistTrackRow(
    index: Int,
    track: TrackItem,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isPlaying) AccentPurple.copy(alpha = 0.15f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Track Index / Speaker
                Box(
                    modifier = Modifier.width(28.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isPlaying) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                    } else {
                        Text(index.toString(), fontSize = 13.sp, color = TextSecondary)
                    }
                }

                // Thumbnail
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF262A3B)
                ) {
                    if (track.artworkUrl.isNotBlank()) {
                        NetworkThumbnail(url = track.artworkUrl, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AccentPurple)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 14.sp,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPlaying) AccentPurple else TextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Duration
            if (track.durationSeconds > 0) {
                Text(
                    text = formatTime(track.durationSeconds * 1000L),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Search & Explore Workspaces
// -----------------------------------------------------------------------------

@Composable
fun SearchWorkspace(
    musicService: DesktopMusicService,
    onPlayTrack: (TrackItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun performSearch() {
        if (searchQuery.isBlank()) return
        isSearching = true
        scope.launch {
            val results = musicService.searchSongs(searchQuery)
            searchResults = results
            isSearching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = AccentPurple)
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search songs, artists, or albums...", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() })
            )
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondary)
                }
            }
            Button(
                onClick = { performSearch() },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Search", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentPurple)
            }
        } else if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { track ->
                    TrackListItem(track = track, onPlay = { onPlayTrack(track) })
                }
            }
        } else {
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
                    Icon(Icons.Filled.Search, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Search YouTube Music", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Direct, quota-free YouTube Music autocomplete, artists, albums, and tracks search.", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ExploreWorkspace(onPlayTrack: (TrackItem) -> Unit) {
    val trendingTracks = remember {
        listOf(
            TrackItem(
                id = "youtube:ddWkP5I1JRc",
                videoId = "ddWkP5I1JRc",
                title = "Ajj Din Chadheya",
                artist = "Pritam, Rahat Fateh Ali Khan",
                artworkUrl = "https://yt3.googleusercontent.com/bI7fR_s3N9aO5HJ5O4B4rZdrJ-6ZUx7nWjvzC3u8c0-J3EFS8kFwmIoNjfoHSD9YO0L-wic4F2olS5so=w544-h544-l90-rj",
                durationSeconds = 315
            ),
            TrackItem(
                id = "youtube:NPC-9hYbkLE",
                videoId = "NPC-9hYbkLE",
                title = "Chahun Main Ya Naa",
                artist = "Arijit Singh, Palak Muchhal",
                artworkUrl = "https://i.ytimg.com/vi/NPC-9hYbkLE/hqdefault.jpg",
                durationSeconds = 305
            ),
            TrackItem(
                id = "youtube:glXgzvL3qpc",
                videoId = "glXgzvL3qpc",
                title = "Tere Hawaale",
                artist = "Arijit Singh, Shilpa Rao, Pritam",
                artworkUrl = "https://i.ytimg.com/vi/glXgzvL3qpc/hqdefault.jpg",
                durationSeconds = 350
            ),
            TrackItem(
                id = "youtube:BddP6PYo2gs",
                videoId = "BddP6PYo2gs",
                title = "Kesariya",
                artist = "Arijit Singh, Pritam",
                artworkUrl = "https://i.ytimg.com/vi/BddP6PYo2gs/hqdefault.jpg",
                durationSeconds = 268
            ),
            TrackItem(
                id = "youtube:IltsaltcaEQ",
                videoId = "IltsaltcaEQ",
                title = "Tum Se Hi",
                artist = "Mohit Chauhan, Pritam",
                artworkUrl = "https://i.ytimg.com/vi/IltsaltcaEQ/hqdefault.jpg",
                durationSeconds = 323
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("🔥 Trending Hits & Quick Play", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Click any song to start playing instantly on your Windows PC.", fontSize = 13.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trendingTracks) { track ->
                TrackListItem(track = track, onPlay = { onPlayTrack(track) })
            }
        }
    }
}

@Composable
fun TrackListItem(track: TrackItem, onPlay: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onPlay() },
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF262A3B)
                ) {
                    if (track.artworkUrl.isNotBlank()) {
                        NetworkThumbnail(
                            url = track.artworkUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AccentPurple)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(text = track.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = track.artist, fontSize = 13.sp, color = TextSecondary, maxLines = 1)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.durationSeconds > 0) {
                    Text(text = formatTime(track.durationSeconds * 1000L), fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.width(16.dp))
                }
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(36.dp)
                        .background(AccentPurple.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = AccentPurple, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun NetworkThumbnail(url: String, modifier: Modifier = Modifier) {
    var imageBitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val bytes = URI(url).toURL().readBytes()
                imageBitmap = loadImageBitmap(ByteArrayInputStream(bytes))
            } catch (_: Exception) {}
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF262A3B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AccentPurple)
        }
    }
}

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

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatPlaylistDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "$hours hr $minutes min" else "$minutes min"
}
