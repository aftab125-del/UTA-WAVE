package com.uta.wave.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.data.DesktopPlaylist

/**
 * Desktop equivalent of mobile's SongMenuBottomSheet.kt.
 * Provides complete feature parity: Track Header with Favorite Heart,
 * Quick Action Pills (Add to playlist, Share), and full vertical action items.
 */
@Composable
fun DesktopSongMenuDialog(
    track: TrackItem,
    database: com.uta.wave.desktop.data.DesktopDatabase,
    isInQueue: Boolean = false,
    playlistId: String? = null,
    onDismiss: () -> Unit,
    onPlayNext: (TrackItem) -> Unit,
    onAddToQueue: (TrackItem) -> Unit,
    onStartRadio: (TrackItem) -> Unit,
    onShare: (TrackItem) -> Unit,
    onRemoveFromQueue: ((TrackItem) -> Unit)? = null,
    onRemoveFromPlaylist: ((String, TrackItem) -> Unit)? = null,
    onViewArtist: (String) -> Unit
) {
    val library = remember(database) { database.loadLibrary() }
    var isFav by remember(track) { mutableStateOf(database.isFavorite(track.id.ifBlank { track.videoId })) }
    var playlistsState by remember { mutableStateOf(library.playlists) }

    DesktopSongMenuDialog(
        track = track,
        playlistId = playlistId,
        isInQueue = isInQueue,
        playlists = playlistsState,
        isFavorite = isFav,
        onToggleFavorite = { t ->
            val nowFav = database.toggleFavorite(t)
            isFav = nowFav
        },
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onPinToSpeedDial = { t ->
            database.addTrackToPlaylist("speed_dial", t)
        },
        onAddToLibrary = { t ->
            database.addTrackToPlaylist("library", t)
        },
        onStartRadio = onStartRadio,
        onViewArtist = onViewArtist,
        onRemoveFromPlaylist = if (playlistId != null) { t ->
            onRemoveFromPlaylist?.invoke(playlistId, t)
        } else null,
        onRemoveFromQueue = onRemoveFromQueue,
        onAddToPlaylist = { t, pId ->
            database.addTrackToPlaylist(pId, t)
            playlistsState = database.loadLibrary().playlists
        },
        onCreatePlaylist = { name, t ->
            database.createPlaylist(name, t)
            playlistsState = database.loadLibrary().playlists
        },
        onShare = onShare,
        onDismiss = onDismiss
    )
}

@Composable
fun DesktopSongMenuDialog(
    track: TrackItem,
    playlistId: String? = null,
    isInQueue: Boolean = false,
    playlists: List<DesktopPlaylist>,
    isFavorite: Boolean,
    onToggleFavorite: (TrackItem) -> Unit,
    onPlayNext: (TrackItem) -> Unit,
    onAddToQueue: (TrackItem) -> Unit,
    onPinToSpeedDial: (TrackItem) -> Unit,
    onAddToLibrary: (TrackItem) -> Unit,
    onStartRadio: (TrackItem) -> Unit,
    onViewArtist: (String) -> Unit,
    onRemoveFromPlaylist: ((TrackItem) -> Unit)? = null,
    onRemoveFromQueue: ((TrackItem) -> Unit)? = null,
    onAddToPlaylist: (TrackItem, String) -> Unit,
    onCreatePlaylist: (String, TrackItem) -> Unit,
    onShare: (TrackItem) -> Unit,
    onDismiss: () -> Unit
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showSongDetails by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF140E18),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Drag Handle & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Song Header: Artwork + Title + Artist + Favorite Heart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        color = Color(0xFF1E1724)
                    ) {
                        if (track.artworkUrl.isNotBlank()) {
                            DesktopNetworkImage(
                                url = track.artworkUrl,
                                contentDescription = track.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier
                            .size(36.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Horizontal Quick Action Pills: Add to playlist & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Add to Playlist Pill
                    DesktopQuickActionPill(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        label = "Add to playlist",
                        onClick = { showPlaylistPicker = true },
                        modifier = Modifier.weight(1f)
                    )

                    // Share Pill
                    DesktopQuickActionPill(
                        icon = Icons.Filled.Share,
                        label = "Share",
                        onClick = {
                            onShare(track)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Vertical Detailed Action Items
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Play Next
                    DesktopActionMenuItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        title = "Play next",
                        subtitle = "Add to the top of your queue",
                        onClick = {
                            onPlayNext(track)
                            onDismiss()
                        }
                    )

                    // Add to Queue
                    DesktopActionMenuItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        title = "Add to queue",
                        subtitle = "Add to the bottom of your queue",
                        onClick = {
                            onAddToQueue(track)
                            onDismiss()
                        }
                    )

                    // Pin to Speed Dial
                    DesktopActionMenuItem(
                        icon = Icons.Filled.PushPin,
                        title = "Pin to Speed dial",
                        subtitle = "Pin for instant one-click playback",
                        onClick = {
                            onPinToSpeedDial(track)
                            onDismiss()
                        }
                    )

                    // Add to Library
                    DesktopActionMenuItem(
                        icon = Icons.Filled.BookmarkAdd,
                        title = "Add to library",
                        subtitle = "Save to your library collection",
                        onClick = {
                            onAddToLibrary(track)
                            onDismiss()
                        }
                    )

                    // Start Radio
                    DesktopActionMenuItem(
                        icon = Icons.Filled.Radio,
                        title = "Start Radio",
                        subtitle = "Start automated recommendations from this song",
                        onClick = {
                            onStartRadio(track)
                            onDismiss()
                        }
                    )

                    // View Artist
                    DesktopActionMenuItem(
                        icon = Icons.Filled.Person,
                        title = "View artist",
                        subtitle = track.artist,
                        onClick = {
                            onViewArtist(track.artist)
                            onDismiss()
                        }
                    )

                    // Details
                    DesktopActionMenuItem(
                        icon = Icons.Filled.Info,
                        title = "Details",
                        subtitle = "View song information and codec",
                        onClick = { showSongDetails = true }
                    )

                    // Contextual: Remove from Playlist (if opened within playlist)
                    if (playlistId != null && onRemoveFromPlaylist != null) {
                        DesktopActionMenuItem(
                            icon = Icons.Filled.Delete,
                            title = "Remove from playlist",
                            subtitle = "Remove this song from the current playlist",
                            iconTint = Color(0xFFEF4444),
                            titleColor = Color(0xFFEF4444),
                            onClick = {
                                onRemoveFromPlaylist(track)
                                onDismiss()
                            }
                        )
                    }

                    // Contextual: Remove from Queue (if opened within active queue)
                    if (isInQueue && onRemoveFromQueue != null) {
                        DesktopActionMenuItem(
                            icon = Icons.Filled.Delete,
                            title = "Remove from queue",
                            subtitle = "Remove this song from active queue",
                            iconTint = Color(0xFFEF4444),
                            titleColor = Color(0xFFEF4444),
                            onClick = {
                                onRemoveFromQueue(track)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    // Sub-Dialog 1: Playlist Selection
    if (showPlaylistPicker) {
        DesktopPlaylistPickerModal(
            track = track,
            playlists = playlists,
            onSelectPlaylist = { pId ->
                onAddToPlaylist(track, pId)
                showPlaylistPicker = false
                onDismiss()
            },
            onCreateNewPlaylist = { name ->
                onCreatePlaylist(name, track)
                showPlaylistPicker = false
                onDismiss()
            },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    // Sub-Dialog 2: Song Details
    if (showSongDetails) {
        DesktopSongDetailsModal(
            track = track,
            onDismiss = { showSongDetails = false }
        )
    }
}

@Composable
private fun DesktopQuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun DesktopActionMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = Color.White,
    titleColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
            Spacer(modifier = Modifier.height(1.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.50f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Playlist Selector Sub-Dialog with instant new playlist creation.
 */
@Composable
fun DesktopPlaylistPickerModal(
    track: TrackItem,
    playlists: List<DesktopPlaylist>,
    onSelectPlaylist: (String) -> Unit,
    onCreateNewPlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(18.dp)),
            color = Color(0xFF16121C),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add to Playlist", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // New Playlist input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("New playlist name...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreateNewPlaylist(newPlaylistName.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("YOUR PLAYLISTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)

                Spacer(modifier = Modifier.height(8.dp))

                if (playlists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No playlists yet. Create one above!", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(playlists) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectPlaylist(p.id) }
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                                    color = Color(0xFF221C2A)
                                ) {
                                    if (!p.coverUrl.isNullOrBlank()) {
                                        DesktopNetworkImage(url = p.coverUrl, contentDescription = p.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                                    Text("${p.trackCount} tracks", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Song Metadata Details Sub-Dialog.
 */
@Composable
fun DesktopSongDetailsModal(
    track: TrackItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(18.dp)),
            color = Color(0xFF16121C),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Song Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailItemRow(label = "Title", value = track.title)
                DetailItemRow(label = "Artist", value = track.artist)
                DetailItemRow(label = "Album", value = track.album ?: "Single / YouTube Music")
                DetailItemRow(label = "Video ID", value = track.videoId.ifBlank { track.id })
                DetailItemRow(label = "Audio Codec", value = "Opus / AAC (Lossless Stream)")
                DetailItemRow(label = "Bitrate", value = "256 kbps (HQ Universe Lossless)")
                DetailItemRow(label = "Duration", value = "${track.durationSeconds / 60}:${"%02d".format(track.durationSeconds % 60)}")

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
