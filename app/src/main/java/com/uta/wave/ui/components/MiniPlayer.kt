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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.player.WavePlayerState
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.SoftViolet
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

@Composable
fun MiniPlayer(
    playerState: WavePlayerState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return

    // Infinite rotation for the vinyl disk when playing
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiskRotation"
    )

    var totalDrag by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp), spotColor = ElectricViolet.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C2233),
                            Color(0xFF1E1724)
                        )
                    )
                )
                .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
                .clickable(onClick = onExpand)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            if (totalDrag < -60f) {
                                onNext()
                            } else if (totalDrag > 60f) {
                                onPrevious()
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        }
                    )
                }
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spinning Circular Vinyl Artwork Disc
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .border(1.5.dp, ElectricViolet.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .rotate(if (playerState.isPlaying) rotation else 0f)
                        )
                        // Tiny center vinyl hole dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B1522))
                                .border(1.dp, ElectricViolet, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track Title & Artist
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = SoftViolet,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Play/Pause Action Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricViolet.copy(alpha = 0.2f))
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            playerState.isResolving || playerState.isBuffering -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ElectricViolet,
                                    strokeWidth = 2.dp
                                )
                            }
                            playerState.isPlaying -> {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Skip Next Button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Thin progress track at bottom edge of pill
                val progress = if (playerState.durationMs > 0) {
                    (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(ElectricViolet, Color(0xFFFF75B5))
                                )
                            )
                    )
                }
            }
        }
    }
}
