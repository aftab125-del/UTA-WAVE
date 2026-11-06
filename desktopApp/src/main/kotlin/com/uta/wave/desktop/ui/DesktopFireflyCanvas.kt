package com.uta.wave.desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.util.Random
import kotlin.math.sin

private data class FireflyParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val baseAlpha: Float,
    val pulseSpeed: Float,
    val phase: Float,
    val color: Color
)

/**
 * Ambient Drifting Fireflies & Glowing Embers for UTA-WAVE Ambient Mode.
 * Replicates the `Fireflies` particle system from Hope麻匪 Wallpaper Engine workshop.
 */
@Composable
fun DesktopFireflyCanvas(
    modifier: Modifier = Modifier,
    particleCount: Int = 48,
    accentColor: Color = Color(0xFF8B5CF6),
    secondaryColor: Color = Color(0xFF38BDF8)
) {
    val particles = remember(particleCount, accentColor, secondaryColor) {
        val rng = Random(42)
        val list = mutableListOf<FireflyParticle>()
        for (i in 0 until particleCount) {
            val color = if (i % 3 == 0) {
                Color.White
            } else if (i % 2 == 0) {
                accentColor
            } else {
                secondaryColor
            }
            list.add(
                FireflyParticle(
                    x = rng.nextFloat(),
                    y = rng.nextFloat(),
                    vx = (rng.nextFloat() - 0.5f) * 0.00045f,
                    vy = -0.0003f - (rng.nextFloat() * 0.00045f), // Gentle upward drift
                    radius = 2.5f + (rng.nextFloat() * 4.5f),
                    baseAlpha = 0.25f + (rng.nextFloat() * 0.55f),
                    pulseSpeed = 1.2f + (rng.nextFloat() * 2.5f),
                    phase = rng.nextFloat() * 6.28f,
                    color = color
                )
            )
        }
        list
    }

    // Animation ticker driving continuous particle movement
    val infiniteTransition = rememberInfiniteTransition(label = "fireflyTick")
    val timeSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            // Update position with soft horizontal swaying
            val sway = sin(timeSec * 2f + p.phase) * 0.00035f
            p.x = (p.x + p.vx + sway + 1.0f) % 1.0f
            p.y = (p.y + p.vy + 1.0f) % 1.0f

            val screenX = p.x * w
            val screenY = p.y * h

            // Twinkle / Breathing Alpha
            val pulse = (sin(timeSec * p.pulseSpeed + p.phase) * 0.5f + 0.5f)
            val currentAlpha = (p.baseAlpha * (0.4f + pulse * 0.6f)).coerceIn(0f, 1f)

            // Outer Soft Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        p.color.copy(alpha = currentAlpha * 0.7f),
                        p.color.copy(alpha = currentAlpha * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(screenX, screenY),
                    radius = p.radius * 3.5f
                ),
                radius = p.radius * 3.5f,
                center = Offset(screenX, screenY)
            )

            // Inner Core Spark
            drawCircle(
                color = Color.White.copy(alpha = currentAlpha),
                radius = p.radius * 0.85f,
                center = Offset(screenX, screenY)
            )
        }
    }
}
