package com.uta.wave.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.data.DesktopDatabase
import com.uta.wave.desktop.data.DesktopLyricsData
import com.uta.wave.desktop.data.DesktopLyricsService
import com.uta.wave.desktop.data.DesktopMusicService

enum class FullscreenRightPanelMode {
    LYRICS,
    QUEUE
}

/**
 * Authentic UTA-WAVE Cinematic Fullscreen Player for Windows Desktop:
 * 1. Fullscreen dynamic blurry album artwork background with atmospheric dark scrim.
 * 2. Left side: Truly dissolved album artwork (fading smoothly via radial alpha mask with ZERO borderlines),
 *    song metadata, and muted frosted glass controls.
 * 3. Right side: Synchronized real-time word-by-word karaoke lyrics with clean white typography & zero box halos.
 * 4. Geometry: Horizontally centered 932dp cluster bringing artwork & lyrics closer together.
 */
@Composable
fun DesktopFullscreenPlayerOverlay(
    visible: Boolean,
    playbackState: UnifiedPlaybackState,
    queue: List<TrackItem>,
    currentQueueIndex: Int,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSelectQueueTrack: (Int) -> Unit,
    isShuffle: Boolean = false,
    repeatMode: DesktopRepeatMode = DesktopRepeatMode.OFF,
    isQueueLocked: Boolean = false,
    database: DesktopDatabase? = null,
    musicService: DesktopMusicService? = null,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleLock: () -> Unit = {},
    onMoveTrack: (fromIndex: Int, delta: Int) -> Unit = { _, _ -> },
    onRemoveFromQueue: (TrackItem) -> Unit = {},
    onRemoveMultipleFromQueue: (Set<String>) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onSaveQueueAsPlaylist: (String) -> Unit = {},
    onInsertTrackNext: (TrackItem) -> Unit = {},
    onAddToQueue: (TrackItem) -> Unit = {},
    onTrackMoreClick: ((TrackItem) -> Unit)? = null,
    onMinimize: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(250)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
    ) {
        val artwork = playbackState.artworkUrl
        val palette = rememberAlbumArtPalette(artwork)

        // Lyrics Service integration
        val lyricsService = remember { DesktopLyricsService() }
        var lyricsData by remember { mutableStateOf(DesktopLyricsData(emptyList(), false, "")) }
        var isLyricsLoading by remember { mutableStateOf(false) }
        var rightPanelMode by remember { mutableStateOf(FullscreenRightPanelMode.LYRICS) }

        LaunchedEffect(playbackState.title, playbackState.artist) {
            if (playbackState.title.isNotBlank()) {
                isLyricsLoading = true
                lyricsData = lyricsService.getLyrics(
                    title = playbackState.title,
                    artist = playbackState.artist,
                    durationSeconds = (playbackState.durationMs / 1000).toInt()
                )
                isLyricsLoading = false
            }
        }

        // Scrubber drag state (with latch protection to prevent stale snapback)
        var isDraggingSlider by remember { mutableStateOf(false) }
        var sliderPosition by remember { mutableStateOf(0f) }
        var lastSeekFinishedMs by remember { mutableStateOf(0L) }

        if (!isDraggingSlider && System.currentTimeMillis() - lastSeekFinishedMs > 600L) {
            sliderPosition = if (playbackState.durationMs > 0) {
                (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }

        var isShuffleOn by remember { mutableStateOf(false) }
        var isRepeatOn by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            // =================================================================
            // LAYER 0 & 1: Atmospheric Fullscreen Blurred Album Artwork Background
            // =================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07080D))
            )

            if (!artwork.isNullOrBlank()) {
                DesktopNetworkImage(
                    url = artwork,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.35f
                            scaleY = 1.35f
                        }
                        .blur(75.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Atmospheric Dark Vertical Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.78f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // Dynamic Radial Glow vignette matching album art dominant color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                palette.glowColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(320f, 400f),
                            radius = 800f
                        )
                    )
            )

            // =================================================================
            // MAIN CONTENT CANVAS
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                // -------------------------------------------------------------
                // Top Header Row
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Collapse Button
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Center Pill Badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NOW PLAYING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.55f),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UTA·WAVE Universe Lossless Audio",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.38f)
                        )
                    }

                    // Right Actions: Mode Switcher Pill + Window Control Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mode Switcher Pill: Refined Frosted Charcoal Glass
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(if (rightPanelMode == FullscreenRightPanelMode.LYRICS) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                                        .clickable { rightPanelMode = FullscreenRightPanelMode.LYRICS }
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Lyrics",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (rightPanelMode == FullscreenRightPanelMode.LYRICS) Color.White else Color.White.copy(alpha = 0.60f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(if (rightPanelMode == FullscreenRightPanelMode.QUEUE) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                                        .clickable { rightPanelMode = FullscreenRightPanelMode.QUEUE }
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Queue (${queue.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (rightPanelMode == FullscreenRightPanelMode.QUEUE) Color.White else Color.White.copy(alpha = 0.60f)
                                    )
                                }
                            }
                        }

                        // Window Control Capsule (Minimize, Restore/Exit, Close)
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Minimize Button
                                IconButton(
                                    onClick = onMinimize,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(10.dp)
                                            .height(2.dp)
                                            .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(1.dp))
                                    )
                                }

                                // Restore / Exit Fullscreen Button
                                IconButton(
                                    onClick = onCollapse,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .border(1.5.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(1.dp))
                                    )
                                }

                                // Close Button
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // -------------------------------------------------------------
                // Centered Harmonious Cluster: [Left: 340dp] <--- 72dp ---> [Right: 520dp]
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // =========================================================
                    // LEFT COLUMN: Truly Dissolved Artwork + Song Info + Muted Controls
                    // =========================================================
                    Column(
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 1. Dissolved Album Artwork (Radial alpha fade to transparent, ZERO borderlines)
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    // Soft peripheral radial vignette: center 100% visible, fading out smoothly to 0% at edges
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.White,
                                                0.45f to Color.White,
                                                0.70f to Color.White.copy(alpha = 0.55f),
                                                0.86f to Color.White.copy(alpha = 0.18f),
                                                1.0f to Color.Transparent
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.width * 0.49f
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!artwork.isNullOrBlank()) {
                                DesktopNetworkImage(
                                    url = artwork,
                                    contentDescription = playbackState.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(palette.dominantColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2. Track Title & Artist
                        Text(
                            text = playbackState.title.ifBlank { "Ready to play" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = playbackState.artist.ifBlank { "UTA-WAVE" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Lossless Audio Badge Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Text(
                                text = "HQ · 24-bit 96kHz Lossless Audio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. High-Precision Scrubber Slider (Instant jump on click & smooth drag)
                        Slider(
                            value = sliderPosition,
                            onValueChange = {
                                isDraggingSlider = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                val targetMs = (sliderPosition * playbackState.durationMs.toFloat()).toLong()
                                onSeekTo(targetMs)
                                lastSeekFinishedMs = System.currentTimeMillis()
                                isDraggingSlider = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accentColor,
                                activeTrackColor = palette.accentColor.copy(alpha = 0.85f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                        )

                        // Timestamps Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val elapsedMs = if (isDraggingSlider || System.currentTimeMillis() - lastSeekFinishedMs <= 600L) {
                                (sliderPosition * playbackState.durationMs).toLong()
                            } else playbackState.positionMs

                            Text(
                                text = formatFullscreenTime(elapsedMs),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                            Text(
                                text = formatFullscreenTime(playbackState.durationMs),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Transport Controls (Frosted Glass Play FAB & Muted Accents)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle
                            IconButton(
                                onClick = { isShuffleOn = !isShuffleOn },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(
                                    Icons.Filled.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffleOn) palette.accentColor else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Previous Track
                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(
                                    Icons.Filled.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Frosted Glass Play/Pause FAB
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(60.dp)
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = CircleShape,
                                        ambientColor = palette.glowColor,
                                        spotColor = palette.accentColor.copy(alpha = 0.35f)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                palette.accentColor.copy(alpha = 0.85f),
                                                palette.accentColor.copy(alpha = 0.65f)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Next Track
                            IconButton(
                                onClick = onNext,
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(
                                    Icons.Filled.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Repeat
                            IconButton(
                                onClick = { isRepeatOn = !isRepeatOn },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(
                                    Icons.Filled.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (isRepeatOn) palette.accentColor else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 5. Volume Slider Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val targetVol = if (playbackState.volume > 0f) 0f else 0.8f
                                    onVolumeChange(targetVol)
                                },
                                modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(
                                    imageVector = if (playbackState.volume > 0f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Volume",
                                    tint = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Slider(
                                value = playbackState.volume,
                                onValueChange = onVolumeChange,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.accentColor,
                                    activeTrackColor = palette.accentColor.copy(alpha = 0.85f),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(18.dp)
                                    .pointerHoverIcon(PointerIcon.Hand)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "${(playbackState.volume * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }

                    // Comfortable 72dp separation gap bringing artwork & lyrics closer toward center
                    Spacer(modifier = Modifier.width(72.dp))

                    // =========================================================
                    // RIGHT COLUMN: Real-Time Synced Karaoke Lyrics (Clean White, No Box Halos)
                    // =========================================================
                    Box(
                        modifier = Modifier
                            .width(520.dp)
                            .fillMaxHeight()
                    ) {
                        if (rightPanelMode == FullscreenRightPanelMode.LYRICS) {
                            DesktopSyncedLyricsView(
                                lyrics = lyricsData,
                                currentPositionMs = playbackState.positionMs,
                                isPlaying = playbackState.isPlaying,
                                palette = palette,
                                isLoading = isLyricsLoading,
                                onSeekTo = onSeekTo,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Mobile Parity DesktopQueueView
                            if (database != null && musicService != null) {
                                DesktopQueueView(
                                    queue = queue,
                                    currentQueueIndex = currentQueueIndex,
                                    isPlaying = playbackState.isPlaying,
                                    isShuffle = isShuffle,
                                    repeatMode = repeatMode,
                                    isLocked = isQueueLocked,
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
                                    onTrackMoreClick = onTrackMoreClick,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFullscreenTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
