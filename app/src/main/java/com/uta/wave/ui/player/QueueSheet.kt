package com.uta.wave.ui.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.player.PlaybackManager
import com.uta.wave.player.RepeatMode
import com.uta.wave.ui.components.JumpingEqualizer
import com.uta.wave.ui.components.SongMenuBottomSheet
import com.uta.wave.ui.recognizer.BlushRose
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    val playerState by playbackManager.state.collectAsState()
    val favoritesList by localDatabase.favoritesFlow.collectAsState(initial = emptyList())

    var isLocked by remember { mutableStateOf(false) }
    var inSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }

    // Drag and Drop State (EchoMusic 1:1 sh.calvin.reorderable engine)
    val localQueue = remember(queue) { queue.toMutableStateList() }
    val lazyListState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localQueue.add(to.index, localQueue.removeAt(from.index))
        dragInfo = if (dragInfo == null) {
            from.index to to.index
        } else {
            dragInfo!!.first to to.index
        }
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Dispatch reorder to PlaybackManager once user releases the dragging handle
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                if (from != to) {
                    onReorderQueue?.invoke(from, to)
                }
                dragInfo = null
            }
        }
    }

    // 3-dots context menu state
    var selectedTrackForMenu by remember { mutableStateOf<TrackItem?>(null) }

    BackHandler(enabled = selectedTrackForMenu != null) {
        selectedTrackForMenu = null
    }
    BackHandler(enabled = inSelectMode) {
        inSelectMode = false
        selectedIds.clear()
    }
    BackHandler(enabled = !inSelectMode && selectedTrackForMenu == null) {
        onDismiss()
    }

    var automixTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isLoadingAutomix by remember { mutableStateOf(false) }

    // Keep localQueue in sync with external queue changes when not actively dragging
    LaunchedEffect(queue) {
        if (!reorderableState.isAnyItemDragging) {
            localQueue.clear()
            localQueue.addAll(queue)
        }
    }

    // Dynamic automix recommendations
    val activeTrack = currentTrack ?: queue.getOrNull(currentIndex)
    LaunchedEffect(activeTrack?.videoId) {
        val vid = activeTrack?.videoId ?: return@LaunchedEffect
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

    // Remaining Queue Duration calculation
    val remainingDurationSeconds = remember(queue, currentIndex) {
        val activeSlice = if (currentIndex in queue.indices) queue.drop(currentIndex) else queue
        activeSlice.sumOf { it.durationSeconds.toLong() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF140E18),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = null,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top Sheet Pill Handle
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Now Playing Mini Header (Screenshot 2: artwork, title/artist, heart, lock, 3-dots)
                if (activeTrack != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF221727))
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark)
                        ) {
                            AsyncImage(
                                model = activeTrack.artworkUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            if (isPlaying) {
                                Box(
                                    modifier = Modifier.matchParentSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    JumpingEqualizer(isPlaying = true, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeTrack.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = activeTrack.artist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Favorite toggle
                            IconButton(
                                onClick = {
                                    coroutineScope.launch { localDatabase.toggleFavorite(activeTrack) }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (favoritesList.any { it.id == activeTrack.id }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (favoritesList.any { it.id == activeTrack.id }) BlushRose else TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Lock Queue toggle
                            IconButton(
                                onClick = { isLocked = !isLocked },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isLocked) "Locked" else "Unlocked",
                                    tint = if (isLocked) BlushRose else TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // 3-dots options
                            IconButton(
                                onClick = { selectedTrackForMenu = activeTrack },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. Control Pills Row: Shuffle, Repeat, Radio (Screenshot 2: 3 wide segmented pills)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Shuffle Pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (playerState.isShuffle) BlushRose.copy(alpha = 0.22f) else Color(0xFF221727))
                            .border(
                                1.dp,
                                if (playerState.isShuffle) BlushRose else CardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { playbackManager.toggleShuffle() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.isShuffle) BlushRose else TextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shuffle",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = if (playerState.isShuffle) BlushRose else TextPrimary
                        )
                    }

                    // Repeat Pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (playerState.repeatMode != RepeatMode.OFF) BlushRose.copy(alpha = 0.22f) else Color(0xFF221727))
                            .border(
                                1.dp,
                                if (playerState.repeatMode != RepeatMode.OFF) BlushRose else CardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { playbackManager.toggleRepeat() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (playerState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (playerState.repeatMode != RepeatMode.OFF) BlushRose else TextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (playerState.repeatMode) {
                                RepeatMode.ONE -> "Repeat 1"
                                RepeatMode.ALL -> "Repeat All"
                                RepeatMode.OFF -> "Repeat"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = if (playerState.repeatMode != RepeatMode.OFF) BlushRose else TextPrimary
                        )
                    }

                    // Radio Pill
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isRadioActive) BlushRose.copy(alpha = 0.22f) else Color(0xFF221727))
                            .border(
                                1.dp,
                                if (isRadioActive) BlushRose else CardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onStartRadio?.invoke()
                                    ?: playbackManager.startRadioFromCurrentTrack()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Radio",
                            tint = if (isRadioActive) BlushRose else TextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Radio",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = if (isRadioActive) BlushRose else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Queue Section Header (Screenshot 2: Continue Playing / Next in queue, stats & multi-select)
                if (inSelectMode) {
                    // Multi-Select Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF221727))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${selectedIds.size} selected",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = TextPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedIds.size == queue.size && queue.isNotEmpty(),
                                onCheckedChange = { checked ->
                                    selectedIds.clear()
                                    if (checked) selectedIds.addAll(queue.map { it.id })
                                },
                                colors = CheckboxDefaults.colors(checkedColor = BlushRose)
                            )

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
                            Text(
                                text = "Continue Playing",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Next in Queue",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${queue.size} songs",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                    color = TextPrimary
                                )
                                Text(
                                    text = formatDuration(remainingDurationSeconds.toInt()),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = { inSelectMode = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF221727))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Select Mode",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Interactive Drag & Drop Queue List
                if (localQueue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Queue is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .height((localQueue.size * 68).coerceAtMost(380).dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = localQueue,
                            key = { _, item -> item.id }
                        ) { index, track ->
                            val isCurrent = index == currentIndex
                            val isSelected = selectedIds.contains(track.id)

                            // Dismiss state with 1-tap Undo (active only when not locked and not in select mode)
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissVal ->
                                    if (!isLocked && !inSelectMode && dismissVal == SwipeToDismissBoxValue.EndToStart) {
                                        onRemoveFromQueue?.invoke(track.id)
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Removed \"${track.title}\"",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                onInsertTrackNext?.invoke(track)
                                            }
                                        }
                                        true
                                    } else false
                                }
                            )

                            ReorderableItem(
                                state = reorderableState,
                                key = track.id
                            ) { isDragging ->
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = !isLocked && !inSelectMode && !isDragging,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                Color(0xFFEF5350).copy(alpha = 0.85f)
                                            } else Color.Transparent,
                                            label = "DismissColor"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(color)
                                                .padding(end = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                ) {
                                    DragDropQueueRow(
                                        track = track,
                                        index = index,
                                        isCurrent = isCurrent,
                                        isPlaying = isPlaying && isCurrent,
                                        isLocked = isLocked,
                                        inSelectMode = inSelectMode,
                                        isSelected = isSelected,
                                        isDragging = isDragging,
                                        onToggleSelect = {
                                            if (isSelected) selectedIds.remove(track.id)
                                            else selectedIds.add(track.id)
                                        },
                                        onClick = {
                                            if (inSelectMode) {
                                                if (isSelected) selectedIds.remove(track.id)
                                                else selectedIds.add(track.id)
                                            } else {
                                                onSelectTrack(track)
                                            }
                                        },
                                        onMoreClick = {
                                            selectedTrackForMenu = track
                                        },
                                        dragHandleModifier = Modifier.draggableHandle(
                                            enabled = !isLocked && !inSelectMode,
                                            onDragStarted = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Automix Recommendations at Queue Bottom (Similar Tracks)
                if (automixTracks.isNotEmpty() && !inSelectMode) {
                    Spacer(modifier = Modifier.height(14.dp))
                    AutomixSection(
                        automixTracks = automixTracks,
                        isLoading = isLoadingAutomix,
                        onPlayNext = { track ->
                            onInsertTrackNext?.invoke(track)
                            automixTracks = automixTracks.filter { it.id != track.id }
                            Toast.makeText(context, "Added \"${track.title}\" to play next", Toast.LENGTH_SHORT).show()
                        },
                        onAddToQueue = { track ->
                            onAddToQueue?.invoke(track)
                            automixTracks = automixTracks.filter { it.id != track.id }
                            Toast.makeText(context, "Added \"${track.title}\" to queue", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Embedded Snackbar Host for 1-Tap Undo
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }

    // Song Menu Options Bottom Sheet (when tapping 3-dots on any queue item)
    if (selectedTrackForMenu != null) {
        val item = selectedTrackForMenu!!
        SongMenuBottomSheet(
            track = item,
            isFavorite = favoritesList.any { it.id == item.id },
            onToggleFavorite = { trackToToggle ->
                coroutineScope.launch { localDatabase.toggleFavorite(trackToToggle) }
            },
            onPlayNext = { trackToPlayNext ->
                onInsertTrackNext?.invoke(trackToPlayNext)
            },
            onAddToQueue = { trackToQueue ->
                onAddToQueue?.invoke(trackToQueue)
            },
            onStartRadio = { trackToRadio ->
                onStartRadio?.invoke()
                    ?: playbackManager.playRadio(trackToRadio)
            },
            onOpenArtist = { artistName ->
                // Dismiss sheet and open artist
            },
            onRemoveFromQueue = { trackToRemove ->
                onRemoveFromQueue?.invoke(trackToRemove.id)
            },
            onDismiss = { selectedTrackForMenu = null }
        )
    }
}

@Composable
fun DragDropQueueRow(
    track: TrackItem,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isLocked: Boolean,
    inSelectMode: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 1f)
            .graphicsLayer {
                if (isDragging) {
                    shadowElevation = 14f
                    scaleX = 1.02f
                    scaleY = 1.02f
                } else {
                    shadowElevation = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color(0xFF2B1D32) else Color(0xFF201626))
            .border(
                width = 1.dp,
                color = if (isCurrent) ElectricViolet.copy(alpha = 0.6f) else if (isDragging) BlushRose.copy(alpha = 0.8f) else CardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (inSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = BlushRose),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Thumbnail with Playing Indicator
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        JumpingEqualizer(isPlaying = true, color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Title and Artist • Duration
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = if (isCurrent) ElectricViolet else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${track.artist} • ${formatDuration(track.durationSeconds)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3-dots Menu button
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Drag Handle ≡ (Screenshot 2)
        Box(
            modifier = dragHandleModifier
                .size(32.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = if (isLocked) TextMuted.copy(alpha = 0.3f) else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AutomixSection(
    automixTracks: List<TrackItem>,
    isLoading: Boolean,
    onPlayNext: (TrackItem) -> Unit,
    onAddToQueue: (TrackItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1624))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = BlushRose,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Automix • Similar Tracks",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = TextPrimary
                )
                if (automixTracks.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${automixTracks.size})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(14.dp),
                        strokeWidth = 2.dp,
                        color = BlushRose
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Automix" else "Expand Automix",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))

                automixTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onPlayNext(track) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = "Play Next",
                                    tint = BlushRose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { onAddToQueue(track) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to Queue",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationSeconds: Int): String {
    if (durationSeconds <= 0) return "0:00"
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
