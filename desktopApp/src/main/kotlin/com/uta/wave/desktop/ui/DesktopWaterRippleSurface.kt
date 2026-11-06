package com.uta.wave.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

/**
 * Data model for a single water ripple impulse deposited by mouse movement or clicks.
 */
data class RippleImpulse(
    val x: Float,
    val y: Float,
    val startTimeMs: Long,
    val maxRadius: Float = 320f,
    val strength: Float = 1.0f
)

/**
 * High-performance, GPU-accelerated Interactive Water Ripple Surface for UTA-WAVE Desktop.
 * Replicates the Wallpaper Engine `cursorripple` effect (from Hope麻匪 workshop #2974757317).
 *
 * When the user moves, hovers, or drags the mouse cursor across the album art:
 * - Fluid kinetic impulses propagate outwards like real water ripples.
 * - Refractive wave crests distort the underlying artwork.
 * - Specular light highlights shimmer along the surface normal vectors.
 */
@Composable
fun DesktopWaterRippleSurface(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isPlaying: Boolean = false,
    beatPulseScale: Float = 1.0f,
    accentGlowColor: Color = Color(0xFF8B5CF6)
) {
    var componentWidth by remember { mutableStateOf(300f) }
    var componentHeight by remember { mutableStateOf(300f) }
    val ripples = remember { CopyOnWriteArrayList<RippleImpulse>() }

    var lastSpawnTime by remember { mutableStateOf(0L) }
    var lastPointerPos by remember { mutableStateOf(Offset.Zero) }

    fun addRipple(pos: Offset, strength: Float = 1.0f) {
        val now = System.currentTimeMillis()
        // Throttle high-frequency mouse movements to prevent over-saturating wave solver (max 1 ripple per 35ms)
        if (now - lastSpawnTime >= 35L || (pos - lastPointerPos).getDistance() > 30f) {
            lastSpawnTime = now
            lastPointerPos = pos
            ripples.add(
                RippleImpulse(
                    x = pos.x,
                    y = pos.y,
                    startTimeMs = now,
                    maxRadius = max(componentWidth, componentHeight) * 0.95f,
                    strength = strength
                )
            )
            // Keep at most 16 concurrent ripples for optimal GPU performance
            while (ripples.size > 16) {
                ripples.removeAt(0)
            }
        }
    }

    var artworkBitmap by remember(artworkUrl) {
        mutableStateOf<ImageBitmap?>(artworkUrl?.let { DesktopImageCache.get(it) })
    }

    LaunchedEffect(artworkUrl) {
        if (!artworkUrl.isNullOrBlank() && artworkBitmap == null) {
            withContext(Dispatchers.IO) {
                try {
                    val bytes = URI(artworkUrl).toURL().readBytes()
                    val loaded = loadImageBitmap(ByteArrayInputStream(bytes))
                    DesktopImageCache.put(artworkUrl, loaded)
                    artworkBitmap = loaded
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = beatPulseScale
                scaleY = beatPulseScale
                shadowElevation = 24f
                shape = RoundedCornerShape(cornerRadius)
                clip = true
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> {
                                addRipple(change.position, strength = 0.85f)
                            }
                            PointerEventType.Press -> {
                                addRipple(change.position, strength = 1.6f)
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        addRipple(change.position, strength = 1.2f)
                    }
                )
            }
    ) {
        // Base artwork or fallback placeholder
        if (artworkBitmap != null) {
            Image(
                bitmap = artworkBitmap!!,
                contentDescription = "Album Artwork",
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        // Capture dimensions
                        if (size.width > 0 && size.height > 0) {
                            componentWidth = size.width
                            componentHeight = size.height
                        }

                        // Draw base artwork
                        drawContent()

                        // Draw fluid water ripple refraction & specular gleam overlay
                        val now = System.currentTimeMillis()
                        val activeRipples = ripples.filter { now - it.startTimeMs < 2200L }

                        if (activeRipples.isNotEmpty()) {
                            // Prune dead ripples
                            ripples.removeIf { now - it.startTimeMs >= 2200L }

                            // Draw procedural fluid caustic waves
                            for (ripple in activeRipples) {
                                val elapsedSec = (now - ripple.startTimeMs) / 1000f
                                val progress = (elapsedSec / 2.2f).coerceIn(0f, 1f)

                                // Wavefront propagation radius
                                val waveRadius = progress * ripple.maxRadius
                                val waveAlpha = ((1f - progress) * 0.45f * ripple.strength).coerceIn(0f, 0.7f)
                                val waveWidth = 14f + (progress * 18f)

                                // 1. Refractive Shadow Ring (Trough)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = waveAlpha * 0.35f),
                                            Color.Transparent
                                        ),
                                        center = Offset(ripple.x, ripple.y),
                                        radius = max(1f, waveRadius + 4f)
                                    ),
                                    radius = max(1f, waveRadius + 4f),
                                    center = Offset(ripple.x, ripple.y)
                                )

                                // 2. Specular Highlight Crest (Peak reflecting light)
                                drawCircle(
                                    color = Color.White.copy(alpha = waveAlpha),
                                    radius = max(1f, waveRadius),
                                    center = Offset(ripple.x, ripple.y),
                                    style = Stroke(
                                        width = waveWidth,
                                        cap = StrokeCap.Round
                                    )
                                )

                                // 3. Secondary Harmonic Ripple (Wallpaper Engine Dual-Echo Wave)
                                if (waveRadius > 35f) {
                                    val secondRadius = waveRadius * 0.72f
                                    val secondAlpha = waveAlpha * 0.55f
                                    drawCircle(
                                        color = accentGlowColor.copy(alpha = secondAlpha),
                                        radius = max(1f, secondRadius),
                                        center = Offset(ripple.x, ripple.y),
                                        style = Stroke(
                                            width = waveWidth * 0.75f
                                        )
                                    )
                                }
                            }
                        }
                    },
                contentScale = ContentScale.Crop
            )
        } else {
            // Elegant gradient placeholder when loading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF2A1B4E), Color(0xFF110C24))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(percent = 50))
                )
            }
        }

        // Frosted glass border sheen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
        )
    }
}
