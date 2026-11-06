package com.uta.wave.desktop.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import kotlinx.coroutines.delay

/**
 * Master Ambient Canvas Mode for UTA-WAVE Desktop.
 * Replicates the full visual aesthetics of Hope麻匪's Wallpaper Engine workshop (#2974757317):
 * - Interactive Liquid Water Ripple on the centered album artwork.
 * - Rotating vinyl record sliding out during playback.
 * - Dual-tone neon spectrum visualizer wave.
 * - Ambient drifting firefly particles.
 * - Floating Frosted Glass card with dynamic palette harmonization.
 * - Auto-hiding HUD playback controls for an immersive screen saver experience.
 */
@Composable
fun DesktopAmbientModeOverlay(
    playbackState: UnifiedPlaybackState,
    currentTrack: TrackItem?,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    isShuffle: Boolean,
    repeatMode: DesktopRepeatMode,
    onExitAmbientMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Extract dynamic color palette from track artwork
    val palette = rememberAlbumArtPalette(currentTrack?.artworkUrl)

    // 2. Audio-reactive beat bounce scale (oscillates smoothly on rhythm when playing)
    val infiniteTransition = rememberInfiniteTransition(label = "ambientBeatPulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "beatPhase"
    )
    val beatScale = if (playbackState.isPlaying) {
        1.0f + (kotlin.math.sin(pulsePhase * 2f).coerceAtLeast(0f) * 0.028f)
    } else {
        1.0f
    }

    // 3. Auto-hiding HUD controls timer (fades out after 3.5s of mouse inactivity)
    var isControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastInteractionTime) {
        delay(3500)
        isControlsVisible = false
    }

    fun reportUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        if (!isControlsVisible) {
            isControlsVisible = true
        }
    }

    val controlsAlpha by animateFloatAsState(
        targetValue = if (isControlsVisible) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "hudFade"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080D))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move || event.type == PointerEventType.Press) {
                            reportUserInteraction()
                        }
                    }
                }
            }
    ) {
        // =================================================================
        // 1. BACKGROUND ATMOSPHERE: Dynamic Deep Gradient & Diffused Palette
        // =================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            palette.dominantColor.copy(alpha = 0.35f),
                            palette.accentColor.copy(alpha = 0.15f),
                            Color(0xFF090A10),
                            Color(0xFF050508)
                        ),
                        radius = 1400f
                    )
                )
        )

        // =================================================================
        // 2. AMBIENT DRIFTING FIREFLIES & GLOWING EMBERS
        // =================================================================
        DesktopFireflyCanvas(
            modifier = Modifier.fillMaxSize(),
            particleCount = 52,
            accentColor = palette.accentColor,
            secondaryColor = palette.secondaryColor
        )

        // =================================================================
        // 3. TOP-RIGHT EXIT / WINDOW BUTTONS (HUD)
        // =================================================================
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .graphicsLayer { alpha = controlsAlpha },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onExitAmbientMode() }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit Ambient Mode",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Exit Ambient",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // =================================================================
        // 4. CENTER STAGE: FLOATING FROSTED GLASS CARD (Hope麻匪 悬浮窗)
        // =================================================================
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl Disc Sliding Out Behind the Floating Card
            val vinylOffset by animateDpAsState(
                targetValue = if (playbackState.isPlaying) 150.dp else 0.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                label = "vinylSlide"
            )

            Box(
                modifier = Modifier
                    .offset(x = vinylOffset)
            ) {
                DesktopVinylDisc(
                    artworkUrl = currentTrack?.artworkUrl,
                    size = 280.dp,
                    isPlaying = playbackState.isPlaying
                )
            }

            // Master Floating Card: Frosted Liquid Glass Body
            Surface(
                modifier = Modifier
                    .width(360.dp)
                    .shadow(
                        elevation = 40.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = palette.glowColor,
                        spotColor = palette.accentColor.copy(alpha = 0.45f)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF10121C).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            palette.accentColor.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered Album Art with Liquid Water Ripple Surface
                    DesktopWaterRippleSurface(
                        artworkUrl = currentTrack?.artworkUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.0f),
                        cornerRadius = 20.dp,
                        isPlaying = playbackState.isPlaying,
                        beatPulseScale = beatScale,
                        accentGlowColor = palette.accentColor
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Track Title
                    Text(
                        text = currentTrack?.title ?: "No track playing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Artist Name
                    Text(
                        text = currentTrack?.artist ?: "UTA·WAVE Universe",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.accentColor.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Dual-Tone Smooth Audio Visualizer Bars
                    DesktopAudioSpectrumWave(
                        isPlaying = playbackState.isPlaying,
                        height = 36.dp,
                        barCount = 32,
                        primaryColor = palette.accentColor,
                        secondaryColor = palette.secondaryColor,
                        playbackProgressSeconds = (playbackState.positionMs / 1000f)
                    )
                }
            }
        }

        // =================================================================
        // 5. BOTTOM FLOATING PLAYBACK CONTROLS (HUD)
        // =================================================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .graphicsLayer { alpha = controlsAlpha }
        ) {
            Surface(
                modifier = Modifier
                    .width(620.dp)
                    .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F111A).copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    // Progress Slider & Timestamps
                    val totalSec = ((currentTrack?.durationSeconds ?: 0)).coerceAtLeast(1)
                    val currentSec = (playbackState.positionMs / 1000f).coerceIn(0f, totalSec.toFloat())
                    val progressFraction = (currentSec / totalSec).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentSec.toInt()),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = progressFraction,
                            onValueChange = { frac ->
                                reportUserInteraction()
                                onSeek(frac * totalSec)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .height(16.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = palette.accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        Text(
                            text = formatTime(totalSec),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Transport Buttons (Shuffle, Prev, Play/Pause, Next, Repeat, Volume)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Shuffle Toggle
                        IconButton(
                            onClick = {
                                reportUserInteraction()
                                onToggleShuffle()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) palette.accentColor else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Prev Track
                        IconButton(
                            onClick = {
                                reportUserInteraction()
                                onSkipPrevious()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Main Play / Pause Button
                        Surface(
                            shape = CircleShape,
                            color = palette.accentColor,
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(12.dp, CircleShape, spotColor = palette.accentColor)
                                .clickable {
                                    reportUserInteraction()
                                    onPlayPause()
                                }
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // Next Track
                        IconButton(
                            onClick = {
                                reportUserInteraction()
                                onSkipNext()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Repeat Toggle
                        IconButton(
                            onClick = {
                                reportUserInteraction()
                                onToggleRepeat()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    DesktopRepeatMode.ONE -> Icons.Filled.RepeatOne
                                    else -> Icons.Filled.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (repeatMode != DesktopRepeatMode.OFF) palette.accentColor else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
