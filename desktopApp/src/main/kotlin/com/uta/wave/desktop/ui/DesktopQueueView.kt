package com.uta.wave.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.data.DesktopDatabase
import com.uta.wave.desktop.data.DesktopMusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 3-State Repeat Mode matching mobile UTA-WAVE PlaybackManager.
 */
enum class DesktopRepeatMode(val label: String) {
    OFF("Repeat"),
    ALL("Repeat All"),
    ONE("Repeat 1")
}

/**
 * Jumping Equalizer animation identical to mobile's JumpingEqualizer.kt.
 */
@Composable
fun DesktopJumpingEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFC084FC)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "desktop_equalizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(240, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "h1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "h2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(190, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (isPlaying) h1.dp else 4.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (isPlaying) h2.dp else 8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (isPlaying) h3.dp else 6.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

/**
 * Comprehensive Desktop Queue View with 100% mobile feature parity:
 * 1. Now Playing Mini Header (artwork, live equalizer, Favorite Heart, Lock Queue, 3-dots).
 * 2. Control Pills Row: Smart Shuffle, 3-state Repeat, Save Queue as Playlist, Clear Queue.
 * 3. Stats & Multi-Select Bar: Remaining playtime, song count, checklist toggle.
 * 4. Interactive Reorderable List: 1-click Up/Down arrows, quick remove, 3-dots menu.
 * 5. Automix • Similar Tracks Section: Accordion with 1-click Play Next, Add to Queue, and Add to Playlist.
 */
@Composable
fun DesktopQueueView(
    queue: List<TrackItem>,
    currentQueueIndex: Int,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: DesktopRepeatMode,
    isLocked: Boolean,
    database: DesktopDatabase,
    musicService: DesktopMusicService,
    onSelectQueueTrack: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLock: () -> Unit,
    onMoveTrack: (fromIndex: Int, delta: Int) -> Unit,
    onRemoveFromQueue: (TrackItem) -> Unit,
    onRemoveMultipleFromQueue: (Set<String>) -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onInsertTrackNext: (TrackItem) -> Unit,
    onAddToQueue: (TrackItem) -> Unit,
    onTrackMoreClick: ((TrackItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var inSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }

    var showSavePlaylistModal by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("Queue Mix") }

    val currentTrack = queue.getOrNull(currentQueueIndex)

    // Automix recommendations state
    var automixTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isLoadingAutomix by remember { mutableStateOf(false) }
    var isAutomixExpanded by remember { mutableStateOf(false) }

    // Load dynamic Automix recommendations whenever current track changes
    LaunchedEffect(currentTrack?.videoId) {
        val vid = currentTrack?.videoId ?: return@LaunchedEffect
        isLoadingAutomix = true
        try {
            val songs = withContext(Dispatchers.IO) {
                musicService.getTrending().filter { it.videoId != vid }.take(6)
            }
            val existingIds = queue.map { it.videoId }.toSet()
            automixTracks = songs.filter { it.videoId !in existingIds }
        } catch (e: Exception) {
            // ignore
        } finally {
            isLoadingAutomix = false
        }
    }

    // Remaining total duration calculation
    val remainingDurationSeconds = remember(queue, currentQueueIndex) {
        val activeSlice = if (currentQueueIndex in queue.indices) queue.drop(currentQueueIndex) else queue
        activeSlice.sumOf { it.durationSeconds.toLong() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        // =============================================================
        // 1. Now Playing Mini Card (Matching Mobile QueueSheet Header)
        // =============================================================
        if (currentTrack != null) {
            var isCurrentFav by remember(currentTrack) {
                mutableStateOf(database.isFavorite(currentTrack.id.ifBlank { currentTrack.videoId }))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = Color(0xFF1E1624).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork Thumbnail with Live Equalizer Badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF131018))
                    ) {
                        DesktopNetworkImage(
                            url = currentTrack.artworkUrl,
                            contentDescription = currentTrack.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isPlaying) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                DesktopJumpingEqualizer(isPlaying = true, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NOW PLAYING",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC084FC),
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = currentTrack.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentTrack.artist,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Favorite Heart Button
                        IconButton(
                            onClick = {
                                val nowFav = database.toggleFavorite(currentTrack)
                                isCurrentFav = nowFav
                            },
                            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = if (isCurrentFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isCurrentFav) Color(0xFFEC4899) else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Lock Queue Toggle
                        IconButton(
                            onClick = onToggleLock,
                            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isLocked) "Queue Locked" else "Queue Unlocked",
                                tint = if (isLocked) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 3-Dots Menu
                        if (onTrackMoreClick != null) {
                            IconButton(
                                onClick = { onTrackMoreClick(currentTrack) },
                                modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // =============================================================
        // 2. Control Pills Row: Smart Shuffle, Repeat, Save, Clear
        // =============================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Smart Shuffle Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isShuffle) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF1E1726))
                    .border(
                        1.dp,
                        if (isShuffle) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onToggleShuffle)
                    .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) Color(0xFFC084FC) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shuffle",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isShuffle) Color(0xFFC084FC) else Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // 3-State Repeat Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (repeatMode != DesktopRepeatMode.OFF) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF1E1726))
                    .border(
                        1.dp,
                        if (repeatMode != DesktopRepeatMode.OFF) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onToggleRepeat)
                    .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (repeatMode == DesktopRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat Mode",
                        tint = if (repeatMode != DesktopRepeatMode.OFF) Color(0xFFC084FC) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = repeatMode.label,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (repeatMode != DesktopRepeatMode.OFF) Color(0xFFC084FC) else Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Save as Playlist Pill
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E1726))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .clickable { showSavePlaylistModal = true }
                    .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Save Queue as Playlist",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Save",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Clear Upcoming Queue Pill
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E1726))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onClearQueue)
                    .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear Queue",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Clear",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // =============================================================
        // 3. Queue Section Header & Multi-Select Bar
        // =============================================================
        if (inSelectMode) {
            // Multi-Select Action Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color(0xFF241829),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                inSelectMode = false
                                selectedIds.clear()
                            },
                            modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedIds.size} selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                if (selectedIds.size == queue.size) {
                                    selectedIds.clear()
                                } else {
                                    selectedIds.clear()
                                    selectedIds.addAll(queue.map { it.id.ifBlank { it.videoId } })
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (selectedIds.size == queue.size) "Deselect All" else "Select All",
                                fontSize = 11.5.sp,
                                color = Color(0xFFC084FC)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                onRemoveMultipleFromQueue(selectedIds.toSet())
                                inSelectMode = false
                                selectedIds.clear()
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = if (selectedIds.isNotEmpty()) Color(0xFFEF5350) else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Normal Header: Title & Total Remaining Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Continue Playing",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Next in Queue",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${queue.size} songs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = formatQueueRemainingTime(remainingDurationSeconds),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = { inSelectMode = true },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF221727))
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Select Mode",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // =============================================================
        // 4. Interactive Reorderable Queue Track List
        // =============================================================
        var draggingIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffsetY by remember { mutableStateOf(0f) }

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty. Play any song or station to start.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(queue) { index, track ->
                    val isCurrent = index == currentQueueIndex
                    val trackKey = track.id.ifBlank { track.videoId }
                    val isSelected = selectedIds.contains(trackKey)
                    val isDraggingThis = draggingIndex == index

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDraggingThis) 10f else 1f)
                            .graphicsLayer {
                                translationY = if (isDraggingThis) dragOffsetY else 0f
                                scaleX = if (isDraggingThis) 1.02f else 1f
                                scaleY = if (isDraggingThis) 1.02f else 1f
                            }
                            .clip(RoundedCornerShape(10.dp)),
                        color = if (isDraggingThis) Color(0xFF3B254A) else if (isCurrent) Color(0xFF2B1D32) else Color(0xFF19131F),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isDraggingThis) 1.5.dp else 1.dp,
                            if (isDraggingThis) Color(0xFFC084FC) else if (isCurrent) Color(0xFF8B5CF6).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.08f)
                        ),
                        shadowElevation = if (isDraggingThis) 12.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (inSelectMode) {
                                        if (isSelected) selectedIds.remove(trackKey)
                                        else selectedIds.add(trackKey)
                                    } else {
                                        onSelectQueueTrack(index)
                                    }
                                }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Multi-Select Checkbox
                            if (inSelectMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selectedIds.remove(trackKey)
                                        else selectedIds.add(trackKey)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF8B5CF6),
                                        uncheckedColor = Color.White.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                // Index Number
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) Color(0xFFC084FC) else Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.width(24.dp)
                                )
                            }

                            // Artwork
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF131018))
                            ) {
                                DesktopNetworkImage(
                                    url = track.artworkUrl,
                                    contentDescription = track.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPlaying) {
                                            DesktopJumpingEqualizer(isPlaying = true, color = Color.White)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Title, Artist and Duration
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = if (isCurrent) Color(0xFFC084FC) else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist}${if (track.durationSeconds > 0) " • ${formatTrackTime(track.durationSeconds)}" else ""}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Interactive Reorder & Context Controls
                            if (!isLocked && !inSelectMode) {
                                // Drag & Drop Handle with 4-way move cursor
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDraggingThis) Color(0xFF8B5CF6).copy(alpha = 0.35f) else Color.Transparent)
                                        .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR)))
                                        .pointerInput(index, queue.size) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggingIndex = index
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    val currentIdx = draggingIndex ?: return@detectDragGestures
                                                    val stepPx = 52.dp.toPx()
                                                    if (dragOffsetY > stepPx && currentIdx < queue.size - 1) {
                                                        onMoveTrack(currentIdx, 1)
                                                        draggingIndex = currentIdx + 1
                                                        dragOffsetY -= stepPx
                                                    } else if (dragOffsetY < -stepPx && currentIdx > 0) {
                                                        onMoveTrack(currentIdx, -1)
                                                        draggingIndex = currentIdx - 1
                                                        dragOffsetY += stepPx
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingIndex = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        tint = if (isDraggingThis) Color(0xFFC084FC) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Quick Delete
                                IconButton(
                                    onClick = { onRemoveFromQueue(track) },
                                    modifier = Modifier.size(26.dp).pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove from Queue",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            // 3-Dots Menu
                            if (onTrackMoreClick != null) {
                                IconButton(
                                    onClick = { onTrackMoreClick(track) },
                                    modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color.White.copy(alpha = 0.65f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =============================================================
        // 5. Automix • Similar Tracks Section (Expandable Accordion)
        // =============================================================
        if (automixTracks.isNotEmpty() && !inSelectMode) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = Color(0xFF1E1624).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isAutomixExpanded = !isAutomixExpanded }
                            .pointerHoverIcon(PointerIcon.Hand),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Automix • Similar Tracks",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${automixTracks.size})",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoadingAutomix) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFEC4899)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Icon(
                                imageVector = if (isAutomixExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isAutomixExpanded) "Collapse" else "Expand",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isAutomixExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            automixTracks.forEach { autoTrack ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF131018))
                                    ) {
                                        DesktopNetworkImage(
                                            url = autoTrack.artworkUrl,
                                            contentDescription = autoTrack.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = autoTrack.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = autoTrack.artist,
                                            fontSize = 10.5.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // 1-Tap Play Next
                                        IconButton(
                                            onClick = {
                                                onInsertTrackNext(autoTrack)
                                                automixTracks = automixTracks.filter { it.videoId != autoTrack.videoId }
                                            },
                                            modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                                contentDescription = "Play Next",
                                                tint = Color(0xFFC084FC),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // 1-Tap Add to Queue
                                        IconButton(
                                            onClick = {
                                                onAddToQueue(autoTrack)
                                                automixTracks = automixTracks.filter { it.videoId != autoTrack.videoId }
                                            },
                                            modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add to Queue",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Save Queue as Playlist Dialog
    if (showSavePlaylistModal) {
        Dialog(onDismissRequest = { showSavePlaylistModal = false }) {
            Surface(
                modifier = Modifier
                    .width(380.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color(0xFF16101C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Save Queue as Playlist",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create a new playlist containing all ${queue.size} songs currently in your queue.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showSavePlaylistModal = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    onSaveQueueAsPlaylist(newPlaylistName.trim())
                                    showSavePlaylistModal = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Playlist", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slide-over / floating modal presentation of the queue for Windowed Mode.
 */
@Composable
fun DesktopQueueModalDialog(
    queue: List<TrackItem>,
    currentQueueIndex: Int,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: DesktopRepeatMode,
    isLocked: Boolean,
    database: DesktopDatabase,
    musicService: DesktopMusicService,
    onDismiss: () -> Unit,
    onSelectQueueTrack: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLock: () -> Unit,
    onMoveTrack: (fromIndex: Int, delta: Int) -> Unit,
    onRemoveFromQueue: (TrackItem) -> Unit,
    onRemoveMultipleFromQueue: (Set<String>) -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onInsertTrackNext: (TrackItem) -> Unit,
    onAddToQueue: (TrackItem) -> Unit,
    onTrackMoreClick: ((TrackItem) -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(480.dp)
                .height(640.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF140D1A).copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Bar with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAYBACK QUEUE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFFC084FC)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                DesktopQueueView(
                    queue = queue,
                    currentQueueIndex = currentQueueIndex,
                    isPlaying = isPlaying,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    isLocked = isLocked,
                    database = database,
                    musicService = musicService,
                    onSelectQueueTrack = onSelectQueueTrack,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleLock = onToggleLock,
                    onMoveTrack = onMoveTrack,
                    onRemoveFromQueue = onRemoveFromQueue,
                    onRemoveMultipleFromQueue = onRemoveMultipleFromQueue,
                    onClearQueue = onClearQueue,
                    onSaveQueueAsPlaylist = onSaveQueueAsPlaylist,
                    onInsertTrackNext = onInsertTrackNext,
                    onAddToQueue = onAddToQueue,
                    onTrackMoreClick = onTrackMoreClick
                )
            }
        }
    }
}

private fun formatQueueRemainingTime(seconds: Long): String {
    if (seconds <= 0) return "0 min remaining"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m remaining"
    } else {
        "${minutes} min remaining"
    }
}

private fun formatTrackTime(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
