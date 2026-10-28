package com.uta.wave.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.uta.wave.ui.recognizer.BlushRose
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuBottomSheet(
    track: TrackItem,
    isFavorite: Boolean = false,
    onToggleFavorite: (TrackItem) -> Unit,
    onPlayNext: ((TrackItem) -> Unit)? = null,
    onAddToQueue: ((TrackItem) -> Unit)? = null,
    onStartRadio: ((TrackItem) -> Unit)? = null,
    onOpenArtist: ((String) -> Unit)? = null,
    onRemoveFromQueue: ((TrackItem) -> Unit)? = null,
    playlistId: String? = null,
    onRemoveFromPlaylist: ((TrackItem) -> Unit)? = null,
    onTrackUpdated: ((TrackItem) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    val downloadManager = remember { AudioDownloadManager.getInstance(context) }

    val playlists by localDatabase.playlistsFlow.collectAsState(initial = emptyList())
    val hiddenTrackIds by localDatabase.hiddenTrackIdsFlow.collectAsState(initial = emptySet())
    val isDownloaded = remember(track.id) { downloadManager.isDownloaded(track.id) }
    val isSongHidden = hiddenTrackIds.contains(track.id)
    val isPlaylistMode = !playlistId.isNullOrBlank()
    val isLocal = track.videoId.startsWith("content://") || track.videoId.startsWith("file://") || track.id.startsWith("local")

    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showSongDetails by remember { mutableStateOf(false) }
    var showTrimRingtone by remember { mutableStateOf(false) }
    var showEditSongDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF140E18),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Drag Handle Indicator
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Song Header: Artwork + Title + Artist + Favorite Heart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(track) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) BlushRose else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Horizontal Quick Action Pills: Add to Playlist, Share (Non-playlist mode)
            if (!isPlaylistMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Add to Playlist Pill
                    QuickActionPill(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        label = "Add to playlist",
                        onClick = {
                            showPlaylistPicker = true
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Share Pill
                    QuickActionPill(
                        icon = Icons.Default.Share,
                        label = "Share",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val streamUrl = if (track.videoId.startsWith("http")) track.videoId else "https://music.youtube.com/watch?v=${track.videoId}"
                                putExtra(Intent.EXTRA_SUBJECT, track.title)
                                putExtra(Intent.EXTRA_TEXT, "${track.title} by ${track.artist} on UTA-WAVE: $streamUrl")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Vertical Detailed Action Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isPlaylistMode) {
                    // 1. Edit Song
                    ActionMenuItem(
                        icon = Icons.Default.Edit,
                        title = "Edit",
                        subtitle = "Edit song",
                        onClick = {
                            showEditSongDialog = true
                        }
                    )

                    // 2. Play next
                    ActionMenuItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        title = "Play next",
                        subtitle = "Add to the top of your queue",
                        onClick = {
                            onPlayNext?.invoke(track)
                            Toast.makeText(context, "Playing next: ${track.title}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    // 3. Add to queue
                    ActionMenuItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        title = "Add to queue",
                        subtitle = "Add to the bottom of your queue",
                        onClick = {
                            onAddToQueue?.invoke(track)
                            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    // 4. Pin to Speed dial
                    ActionMenuItem(
                        icon = Icons.Default.PushPin,
                        title = "Pin to Speed dial",
                        subtitle = "Pin for instant one-tap playback",
                        onClick = {
                            scope.launch {
                                localDatabase.pinToSpeedDial(track)
                                Toast.makeText(context, "Pinned to speed dial", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    )

                    // 5. Add to library
                    ActionMenuItem(
                        icon = Icons.Default.BookmarkAdd,
                        title = "Add to library",
                        subtitle = "Save to your library tracks",
                        onClick = {
                            scope.launch {
                                localDatabase.insertTrack(track)
                                Toast.makeText(context, "Saved to library", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    )

                    // 6. Hide from Home
                    ActionMenuItem(
                        icon = Icons.Default.VisibilityOff,
                        title = if (isSongHidden) "Unhide from Home" else "Hide from Home",
                        subtitle = if (isSongHidden) "Allow this song in recommendations" else "Keep this song out of Quick Picks and Keep Listening",
                        onClick = {
                            scope.launch {
                                if (isSongHidden) {
                                    localDatabase.unhideSongFromHome(track.id)
                                    Toast.makeText(context, "Unhidden from Home", Toast.LENGTH_SHORT).show()
                                } else {
                                    localDatabase.hideSongFromHome(track.id)
                                    Toast.makeText(context, "Hidden from Home", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        }
                    )

                    // 7. Remove from playlist
                    ActionMenuItem(
                        icon = Icons.Default.Delete,
                        title = "Remove from playlist",
                        subtitle = "Remove this song from the current playlist",
                        iconTint = Color(0xFFEF5350),
                        titleColor = Color(0xFFEF5350),
                        onClick = {
                            scope.launch {
                                if (onRemoveFromPlaylist != null) {
                                    onRemoveFromPlaylist(track)
                                } else {
                                    localDatabase.removeTrackFromPlaylist(playlistId, track.id)
                                }
                                Toast.makeText(context, "Removed from playlist", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    )

                    // 8. Remove download / Download (Not applicable for local audio)
                    if (!isLocal) {
                        ActionMenuItem(
                            icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                            title = if (isDownloaded) "Remove download" else "Download",
                            subtitle = if (isDownloaded) "Delete downloaded file" else "Make available for offline playback",
                            iconTint = if (isDownloaded) ElectricViolet else TextPrimary,
                            onClick = {
                                if (isDownloaded) {
                                    downloadManager.deleteDownload(track.id)
                                    Toast.makeText(context, "Download removed", Toast.LENGTH_SHORT).show()
                                } else {
                                    downloadManager.downloadTrack(track)
                                    Toast.makeText(context, "Downloading track in Lossless quality...", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        )
                    }

                    // 9. Set as Ringtone
                    ActionMenuItem(
                        icon = Icons.Default.Notifications,
                        title = "Set as Ringtone",
                        subtitle = "Trim and set as device ringtone",
                        onClick = {
                            showTrimRingtone = true
                        }
                    )
                } else {
                    // Play Next
                    ActionMenuItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        title = "Play next",
                        subtitle = "Add to the top of your queue",
                        onClick = {
                            onPlayNext?.invoke(track)
                            Toast.makeText(context, "Playing next: ${track.title}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    // Add to Queue
                    ActionMenuItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        title = "Add to queue",
                        subtitle = "Add to the bottom of your queue",
                        onClick = {
                            onAddToQueue?.invoke(track)
                            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    // Pin to Speed Dial
                    ActionMenuItem(
                        icon = Icons.Default.PushPin,
                        title = "Pin to Speed dial",
                        subtitle = "Pin for instant one-tap playback",
                        onClick = {
                            scope.launch {
                                localDatabase.pinToSpeedDial(track)
                                Toast.makeText(context, "Pinned to speed dial", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    )

                    // Add to Library
                    ActionMenuItem(
                        icon = Icons.Default.BookmarkAdd,
                        title = "Add to library",
                        subtitle = "Save to your library tracks",
                        onClick = {
                            scope.launch {
                                localDatabase.insertTrack(track)
                                Toast.makeText(context, "Saved to library", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    )

                    // Download (Not applicable for local audio)
                    if (!isLocal) {
                        ActionMenuItem(
                            icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                            title = if (isDownloaded) "Downloaded" else "Download",
                            subtitle = if (isDownloaded) "Downloaded for offline playback" else "Make available for offline playback",
                            iconTint = if (isDownloaded) ElectricViolet else TextPrimary,
                            onClick = {
                                if (isDownloaded) {
                                    downloadManager.deleteDownload(track.id)
                                    Toast.makeText(context, "Download removed", Toast.LENGTH_SHORT).show()
                                } else {
                                    downloadManager.downloadTrack(track)
                                    Toast.makeText(context, "Downloading track in Lossless quality...", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        )
                    }

                    // Set as Ringtone
                    ActionMenuItem(
                        icon = Icons.Default.Notifications,
                        title = "Set as Ringtone",
                        subtitle = "Trim and set as device ringtone",
                        onClick = {
                            showTrimRingtone = true
                        }
                    )

                    // View Artist
                    ActionMenuItem(
                        icon = Icons.Default.Person,
                        title = "View artist",
                        subtitle = track.artist,
                        onClick = {
                            onOpenArtist?.invoke(track.artist)
                            onDismiss()
                        }
                    )

                    // Details
                    ActionMenuItem(
                        icon = Icons.Default.Info,
                        title = "Details",
                        subtitle = "View the song's information",
                        onClick = {
                            showSongDetails = true
                        }
                    )

                    // Remove from Queue (Optional Context Action)
                    if (onRemoveFromQueue != null) {
                        ActionMenuItem(
                            icon = Icons.Default.Delete,
                            title = "Remove from queue",
                            subtitle = "Remove this song from active playback",
                            iconTint = Color(0xFFEF5350),
                            titleColor = Color(0xFFEF5350),
                            onClick = {
                                onRemoveFromQueue(track)
                                Toast.makeText(context, "Removed from queue", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    // Sub-Dialog 1: Add to Playlist Sheet
    if (showPlaylistPicker) {
        PlaylistSelectionSheet(
            track = track,
            playlists = playlists,
            onAddToPlaylist = { playlistId ->
                scope.launch {
                    localDatabase.addTrackToPlaylist(playlistId, track)
                    Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                }
                showPlaylistPicker = false
                onDismiss()
            },
            onCreatePlaylist = { playlistName ->
                scope.launch {
                    val newId = com.uta.wave.data.account.AccountSyncManager.getInstance(context).createPlaylist(playlistName)
                    localDatabase.addTrackToPlaylist(newId, track)
                    if (newId.startsWith("PL") || newId.length >= 24) {
                        com.uta.wave.data.innertube.InnerTubeClient().addTrackToRemotePlaylist(newId, track.videoId)
                    }
                    Toast.makeText(context, "Created and added to $playlistName", Toast.LENGTH_SHORT).show()
                }
                showPlaylistPicker = false
                onDismiss()
            },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    // Sub-Dialog 2: Song Details Modal
    if (showSongDetails) {
        SongDetailsDialog(
            track = track,
            durationMs = track.durationSeconds * 1000L,
            onDismiss = {
                showSongDetails = false
                onDismiss()
            }
        )
    }

    // Sub-Dialog 3: Trim Ringtone Bottom Sheet
    if (showTrimRingtone) {
        TrimRingtoneBottomSheet(
            track = track,
            durationSeconds = if (track.durationSeconds > 0) track.durationSeconds else 180,
            onDismiss = {
                showTrimRingtone = false
                onDismiss()
            }
        )
    }

    // Sub-Dialog 4: Edit Song Dialog
    if (showEditSongDialog) {
        EditSongDialog(
            track = track,
            onSave = { updatedTitle, updatedArtist, updatedAlbum ->
                scope.launch {
                    localDatabase.updateTrackMetadata(track.id, updatedTitle, updatedArtist, updatedAlbum)
                    val updated = track.copy(title = updatedTitle, artist = updatedArtist, album = updatedAlbum)
                    onTrackUpdated?.invoke(updated)
                    Toast.makeText(context, "Song updated", Toast.LENGTH_SHORT).show()
                    showEditSongDialog = false
                    onDismiss()
                }
            },
            onDismiss = { showEditSongDialog = false }
        )
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF221828))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = TextPrimary,
    titleColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E1524)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = titleColor
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditSongDialog(
    track: TrackItem,
    onSave: (title: String, artist: String, album: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var editTitle by remember { mutableStateOf(track.title) }
    var editArtist by remember { mutableStateOf(track.artist) }
    var editAlbum by remember { mutableStateOf(track.album ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1724),
        title = {
            Text(
                text = "Edit Song",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Title", color = TextSecondary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF140E18),
                        unfocusedContainerColor = Color(0xFF140E18),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = BlushRose
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = { editArtist = it },
                    label = { Text("Artist", color = TextSecondary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF140E18),
                        unfocusedContainerColor = Color(0xFF140E18),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = BlushRose
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editAlbum,
                    onValueChange = { editAlbum = it },
                    label = { Text("Album (Optional)", color = TextSecondary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF140E18),
                        unfocusedContainerColor = Color(0xFF140E18),
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
                    if (editTitle.isNotBlank() && editArtist.isNotBlank()) {
                        onSave(editTitle.trim(), editArtist.trim(), editAlbum.trim().ifEmpty { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BlushRose)
            ) {
                Text("Save", color = Color(0xFF140E18), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
