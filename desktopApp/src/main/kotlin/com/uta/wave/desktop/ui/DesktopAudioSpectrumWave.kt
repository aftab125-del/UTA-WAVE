package com.uta.wave.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * Dual-Tone Smooth Audio Spectrum Bars for UTA-WAVE Ambient Mode.
 * Replicates the `Simple_Audio_Bars` & `音频1/2` effects from Hope麻匪 Wallpaper Engine workshop.
 */
@Composable
fun DesktopAudioSpectrumWave(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 36,
    height: Dp = 48.dp,
    primaryColor: Color = Color(0xFF38BDF8), // Cyan neon
    secondaryColor: Color = Color(0xFFF43F5E), // Pink / Rose neon
    playbackProgressSeconds: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWaveTick")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseTick"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val totalSpacing = w * 0.15f
        val barWidth = (w - totalSpacing) / barCount
        val barGap = totalSpacing / (barCount - 1).coerceAtLeast(1)

        val energy = if (isPlaying) 1.0f else 0.18f

        for (i in 0 until barCount) {
            val normalizedIdx = i.toFloat() / barCount
            val x = i * (barWidth + barGap)

            // Multi-harmonic audio frequency simulation
            // Emulates low-end bass in the center/left and high treble on the edges
            val harmonic1 = sin(phase * 2.2f + normalizedIdx * 4.5f)
            val harmonic2 = sin(phase * 1.4f - normalizedIdx * 7.2f)
            val harmonic3 = sin(playbackProgressSeconds * 3.0f + i * 0.8f)

            val rawAmplitude = abs((harmonic1 * 0.45f) + (harmonic2 * 0.35f) + (harmonic3 * 0.20f))
            // Bell-curve envelope to create symmetrical mountain visualizer wave
            val bellEnvelope = sin(normalizedIdx * Math.PI.toFloat()).coerceIn(0.25f, 1.0f)
            val barHeight = ((rawAmplitude * bellEnvelope * h * energy) + (h * 0.08f)).coerceIn(4f, h)

            val y = h - barHeight

            // Dual-tone vertical gradient for each bar
            val barGradient = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.95f),
                    secondaryColor.copy(alpha = 0.75f)
                ),
                startY = y,
                endY = h
            )

            // Draw glowing rounded bar
            drawRoundRect(
                brush = barGradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // Subtle neon cap highlight
            drawCircle(
                color = Color.White.copy(alpha = if (isPlaying) 0.85f else 0.3f),
                radius = barWidth * 0.45f,
                center = Offset(x + barWidth / 2f, y + barWidth / 2f)
            )
        }
    }
}
