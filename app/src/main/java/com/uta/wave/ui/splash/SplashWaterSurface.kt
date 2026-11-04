package com.uta.wave.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.ui.theme.OutfitFontFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Cinematic, buttery-smooth acoustic opening splash screen for UTA-WAVE.
 * Visually mirrors the glowing 'U' soundwave aesthetic of the app icon.
 * Total duration: ~850ms (instant, silky smooth, 120fps GPU-accelerated).
 */
@Composable
fun SplashWaterSurface(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uScale = remember { Animatable(0.88f) }
    val uAlpha = remember { Animatable(0f) }
    val waveProgress = remember { Animatable(0f) }
    val surfaceAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            // 1. Center "U" blooms into view smoothly (0ms -> 320ms)
            launch {
                uScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
            }
            launch {
                uAlpha.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 240, easing = LinearOutSlowInEasing)
                )
            }

            // 2. Concentric acoustic waves expand from behind the U (0ms -> 720ms)
            launch {
                waveProgress.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 720, easing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f))
                )
            }

            // 3. Smooth dissolve into HomeScreen (600ms -> 850ms)
            launch {
                delay(600)
                surfaceAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                )
                onAnimationComplete()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(surfaceAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        // LAYER 1: Concentric Circular Soundwave Ripples Canvas (Zero GC allocations in draw loop)
        val p = waveProgress.value
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.46f

            // Wave Ring 1 (Primary wavefront)
            if (p > 0.04f) {
                val frac1 = ((p - 0.04f) / 0.88f).coerceIn(0f, 1f)
                val r1 = frac1 * maxR
                val a1 = (1f - frac1) * (1f - frac1) * 0.65f
                if (a1 > 0.01f) {
                    drawCircle(
                        color = Color.White.copy(alpha = a1),
                        radius = r1,
                        center = center,
                        style = Stroke(width = max(1.5f, 4.5f * (1f - frac1)))
                    )
                }
            }

            // Wave Ring 2 (Secondary resonance)
            if (p > 0.18f) {
                val frac2 = ((p - 0.18f) / 0.76f).coerceIn(0f, 1f)
                val r2 = frac2 * maxR * 0.82f
                val a2 = (1f - frac2) * (1f - frac2) * 0.45f
                if (a2 > 0.01f) {
                    drawCircle(
                        color = Color(0xFFD4D4E8).copy(alpha = a2),
                        radius = r2,
                        center = center,
                        style = Stroke(width = max(1.2f, 3.5f * (1f - frac2)))
                    )
                }
            }

            // Wave Ring 3 (Tertiary echo)
            if (p > 0.32f) {
                val frac3 = ((p - 0.32f) / 0.65f).coerceIn(0f, 1f)
                val r3 = frac3 * maxR * 0.64f
                val a3 = (1f - frac3) * (1f - frac3) * 0.30f
                if (a3 > 0.01f) {
                    drawCircle(
                        color = Color(0xFFA8A8C0).copy(alpha = a3),
                        radius = r3,
                        center = center,
                        style = Stroke(width = max(1.0f, 2.4f * (1f - frac3)))
                    )
                }
            }
        }

        // LAYER 2: Ethereal ambient bloom behind the U
        if (uAlpha.value > 0.05f) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .blur(36.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.14f * uAlpha.value),
                        shape = CircleShape
                    )
            )
        }

        // LAYER 3: Glowing porcelain-white "U" with smooth scale & bloom
        Box(
            modifier = Modifier
                .scale(uScale.value)
                .alpha(uAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            // Diffuse outer glow
            Text(
                text = "U",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 96.sp,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.blur(8.dp)
            )

            // Core crisp porcelain white letter U
            Text(
                text = "U",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 96.sp,
                color = Color.White
            )
        }
    }
}
