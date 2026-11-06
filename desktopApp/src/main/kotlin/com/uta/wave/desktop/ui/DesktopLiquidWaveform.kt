package com.uta.wave.desktop.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Solid White Liquid Audio Wave Silhouette for UTA-WAVE Exact Ambient Mode.
 * Replicates the `底部音频背景` & `音频1` effects from Hope麻匪 Wallpaper Engine workshop (#2974757317):
 * - A continuous, smooth flowing liquid white wave crest spanning the entire width of the card.
 * - Sits directly over the bottom of the album art, flowing seamlessly downward into the white footer card.
 */
@Composable
fun DesktopLiquidWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    waveColor: Color = Color.White,
    pointCount: Int = 32
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquidWaveMotion")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseLoop"
    )

    val breathingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathingLoop"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height

        val energy = if (isPlaying) 1.0f else 0.22f
        val points = mutableListOf<Pair<Float, Float>>()

        // Generate smooth wave crest control points
        for (i in 0..pointCount) {
            val progress = i.toFloat() / pointCount
            val x = progress * w

            // Multi-harmonic audio frequency simulation emulating real music response
            val h1 = sin(wavePhase * 2.1f + progress * 7.5f)
            val h2 = sin(wavePhase * 1.3f - progress * 11.2f)
            val h3 = sin(breathingPhase + progress * 4.2f)

            // Organic liquid wave elevation
            val rawAmp = (abs(h1 * 0.45f + h2 * 0.35f + h3 * 0.20f))
            // Symmetrical envelope: higher crests in center and dynamic movement across the card
            val envelope = sin(progress * PI.toFloat()).coerceIn(0.2f, 1.0f)
            val waveElevation = (rawAmp * envelope * h * 0.85f * energy) + (h * 0.12f)

            // Y coordinate: 0 is top, h is bottom. The wave crest rises toward the top of this canvas
            val y = (h - waveElevation).coerceIn(2f, h - 2f)
            points.add(x to y)
        }

        // Construct smooth cubic bezier spline through the points
        val wavePath = Path().apply {
            moveTo(0f, h) // Bottom left
            lineTo(points[0].first, points[0].second)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.first + p1.first) / 2f
                val midY = (p0.second + p1.second) / 2f
                quadraticTo(p0.first, p0.second, midX, midY)
            }

            val last = points.last()
            lineTo(last.first, last.second)
            lineTo(w, h) // Bottom right
            close()
        }

        // Draw solid liquid white fill
        drawPath(
            path = wavePath,
            color = waveColor
        )
    }
}
