package com.uta.wave.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.audio.WindowsAudioPlayerEngine
import com.uta.wave.desktop.data.DesktopMusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.loadImageBitmap
import java.io.ByteArrayInputStream
import java.awt.Dimension
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

    // Dedicated Windows audio engine & InnerTube search service
    val audioEngine = remember { WindowsAudioPlayerEngine() }
    val musicService = remember { DesktopMusicService() }
    val playbackState by audioEngine.state.collectAsState()
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(DesktopNavTab.EXPLORE) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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
                    isCloudSynced = true
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
                    IconButton(onClick = { onSeek(0L) }) {
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
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = TextPrimary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Repeat, contentDescription = "Repeat", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Interactive Scrubber Slider
                val currentPos = playbackState.positionMs.toFloat()
                val totalDuration = if (playbackState.durationMs > 0) playbackState.durationMs.toFloat() else 1f

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
                    Slider(
                        value = (currentPos / totalDuration).coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            onSeek((fraction * totalDuration).toLong())
                        },
                        modifier = Modifier.weight(1f).height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPurple,
                            activeTrackColor = AccentPurple,
                            inactiveTrackColor = Color(0xFF262A3B)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTime(playbackState.durationMs),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Right: Volume Slider
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
// Interactive Workspaces: Search & Explore
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
        // Search Input Bar
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

        // Search Results List
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
            // Initial Empty Search State
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
                    Text("Type a song title, artist, or album and press Search.", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ExploreWorkspace(onPlayTrack: (TrackItem) -> Unit) {
    // Trending quick-play tracks ready on desktop
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
                id = "youtube:Mz2yP0A3t4o",
                videoId = "Mz2yP0A3t4o",
                title = "Chahun Main Ya Naa",
                artist = "Arijit Singh, Palak Muchhal",
                artworkUrl = "https://i.ytimg.com/vi/Mz2yP0A3t4o/hqdefault.jpg",
                durationSeconds = 304
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

// Remaining Workspace Placeholders
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
