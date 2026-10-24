package com.uta.wave.ui.library

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.account.AccountManager
import com.uta.wave.data.downloads.AudioDownloadManager
import com.uta.wave.data.importer.PlaylistImporter
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.data.local.LocalMediaScanner
import com.uta.wave.data.local.PlaylistRecord
import com.uta.wave.ui.components.TrackRow
import com.uta.wave.ui.recognizer.BlushRose
import com.uta.wave.ui.recognizer.DarkBlushText
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SoftViolet
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.launch

data class QuickFeatureCard(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val countString: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    currentTrackId: String?,
    isPlaying: Boolean,
    favoritesList: List<TrackItem>,
    playlists: List<PlaylistRecord>,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onExportJson: () -> Unit,
    onOpenDiagnostics: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onOpenListeningSummary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    val accountManager = remember { AccountManager.getInstance(context) }
    val userProfile by accountManager.profileFlow.collectAsState()
    val downloadManager = remember { AudioDownloadManager.getInstance(context) }

    val downloadedTracks by localDatabase.downloadsFlow.collectAsState(initial = emptyList())

    // Navigation states within Library tab
    var selectedPlaylist by remember { mutableStateOf<PlaylistRecord?>(null) }
    var showSpotifyImport by remember { mutableStateOf(false) }
    var selectedCategoryView by remember { mutableStateOf<String?>(null) }

    // Bottom Sheet states
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var isImportOptionsExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showYtmDialog by remember { mutableStateOf(false) }

    var newPlaylistName by remember { mutableStateOf("") }
    var ytmLinkInput by remember { mutableStateOf("") }
    var isImportingYtm by remember { mutableStateOf(false) }
    var ytmImportProgressText by remember { mutableStateOf("") }
    var ytmCurrentTrackText by remember { mutableStateOf("") }

    // Category sub-tabs and sort order
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Playlists, 1: Songs, 2: Albums, 3: Artists
    val subTabs = listOf("Playlists", "Songs", "Albums", "Artists")
    var sortAscending by remember { mutableStateOf(true) }
    var sortCriteria by remember { mutableStateOf("Date added") }

    // Data for feature cards
    var top50Tracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var bottom50Tracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var localTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var allLibraryTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        top50Tracks = localDatabase.getTop50()
        bottom50Tracks = localDatabase.getBottom50()
        localTracks = LocalMediaScanner.scanLocalTracks(context)
        allLibraryTracks = localDatabase.getAllTracks()
    }

    // Handle Sub-Screens
    if (showSpotifyImport) {
        SpotifyImportScreen(
            onNavigateBack = { showSpotifyImport = false },
            modifier = modifier
        )
        return
    }

    if (selectedPlaylist != null) {
        PlaylistDetailView(
            playlist = selectedPlaylist!!,
            currentTrackId = currentTrackId,
            isPlaying = isPlaying,
            onPlayTrack = onPlayTrack,
            onToggleFavorite = onToggleFavorite,
            onNavigateBack = { selectedPlaylist = null },
            modifier = modifier
        )
        return
    }

    // Active Category List View (e.g., Liked, Downloaded, Cached, Top 50, Bottom 50, Local)
    if (selectedCategoryView != null) {
        val categoryName = selectedCategoryView!!
        val activeTracks = when (categoryName) {
            "Liked" -> favoritesList
            "Downloaded" -> downloadedTracks
            "Cached" -> allLibraryTracks.take(30)
            "My top 50" -> if (top50Tracks.isNotEmpty()) top50Tracks else favoritesList.take(50)
            "My bottom 50" -> if (bottom50Tracks.isNotEmpty()) bottom50Tracks else favoritesList.reversed().take(50)
            "Local" -> localTracks
            else -> emptyList()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedCategoryView = null }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = TextPrimary
                            )
                        }

                        if (activeTracks.isNotEmpty()) {
                            Button(
                                onClick = { onPlayTrack(activeTracks.first(), activeTracks) },
                                colors = ButtonDefaults.buttonColors(containerColor = BlushRose),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBlushText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play all", color = DarkBlushText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (categoryName == "Cached") {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Stream Cache: ${(allLibraryTracks.size * 3.8).toInt()} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            TextButton(
                                onClick = {
                                    Toast.makeText(context, "Stream cache cleaned", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Clear Cache", color = Color(0xFFEF5350), fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (activeTracks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No tracks in $categoryName yet", color = TextMuted)
                        }
                    }
                } else {
                    items(activeTracks) { track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isPlaying = isPlaying && track.id == currentTrackId,
                            isFavorite = favoritesList.any { it.id == track.id },
                            onTrackClick = { onPlayTrack(track, activeTracks) },
                            onToggleFavorite = { onToggleFavorite(track) }
                        )
                    }
                }
            }
        }
        return
    }

    // MAIN LIBRARY SCREEN
    val featureCards = listOf(
        QuickFeatureCard("Liked", "Liked", Icons.Default.Favorite, "${favoritesList.size}"),
        QuickFeatureCard("Downloaded", "Downloaded", Icons.Default.CheckCircle, "${downloadedTracks.size}"),
        QuickFeatureCard("Exported", "Exported", Icons.Default.Download, "Backup"),
        QuickFeatureCard("Cached", "Cached", Icons.Default.Sync, "${allLibraryTracks.size}"),
        QuickFeatureCard("My top 50", "My top 50", Icons.Default.TrendingUp, "${top50Tracks.size}"),
        QuickFeatureCard("My bottom 50", "My bottom 50", Icons.Default.TrendingDown, "${bottom50Tracks.size}"),
        QuickFeatureCard("Local", "Local", Icons.Default.Folder, "${localTracks.size}")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp)
        ) {
            // -------------------------------------------------------------
            // 1. TOP BAR: "Library" + Action Icons (History, Stats, Cast, Avatar)
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(onClick = onOpenDiagnostics) {
                            Icon(Icons.Default.BugReport, contentDescription = "Diagnostics", tint = ElectricViolet, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = onOpenListeningSummary) {
                            Icon(Icons.Outlined.History, contentDescription = "Listening Summary & History", tint = TextSecondary, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.TrendingUp, contentDescription = "Stats", tint = TextSecondary, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.Cast, contentDescription = "Cast", tint = TextSecondary, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings & Customization", tint = Color(0xFFF2AEC7), modifier = Modifier.size(22.dp))
                        }
                        if (!userProfile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userProfile.avatarUrl,
                                contentDescription = "Profile & Settings",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color(0xFFF2AEC7).copy(alpha = 0.6f), CircleShape)
                                    .clickable(onClick = onOpenSettings)
                            )
                        } else {
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile & Settings", tint = TextSecondary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. SUB-TABS: Playlists, Songs, Albums, Artists
            // -------------------------------------------------------------
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    items(subTabs.indices.toList()) { index ->
                        val isSelected = selectedSubTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFF281C2B) else Color(0xFF1B1420))
                                .border(
                                    1.dp,
                                    if (isSelected) CardBorder else Color.Transparent,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedSubTab = index }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = subTabs[index],
                                color = if (isSelected) TextPrimary else TextMuted,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. SORT ROW: "Date added" + Sort Direction Arrow
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort Criteria Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF221926))
                            .clickable {
                                sortCriteria = when (sortCriteria) {
                                    "Date added" -> "Title A-Z"
                                    "Title A-Z" -> "Play count"
                                    else -> "Date added"
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = sortCriteria,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        )
                    }

                    // Ascending / Descending Toggle Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF221926))
                            .clickable { sortAscending = !sortAscending },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Sort direction",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. 7 QUICK-ACCESS FEATURE CARDS (2 Columns)
            // -------------------------------------------------------------
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    val chunked = featureCards.chunked(2)
                    chunked.forEach { rowCards ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowCards.forEach { card ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1E1624))
                                        .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (card.id == "Exported") {
                                                onExportJson()
                                            } else {
                                                selectedCategoryView = card.id
                                            }
                                        }
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = card.icon,
                                        contentDescription = card.title,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = card.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rowCards.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. "Playlists" Section Header
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                }
            }

            // -------------------------------------------------------------
            // 6. PLAYLISTS 2-COLUMN GRID (with 2x2 Collage Artwork)
            // -------------------------------------------------------------
            if (playlists.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No playlists yet. Tap + to create one!", color = TextMuted)
                    }
                }
            } else {
                val chunkedPlaylists = playlists.chunked(2)
                items(chunkedPlaylists) { rowPlaylists ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        rowPlaylists.forEach { pl ->
                            var playlistTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
                            LaunchedEffect(pl.id) {
                                playlistTracks = localDatabase.getPlaylistTracks(pl.id)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPlaylist = pl }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                                ) {
                                    PlaylistArtCollage(
                                        tracks = playlistTracks,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${pl.trackCount} songs",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextMuted
                                )
                            }
                        }
                        if (rowPlaylists.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 7. CIRCULAR "+" FAB (Matching Screenshot 1 & 3 Blush Pink)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 90.dp)
                .size(56.dp)
                .shadow(16.dp, CircleShape, spotColor = BlushRose.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(BlushRose)
                .clickable {
                    isImportOptionsExpanded = false
                    showAddBottomSheet = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create or Import Playlist",
                tint = DarkBlushText,
                modifier = Modifier.size(28.dp)
            )
        }

        // -------------------------------------------------------------
        // 8. BOTTOM SHEET: "Create playlist" & "Import playlist" (Screenshots 1 & 2)
        // -------------------------------------------------------------
        if (showAddBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAddBottomSheet = false
                    isImportOptionsExpanded = false
                },
                containerColor = Color(0xFF1E1724),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp)
                ) {
                    if (!isImportOptionsExpanded) {
                        // SCREENSHOT 1 OPTIONS: Create playlist & Import playlist
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF281E2E))
                                .clickable {
                                    showAddBottomSheet = false
                                    showCreateDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Create playlist",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF281E2E))
                                .clickable {
                                    isImportOptionsExpanded = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Import playlist",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                color = TextPrimary
                            )
                        }
                    } else {
                        // SCREENSHOT 2 OPTIONS: Import from Spotify & Import from YouTube Music
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showAddBottomSheet = false
                                    isImportOptionsExpanded = false
                                    showSpotifyImport = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Import from Spotify",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showAddBottomSheet = false
                                    isImportOptionsExpanded = false
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clipText.contains("youtube.com") || clipText.contains("youtu.be")) {
                                        ytmLinkInput = clipText.trim()
                                    }
                                    showYtmDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Import from YouTube Music",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 9. CREATE PLAYLIST DIALOG
        // -------------------------------------------------------------
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = Color(0xFF201824),
                shape = RoundedCornerShape(24.dp),
                title = { Text("Create playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist name...", color = TextMuted) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlushRose,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = BlushRose
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylist(newPlaylistName.trim())
                                newPlaylistName = ""
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text("Create", color = BlushRose, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // 10. IMPORT FROM YOUTUBE MUSIC DIALOG (Screenshot 5)
        // -------------------------------------------------------------
        if (showYtmDialog) {
            AlertDialog(
                onDismissRequest = { if (!isImportingYtm) showYtmDialog = false },
                containerColor = Color(0xFF201824),
                shape = RoundedCornerShape(26.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.AddLink,
                        contentDescription = null,
                        tint = BlushRose,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Import playlist from YouTube Music",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Paste the public YouTube or YouTube Music playlist link below:",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = ytmLinkInput,
                            onValueChange = { ytmLinkInput = it },
                            placeholder = { Text("https://music.youtube.com/playlist?list=...", color = TextMuted, fontSize = 12.sp) },
                            shape = RoundedCornerShape(22.dp),
                            singleLine = true,
                            enabled = !isImportingYtm,
                            trailingIcon = {
                                IconButton(
                                    enabled = !isImportingYtm,
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        if (clipText.isNotBlank()) {
                                            ytmLinkInput = clipText.trim()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = BlushRose, modifier = Modifier.size(20.dp))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlushRose,
                                unfocusedBorderColor = BlushRose.copy(alpha = 0.5f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = BlushRose
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isImportingYtm) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = BlushRose, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = ytmImportProgressText.ifBlank { "Connecting to YouTube Music..." },
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (ytmCurrentTrackText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ytmCurrentTrackText,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isImportingYtm && ytmLinkInput.isNotBlank(),
                        onClick = {
                            val link = ytmLinkInput.trim()
                            isImportingYtm = true
                            ytmImportProgressText = "Connecting to YouTube..."
                            ytmCurrentTrackText = ""
                            scope.launch {
                                val (title, count) = PlaylistImporter.importFromYouTubeMusic(
                                    context = context,
                                    playlistUrlOrId = link,
                                    onProgress = { cur, tot, trackTitle ->
                                        ytmImportProgressText = "Importing track $cur of $tot..."
                                        ytmCurrentTrackText = trackTitle
                                    }
                                )
                                isImportingYtm = false
                                showYtmDialog = false
                                ytmLinkInput = ""
                                ytmCurrentTrackText = ""
                                if (count > 0) {
                                    Toast.makeText(context, "Imported \"$title\" ($count tracks)!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Could not import: $title", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Import", color = BlushRose, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isImportingYtm,
                        onClick = { showYtmDialog = false }
                    ) {
                        Text("Cancel", color = BlushRose, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            )
        }
    }
}
