package com.uta.wave.ui.library

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.downloads.AudioDownloadManager
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.data.local.PlaylistRecord
import com.uta.wave.player.PlaybackManager
import com.uta.wave.ui.components.SongMenuBottomSheet
import com.uta.wave.ui.components.TrackRow
import com.uta.wave.ui.recognizer.BlushRose
import com.uta.wave.ui.recognizer.DarkBlushText
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class PlaylistSortOrder(val label: String) {
    CUSTOM("Custom Order"),
    TITLE("Title (A-Z)"),
    ARTIST("Artist (A-Z)"),
    DURATION("Duration (Longest)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailView(
    playlist: PlaylistRecord,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    val downloadManager = remember { AudioDownloadManager.getInstance(context) }

    var playlistName by remember { mutableStateOf(playlist.name) }
    var playlistDesc by remember { mutableStateOf(playlist.description) }
    var tracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isReorderMode by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(PlaylistSortOrder.CUSTOM) }
    var showSortDropdown by remember { mutableStateOf(false) }

    // Track context menu state
    var selectedTrackMenu by remember { mutableStateOf<TrackItem?>(null) }

    val downloadingIds by downloadManager.downloadingIds.collectAsState()
    val downloadedTracks by localDatabase.downloadsFlow.collectAsState(initial = emptyList())
    val downloadedSet = remember(downloadedTracks) { downloadedTracks.map { it.id }.toSet() }

    fun refreshTracks() {
        scope.launch {
            tracks = localDatabase.getPlaylistTracks(playlist.id)
        }
    }

    LaunchedEffect(playlist.id) {
        refreshTracks()
    }

    // Modal & View State Back Handlers
    BackHandler(enabled = selectedTrackMenu != null) {
        selectedTrackMenu = null
    }
    BackHandler(enabled = showSortDropdown) {
        showSortDropdown = false
    }
    BackHandler(enabled = showEditDialog) {
        showEditDialog = false
    }
    BackHandler(enabled = showDeleteConfirmDialog) {
        showDeleteConfirmDialog = false
    }
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }
    BackHandler(enabled = isReorderMode) {
        isReorderMode = false
    }
    BackHandler(enabled = !isSearchActive && !isReorderMode && !showEditDialog && !showDeleteConfirmDialog && !showSortDropdown && selectedTrackMenu == null) {
        onNavigateBack()
    }

    val filteredTracks = remember(tracks, searchQuery, sortOrder) {
        val searched = if (searchQuery.isBlank()) {
            tracks
        } else {
            val q = searchQuery.trim().lowercase()
            tracks.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                (it.album?.lowercase()?.contains(q) == true)
            }
        }
        when (sortOrder) {
            PlaylistSortOrder.CUSTOM -> searched
            PlaylistSortOrder.TITLE -> searched.sortedBy { it.title.lowercase() }
            PlaylistSortOrder.ARTIST -> searched.sortedBy { it.artist.lowercase() }
            PlaylistSortOrder.DURATION -> searched.sortedByDescending { it.durationSeconds }
        }
    }

    val allDownloaded = tracks.isNotEmpty() && tracks.all { downloadedSet.contains(it.id) }
    val isAnyDownloading = tracks.any { downloadingIds.contains(it.id) }
    val downloadedCount = tracks.count { downloadedSet.contains(it.id) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp)
        ) {
            // Header Bar: Back Arrow + Playlist Title + Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = playlistName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search toggle
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search in Playlist",
                                tint = if (isSearchActive) BlushRose else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Edit Name/Description
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Playlist",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Delete Playlist
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Playlist",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // In-Playlist Search Field
            item {
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filter songs or artists in playlist...", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1724),
                                unfocusedContainerColor = Color(0xFF1E1724),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedIndicatorColor = BlushRose,
                                unfocusedIndicatorColor = CardBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Playlist Hero Card (Collage + Meta + Actions)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                    ) {
                        PlaylistArtCollage(tracks = tracks, modifier = Modifier.matchParentSize())
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = playlistName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp
                        ),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${tracks.size} songs • ${playlistDesc ?: "Custom Playlist"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )

                    if (downloadedCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$downloadedCount of ${tracks.size} downloaded for offline playback",
                                color = ElectricViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Play All & Shuffle Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (filteredTracks.isNotEmpty()) onPlayTrack(filteredTracks.first(), filteredTracks)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlushRose),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBlushText, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play all", color = DarkBlushText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (filteredTracks.isNotEmpty()) {
                                    val shuffled = filteredTracks.shuffled()
                                    onPlayTrack(shuffled.first(), shuffled)
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Controls Row: Download All Button + Reorder Toggle + Sort Menu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Download All / Offline Button
                        TextButton(
                            onClick = {
                                if (allDownloaded) {
                                    downloadManager.deleteTracks(tracks)
                                    Toast.makeText(context, "Removed all offline downloads", Toast.LENGTH_SHORT).show()
                                } else {
                                    downloadManager.downloadTracks(tracks)
                                    Toast.makeText(context, "Downloading playlist songs...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            if (isAnyDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ElectricViolet
                                )
                            } else {
                                Icon(
                                    imageVector = if (allDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (allDownloaded) ElectricViolet else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAnyDownloading) "Downloading..." else if (allDownloaded) "Downloaded" else "Download All",
                                color = if (allDownloaded) ElectricViolet else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Reorder Mode Toggle (active only when sorting is CUSTOM)
                            if (sortOrder == PlaylistSortOrder.CUSTOM && searchQuery.isBlank()) {
                                FilterChip(
                                    selected = isReorderMode,
                                    onClick = { isReorderMode = !isReorderMode },
                                    label = {
                                        Text(
                                            text = if (isReorderMode) "Done" else "Reorder",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.Reorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BlushRose,
                                        selectedLabelColor = DarkBlushText,
                                        selectedLeadingIconColor = DarkBlushText,
                                        containerColor = SurfaceDark,
                                        labelColor = TextSecondary,
                                        iconColor = TextSecondary
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Sort Order Menu
                            Box {
                                IconButton(
                                    onClick = { showSortDropdown = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort Playlist",
                                        tint = if (sortOrder != PlaylistSortOrder.CUSTOM) BlushRose else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortDropdown,
                                    onDismissRequest = { showSortDropdown = false },
                                    modifier = Modifier.background(Color(0xFF1E1724))
                                ) {
                                    PlaylistSortOrder.values().forEach { order ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (sortOrder == order) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = BlushRose, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    Text(
                                                        text = order.label,
                                                        color = if (sortOrder == order) BlushRose else TextPrimary,
                                                        fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            },
                                            onClick = {
                                                sortOrder = order
                                                if (order != PlaylistSortOrder.CUSTOM) isReorderMode = false
                                                showSortDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Playlist Tracks List
            if (filteredTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching songs found" else "No songs in this playlist yet",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                itemsIndexed(filteredTracks, key = { _, track -> "${track.id}_${playlist.id}" }) { index, track ->
                    if (isReorderMode) {
                        // Reorder Row with Move Up, Move Down, and Quick Delete
                        ReorderTrackRow(
                            index = index,
                            track = track,
                            canMoveUp = index > 0,
                            canMoveDown = index < tracks.size - 1,
                            onMoveUp = {
                                scope.launch {
                                    localDatabase.reorderPlaylistTracks(playlist.id, index, index - 1)
                                    refreshTracks()
                                }
                            },
                            onMoveDown = {
                                scope.launch {
                                    localDatabase.reorderPlaylistTracks(playlist.id, index, index + 1)
                                    refreshTracks()
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    localDatabase.removeTrackFromPlaylist(playlist.id, track.id)
                                    refreshTracks()
                                    Toast.makeText(context, "Removed from playlist", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        // Standard Interactive TrackRow
                        TrackRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isPlaying = isPlaying && track.id == currentTrackId,
                            isFavorite = true,
                            onTrackClick = { onPlayTrack(track, filteredTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onMoreClick = {
                                selectedTrackMenu = track
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal: Edit Playlist Name and Description
    if (showEditDialog) {
        var editName by remember { mutableStateOf(playlistName) }
        var editDesc by remember { mutableStateOf(playlistDesc ?: "") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(text = "Edit Playlist", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1724),
                            unfocusedContainerColor = Color(0xFF1E1724),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = BlushRose
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description (Optional)") },
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1724),
                            unfocusedContainerColor = Color(0xFF1E1724),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = BlushRose
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            scope.launch {
                                localDatabase.renamePlaylist(playlist.id, editName, editDesc)
                                playlistName = editName.trim()
                                playlistDesc = editDesc.trim().ifEmpty { null }
                                showEditDialog = false
                                Toast.makeText(context, "Playlist updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlushRose)
                ) {
                    Text("Save", color = DarkBlushText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1D1623)
        )
    }

    // Confirmation Modal: Delete Entire Playlist
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(text = "Delete Playlist", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"$playlistName\"? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            localDatabase.deletePlaylist(playlist.id)
                            showDeleteConfirmDialog = false
                            onNavigateBack()
                            Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1D1623)
        )
    }

    // Bottom Sheet: Track Actions Menu (Full Screenshot 2 Parity)
    selectedTrackMenu?.let { item ->
        val playbackManager = remember { PlaybackManager.getInstance(context) }
        val favoritesList by localDatabase.favoritesFlow.collectAsState(initial = emptyList())
        SongMenuBottomSheet(
            track = item,
            playlistId = playlist.id,
            isFavorite = favoritesList.any { it.id == item.id },
            onToggleFavorite = { onToggleFavorite(it) },
            onPlayNext = { track -> playbackManager.addToQueue(track, insertNext = true) },
            onAddToQueue = { track -> playbackManager.addToQueue(track, insertNext = false) },
            onRemoveFromPlaylist = { track ->
                scope.launch {
                    localDatabase.removeTrackFromPlaylist(playlist.id, track.id)
                    refreshTracks()
                }
            },
            onTrackUpdated = {
                refreshTracks()
            },
            onDismiss = { selectedTrackMenu = null }
        )
    }
}

@Composable
fun ReorderTrackRow(
    index: Int,
    track: TrackItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1724))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = TextMuted,
            modifier = Modifier.width(26.dp)
        )

        AsyncImage(
            model = track.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    tint = if (canMoveUp) TextPrimary else TextMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (canMoveDown) TextPrimary else TextMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistArtCollage(
    tracks: List<TrackItem>,
    modifier: Modifier = Modifier
) {
    val artList = tracks.map { it.artworkUrl }.filter { it.isNotBlank() }.distinct().take(4)

    if (artList.isEmpty()) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(Color(0xFF2C2233), Color(0xFF1B1522)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
        }
    } else if (artList.size < 4) {
        AsyncImage(
            model = artList.first(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Column(modifier = modifier) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AsyncImage(model = artList[0], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).aspectRatio(1f))
                AsyncImage(model = artList[1], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).aspectRatio(1f))
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AsyncImage(model = artList[2], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).aspectRatio(1f))
                AsyncImage(model = artList[3], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).aspectRatio(1f))
            }
        }
    }
}
