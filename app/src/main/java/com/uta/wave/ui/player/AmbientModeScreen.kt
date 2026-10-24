package com.uta.wave.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.LyricLine
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.lyrics.LyricsClient
import com.uta.wave.player.WavePlayerState
import com.uta.wave.ui.components.AmbientCanvas
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Landscape-only Ambient Mode matching Echo Music's signature screensaver experience.
 *
 * Specifications:
 * 1. Landscape-Only: Automatically locks to landscape orientation on entry and restores on exit.
 * 2. Zero Static Buttons: No playback controls or sliders cluttering the view.
 * 3. Pure Gesture Controls:
 *    - Horizontal Swipe: Left for Next song, Right for Previous song (with visual pill indicator).
 *    - Vertical Swipe: Controls volume up/down (with vertical translucent HUD indicator).
 *    - Tap: Toggles Play/Pause with a subtle glowing ripple icon.
 * 4. Single Top-Left 3-Dots Button (⋮):
 *    - Menu with "Exit Ambient Mode" and "Toggle Album Art".
 * 5. Flow Lyrics:
 *    - Kinetic typography where the active line glows and scales smoothly (1.18x), with
 *      upcoming and past lines dynamically dimmed and auto-centered.
 */
@Composable
fun AmbientModeScreen(
    track: TrackItem,
    playerState: WavePlayerState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // 1. Force Landscape & Keep Screen On for the duration of Ambient Mode
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            activity?.requestedOrientation = originalOrientation
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Audio Manager for Gesture Volume Control
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var showVolumeIndicator by remember { mutableStateOf(false) }

    // Gesture Action Feedback States
    var gestureFeedbackText by remember { mutableStateOf<String?>(null) }
    var showPlayPauseFeedback by remember { mutableStateOf(false) }

    // UI Configuration States
    var showMenu by remember { mutableStateOf(false) }
    var isArtworkVisible by remember { mutableStateOf(true) }

    // Lyrics resolution
    val lyricsClient = remember { LyricsClient() }
    var lyricLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isLyricsSynced by remember { mutableStateOf(false) }
    var isLyricsLoading by remember { mutableStateOf(true) }

    val lyricsListState = rememberLazyListState()

    LaunchedEffect(track.id) {
        isLyricsLoading = true
        val result = lyricsClient.getLyrics(track.title, track.artist, track.durationSeconds)
        lyricLines = result.lines
        isLyricsSynced = result.isSynced
        isLyricsLoading = false
    }

    // Active lyric index based on player position
    val activeIndex = remember(lyricLines, playerState.positionMs, isLyricsSynced) {
        if (!isLyricsSynced || lyricLines.isEmpty()) -1
        else {
            val idx = lyricLines.indexOfLast { it.timestampMs <= playerState.positionMs }
            if (idx >= 0) idx else 0
        }
    }

    // Flow Lyrics auto-centering scroll
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && isLyricsSynced) {
            lyricsListState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = -200
            )
        }
    }

    // Gesture Drag Accumulators
    var dragAccumulatorX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatorY by remember { mutableFloatStateOf(0f) }
    var isHorizontalSwipe by remember { mutableStateOf(false) }
    var isVerticalSwipe by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A060E))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onTogglePlayPause()
                        showPlayPauseFeedback = true
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                        isHorizontalSwipe = false
                        isVerticalSwipe = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatorX += dragAmount.x
                        dragAccumulatorY += dragAmount.y

                        val absX = abs(dragAccumulatorX)
                        val absY = abs(dragAccumulatorY)

                        // Determine gesture mode once threshold is met
                        if (!isHorizontalSwipe && !isVerticalSwipe) {
                            if (absX > 30f && absX > absY) {
                                isHorizontalSwipe = true
                            } else if (absY > 30f && absY > absX) {
                                isVerticalSwipe = true
                            }
                        }

                        // Handle Vertical Volume Gesture
                        if (isVerticalSwipe) {
                            showVolumeIndicator = true
                            // Every 35px of vertical drag shifts 1 volume step
                            if (dragAccumulatorY < -35f) {
                                val newVol = (currentVolume + 1).coerceAtMost(maxVolume)
                                if (newVol != currentVolume) {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    currentVolume = newVol
                                }
                                dragAccumulatorY = 0f
                            } else if (dragAccumulatorY > 35f) {
                                val newVol = (currentVolume - 1).coerceAtLeast(0)
                                if (newVol != currentVolume) {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    currentVolume = newVol
                                }
                                dragAccumulatorY = 0f
                            }
                        }
                    },
                    onDragEnd = {
                        // Handle Horizontal Song Skip Gesture
                        if (isHorizontalSwipe) {
                            if (dragAccumulatorX < -90f) {
                                // Swiped Left -> Next Song
                                onNext()
                                gestureFeedbackText = "Next Song"
                            } else if (dragAccumulatorX > 90f) {
                                // Swiped Right -> Previous Song
                                onPrevious()
                                gestureFeedbackText = "Previous Song"
                            }
                        }

                        // Auto-hide volume indicator after brief pause
                        scope.launch {
                            delay(1200)
                            showVolumeIndicator = false
                            gestureFeedbackText = null
                        }

                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                        isHorizontalSwipe = false
                        isVerticalSwipe = false
                    },
                    onDragCancel = {
                        showVolumeIndicator = false
                        gestureFeedbackText = null
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                        isHorizontalSwipe = false
                        isVerticalSwipe = false
                    }
                )
            }
    ) {
        // 1. Dynamic Ambient Canvas Glow
        AmbientCanvas(
            artworkUrl = track.artworkUrl,
            isPlaying = playerState.isPlaying
        )

        // 2. Landscape Split Layout: Left Album Art + Right Flow Lyrics
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Floating Artwork & Track Info (if enabled)
            if (isArtworkVisible) {
                Column(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .shadow(32.dp, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFF1E1724))
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Column: Flow Lyrics (Expanded when artwork is hidden)
            Box(
                modifier = Modifier
                    .weight(if (isArtworkVisible) 0.60f else 1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                FlowLyricsView(
                    lyricLines = lyricLines,
                    activeIndex = activeIndex,
                    isSynced = isLyricsSynced,
                    isLoading = isLyricsLoading,
                    listState = lyricsListState,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.fillMaxWidth(if (!isArtworkVisible) 0.65f else 1f)
                )
            }
        }

        // 3. Top-Left Single 3-Dots Button (⋮)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1724))
            ) {
                // Exit Ambient Mode
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Exit Ambient Mode",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = Color(0xFFF3D7EC)
                        )
                    },
                    onClick = {
                        showMenu = false
                        onExit()
                    }
                )

                // Toggle Album Art
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isArtworkVisible) "Hide Album Art" else "Show Album Art",
                            color = Color.White
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isArtworkVisible) Icons.Outlined.Image else Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    },
                    onClick = {
                        showMenu = false
                        isArtworkVisible = !isArtworkVisible
                    }
                )
            }
        }

        // 4. Gesture Feedback HUD Overlays

        // Horizontal Skip Feedback Pill
        AnimatedVisibility(
            visible = gestureFeedbackText != null,
            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
            exit = fadeOut(tween(200)) + scaleOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (gestureFeedbackText?.contains("Next") == true) Icons.Default.SkipNext else Icons.Default.SkipPrevious,
                        contentDescription = null,
                        tint = Color(0xFFF3D7EC),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = gestureFeedbackText ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Tap Play/Pause Feedback Splash
        LaunchedEffect(showPlayPauseFeedback) {
            if (showPlayPauseFeedback) {
                delay(600)
                showPlayPauseFeedback = false
            }
        }

        AnimatedVisibility(
            visible = showPlayPauseFeedback,
            enter = fadeIn(tween(100)) + scaleIn(tween(100)),
            exit = fadeOut(tween(250)) + scaleOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color(0xFFF3D7EC),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Vertical Swipe Volume Indicator (Right Edge)
        AnimatedVisibility(
            visible = showVolumeIndicator,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(vertical = 14.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val volRatio = currentVolume.toFloat() / maxVolume.toFloat()

                Icon(
                    imageVector = when {
                        currentVolume == 0 -> Icons.AutoMirrored.Filled.VolumeMute
                        volRatio < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = null,
                    tint = Color(0xFFF3D7EC),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Vertical Volume Bar
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((100 * volRatio).dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFF3D7EC))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(volRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Kinetic Flow Lyrics Component.
 * The active lyric line is centered, dynamically scaled, and highlighted with glowing text shadow.
 * Past and upcoming lines smoothly flow and fade.
 */
@Composable
private fun FlowLyricsView(
    lyricLines: List<LyricLine>,
    activeIndex: Int,
    isSynced: Boolean,
    isLoading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = Color(0xFFF3D7EC),
                    strokeWidth = 2.5.dp
                )
            }
        }

        lyricLines.isEmpty() -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = "Lyrics unavailable for this track",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier,
                contentPadding = PaddingValues(vertical = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lyricLines) { index, line ->
                    val isActive = index == activeIndex
                    val distance = abs(index - activeIndex)

                    val animatedScale by animateFloatAsState(
                        targetValue = if (isActive) 1.18f else 0.95f,
                        animationSpec = spring(
                            dampingRatio = 0.75f,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "flowLyricScale"
                    )

                    val alpha = when {
                        !isSynced -> 0.85f
                        isActive -> 1.0f
                        distance == 1 -> 0.40f
                        distance == 2 -> 0.22f
                        else -> 0.12f
                    }

                    val textShadow = if (isActive) {
                        Shadow(
                            color = Color.White.copy(alpha = 0.75f),
                            blurRadius = 24f
                        )
                    } else null

                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = if (isActive) 24.sp else 18.sp,
                            shadow = textShadow,
                            letterSpacing = if (isActive) 0.5.sp else 0.sp
                        ),
                        color = Color.White.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(animatedScale)
                            .clickable {
                                if (isSynced) {
                                    onSeekTo(line.timestampMs)
                                }
                            }
                            .padding(vertical = 14.dp, horizontal = 18.dp)
                    )
                }
            }
        }
    }
}
