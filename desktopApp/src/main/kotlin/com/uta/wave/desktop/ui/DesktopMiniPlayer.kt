package com.uta.wave.desktop.ui
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.audio.UnifiedPlaybackState

/**
 * Authentic UTA-WAVE Liquid Glass Floating MiniPlayer for Windows Desktop.
 *
 * Full parity with mobile MiniPlayer.kt:
 * 1. 46dp Spinning circular vinyl disc with center spindle hole dot.
 * 2. Skia hardware-accelerated Liquid Glass backdrop blur + specular light reflections.
 * 3. Bottom curved 2.5dp neon micro-progress line.
 * 4. Interactive play/pause, next/prev, volume popover, and expand-to-fullscreen.
 */
@Composable
fun DesktopMiniPlayer(
    playbackState: UnifiedPlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    onOpenQueue: () -> Unit = {},
    onVolumeChange: (Float) -> Unit,
    onSeekTo: (Long) -> Unit = {},
    backdrop: DesktopBackdrop? = null,
    modifier: Modifier = Modifier
) {
    if (playbackState.videoId.isNullOrBlank() && playbackState.title.isBlank()) {
        return // Do not render floating capsule when nothing is loaded
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isCapsuleHovered by interactionSource.collectIsHoveredAsState()
    var showVolumeSlider by remember { mutableStateOf(false) }

    // Vinyl Disc 360-degree rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "VinylDiscTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylDiscRotation"
    )

    val currentRotation = if (playbackState.isPlaying) rotation else 0f

    // Micro-progress fraction
    val progress = if (playbackState.durationMs > 0) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Main Floating Capsule Pill
    Box(
        modifier = modifier
            .width(580.dp)
            .height(64.dp)
            .desktopLiquidGlass(
                backdrop = backdrop,
                shape = RoundedCornerShape(percent = 50),
                blurRadius = 36f,
                borderBrush = GlassSpecularBorderBrush,
                elevation = if (isCapsuleHovered) 28.dp else 22.dp
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onExpand
            )
    ) {
        // Inner Container with Controls
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Spinning Circular Vinyl Disc Artwork
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .border(
                            width = 1.25.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.5f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.6f),
                                    Color.White.copy(alpha = 0.2f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val artwork = playbackState.artworkUrl
                    if (!artwork.isNullOrBlank()) {
                        DesktopNetworkImage(
                            url = artwork,
                            contentDescription = playbackState.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(currentRotation),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1728)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Center Vinyl Hole Spindle Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF120E19))
                            .border(1.dp, Color(0xFF8B5CF6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Track Title & Artist Metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = playbackState.title.ifBlank { "Ready to Play" },
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = playbackState.artist.ifBlank { "UTA-WAVE Desktop" },
                            color = Color(0xFFC084FC),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Mobile parity HQ Audio pill badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.22f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (playbackState.isLoading) "BUFFERING" else "HQ AUDIO",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (playbackState.isLoading) Color(0xFFFBBF24) else Color(0xFFC084FC)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 3. Audio Action Controls Group
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Previous Track Button
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play/Pause Action Button with Glass Accent & Spinner
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playbackState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else if (playbackState.isPlaying) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = "Pause",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Skip Next Button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Volume Popover Toggle (Desktop Ergonomics)
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = { showVolumeSlider = !showVolumeSlider },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (playbackState.isMuted || playbackState.volume == 0f) {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = "Volume",
                                tint = if (showVolumeSlider) Color(0xFFC084FC) else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Inline Mini Volume Slider Popover
                        if (showVolumeSlider) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(bottom = 44.dp)
                                    .width(120.dp)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(Color(0xFF13111C).copy(alpha = 0.95f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(17.dp))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = playbackState.volume,
                                    onValueChange = onVolumeChange,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF8B5CF6),
                                        activeTrackColor = Color(0xFF8B5CF6),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }

                    // Playback Queue Button
                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Playback Queue",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Expand to Fullscreen Player Sheet Button
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Expand Fullscreen Player",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 4. Interactive Scrubber Timeline along bottom edge of capsule (Click & Drag to seek anywhere!)
            var isTimelineHovered by remember { mutableStateOf(false) }
            var isScrubbingMini by remember { mutableStateOf(false) }
            var scrubFraction by remember { mutableStateOf(0f) }

            val barHeight by animateDpAsState(
                targetValue = if (isTimelineHovered || isScrubbingMini) 6.dp else 2.5.dp,
                animationSpec = tween(150),
                label = "barHeight"
            )

            val effectiveProgress = (if (isScrubbingMini) scrubFraction else progress).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight + 6.dp) // Generous hit-test padding
                    .pointerHoverIcon(PointerIcon.Hand)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val isOver = event.changes.any { it.pressed } || event.type == androidx.compose.ui.input.pointer.PointerEventType.Move
                                isTimelineHovered = event.type != androidx.compose.ui.input.pointer.PointerEventType.Exit
                            }
                        }
                    }
                    .pointerInput(playbackState.durationMs) {
                        detectTapGestures { offset ->
                            if (size.width > 0 && playbackState.durationMs > 0) {
                                val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val targetMs = (frac * playbackState.durationMs.toFloat()).toLong()
                                onSeekTo(targetMs)
                            }
                        }
                    }
                    .pointerInput(playbackState.durationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isScrubbingMini = true
                                if (size.width > 0) {
                                    scrubFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                }
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                if (size.width > 0) {
                                    scrubFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                }
                            },
                            onDragEnd = {
                                if (playbackState.durationMs > 0) {
                                    val targetMs = (scrubFraction * playbackState.durationMs.toFloat()).toLong()
                                    onSeekTo(targetMs)
                                }
                                isScrubbingMini = false
                            },
                            onDragCancel = {
                                isScrubbingMini = false
                            }
                        )
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .background(Color.White.copy(alpha = if (isTimelineHovered || isScrubbingMini) 0.16f else 0.08f))
                ) {
                    // Active Progress Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = effectiveProgress)
                            .height(barHeight)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF8B5CF6), // Electric Violet
                                        Color(0xFFEC4899), // Blush Pink
                                        Color(0xFF38BDF8)  // Sky Cyan
                                    )
                                )
                            )
                    )
                }

                // Glowing Scrubber Thumb (Visible when hovered or dragging)
                if (isTimelineHovered || isScrubbingMini) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = 520.dp * effectiveProgress - 5.dp) // Approximate capsule width
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.5.dp, Color(0xFFC084FC), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
