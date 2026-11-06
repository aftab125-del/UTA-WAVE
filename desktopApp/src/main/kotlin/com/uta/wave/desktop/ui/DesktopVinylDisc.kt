package com.uta.wave.desktop.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Photorealistic Rotating Vinyl Record Disc for UTA-WAVE Ambient Mode.
 * Replicates the `Logo Rotation` & `头像` vinyl disc from Hope麻匪 Wallpaper Engine workshop.
 */
@Composable
fun DesktopVinylDisc(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    isPlaying: Boolean = false,
    rotationSpeedRpm: Float = 33.3f
) {
    // Smooth continuous vinyl rotation
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (60000f / rotationSpeedRpm).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "discAngle"
    )

    // When paused, maintain the last stopped position smoothly
    val activeAngle = if (isPlaying) rotationAngle else 0f

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 28.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .rotate(activeAngle),
        contentAlignment = Alignment.Center
    ) {
        // 1. Vinyl Body with Micro-Grooves and Specular Anisotropic Sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f

            // Dark Vinyl Resin base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E1E24),
                        Color(0xFF111116),
                        Color(0xFF08080C)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Concentric Micro-Grooves
            val numGrooves = 18
            val grooveStart = outerRadius * 0.44f
            val grooveEnd = outerRadius * 0.96f
            val step = (grooveEnd - grooveStart) / numGrooves

            for (i in 0..numGrooves) {
                val r = grooveStart + (i * step)
                val alpha = if (i % 3 == 0) 0.12f else 0.05f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = r,
                    center = center,
                    style = Stroke(width = if (i % 3 == 0) 1.2f else 0.6f)
                )
            }

            // Anisotropic Vinyl Light Sheen (Two opposing cones of light reflecting off grooves)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center
                ),
                radius = outerRadius * 0.96f,
                center = center
            )

            // Outer Vinyl Rim
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = outerRadius - 1f,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }

        // 2. Circular Center Vinyl Label (Miniaturized Album Art)
        val labelSize = size * 0.38f
        Box(
            modifier = Modifier
                .size(labelSize)
                .clip(CircleShape)
                .background(Color(0xFF181822))
                .border(2.dp, Color(0xFF333344), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUrl.isNullOrBlank()) {
                DesktopNetworkImage(
                    url = artworkUrl,
                    contentDescription = "Vinyl Label",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
                        )
                )
            }

            // Center Spindle Hole (Gold-brass plated brass grommet)
            Box(
                modifier = Modifier
                    .size(labelSize * 0.22f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF090A0F),
                                Color(0xFF221F18),
                                Color(0xFFB4975A)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFFD4AF37), CircleShape)
            )
        }
    }
}
