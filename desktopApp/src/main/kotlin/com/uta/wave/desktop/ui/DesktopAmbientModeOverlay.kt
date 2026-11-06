package com.uta.wave.desktop.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import kotlinx.coroutines.delay

/**
 * Pixel-Perfect Ambient Mode for UTA-WAVE Desktop.
 * Exact 1:1 replication of Hope麻匪's Wallpaper Engine workshop (#2974757317):
 * - BACKGROUND: Fullscreen album artwork ($mediaThumbnail) covered by dark scrim + falling Matrix code rain.
 * - CENTER CARD: Edge-to-edge artwork + living solid-white liquid audio wave + solid white footer with circular badge.
 * - INTERACTION: Dynamic liquid water ripples on mouse movement; click card to play/pause.
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
    // Load track artwork bitmap for background & foreground
    val artworkUrl = currentTrack?.artworkUrl ?: playbackState.artworkUrl
    val palette = rememberAlbumArtPalette(artworkUrl)
    var artworkBitmap by remember(artworkUrl) {
        mutableStateOf<ImageBitmap?>(artworkUrl?.let { DesktopImageCache.get(it) })
    }

    LaunchedEffect(artworkUrl) {
        if (!artworkUrl.isNullOrBlank() && artworkBitmap == null) {
            val loaded = loadDesktopImageBitmapSafe(artworkUrl)
            if (loaded != null) {
                artworkBitmap = loaded
            }
        }
    }

    // Auto-hiding HUD controls timer (fades out after 3.5s of mouse inactivity)
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
        // 1. FULLSCREEN BACKGROUND: Unblurred Album Artwork (Hope麻匪 1比1 / $mediaThumbnail)
        // =================================================================
        if (artworkBitmap != null) {
            Image(
                bitmap = artworkBitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (!artworkUrl.isNullOrBlank()) {
            DesktopNetworkImage(
                url = artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF181528), Color(0xFF090A10), Color(0xFF040407))
                        )
                    )
            )
        }

        // =================================================================
        // 2. BLACK MASK / SCRIM (Hope麻匪 黑色遮罩)
        // Dims the fullscreen background so the center card stands out with high contrast
        // =================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
        )

        // =================================================================
        // 3. MATRIX CODE RAIN (Hope麻匪 黑客 layer)
        // Streaming falling digital glyphs (katakana, letters, numbers) across the background
        // =================================================================
        DesktopMatrixRainCanvas(
            modifier = Modifier.fillMaxSize(),
            columnCount = 28,
            secondaryColor = palette.secondaryColor
        )

        // =================================================================
        // 4. TOP-RIGHT EXIT PILL (HUD with Auto-Hide)
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
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(percent = 50), spotColor = Color.Black)
                    .clickable { onExitAmbientMode() }
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit Ambient Mode",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Exit Ambient",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // =================================================================
        // 5. MASTER FLOATING MEDIA CARD (Hope麻匪 浮窗背景)
        // Center-aligned, edge-to-edge artwork, flowing liquid audio silhouette,
        // and solid white footer card with MF circular avatar badge.
        // =================================================================
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(312.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 40.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black,
                        spotColor = Color.Black.copy(alpha = 0.85f)
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
                            artworkUrl = artworkUrl,
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
                        // Circular Avatar / Logo Badge (Hope麻匪 MF Badge)
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
                                text = currentTrack?.title ?: if (playbackState.title.isNotBlank()) playbackState.title else "No track playing",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = currentTrack?.artist ?: if (playbackState.artist.isNotBlank()) playbackState.artist else "UTA·WAVE Universe",
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
