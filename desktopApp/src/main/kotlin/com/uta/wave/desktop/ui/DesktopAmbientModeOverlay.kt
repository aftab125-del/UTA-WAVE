package com.uta.wave.desktop.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import kotlinx.coroutines.delay

/**
 * Pixel-Perfect Ambient Mode for UTA-WAVE Desktop.
 * Exact replication of Hope麻匪's Wallpaper Engine workshop (#2974757317):
 * - Edge-to-edge album artwork on top with zero inner margins.
 * - Dynamic liquid water ripple caustics on mouse interaction (crystal-clear when idle).
 * - Living solid-white liquid audio wave silhouette directly bridging artwork to footer.
 * - Solid crisp white footer card with circular avatar badge, bold black title, and grey artist name.
 * - Falling digital Matrix code rain streaming in the background.
 * - Clean, uncluttered canvas with zero clunky bottom bars or protruding discs.
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
    // 1. Auto-hiding exit HUD timer (fades out smoothly after 3.5s of mouse inactivity)
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
        // 1. BACKGROUND ATMOSPHERE: Matrix Glyph Rain + Subtle Deep Space
        // =================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF141622),
                            Color(0xFF090A10),
                            Color(0xFF050508)
                        ),
                        radius = 1200f
                    )
                )
        )

        // Matrix Digital Code Rain streams (Replicates the `黑客` layer)
        DesktopMatrixRainCanvas(
            modifier = Modifier.fillMaxSize(),
            columnCount = 55
        )

        // Subtle drifting firefly particles
        DesktopFireflyCanvas(
            modifier = Modifier.fillMaxSize(),
            particleCount = 28,
            accentColor = Color(0xFF38BDF8),
            secondaryColor = Color(0xFFA78BFA)
        )

        // =================================================================
        // 2. TOP-RIGHT EXIT PILL (HUD with Auto-Hide)
        // =================================================================
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .graphicsLayer { alpha = controlsAlpha },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
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
                        modifier = Modifier.size(15.dp)
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
        // 3. CENTER STAGE: FLOATING CARD (Hope麻匪 悬浮窗 Exact Replica)
        // =================================================================
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(312.dp)
                    .shadow(
                        elevation = 36.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black,
                        spotColor = Color.Black.copy(alpha = 0.75f)
                    )
                    .clickable {
                        reportUserInteraction()
                        onPlayPause()
                    }
                    .pointerHoverIcon(PointerIcon.Hand),
                shape = RoundedCornerShape(22.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // -------------------------------------------------------------
                    // UPPER: Edge-to-Edge Artwork with Water Ripple Caustic Surface
                    // -------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.0f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 22.dp,
                                    topEnd = 22.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            )
                    ) {
                        DesktopWaterRippleSurface(
                            artworkUrl = currentTrack?.artworkUrl,
                            modifier = Modifier.fillMaxSize(),
                            topCornerRadius = 22.dp,
                            isPlaying = playbackState.isPlaying,
                            beatPulseScale = 1.0f,
                            accentGlowColor = Color.White
                        )

                        // ---------------------------------------------------------
                        // LIQUID AUDIO WAVE SILHOUETTE (Directly Overlapping Bottom)
                        // ---------------------------------------------------------
                        DesktopLiquidWaveform(
                            isPlaying = playbackState.isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            height = 38.dp,
                            waveColor = Color.White
                        )
                    }

                    // -------------------------------------------------------------
                    // LOWER: Solid White Metadata Footer with Circular Badge
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Avatar / Logo Badge (Hope麻匪 MF / UTA-WAVE Badge)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14161F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MF",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Track Metadata
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentTrack?.title ?: "Sunflower (Spider-Man: Into the Spider-Verse)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = currentTrack?.artist ?: "Post Malone, Swae Lee",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
