package com.uta.wave.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Hardware-accelerated Canvas renderer that paints expanding concentric liquid water ripples
 * with Blinn-Phong specular wave crest highlights, optical chromatic dispersion, and fluid depth shadows.
 */
@Composable
fun WaterRippleCanvasRenderer(
    rings: List<RippleRingSpec>,
    centerOffset: Offset,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        for (ring in rings) {
            // 1. Subtle Outer Amber/Gold Prism Dispersion (Refracted warm light on outer edge)
            drawCircle(
                color = Color(0xFFFF9E44).copy(alpha = (ring.alpha * 0.12f).coerceIn(0f, 1f)),
                radius = ring.radius + ring.chromaticShift,
                center = centerOffset,
                style = Stroke(width = ring.strokeWidth * 1.25f)
            )

            // 2. Subtle Inner Cyan/Blue Prism Dispersion (Refracted cool light on inner edge)
            val innerCyanRadius = (ring.radius - ring.chromaticShift).coerceAtLeast(0f)
            drawCircle(
                color = Color(0xFF4EE2EC).copy(alpha = (ring.alpha * 0.15f).coerceIn(0f, 1f)),
                radius = innerCyanRadius,
                center = centerOffset,
                style = Stroke(width = ring.strokeWidth * 1.20f)
            )

            // 3. Fluid Wave Trough / Meniscus Ambient Shadow (Creates tactile 3D liquid depth)
            val troughRadius = (ring.radius - ring.strokeWidth * 0.65f).coerceAtLeast(0f)
            drawCircle(
                color = Color(0xFF04060A).copy(alpha = (ring.alpha * 0.45f).coerceIn(0f, 1f)),
                radius = troughRadius,
                center = centerOffset,
                style = Stroke(width = ring.strokeWidth * 0.9f)
            )

            // 4. Main Liquid Wave Crest (Specular white water highlight)
            val crestGradient = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (ring.alpha * 0.75f).coerceIn(0f, 1f)),
                    Color(0xFFE2F3FF).copy(alpha = (ring.alpha * 0.35f).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = centerOffset,
                radius = (ring.radius + ring.strokeWidth).coerceAtLeast(1f)
            )

            drawCircle(
                brush = crestGradient,
                radius = ring.radius,
                center = centerOffset,
                style = Stroke(width = ring.strokeWidth)
            )
        }
    }
}
