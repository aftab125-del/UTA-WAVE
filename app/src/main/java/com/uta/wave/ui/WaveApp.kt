package com.uta.wave.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.player.PlaybackManager
import com.uta.wave.ui.components.MiniPlayer
import com.uta.wave.ui.explore.HomeScreen
import com.uta.wave.ui.library.LibraryScreen
import com.uta.wave.ui.player.FullscreenPlayerSheet
import com.uta.wave.ui.search.SearchScreen
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun WaveApp() {
    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val playerState by playbackManager.state.collectAsState()
    val favoritesList by localDatabase.favoritesFlow.collectAsState(initial = emptyList())
    val playlists by localDatabase.playlistsFlow.collectAsState(initial = emptyList())
    val recentHistory by localDatabase.historyFlow.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val currentTrack = playerState.currentTrack
    val isCurrentTrackFavorite = currentTrack != null && favoritesList.any { it.id == currentTrack.id }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = ElectricViolet,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                    label = { Text("Signal", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricViolet,
                        selectedTextColor = ElectricViolet,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = ElectricViolet.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricViolet,
                        selectedTextColor = ElectricViolet,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = ElectricViolet.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                    label = { Text("Library", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricViolet,
                        selectedTextColor = ElectricViolet,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = ElectricViolet.copy(alpha = 0.15f)
                    )
                )
            }
        },
        containerColor = ObsidianBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    }
                )
                1 -> SearchScreen(
                    currentTrackId = currentTrack?.id,
                    isPlaying = playerState.isPlaying,
                    favoritesList = favoritesList,
                    onPlayTrack = { track, queue -> playbackManager.playTrack(track, queue) },
                    onToggleFavorite = { track ->
                        scope.launch { localDatabase.toggleFavorite(track) }
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
                    }
                )
            }

            // Persistent MiniPlayer
            if (currentTrack != null) {
                MiniPlayer(
                    playerState = playerState,
                    onExpand = { isPlayerExpanded = true },
                    onTogglePlayPause = { playbackManager.togglePlayPause() },
                    onNext = { playbackManager.next() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
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
            onSeekTo = { pos -> playbackManager.seekTo(pos) },
            onToggleShuffle = { playbackManager.toggleShuffle() },
            onToggleRepeat = { playbackManager.toggleRepeat() },
            onCollapse = { isPlayerExpanded = false }
        )
    }
}
