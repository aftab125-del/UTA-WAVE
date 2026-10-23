package com.uta.wave.ui.library

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.PlaylistRecord
import com.uta.wave.ui.components.TrackRow
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SoftViolet
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.SurfaceContainerHigh
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

data class LibraryCategory(
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val count: Int
)

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
    modifier: Modifier = Modifier
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>("Liked") }

    val filterChips = listOf("Playlists", "Songs", "Albums", "Artists")

    val categories = listOf(
        LibraryCategory("Liked", Icons.Default.Favorite, Color(0xFFFF5286), favoritesList.size),
        LibraryCategory("Downloaded", Icons.Default.CheckCircle, Color(0xFF4EBE89), 0),
        LibraryCategory("Exported", Icons.Default.Download, Color(0xFF64B5F6), 1),
        LibraryCategory("Cached", Icons.Default.Refresh, Color(0xFFFFB74D), favoritesList.size),
        LibraryCategory("My top 50", Icons.Default.TrendingUp, ElectricViolet, favoritesList.take(50).size),
        LibraryCategory("My bottom 50", Icons.Default.TrendingDown, TextMuted, 0),
        LibraryCategory("Local", Icons.Default.Folder, Color(0xFF81C784), 0)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Top Filter Chips (Playlists, Songs, Albums, Artists)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterChips.indices.toList()) { index ->
                val isSelected = selectedFilterIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) ElectricViolet else SurfaceElevated)
                        .border(1.dp, if (isSelected) ElectricViolet else CardBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedFilterIndex = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filterChips[index],
                        color = if (isSelected) ObsidianBackground else TextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }

        // Sort Header: Date added
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* Toggle sort */ }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Date added",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Sort order",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "${favoritesList.size} items",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 150.dp)
        ) {
            // 2-Column Square Grid of Categories
            item {
                val chunked = categories.chunked(2)
                chunked.forEach { rowCategories ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowCategories.forEach { cat ->
                            val isSelected = selectedCategory == cat.title
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) SurfaceContainerHigh else SurfaceDark)
                                    .border(1.dp, if (isSelected) ElectricViolet.copy(alpha = 0.5f) else CardBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (cat.title == "Exported") {
                                            onExportJson()
                                        } else {
                                            selectedCategory = cat.title
                                        }
                                    }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(cat.iconTint.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.title,
                                        tint = cat.iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        ),
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${cat.count} songs",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Playlist creation card and custom playlists
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricViolet),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Playlist", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Active category songs (e.g. Liked songs)
            if (favoritesList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No songs saved in library yet", color = TextMuted)
                    }
                }
            } else {
                items(favoritesList) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying && track.id == currentTrackId,
                        isFavorite = true,
                        onTrackClick = { onPlayTrack(track, favoritesList) },
                        onToggleFavorite = { onToggleFavorite(track) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Playlist", color = TextPrimary) },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist name...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ElectricViolet,
                        focusedIndicatorColor = ElectricViolet,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Create", color = ObsidianBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
