package com.uta.wave.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.ui.components.GlassEffectConfig
import com.uta.wave.ui.components.LocalAppBackdrop
import com.uta.wave.ui.components.LocalGlassEffectConfig
import com.uta.wave.ui.components.backdrop.backdrops.layerBackdrop
import com.uta.wave.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.player.PlaybackManager
import com.uta.wave.ui.account.AccountBottomSheet
import com.uta.wave.ui.account.AccountScreen
import com.uta.wave.ui.album.AlbumScreen
import com.uta.wave.ui.artist.ArtistScreen
import com.uta.wave.ui.components.DiagnosticSheet
import com.uta.wave.ui.components.FloatingNavBar
import com.uta.wave.ui.components.MiniPlayer
import com.uta.wave.ui.explore.ChartsScreen
import com.uta.wave.ui.explore.HomeScreen
import com.uta.wave.ui.library.LibraryScreen
import com.uta.wave.ui.player.AmbientModeScreen
import com.uta.wave.ui.player.FullscreenPlayerSheet
import com.uta.wave.ui.recognizer.RecognizeMusicScreen
import com.uta.wave.ui.search.SearchScreen
import com.uta.wave.ui.settings.SettingsScreen
import com.uta.wave.ui.stats.ListeningSummaryScreen
import com.uta.wave.ui.theme.ObsidianBackground
import kotlinx.coroutines.launch

@Composable
fun WaveApp() {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences.getInstance(context) }
    val settings by userPrefs.settingsFlow.collectAsState()
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    val accountManager = remember { com.uta.wave.data.account.AccountManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val glassConfig = remember(
        settings.liquidGlassGlobalEnabled,
        settings.liquidGlassVibrancy,
        settings.liquidGlassBlurRadius,
        settings.liquidGlassLensHeight,
        settings.liquidGlassLensAmount,
        settings.liquidGlassChromaticAberration,
        settings.liquidGlassDepthEffect,
        settings.liquidGlassSurfaceTintColorInt,
        settings.liquidGlassSurfaceOpacity,
        settings.liquidGlassTextColorInt,
        settings.liquidGlassPlayerEnabled,
        settings.liquidGlassMiniPlayerEnabled,
        settings.liquidGlassNavBarEnabled
    ) {
        GlassEffectConfig(
            globalEnabled = settings.liquidGlassGlobalEnabled,
            vibrancy = settings.liquidGlassVibrancy,
            blurRadius = settings.liquidGlassBlurRadius,
            lensHeight = settings.liquidGlassLensHeight,
            lensAmount = settings.liquidGlassLensAmount,
            chromaticAberration = settings.liquidGlassChromaticAberration,
            depthEffect = settings.liquidGlassDepthEffect,
            surfaceTintColor = if (settings.liquidGlassSurfaceTintColorInt == 0) Color.Unspecified else Color(settings.liquidGlassSurfaceTintColorInt),
            surfaceOpacity = settings.liquidGlassSurfaceOpacity,
            textColor = if (settings.liquidGlassTextColorInt == 0) Color.Unspecified else Color(settings.liquidGlassTextColorInt),
            playerEnabled = settings.liquidGlassPlayerEnabled,
            miniPlayerEnabled = settings.liquidGlassMiniPlayerEnabled,
            navBarEnabled = settings.liquidGlassNavBarEnabled
        )
    }

    val appBackdrop = rememberLayerBackdrop {
        drawRect(ObsidianBackground)
        drawContent()
    }

    LaunchedEffect(Unit) {
        val profile = accountManager.currentProfile
        if (profile.isLoggedIn) {
            com.uta.wave.data.innertube.InnerTubeClient.authCookie = profile.cookie
            com.uta.wave.data.innertube.InnerTubeClient.authSapisid = profile.sapisid
        }
    }

    val playerState by playbackManager.state.collectAsState()
    val favoritesList by localDatabase.favoritesFlow.collectAsState(initial = emptyList())
    val playlists by localDatabase.playlistsFlow.collectAsState(initial = emptyList())
    val recentHistory by localDatabase.historyFlow.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var isBottomBarCollapsed by remember { mutableStateOf(false) }

    // Artist & Album Viewing Navigation Stack
    var viewingArtistId by remember { mutableStateOf<String?>(null) }
    var viewingAlbumId by remember { mutableStateOf<String?>(null) }
    var viewingAlbumArtistHint by remember { mutableStateOf<String?>(null) }
    var isAmbientModeActive by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isAccountSheetOpen by remember { mutableStateOf(false) }
    var isAccountScreenOpen by remember { mutableStateOf(false) }
    var isListeningSummaryOpen by remember { mutableStateOf(false) }

    val currentTrack = playerState.currentTrack
    val isCurrentTrackFavorite = currentTrack != null && favoritesList.any { it.id == currentTrack.id }

    // Scroll Coordinator: Detects scroll direction to dynamically collapse/expand the bottom bar & MiniPlayer
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -8f) {
                    // Scrolling down: collapse nav bar and merge MiniPlayer inline
                    if (!isBottomBarCollapsed) {
                        isBottomBarCollapsed = true
                    }
                } else if (available.y > 8f) {
                    // Scrolling up: expand nav bar and dock MiniPlayer above
                    if (isBottomBarCollapsed) {
                        isBottomBarCollapsed = false
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Hardware / Gesture Back Navigation Handlers
    BackHandler(enabled = isAccountScreenOpen) {
        isAccountScreenOpen = false
    }
    BackHandler(enabled = isListeningSummaryOpen) {
        isListeningSummaryOpen = false
    }
    BackHandler(enabled = isSettingsOpen) {
        isSettingsOpen = false
    }
    BackHandler(enabled = isAmbientModeActive) {
        isAmbientModeActive = false
    }
    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }
    BackHandler(enabled = viewingAlbumId != null) {
        viewingAlbumId = null
    }
    BackHandler(enabled = viewingArtistId != null) {
        viewingArtistId = null
    }

    CompositionLocalProvider(
        LocalAppBackdrop provides appBackdrop,
        LocalGlassEffectConfig provides glassConfig
    ) {
        Scaffold(
            containerColor = ObsidianBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .nestedScroll(nestedScrollConnection)
            ) {
                // Main Screen Content based on selected Tab (recorded in appBackdrop for liquid glass sampling)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(appBackdrop)
                ) {
                    when (selectedTab) {
                        0 -> HomeScreen(
                    currentTrackId = currentTrack?.id,
                    isPlaying = playerState.isPlaying,
                    favoritesList = favoritesList,
                    recentHistory = recentHistory,
                    onPlayTrack = { track, queue -> playbackManager.playTrack(track, queue) },
                    onToggleFavorite = { track ->
                        scope.launch { localDatabase.toggleFavorite(track) }
                    },
                    onNavigateToCharts = {
                        selectedTab = 1
                        isBottomBarCollapsed = false
                    },
                    onNavigateToSearch = {
                        selectedTab = 3
                        isBottomBarCollapsed = false
                    },
                    onOpenArtist = { artistId ->
                        viewingArtistId = artistId
                    },
                    onOpenAlbum = { albumId ->
                        viewingAlbumId = albumId
                    },
                    onOpenDiagnostics = { showDiagnostics = true },
                    onOpenAccount = { isAccountScreenOpen = true },
                    onOpenListeningSummary = { isListeningSummaryOpen = true },
                    onOpenSettings = { isSettingsOpen = true }
                )
                1 -> RecognizeMusicScreen(
                    currentTrackId = currentTrack?.id,
                    isPlaying = playerState.isPlaying,
                    onPlayTrack = { track, queue -> playbackManager.playTrack(track, queue) },
                    onToggleFavorite = { track ->
                        scope.launch { localDatabase.toggleFavorite(track) }
                    },
                    onNavigateBack = {
                        selectedTab = 0
                        isBottomBarCollapsed = false
                    }
                )
                2 -> LibraryScreen(
                    currentTrackId = currentTrack?.id,
                    isPlaying = playerState.isPlaying,
                    favoritesList = favoritesList,
                    playlists = playlists,
                    onPlayTrack = { track, queue -> playbackManager.playTrack(track, queue) },
                    onToggleFavorite = { track ->
                        scope.launch { localDatabase.toggleFavorite(track) }
                    },
                    onCreatePlaylist = { name ->
                        scope.launch { localDatabase.createPlaylist(name) }
                    },
                    onExportJson = {
                        scope.launch {
                            val json = localDatabase.exportJson()
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "Library JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                        }
                    },
                    onOpenDiagnostics = { showDiagnostics = true },
                    onOpenAccount = { isAccountScreenOpen = true },
                    onOpenListeningSummary = { isListeningSummaryOpen = true },
                    onOpenSettings = { isSettingsOpen = true }
                )
                3 -> SearchScreen(
                    currentTrackId = currentTrack?.id,
                    isPlaying = playerState.isPlaying,
                    favoritesList = favoritesList,
                    onPlayTrack = { track, queue -> playbackManager.playTrack(track, queue) },
                    onToggleFavorite = { track ->
                        scope.launch { localDatabase.toggleFavorite(track) }
                    }
                )
            }
        }

            // 1. Floating Capsule MiniPlayer (Docks directly above the floating nav bar in Expanded Mode)
            AnimatedVisibility(
                visible = !isBottomBarCollapsed && currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
                    .fillMaxWidth()
            ) {
                MiniPlayer(
                    playerState = playerState,
                    onExpand = { isPlayerExpanded = true },
                    onTogglePlayPause = { playbackManager.togglePlayPause() },
                    onNext = { playbackManager.next() },
                    onPrevious = { playbackManager.previous() },
                    onOpenDiagnostics = { showDiagnostics = true }
                )
            }

            // 2. Floating Navigation Bar (Supports both expanded split pill+circle, and collapsed inline merged pill)
            FloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    isBottomBarCollapsed = false
                },
                isCollapsed = isBottomBarCollapsed,
                onExpandBar = { isBottomBarCollapsed = false },
                currentTrack = currentTrack,
                playerState = playerState,
                onExpandPlayer = { isPlayerExpanded = true },
                onTogglePlayPause = { playbackManager.togglePlayPause() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Fullscreen Player Sheet
    AnimatedVisibility(
        visible = isPlayerExpanded,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        FullscreenPlayerSheet(
            playerState = playerState,
            isFavorite = isCurrentTrackFavorite,
            onToggleFavorite = {
                currentTrack?.let { track ->
                    scope.launch { localDatabase.toggleFavorite(track) }
                }
            },
            onTogglePlayPause = { playbackManager.togglePlayPause() },
            onNext = { playbackManager.next() },
            onPrevious = { playbackManager.previous() },
            onSeekTo = { positionMs -> playbackManager.seekTo(positionMs) },
            onToggleShuffle = { playbackManager.toggleShuffle() },
            onToggleRepeat = { playbackManager.toggleRepeat() },
            onCollapse = { isPlayerExpanded = false },
            queueTitle = if (playerState.queue.size > 1) "Now Playing (${playerState.queue.size})" else "Trending - Global",
            onSelectQueueTrack = { selectedTrack ->
                playbackManager.playTrack(selectedTrack, playerState.queue)
            },
            onClearQueue = {
                currentTrack?.let { current ->
                    playbackManager.playTrack(current, listOf(current))
                }
            },
            onViewArtist = { artistName ->
                isPlayerExpanded = false
                viewingArtistId = currentTrack?.artistId ?: artistName
            },
            onViewAlbum = { albumName ->
                isPlayerExpanded = false
                viewingAlbumId = currentTrack?.albumId ?: (currentTrack?.album ?: albumName)
                viewingAlbumArtistHint = currentTrack?.artist
            },
            playlists = playlists,
            onCreatePlaylist = { name ->
                scope.launch { localDatabase.createPlaylist(name) }
            },
            onAddToPlaylist = { playlistId, track ->
                scope.launch { localDatabase.addTrackToPlaylist(playlistId, track) }
            },
            onLaunchAmbientMode = {
                isPlayerExpanded = false
                isAmbientModeActive = true
            }
        )
    }

    // Ambient Mode Cinematic Screen Layer
    AnimatedVisibility(
        visible = isAmbientModeActive && currentTrack != null,
        enter = fadeIn(tween(350)),
        exit = fadeOut(tween(350))
    ) {
        currentTrack?.let { track ->
            AmbientModeScreen(
                track = track,
                playerState = playerState,
                onTogglePlayPause = { playbackManager.togglePlayPause() },
                onNext = { playbackManager.next() },
                onPrevious = { playbackManager.previous() },
                onSeekTo = { posMs -> playbackManager.seekTo(posMs) },
                onExit = { isAmbientModeActive = false }
            )
        }
    }

    // Artist Profile Screen Layer
    AnimatedVisibility(
        visible = viewingArtistId != null,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        viewingArtistId?.let { artistTarget ->
            ArtistScreen(
                artistIdOrName = artistTarget,
                currentTrackId = currentTrack?.id,
                isPlaying = playerState.isPlaying,
                favoritesList = favoritesList,
                onPlayTrack = { track, queue -> playbackManager.playTrack(track, queue) },
                onToggleFavorite = { track ->
                    scope.launch { localDatabase.toggleFavorite(track) }
                },
                onOpenAlbum = { albumId -> viewingAlbumId = albumId },
                onOpenArtist = { artistId -> viewingArtistId = artistId },
                onBack = { viewingArtistId = null }
            )
        }
    }

    // Album Tracklist Screen Layer
    AnimatedVisibility(
        visible = viewingAlbumId != null,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        viewingAlbumId?.let { albumTarget ->
            AlbumScreen(
                albumIdOrName = albumTarget,
                artistNameHint = viewingAlbumArtistHint ?: currentTrack?.artist,
                currentTrackId = currentTrack?.id,
                isPlaying = playerState.isPlaying,
                favoritesList = favoritesList,
                onPlayTrack = { track, queue -> playbackManager.playTrack(track, queue) },
                onToggleFavorite = { track ->
                    scope.launch { localDatabase.toggleFavorite(track) }
                },
                onOpenArtist = { artistId -> viewingArtistId = artistId },
                onBack = { viewingAlbumId = null }
            )
        }
    }

    // Diagnostic Log & Stream Sheet
    if (showDiagnostics) {
        DiagnosticSheet(
            playerState = playerState,
            onDismiss = { showDiagnostics = false }
        )
    }

    // Account Profile Bottom Sheet
    if (isAccountSheetOpen) {
        AccountBottomSheet(
            onDismiss = { isAccountSheetOpen = false },
            onOpenSettings = {
                isAccountSheetOpen = false
                isSettingsOpen = true
            },
            onOpenBackup = {
                isAccountSheetOpen = false
                scope.launch {
                    val json = localDatabase.exportJson()
                    clipboardManager.setText(AnnotatedString(json))
                    Toast.makeText(context, "Library backup JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Settings Screen Layer
    AnimatedVisibility(
        visible = isSettingsOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        SettingsScreen(
            onBack = { isSettingsOpen = false },
            onNavigateToAccount = {
                isAccountScreenOpen = true
            },
            onNavigateToListeningSummary = {
                isListeningSummaryOpen = true
            },
            onPlayTrack = { track, queue ->
                playbackManager.playTrack(track, queue)
            }
        )
    }

    // Account Screen Layer (1:1 Replica from Echo Music)
    AnimatedVisibility(
        visible = isAccountScreenOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        AccountScreen(
            onBack = { isAccountScreenOpen = false },
            onOpenSettings = {
                isAccountScreenOpen = false
                isSettingsOpen = true
            }
        )
    }

    // Listening Summary Screen Layer (1:1 Replica with Weekly 7-day Bar Chart)
    AnimatedVisibility(
        visible = isListeningSummaryOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        ListeningSummaryScreen(
            onBack = { isListeningSummaryOpen = false }
        )
    }
    }
}
