package com.uta.wave.ui.player

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<TrackItem>,
    currentIndex: Int,
    isPlaying: Boolean,
    queueTitle: String? = null,
    isRadioActive: Boolean = false,
    currentTrack: TrackItem? = null,
    onSelectTrack: (TrackItem) -> Unit,
    onReorderQueue: ((from: Int, to: Int) -> Unit)? = null,
    onRemoveFromQueue: ((String) -> Unit)? = null,
    onInsertTrackNext: ((TrackItem) -> Unit)? = null,
    onAddToQueue: ((TrackItem) -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onClearQueue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLocked by remember { mutableStateOf(false) }
    var inSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }

    var automixTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isLoadingAutomix by remember { mutableStateOf(false) }

    // Fetch dynamic automix / similar content based on current track
    LaunchedEffect(currentTrack?.videoId) {
        val vid = currentTrack?.videoId ?: return@LaunchedEffect
        isLoadingAutomix = true
        try {
            val result = withContext(Dispatchers.IO) {
                InnerTubeClient().getNext(videoId = vid, playlistId = "RDAMVM$vid")
            }
            val existingIds = queue.map { it.videoId }.toSet()
            automixTracks = result?.items?.filter { it.videoId !in existingIds }?.take(6) ?: emptyList()
        } catch (e: Exception) {
            // ignore
        } finally {
            isLoadingAutomix = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF140F18),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Drag handle pill
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Standard Header or Multi-Selection Header
                if (inSelectMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF231B2A))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                inSelectMode = false
                                selectedIds.clear()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextPrimary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedIds.size} selected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Select All checkbox
                            Checkbox(
                                checked = selectedIds.size == queue.size && queue.isNotEmpty(),
                                onCheckedChange = { checked ->
                                    selectedIds.clear()
                                    if (checked) {
                                        selectedIds.addAll(queue.map { it.id })
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = ElectricViolet)
                            )

                            // Batch remove
                            IconButton(
                                onClick = {
                                    val toRemove = selectedIds.toList()
                                    toRemove.forEach { onRemoveFromQueue?.invoke(it) }
                                    inSelectMode = false
                                    selectedIds.clear()
                                    Toast.makeText(context, "Removed ${toRemove.size} tracks", Toast.LENGTH_SHORT).show()
                                },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Selected",
                                    tint = if (selectedIds.isNotEmpty()) Color(0xFFEF5350) else TextMuted
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = queueTitle ?: "Now Playing Queue",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isRadioActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ElectricViolet.copy(alpha = 0.25f))
                                            .border(1.dp, ElectricViolet.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RADIO",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFF3D7EC)
                                            )
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${queue.size} tracks",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Start Radio button
                            IconButton(
                                onClick = {
                                    onStartRadio?.invoke()
                                    Toast.makeText(context, "Starting algorithmic radio mix...", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = "Start Radio",
                                    tint = if (isRadioActive) ElectricViolet else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Lock Queue toggle
                            IconButton(onClick = { isLocked = !isLocked }) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isLocked) "Queue Locked" else "Queue Unlocked",
                                    tint = if (isLocked) ElectricViolet else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Multi-selection button
                            IconButton(onClick = { inSelectMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Select Mode",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Clear Queue
                            if (queue.isNotEmpty()) {
                                IconButton(onClick = onClearQueue) {
                                    Icon(
                                        imageVector = Icons.Default.ClearAll,
                                        contentDescription = "Clear Queue",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Queue is empty",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(440.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = queue,
                            key = { index, track -> "${track.id}_$index" }
                        ) { index, track ->
                            val isCurrent = index == currentIndex
                            val isSelected = selectedIds.contains(track.id)

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissVal ->
                                    if (!isLocked && !inSelectMode && (dismissVal == SwipeToDismissBoxValue.StartToEnd || dismissVal == SwipeToDismissBoxValue.EndToStart)) {
                                        onRemoveFromQueue?.invoke(track.id)
                                        coroutineScope.launch {
                                            val snackbarResult = snackbarHostState.showSnackbar(
                                                message = "Removed \"${track.title}\"",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                                onInsertTrackNext?.invoke(track)
                                            }
                                        }
                                        true
                                    } else false
                                }
                            )

                            if (isLocked || inSelectMode) {
                                InteractiveQueueTrackRow(
                                    track = track,
                                    index = index,
                                    totalSize = queue.size,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    isLocked = isLocked,
                                    inSelectMode = inSelectMode,
                                    isSelected = isSelected,
                                    onToggleSelect = {
                                        if (isSelected) selectedIds.remove(track.id)
                                        else selectedIds.add(track.id)
                                    },
                                    onMoveUp = { onReorderQueue?.invoke(index, (index - 1).coerceAtLeast(0)) },
                                    onMoveDown = { onReorderQueue?.invoke(index, (index + 1).coerceAtMost(queue.size - 1)) },
                                    onPlayNext = { onInsertTrackNext?.invoke(track) },
                                    onRemove = { onRemoveFromQueue?.invoke(track.id) },
                                    onClick = {
                                        if (inSelectMode) {
                                            if (isSelected) selectedIds.remove(track.id)
                                            else selectedIds.add(track.id)
                                        } else {
                                            onSelectTrack(track)
                                            onDismiss()
                                        }
                                    }
                                )
                            } else {
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            targetValue = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) Color(0xFFB71C1C) else Color.Transparent,
                                            label = "dismissColor"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(color)
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                ) {
                                    InteractiveQueueTrackRow(
                                        track = track,
                                        index = index,
                                        totalSize = queue.size,
                                        isCurrent = isCurrent,
                                        isPlaying = isCurrent && isPlaying,
                                        isLocked = isLocked,
                                        inSelectMode = inSelectMode,
                                        isSelected = isSelected,
                                        onToggleSelect = {
                                            if (isSelected) selectedIds.remove(track.id)
                                            else selectedIds.add(track.id)
                                        },
                                        onMoveUp = { onReorderQueue?.invoke(index, (index - 1).coerceAtLeast(0)) },
                                        onMoveDown = { onReorderQueue?.invoke(index, (index + 1).coerceAtMost(queue.size - 1)) },
                                        onPlayNext = { onInsertTrackNext?.invoke(track) },
                                        onRemove = { onRemoveFromQueue?.invoke(track.id) },
                                        onClick = {
                                            onSelectTrack(track)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }

                        // Automix ("Similar Content") Section
                        if (automixTracks.isNotEmpty()) {
                            item(key = "automix_divider") {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = ElectricViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Similar Tracks",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Automix",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            itemsIndexed(
                                items = automixTracks,
                                key = { _, item -> "automix_${item.videoId}" }
                            ) { _, recTrack ->
                                AutomixTrackRow(
                                    track = recTrack,
                                    onPlayNext = {
                                        onInsertTrackNext?.invoke(recTrack)
                                        automixTracks = automixTracks.filter { it.videoId != recTrack.videoId }
                                        Toast.makeText(context, "Playing next: ${recTrack.title}", Toast.LENGTH_SHORT).show()
                                    },
                                    onAddToQueue = {
                                        onAddToQueue?.invoke(recTrack)
                                        automixTracks = automixTracks.filter { it.videoId != recTrack.videoId }
                                        Toast.makeText(context, "Added to queue: ${recTrack.title}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Snackbar Host overlay
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun InteractiveQueueTrackRow(
    track: TrackItem,
    index: Int,
    totalSize: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isLocked: Boolean,
    inSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color(0xFF261D2B) else Color(0xFF17111D))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (inSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = ElectricViolet),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Artwork
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1724)),
            contentAlignment = Alignment.Center
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Playing",
                        tint = Color(0xFFF3D7EC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.5.sp
                ),
                color = if (isCurrent) Color(0xFFF3D7EC) else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Quick Reorder and Actions when unlocked
        if (!isLocked && !inSelectMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (index < totalSize - 1) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onPlayNext,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = "Play Next",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AutomixTrackRow(
    track: TrackItem,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1420))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF221A2B)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
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
                onClick = onPlayNext,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = "Play Next",
                    tint = ElectricViolet,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onAddToQueue,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to Queue",
                    tint = Color(0xFFF3D7EC),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
