package com.uta.wave.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.account.DesktopAccountManager
import com.uta.wave.desktop.account.DesktopUserProfile
import com.uta.wave.desktop.audio.WindowsAudioPlayerEngine
import com.uta.wave.desktop.audio.NextTrackCandidate
import com.uta.wave.desktop.data.DesktopDatabase
import com.uta.wave.desktop.data.DesktopMusicService
import com.uta.wave.desktop.data.DesktopPlaylist
import com.uta.wave.desktop.ui.*
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
    HOME("Home", { Icon(Icons.Filled.Home, contentDescription = "Home") }),
    LIBRARY("Library", { Icon(Icons.Filled.LibraryMusic, contentDescription = "Library") }),
    SETTINGS("Settings", { Icon(Icons.Filled.Settings, contentDescription = "Settings") }),
    SEARCH("Search", { Icon(Icons.Filled.Search, contentDescription = "Search") })
}

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("[Fatal] Uncaught exception on ${thread.name}: ${throwable.message}")
        kotlin.system.exitProcess(1)
    }

    application {
    val windowState = rememberWindowState(
        size = DpSize(1240.dp, 840.dp),
        position = WindowPosition(Alignment.Center)
    )

    val audioEngine = remember { WindowsAudioPlayerEngine() }
    val musicService = remember { DesktopMusicService() }
    val accountManager = remember { DesktopAccountManager.getInstance() }
    val database = remember { DesktopDatabase.getInstance() }
    val smtcManager = remember { WindowsSmtcManager() }

    val playbackState by audioEngine.state.collectAsState()
    val userProfile by accountManager.profile.collectAsState()
    val libraryData by database.library.collectAsState()
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(DesktopNavTab.LIBRARY) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var isBottomBarCollapsed by remember { mutableStateOf(false) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isAmbientModeActive by remember { mutableStateOf(false) }
    var previousWindowStatePlacement by remember { mutableStateOf(WindowPlacement.Floating) }

    LaunchedEffect(isPlayerExpanded) {
        if (isPlayerExpanded) {
            previousWindowStatePlacement = windowState.placement
            windowState.placement = WindowPlacement.Maximized
        } else {
            windowState.placement = previousWindowStatePlacement
        }
    }



    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -8f && !isBottomBarCollapsed) {
                    isBottomBarCollapsed = true
                } else if (delta > 8f && isBottomBarCollapsed) {
                    isBottomBarCollapsed = false
                }
                return Offset.Zero
            }
        }
    }

    // Playlist Queue management
    var activeQueue by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var currentQueueIndex by remember { mutableStateOf(-1) }

    // Smart Queue Management & Playback Modes
    var repeatMode by remember { mutableStateOf(DesktopRepeatMode.OFF) }
    var isShuffle by remember { mutableStateOf(false) }
    var unshuffledQueue by remember { mutableStateOf<List<TrackItem>?>(null) }
    var isQueueLocked by remember { mutableStateOf(false) }
    var showQueueSheetModal by remember { mutableStateOf(false) }
    var activeRadioVideoId by remember { mutableStateOf<String?>(null) }
    var isFetchingRadioContinuation by remember { mutableStateOf(false) }

    // 3-Dots Song Menu Dialog State
    var activeSongMenuTrack by remember { mutableStateOf<TrackItem?>(null) }
    var activeSongMenuPlaylistId by remember { mutableStateOf<String?>(null) }
    var activeSongMenuIsInQueue by remember { mutableStateOf(false) }

    fun playNext(track: TrackItem) {
        val list = activeQueue.toMutableList()
        val insertIdx = if (currentQueueIndex >= 0 && currentQueueIndex < list.size) currentQueueIndex + 1 else 0
        list.add(insertIdx, track)
        activeQueue = list
        statusMessage = "Playing \"${track.title}\" next."
    }

    fun addToQueue(track: TrackItem) {
        activeQueue = activeQueue + track
        statusMessage = "Added \"${track.title}\" to queue."
    }

    fun shareTrack(track: TrackItem) {
        val url = "https://music.youtube.com/watch?v=${track.videoId}"
        try {
            val selection = java.awt.datatransfer.StringSelection(url)
            java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
            statusMessage = "Link copied to clipboard: $url"
        } catch (e: Exception) {
            statusMessage = "Could not copy link to clipboard."
        }
    }

    val secureRandom = remember { java.security.SecureRandom() }
    val recentShuffleOpeningIds = remember { java.util.concurrent.ConcurrentLinkedDeque<String>() }
    val maxRecentOpeningHistory = 15

    fun smartShuffle(tracks: List<TrackItem>, currentTrack: TrackItem? = null): List<TrackItem> {
        if (tracks.size <= 1) return tracks

        // 1. Determine opening track
        val openingTrack: TrackItem = if (currentTrack != null) {
            currentTrack
        } else {
            val candidatePool = tracks.toList()
            var chosen = candidatePool[secureRandom.nextInt(candidatePool.size)]
            if (candidatePool.size > maxRecentOpeningHistory) {
                var attempts = 0
                while (attempts < 12 && (chosen.id in recentShuffleOpeningIds || chosen.videoId in recentShuffleOpeningIds)) {
                    chosen = candidatePool[secureRandom.nextInt(candidatePool.size)]
                    attempts++
                }
            }
            recentShuffleOpeningIds.addLast(chosen.id)
            if (chosen.videoId.isNotBlank()) recentShuffleOpeningIds.addLast(chosen.videoId)
            while (recentShuffleOpeningIds.size > maxRecentOpeningHistory * 2) {
                recentShuffleOpeningIds.pollFirst()
            }
            chosen
        }

        // 2. High-entropy Fisher-Yates shuffle on the remainder
        val rest = tracks.filterNot { it.id == openingTrack.id || (it.videoId.isNotBlank() && it.videoId == openingTrack.videoId) }.toMutableList()
        for (i in rest.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = rest[i]
            rest[i] = rest[j]
            rest[j] = tmp
        }

        // 3. Assemble combined queue
        val result = ArrayList<TrackItem>(tracks.size)
        result.add(openingTrack)
        result.addAll(rest)

        // 4. Adjacent-artist relaxation pass
        if (result.size > 2) {
            for (i in 1 until (result.size - 1)) {
                val currentArtist = result[i].artist.trim()
                val prevArtist = result[i - 1].artist.trim()

                if (currentArtist.isNotBlank() && currentArtist.equals(prevArtist, ignoreCase = true)) {
                    val searchLimit = minOf(result.size, i + 8)
                    for (j in (i + 1) until searchLimit) {
                        val candidateArtist = result[j].artist.trim()
                        if (!candidateArtist.equals(prevArtist, ignoreCase = true)) {
                            val swapTmp = result[i]
                            result[i] = result[j]
                            result[j] = swapTmp
                            break
                        }
                    }
                }
            }
        }

        return result
    }

    fun toggleShuffle() {
        if (activeQueue.isEmpty()) {
            isShuffle = !isShuffle
            return
        }
        val newShuffle = !isShuffle
        if (newShuffle) {
            unshuffledQueue = activeQueue
            val currentTrack = activeQueue.getOrNull(currentQueueIndex)
            val shuffled = smartShuffle(activeQueue, currentTrack)
            activeQueue = shuffled
            currentQueueIndex = 0
            isShuffle = true
        } else {
            val restored = unshuffledQueue ?: activeQueue
            unshuffledQueue = null
            val currentTrack = activeQueue.getOrNull(currentQueueIndex)
            val newIndex = if (currentTrack != null) {
                restored.indexOfFirst { it.id == currentTrack.id || it.videoId == currentTrack.videoId }.coerceAtLeast(0)
            } else 0
            activeQueue = restored
            currentQueueIndex = newIndex
            isShuffle = false
        }
    }

    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            DesktopRepeatMode.OFF -> DesktopRepeatMode.ALL
            DesktopRepeatMode.ALL -> DesktopRepeatMode.ONE
            DesktopRepeatMode.ONE -> DesktopRepeatMode.OFF
        }
        statusMessage = "Repeat mode: ${repeatMode.label}"
    }

    fun moveQueueTrack(fromIndex: Int, delta: Int) {
        if (isQueueLocked) return
        val toIndex = fromIndex + delta
        if (fromIndex !in activeQueue.indices || toIndex !in activeQueue.indices) return
        val list = activeQueue.toMutableList()
        val currentTrack = activeQueue.getOrNull(currentQueueIndex)
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        activeQueue = list
        if (currentTrack != null) {
            val newIdx = list.indexOfFirst { it.id == currentTrack.id || it.videoId == currentTrack.videoId }
            if (newIdx >= 0) currentQueueIndex = newIdx
        }
    }

    fun removeFromQueue(track: TrackItem) {
        if (isQueueLocked) return
        val cleanId = track.videoId.ifBlank { track.id }
        val list = activeQueue.toMutableList()
        val idx = list.indexOfFirst { it.videoId == cleanId || it.id == track.id }
        if (idx >= 0) {
            list.removeAt(idx)
            activeQueue = list
            if (currentQueueIndex > idx) currentQueueIndex--
            else if (currentQueueIndex >= list.size) currentQueueIndex = list.size - 1
            statusMessage = "Removed \"${track.title}\" from queue."
        }
    }

    fun removeMultipleFromQueue(ids: Set<String>) {
        if (isQueueLocked || ids.isEmpty()) return
        val currentTrack = activeQueue.getOrNull(currentQueueIndex)
        val filtered = activeQueue.filter { it.id !in ids && it.videoId !in ids }
        activeQueue = filtered
        val newIdx = if (currentTrack != null) {
            filtered.indexOfFirst { it.id == currentTrack.id || it.videoId == currentTrack.videoId }.coerceAtLeast(0)
        } else 0
        currentQueueIndex = newIdx
        statusMessage = "Removed ${ids.size} tracks from queue."
    }

    fun clearUpcomingQueue() {
        if (isQueueLocked) return
        val currentTrack = activeQueue.getOrNull(currentQueueIndex)
        activeQueue = if (currentTrack != null) listOf(currentTrack) else emptyList()
        currentQueueIndex = 0
        statusMessage = "Upcoming queue cleared."
    }

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
            val track = activeQueue[index]
            playTrack(track)

            // Auto-continuation for algorithmic radio stations
            if (activeRadioVideoId != null && index >= activeQueue.size - 3 && !isFetchingRadioContinuation) {
                scope.launch {
                    isFetchingRadioContinuation = true
                    try {
                        val trending = musicService.getTrending()
                        val currentKeys = activeQueue.map { it.videoId }.toSet()
                        val more = trending.filter { it.videoId !in currentKeys }.take(8)
                        if (more.isNotEmpty()) {
                            activeQueue = activeQueue + more
                        }
                    } catch (e: Exception) {
                        // ignore
                    } finally {
                        isFetchingRadioContinuation = false
                    }
                }
            }
        }
    }

    fun saveQueueAsPlaylist(name: String) {
        if (activeQueue.isEmpty()) return
        database.createPlaylist(name, null)
        val playlists = database.loadLibrary().playlists
        val created = playlists.firstOrNull { it.name == name }
        if (created != null) {
            activeQueue.forEach { database.addTrackToPlaylist(created.id, it) }
            statusMessage = "Saved ${activeQueue.size} tracks to \"$name\"."
        }
    }

    fun startRadio(track: TrackItem) {
        activeRadioVideoId = track.videoId
        statusMessage = "Starting \"${track.title}\" Radio..."
        scope.launch {
            try {
                val trending = musicService.getTrending().filter { it.videoId != track.videoId }
                val radioList = listOf(track) + trending
                activeQueue = radioList
                currentQueueIndex = 0
                playTrack(track)
                statusMessage = "Playing \"${track.title}\" Radio."
            } catch (e: Exception) {
                playTrack(track)
            }
        }
    }

    fun playPlaylist(playlist: DesktopPlaylist, startIndex: Int = 0) {
        if (playlist.tracks.isNotEmpty()) {
            activeQueue = playlist.tracks
            playQueueTrack(startIndex)
        } else {
            val cookie = userProfile.cookie
            if (!cookie.isNullOrBlank()) {
                scope.launch {
                    statusMessage = "Loading \"${playlist.name}\" tracks..."
                    try {
                        val (_, tracks) = musicService.getPlaylist(playlist.id, cookie, userProfile.sapisid)
                        if (tracks.isNotEmpty()) {
                            database.updatePlaylistTracks(playlist.id, tracks)
                            activeQueue = tracks
                            playQueueTrack(startIndex)
                            statusMessage = null
                        } else {
                            statusMessage = "No tracks found for \"${playlist.name}\"."
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error loading playlist: ${e.message}"
                    }
                }
            }
        }
    }

    fun shufflePlaylist(playlist: DesktopPlaylist) {
        if (playlist.tracks.isNotEmpty()) {
            activeQueue = playlist.tracks.shuffled()
            playQueueTrack(0)
        } else {
            val cookie = userProfile.cookie
            if (!cookie.isNullOrBlank()) {
                scope.launch {
                    statusMessage = "Loading \"${playlist.name}\" tracks..."
                    try {
                        val (_, tracks) = musicService.getPlaylist(playlist.id, cookie, userProfile.sapisid)
                        if (tracks.isNotEmpty()) {
                            database.updatePlaylistTracks(playlist.id, tracks)
                            activeQueue = tracks.shuffled()
                            playQueueTrack(0)
                            statusMessage = null
                        } else {
                            statusMessage = "No tracks found for \"${playlist.name}\"."
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error loading playlist: ${e.message}"
                    }
                }
            }
        }
    }

    // Auto-advance & Mobile-Parity Equal-Power Crossfade Orchestration
    LaunchedEffect(repeatMode, activeQueue, currentQueueIndex) {
        audioEngine.streamUrlResolver = { videoId ->
            musicService.resolveStreamUrl(videoId)
        }

        audioEngine.nextTrackProvider = {
            if (activeQueue.isEmpty() || currentQueueIndex < 0) null
            else {
                val nextIndex = when (repeatMode) {
                    DesktopRepeatMode.ONE -> currentQueueIndex
                    DesktopRepeatMode.ALL -> (currentQueueIndex + 1) % activeQueue.size
                    DesktopRepeatMode.OFF -> {
                        if (currentQueueIndex + 1 < activeQueue.size) currentQueueIndex + 1 else -1
                    }
                }
                if (nextIndex in activeQueue.indices) {
                    val currentTrack = activeQueue.getOrNull(currentQueueIndex)
                    val nextTrack = activeQueue[nextIndex]
                    val isSameAlbum = !currentTrack?.album.isNullOrBlank() &&
                            currentTrack?.album.equals(nextTrack.album, ignoreCase = true)
                    NextTrackCandidate(
                        track = nextTrack,
                        queueIndex = nextIndex,
                        isSameAlbum = isSameAlbum
                    )
                } else null
            }
        }

        audioEngine.onTrackSwapped = { _, newIndex ->
            currentQueueIndex = newIndex
            // Auto-continuation check for algorithmic radio stations
            if (activeRadioVideoId != null && newIndex >= activeQueue.size - 3 && !isFetchingRadioContinuation) {
                scope.launch {
                    isFetchingRadioContinuation = true
                    try {
                        val trending = musicService.getTrending()
                        val currentKeys = activeQueue.map { it.videoId }.toSet()
                        val more = trending.filter { it.videoId !in currentKeys }.take(8)
                        if (more.isNotEmpty()) {
                            activeQueue = activeQueue + more
                        }
                    } catch (_: Exception) {
                    } finally {
                        isFetchingRadioContinuation = false
                    }
                }
            }
        }

        audioEngine.onTrackEnded = {
            when (repeatMode) {
                DesktopRepeatMode.ONE -> {
                    audioEngine.seekTo(0L)
                    audioEngine.play()
                }
                DesktopRepeatMode.ALL -> {
                    if (activeQueue.isNotEmpty()) {
                        val nextIndex = (currentQueueIndex + 1) % activeQueue.size
                        playQueueTrack(nextIndex)
                    }
                }
                DesktopRepeatMode.OFF -> {
                    if (currentQueueIndex + 1 < activeQueue.size) {
                        playQueueTrack(currentQueueIndex + 1)
                    }
                }
            }
        }
    }

    // Connect Windows System Media Transport Controls (SMTC) for Dynamic Island & volume overlay
    LaunchedEffect(activeQueue, currentQueueIndex) {
        smtcManager.initSystemMediaTransportControls(
            onPlayClicked = {
                if (!playbackState.isPlaying) {
                    audioEngine.play()
                }
            },
            onPauseClicked = {
                if (playbackState.isPlaying) {
                    audioEngine.pause()
                }
            },
            onNextClicked = {
                if (currentQueueIndex + 1 < activeQueue.size) {
                    playQueueTrack(currentQueueIndex + 1)
                }
            },
            onPreviousClicked = {
                if (playbackState.positionMs > 3000L) {
                    audioEngine.seekTo(0L)
                } else if (currentQueueIndex > 0) {
                    playQueueTrack(currentQueueIndex - 1)
                } else {
                    audioEngine.seekTo(0L)
                }
            },
            onStopClicked = {
                audioEngine.pause()
            }
        )
    }

    LaunchedEffect(playbackState.title, playbackState.artist, playbackState.isPlaying, playbackState.artworkUrl) {
        smtcManager.updateSmtcPlaybackState(playbackState)
    }

    fun triggerMobileSync() {
        if (isSyncing) return
        isSyncing = true
        scope.launch {
            try {
                val currentP = accountManager.currentProfile
                val cookie = currentP.cookie
                if (!cookie.isNullOrBlank()) {
                    statusMessage = "Syncing with UTA-WAVE Mobile account..."
                    val realProfile = musicService.fetchAccountProfile(cookie, currentP.sapisid)
                    if (realProfile != null && realProfile.name.isNotBlank()) {
                        accountManager.saveSession(
                            cookieHeader = cookie,
                            userName = realProfile.name,
                            userEmail = realProfile.email,
                            avatarUrl = realProfile.avatarUrl,
                            handle = realProfile.handle
                        )
                    }
                    musicService.syncFullLibrary(cookie, currentP.sapisid)
                    statusMessage = null
                } else {
                    statusMessage = "No active mobile session cookie."
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
                smtcManager.release()
                audioEngine.release()
            } catch (_: Exception) {}
            exitApplication()
            kotlin.system.exitProcess(0)
        },
        state = windowState,
        title = "UTA-WAVE — A Universe of Music",
        undecorated = true,
        icon = painterResource("icon.png")
    ) {
        window.minimumSize = Dimension(960, 640)
        val desktopBackdrop = rememberDesktopBackdrop()

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
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF160E26), Color(0xFF090A0F)),
                            center = androidx.compose.ui.geometry.Offset(700f, 350f),
                            radius = 1200f
                        )
                    )
            ) {
                // Top Custom Header / Window Control Bar (Hidden when in Fullscreen or Ambient Mode)
                if (!isPlayerExpanded && !isAmbientModeActive) {
                    WindowDraggableArea {
                        DesktopTopHeader(
                            activeSessionDevice = "Windows PC",
                            isCloudSynced = !isSyncing,
                            userProfile = userProfile,
                            onAccountClick = { showAccountDialog = true },
                            onMinimize = { windowState.isMinimized = true },
                            onMaximizeRestore = {
                                windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                    WindowPlacement.Floating
                                } else {
                                    WindowPlacement.Maximized
                                }
                            },
                            onClose = {
                                try { audioEngine.release() } catch (_: Exception) {}
                                exitApplication()
                                kotlin.system.exitProcess(0)
                            },
                            isMaximized = windowState.placement == WindowPlacement.Maximized,
                            onToggleAmbientMode = { isAmbientModeActive = !isAmbientModeActive }
                        )
                    }
                }



                // Main Content Canvas with Floating Liquid Glass Navigation Dock & MiniPlayer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    // Full-Bleed Content Workspace (recorded into backdrop layer for live refraction)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .desktopBackdropLayer(desktopBackdrop)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        when (currentTab) {
                            DesktopNavTab.HOME -> DesktopHomeScreen(
                                musicService = musicService,
                                library = libraryData,
                                userProfile = userProfile,
                                currentTrackId = playbackState.videoId,
                                onPlayTrack = { track, queue ->
                                    activeQueue = queue
                                    val idx = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                    playQueueTrack(idx)
                                },
                                onNavigateToLibrary = { currentTab = DesktopNavTab.LIBRARY },
                                onNavigateToSearch = { currentTab = DesktopNavTab.SEARCH },
                                onMoreClick = { track ->
                                    activeSongMenuTrack = track
                                    activeSongMenuPlaylistId = null
                                    activeSongMenuIsInQueue = false
                                }
                            )
                            DesktopNavTab.SEARCH -> SearchWorkspace(
                                musicService = musicService,
                                onPlayTrack = ::playTrack,
                                onMoreClick = { track ->
                                    activeSongMenuTrack = track
                                    activeSongMenuPlaylistId = null
                                    activeSongMenuIsInQueue = false
                                }
                            )
                            DesktopNavTab.LIBRARY -> LibraryWorkspace(
                                library = libraryData,
                                userProfile = userProfile,
                                isSyncing = isSyncing,
                                currentlyPlayingVideoId = playbackState.videoId,
                                onSyncNow = ::triggerMobileSync,
                                onSignInClick = { showAccountDialog = true },
                                onPlayPlaylist = ::playPlaylist,
                                onShufflePlaylist = ::shufflePlaylist,
                                onPlayTrackInPlaylist = { playlist, index ->
                                    activeQueue = playlist.tracks
                                    playQueueTrack(index)
                                },
                                onHydratePlaylist = { playlist ->
                                    val cookie = userProfile.cookie
                                    val sapisid = userProfile.sapisid
                                    if (!cookie.isNullOrBlank()) {
                                        val (_, tracks) = musicService.getPlaylist(playlist.id, cookie, sapisid)
                                        if (tracks.isNotEmpty()) {
                                            database.updatePlaylistTracks(playlist.id, tracks)
                                        }
                                    }
                                },
                                onTrackMoreClick = { track, playlist ->
                                    activeSongMenuTrack = track
                                    activeSongMenuPlaylistId = playlist?.id
                                    activeSongMenuIsInQueue = false
                                }
                            )
                            DesktopNavTab.SETTINGS -> DesktopSettingsScreen(
                                userProfile = userProfile,
                                database = database,
                                onSyncNow = ::triggerMobileSync,
                                onOpenAccountDialog = { showAccountDialog = true },
                                onSignOut = {
                                    accountManager.clearSession()
                                    statusMessage = null
                                }
                            )
                        }
                    }

                    val hasTrack = currentQueueIndex in activeQueue.indices || !playbackState.videoId.isNullOrBlank()
                    val currentTrackItem = activeQueue.getOrNull(currentQueueIndex)

                    // 1. Floating MiniPlayer (Docked above floating nav bar, collapsed on scroll)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isBottomBarCollapsed && hasTrack,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(220)),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(180)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 86.dp)
                    ) {
                        DesktopMiniPlayer(
                            playbackState = playbackState,
                            onTogglePlayPause = {
                                if (playbackState.isPlaying) {
                                    audioEngine.pause()
                                } else {
                                    audioEngine.play()
                                }
                            },
                            onNext = {
                                if (currentQueueIndex + 1 < activeQueue.size) {
                                    playQueueTrack(currentQueueIndex + 1)
                                }
                            },
                            onPrevious = {
                                if (currentQueueIndex > 0) {
                                    playQueueTrack(currentQueueIndex - 1)
                                } else {
                                    audioEngine.seekTo(0L)
                                }
                            },
                            onExpand = { isPlayerExpanded = true },
                            onOpenQueue = { showQueueSheetModal = true },
                            onVolumeChange = { audioEngine.setVolume(it) },
                            onSeekTo = { audioEngine.seekTo(it) },
                            backdrop = desktopBackdrop
                        )
                    }

                    // 2. Iconic UTA-WAVE Liquid Glass Floating Navigation Bar (Docked Bottom-Center, Dual-Mode)
                    DesktopFloatingNavBar(
                        selectedTab = currentTab,
                        onTabSelected = { currentTab = it },
                        isCollapsed = isBottomBarCollapsed,
                        onExpandBar = { isBottomBarCollapsed = false },
                        currentTrack = currentTrackItem,
                        playbackState = playbackState,
                        onTogglePlayPause = {
                            if (playbackState.isPlaying) {
                                audioEngine.pause()
                            } else {
                                audioEngine.play()
                            }
                        },
                        onExpandPlayer = { isPlayerExpanded = true },
                        backdrop = desktopBackdrop,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                    )
                }
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
                        accountManager.importToken(token, "Connected User")
                        triggerMobileSync()
                    },
                    onImportBackup = { jsonStr ->
                        val ok = database.importMobileBackupJson(jsonStr)
                        if (ok) {
                            statusMessage = "Restored mobile library backup successfully!"
                        }
                    },
                    onSignOut = {
                        accountManager.clearSession()
                        statusMessage = "Signed out and reset desktop data."
                    }
                )
            }

            // Fullscreen Cinematic Now-Playing Overlay
            DesktopFullscreenPlayerOverlay(
                visible = isPlayerExpanded,
                playbackState = playbackState,
                queue = activeQueue,
                currentQueueIndex = currentQueueIndex,
                onCollapse = { isPlayerExpanded = false },
                onMinimize = { windowState.isMinimized = true },
                onClose = {
                    try { audioEngine.release() } catch (_: Exception) {}
                    exitApplication()
                    kotlin.system.exitProcess(0)
                },
                onTogglePlayPause = {
                    if (playbackState.isPlaying) {
                        audioEngine.pause()
                    } else {
                        audioEngine.play()
                    }
                },
                onNext = {
                    if (currentQueueIndex + 1 < activeQueue.size) {
                        playQueueTrack(currentQueueIndex + 1)
                    }
                },
                onPrevious = {
                    if (currentQueueIndex > 0) {
                        playQueueTrack(currentQueueIndex - 1)
                    } else {
                        audioEngine.seekTo(0L)
                    }
                },
                onSeekTo = { audioEngine.seekTo(it) },
                onVolumeChange = { audioEngine.setVolume(it) },
                onSelectQueueTrack = { index -> playQueueTrack(index) },
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                isQueueLocked = isQueueLocked,
                database = database,
                musicService = musicService,
                onToggleShuffle = ::toggleShuffle,
                onToggleRepeat = ::toggleRepeat,
                onToggleLock = { isQueueLocked = !isQueueLocked },
                onMoveTrack = ::moveQueueTrack,
                onRemoveFromQueue = ::removeFromQueue,
                onRemoveMultipleFromQueue = ::removeMultipleFromQueue,
                onClearQueue = ::clearUpcomingQueue,
                onSaveQueueAsPlaylist = ::saveQueueAsPlaylist,
                onInsertTrackNext = ::playNext,
                onAddToQueue = ::addToQueue,
                onTrackMoreClick = { track ->
                    activeSongMenuTrack = track
                    activeSongMenuPlaylistId = null
                    activeSongMenuIsInQueue = true
                },
                onOpenAmbientMode = {
                    isPlayerExpanded = false
                    isAmbientModeActive = true
                }
            )

            // Master Ambient Canvas Mode Overlay ("Floating Water Media Player")
            if (isAmbientModeActive) {
                DesktopAmbientModeOverlay(
                    playbackState = playbackState,
                    currentTrack = activeQueue.getOrNull(currentQueueIndex) ?: if (playbackState.title.isNotBlank()) {
                        TrackItem(
                            id = playbackState.videoId ?: "",
                            videoId = playbackState.videoId ?: "",
                            title = playbackState.title,
                            artist = playbackState.artist,
                            artworkUrl = playbackState.artworkUrl ?: "",
                            durationSeconds = (playbackState.durationMs / 1000).toInt()
                        )
                    } else null,
                    onPlayPause = {
                        if (playbackState.isPlaying) {
                            audioEngine.pause()
                        } else {
                            audioEngine.play()
                        }
                    },
                    onSkipNext = {
                        if (currentQueueIndex + 1 < activeQueue.size) {
                            playQueueTrack(currentQueueIndex + 1)
                        }
                    },
                    onSkipPrevious = {
                        if (currentQueueIndex > 0) {
                            playQueueTrack(currentQueueIndex - 1)
                        } else {
                            audioEngine.seekTo(0L)
                        }
                    },
                    onSeek = { seconds ->
                        audioEngine.seekTo((seconds * 1000).toLong())
                    },
                    onVolumeChange = { audioEngine.setVolume(it) },
                    onToggleShuffle = ::toggleShuffle,
                    onToggleRepeat = ::toggleRepeat,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onExitAmbientMode = { isAmbientModeActive = false }
                )
            }

            // Floating / Windowed Queue Sheet Modal Dialog
            if (showQueueSheetModal) {
                DesktopQueueModalDialog(
                    queue = activeQueue,
                    currentQueueIndex = currentQueueIndex,
                    isPlaying = playbackState.isPlaying,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    isLocked = isQueueLocked,
                    database = database,
                    musicService = musicService,
                    onDismiss = { showQueueSheetModal = false },
                    onSelectQueueTrack = ::playQueueTrack,
                    onToggleShuffle = ::toggleShuffle,
                    onToggleRepeat = ::toggleRepeat,
                    onToggleLock = { isQueueLocked = !isQueueLocked },
                    onMoveTrack = ::moveQueueTrack,
                    onRemoveFromQueue = ::removeFromQueue,
                    onRemoveMultipleFromQueue = ::removeMultipleFromQueue,
                    onClearQueue = ::clearUpcomingQueue,
                    onSaveQueueAsPlaylist = ::saveQueueAsPlaylist,
                    onInsertTrackNext = ::playNext,
                    onAddToQueue = ::addToQueue,
                    onTrackMoreClick = { track ->
                        activeSongMenuTrack = track
                        activeSongMenuPlaylistId = null
                        activeSongMenuIsInQueue = true
                    }
                )
            }

            // 3-Dots Contextual Song Menu Dialog (Parity with Mobile SongMenuBottomSheet)
            val currentSongMenuTrack = activeSongMenuTrack
            if (currentSongMenuTrack != null) {
                DesktopSongMenuDialog(
                    track = currentSongMenuTrack,
                    database = database,
                    isInQueue = activeSongMenuIsInQueue,
                    playlistId = activeSongMenuPlaylistId,
                    onDismiss = {
                        activeSongMenuTrack = null
                        activeSongMenuPlaylistId = null
                        activeSongMenuIsInQueue = false
                    },
                    onPlayNext = { t ->
                        playNext(t)
                    },
                    onAddToQueue = { t ->
                        addToQueue(t)
                    },
                    onStartRadio = { t ->
                        startRadio(t)
                    },
                    onShare = { t ->
                        shareTrack(t)
                    },
                    onRemoveFromQueue = { t ->
                        val list = activeQueue.toMutableList()
                        val idx = list.indexOfFirst { it.videoId == t.videoId || it.id == t.id }
                        if (idx >= 0) {
                            list.removeAt(idx)
                            activeQueue = list
                            if (currentQueueIndex > idx) currentQueueIndex--
                        }
                    },
                    onRemoveFromPlaylist = { pId, t ->
                        database.removeTrackFromPlaylist(pId, t.id.ifBlank { t.videoId })
                    },
                    onViewArtist = { artistName ->
                        currentTab = DesktopNavTab.SEARCH
                    }
                )
            }
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
    onAccountClick: () -> Unit,
    onMinimize: () -> Unit = {},
    onMaximizeRestore: () -> Unit = {},
    onClose: () -> Unit = {},
    isMaximized: Boolean = false,
    onToggleAmbientMode: () -> Unit = {}
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource("icon.png"),
                contentDescription = "UTA-WAVE Icon",
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Text(
                text = "UTA-WAVE",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AccentPurple,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Desktop Edition (Windows)",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (userProfile.isLoggedIn) {
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
                            text = "Device: Windows PC • Synced with Mobile",
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
                            text = if (isCloudSynced) "Synced" else "Syncing...",
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
            } else {
                // NOT LOGGED IN / GUEST: Prominent Sign In Button
                Button(
                    onClick = onAccountClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Sign In",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign In",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Ambient Canvas Mode Pill Button (Hope麻匪 Floating Water Player)
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = Color(0xFF1E2230),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onToggleAmbientMode() }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Ambient Mode",
                        tint = AccentPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Ambient Mode",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Window Action Buttons (Minimize, Maximize/Restore, Close)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Minimize
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.dp)
                            .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(1.dp))
                    )
                }

                // Maximize / Restore
                IconButton(
                    onClick = onMaximizeRestore,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(1.dp))
                    )
                }

                // Close
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(14.dp)
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
    onImportBackup: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }
    var backupInput by remember { mutableStateOf("") }
    var showBackupField by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(520.dp),
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
                    Text(
                        text = if (userProfile.isLoggedIn) "UTA-WAVE Mobile Account" else "Sign In with Mobile Account",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (userProfile.isLoggedIn) {
                    // Logged In Card
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

                            Column(modifier = Modifier.weight(1f)) {
                                Text(userProfile.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(userProfile.email, fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Connected with Mobile", fontSize = 11.sp, color = Color(0xFF34D399))
                                }
                            }
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

                    // Sign Out & Disconnect Button
                    OutlinedButton(
                        onClick = {
                            onSignOut()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out & Clear Desktop Data", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Import Backup Option
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

                } else {
                    // LOGGED OUT / GUEST VIEW: Clear instructions and token paste input!
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF151926),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("📱 How to sign in with your UTA-WAVE Mobile account:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "1. Open the UTA-WAVE app on your Android phone\n" +
                                "2. Go to the Account tab\n" +
                                "3. Tap 'LOGIN WITH TOKEN: Tap to show token'\n" +
                                "4. Tap 'Copy Token' and paste it in the box below",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Mobile Session Token / Cookie", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("Paste token copied from UTA-WAVE Mobile here...", fontSize = 12.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (tokenInput.isNotBlank()) {
                                onImportToken(tokenInput)
                                tokenInput = ""
                                onDismiss()
                            }
                        },
                        enabled = tokenInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Account & Sync Library", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Or import backup file option
                    TextButton(
                        onClick = { showBackupField = !showBackupField },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (showBackupField) "▲ Hide Backup Option" else "▼ Or import uta_wave_library_backup.json",
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
                                    onDismiss()
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
        color = Color(0xF2100C18),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))
            )
        )
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
    userProfile: DesktopUserProfile,
    isSyncing: Boolean,
    currentlyPlayingVideoId: String?,
    onSyncNow: () -> Unit,
    onSignInClick: () -> Unit,
    onPlayPlaylist: (DesktopPlaylist, Int) -> Unit,
    onShufflePlaylist: (DesktopPlaylist) -> Unit,
    onPlayTrackInPlaylist: (DesktopPlaylist, Int) -> Unit,
    onHydratePlaylist: (suspend (DesktopPlaylist) -> Unit)? = null,
    onTrackMoreClick: ((TrackItem, DesktopPlaylist?) -> Unit)? = null
) {
    var selectedPlaylist by remember { mutableStateOf<DesktopPlaylist?>(null) }
    var isHydrating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // If a playlist is selected, keep its reference updated if library re-syncs
    val activePlaylist = selectedPlaylist?.let { cur ->
        library.playlists.firstOrNull { it.id == cur.id } ?: cur
    }

    // Auto-hydrate playlist tracks if selected playlist has 0 tracks but user is signed in
    LaunchedEffect(activePlaylist?.id) {
        if (activePlaylist != null && activePlaylist.tracks.isEmpty() && !userProfile.cookie.isNullOrBlank()) {
            isHydrating = true
            try {
                onHydratePlaylist?.invoke(activePlaylist)
            } catch (e: Exception) {
                println("[LibraryWorkspace] Hydrate error: ${e.message}")
            } finally {
                isHydrating = false
            }
        }
    }

    if (activePlaylist != null) {
        // Detailed Playlist View (matching Image 1)
        PlaylistDetailView(
            playlist = activePlaylist,
            isHydrating = isHydrating,
            currentlyPlayingVideoId = currentlyPlayingVideoId,
            onBack = { selectedPlaylist = null },
            onPlayAll = { onPlayPlaylist(activePlaylist, 0) },
            onShuffle = { onShufflePlaylist(activePlaylist) },
            onPlayTrack = { index -> onPlayTrackInPlaylist(activePlaylist, index) },
            onTrackMoreClick = { track -> onTrackMoreClick?.invoke(track, activePlaylist) },
            onReload = {
                if (!userProfile.cookie.isNullOrBlank()) {
                    scope.launch {
                        isHydrating = true
                        try {
                            onHydratePlaylist?.invoke(activePlaylist)
                        } catch (e: Exception) {
                            println("[LibraryWorkspace] Reload error: ${e.message}")
                        } finally {
                            isHydrating = false
                        }
                    }
                }
            }
        )
    } else if (!userProfile.isLoggedIn && library.playlists.isEmpty() && library.likedSongs.isEmpty()) {
        // Clean Signed-Out / Guest State for Library
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.width(480.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF131722),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(68.dp),
                        shape = CircleShape,
                        color = AccentPurple.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Sign in to Sync Your Library",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Connect your UTA-WAVE Mobile account using your session token to load your playlists, favorite tracks, and saved albums right here on Windows.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In with Mobile Account", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    } else {
        // Main Library Overview (Grid of Playlists)
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("UTA-WAVE Library", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (userProfile.isLoggedIn) "Playlists and liked songs synced with ${userProfile.name}."
                        else "Imported playlists and liked songs.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                if (userProfile.isLoggedIn) {
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
                } else {
                    Button(
                        onClick = onSignInClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Login, contentDescription = "Sign In", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In", fontSize = 13.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (library.playlists.isEmpty() && library.likedSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isSyncing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentPurple)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Hydrating your playlists from Cloud...", color = TextSecondary, fontSize = 14.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Your library is empty", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Click 'Sync Now' to pull playlists from your mobile account or import a backup.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (library.likedSongs.isNotEmpty()) {
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
    isHydrating: Boolean = false,
    currentlyPlayingVideoId: String?,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onReload: () -> Unit = {},
    onTrackMoreClick: ((TrackItem) -> Unit)? = null
) {
    val totalSeconds = playlist.tracks.sumOf { it.durationSeconds }
    val formattedDuration = formatPlaylistDuration(totalSeconds)
    val displayTrackCount = if (playlist.tracks.isNotEmpty()) playlist.tracks.size else playlist.trackCount
    val durationText = if (playlist.tracks.isNotEmpty()) " · $formattedDuration" else ""

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
                Text("$displayTrackCount tracks$durationText", fontSize = 13.sp, color = TextSecondary)

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

        // Tracks List or Hydration Progress State
        if (isHydrating) {
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = AccentPurple,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Loading tracks from Google Account...", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else if (playlist.tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No tracks loaded for this playlist yet.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onReload,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetch Tracks from Cloud", color = AccentPurple, fontSize = 13.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(playlist.tracks) { index, track ->
                    val isPlaying = track.videoId == currentlyPlayingVideoId
                    PlaylistTrackRow(
                        index = index + 1,
                        track = track,
                        isPlaying = isPlaying,
                        onClick = { onPlayTrack(index) },
                        onMoreClick = { onTrackMoreClick?.invoke(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistTrackRow(
    index: Int,
    track: TrackItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.durationSeconds > 0) {
                    Text(
                        text = formatTime(track.durationSeconds * 1000L),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onMoreClick?.invoke() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Track options",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
    onPlayTrack: (TrackItem) -> Unit,
    onMoreClick: ((TrackItem) -> Unit)? = null
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
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { track ->
                    TrackListItem(
                        track = track,
                        onPlay = { onPlayTrack(track) },
                        onMoreClick = { onMoreClick?.invoke(track) }
                    )
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
fun ExploreWorkspace(
    onPlayTrack: (TrackItem) -> Unit,
    onMoreClick: ((TrackItem) -> Unit)? = null
) {
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
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trendingTracks) { track ->
                TrackListItem(
                    track = track,
                    onPlay = { onPlayTrack(track) },
                    onMoreClick = { onMoreClick?.invoke(track) }
                )
            }
        }
    }
}

@Composable
fun TrackListItem(
    track: TrackItem,
    onPlay: () -> Unit,
    onMoreClick: (() -> Unit)? = null
) {
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
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onMoreClick?.invoke() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = TextSecondary, modifier = Modifier.size(18.dp))
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