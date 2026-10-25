package com.uta.wave.ui.components

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.canvas.CanvasArtwork
import com.uta.wave.data.canvas.CanvasArtworkProvider
import kotlinx.coroutines.delay

@Composable
fun ArtworkThumbnailView(
    artworkUrl: String?,
    songId: String,
    title: String,
    artist: String,
    album: String? = null,
    isPlaying: Boolean,
    isRotatingEnabled: Boolean,
    isCanvasEnabled: Boolean,
    onSeekDelta: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    contentScale: ContentScale = ContentScale.Crop,
    onSeekBackward: (() -> Unit)? = null,
    onSeekForward: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    // Rotation Transition (20s turntable spin)
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpinTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRotatingEnabled && isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylSpinAngle"
    )

    // Canvas artwork video state (precedence: rotating thumbnail disables canvas)
    var canvasArtwork by remember(songId) { mutableStateOf<CanvasArtwork?>(null) }
    LaunchedEffect(songId, isCanvasEnabled, isRotatingEnabled) {
        if (isCanvasEnabled && !isRotatingEnabled) {
            canvasArtwork = CanvasArtworkProvider.getCanvas(
                context = context,
                mediaId = songId,
                title = title,
                artist = artist,
                album = album
            )
        } else {
            canvasArtwork = null
        }
    }

    // Double-tap seek feedback overlay
    var seekFeedbackDirection by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(seekFeedbackDirection) {
        if (seekFeedbackDirection != null) {
            delay(850)
            seekFeedbackDirection = null
        }
    }

    val artworkShape = if (isRotatingEnabled) CircleShape else RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val leftBound = size.width * 0.33f
                        val rightBound = size.width * 0.67f

                        val isLeft = (layoutDirection == LayoutDirection.Ltr && offset.x < leftBound) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x > rightBound)
                        val isRight = (layoutDirection == LayoutDirection.Ltr && offset.x > rightBound) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x < leftBound)

                        if (isLeft) {
                            if (onSeekBackward != null) {
                                onSeekBackward()
                            } else {
                                onSeekDelta(-10000L)
                                seekFeedbackDirection = "-10s"
                            }
                        } else if (isRight) {
                            if (onSeekForward != null) {
                                onSeekForward()
                            } else {
                                onSeekDelta(+10000L)
                                seekFeedbackDirection = "+10s"
                            }
                        } else {
                            onTogglePlayPause()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Rotating disk container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isRotatingEnabled) {
                        rotationZ = rotation
                    }
                }
                .clip(artworkShape)
                .background(Color(0xFF161219))
                .drawWithContent {
                    drawContent()
                    if (isRotatingEnabled) {
                        // Draw concentric vinyl record grooves
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = size.width / 2f

                        val grooveSteps = listOf(0.92f, 0.84f, 0.76f, 0.68f, 0.60f, 0.52f, 0.44f, 0.36f)
                        for (fraction in grooveSteps) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = maxRadius * fraction,
                                center = center,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }
                    }
                }
        ) {
            // Layer 1: Static artwork image
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 2: Canvas looping video artwork (if active and not rotating thumbnail)
            if (isCanvasEnabled && !isRotatingEnabled && canvasArtwork?.preferredAnimationUrl != null) {
                CanvasArtworkPlayer(
                    primaryUrl = canvasArtwork?.preferredAnimationUrl,
                    fallbackUrl = canvasArtwork?.videoUrl,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Layer 3: Center Vinyl Spindle Hole (when rotating vinyl is active)
            if (isRotatingEnabled) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0xFF0F0B13))
                        .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.90f))
                    )
                }
            }
        }

        // Seek Feedback Overlay (-10s / +10s)
        AnimatedVisibility(
            visible = seekFeedbackDirection != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val direction = seekFeedbackDirection
            if (direction != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (direction.startsWith("-")) Icons.Default.FastRewind else Icons.Default.FastForward,
                            contentDescription = direction,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = direction,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
