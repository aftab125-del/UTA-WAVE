package com.uta.wave.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.uta.wave.ui.theme.OutfitFontFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Cinematic, buttery-smooth acoustic opening splash screen for UTA-WAVE.
 * Visually mirrors the glowing 'U' with concentric acoustic soundwaves from the brand icon.
 *
 * Total duration: ~1.65 seconds (luxurious, silky smooth, pure GPU Canvas radial rendering,
 * zero rectangular bounding-box blur artifacts).
 */
@Composable
fun SplashWaterSurface(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uScale = remember { Animatable(0.90f) }
    val uAlpha = remember { Animatable(0f) }
    val waveProgress = remember { Animatable(0f) }
    val surfaceAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            // 1. Center porcelain-white "U" gently materializes with smooth scale & bloom (0ms -> 450ms)
            launch {
                uScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 450, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))
                )
            }
            launch {
                uAlpha.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 380, easing = LinearOutSlowInEasing)
                )
            }

            // 2. Concentric acoustic wave rings smoothly radiate outward (200ms -> 1300ms)
            launch {
                delay(200)
                waveProgress.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 1100, easing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f))
                )
            }

            // 3. Luxurious crossfade dissolve into HomeScreen (1200ms -> 1650ms)
            launch {
                delay(1200)
                surfaceAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
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
        val p = waveProgress.value
        val ua = uAlpha.value

        // FULLSCREEN GPU CANVAS: Infinite radial gradient bloom & concentric acoustic wave rings.
        // Zero layout bounding-box clamping and zero GC allocations.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.48f

            // LAYER 1: Infinite radial ambient bloom behind the U (smoothly fades to transparent)
            if (ua > 0.01f) {
                val bloomR = size.minDimension * 0.44f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f * ua),
                            Color(0xFFE2E2F2).copy(alpha = 0.07f * ua),
                            Color(0xFFA8A8C0).copy(alpha = 0.02f * ua),
                            Color.Transparent
                        ),
                        center = center,
                        radius = bloomR
                    ),
                    radius = bloomR,
                    center = center
                )
            }

            // LAYER 2: Concentric Acoustic Soundwave Rings (Metallic Silber & White)
            // Ring 1 (Primary wave crest)
            if (p > 0.02f) {
                val frac1 = ((p - 0.02f) / 0.94f).coerceIn(0f, 1f)
                val r1 = frac1 * maxR
                val a1 = (1f - frac1) * (1f - frac1) * 0.72f
                if (a1 > 0.01f) {
                    drawCircle(
                        color = Color.White.copy(alpha = a1),
                        radius = r1,
                        center = center,
                        style = Stroke(width = max(1.8f, 5.0f * (1f - frac1)))
                    )
                }
            }

            // Ring 2 (Secondary metallic harmonic)
            if (p > 0.14f) {
                val frac2 = ((p - 0.14f) / 0.84f).coerceIn(0f, 1f)
                val r2 = frac2 * maxR * 0.85f
                val a2 = (1f - frac2) * (1f - frac2) * 0.52f
                if (a2 > 0.01f) {
                    drawCircle(
                        color = Color(0xFFE4E4F4).copy(alpha = a2),
                        radius = r2,
                        center = center,
                        style = Stroke(width = max(1.5f, 4.0f * (1f - frac2)))
                    )
                }
            }

            // Ring 3 (Tertiary acoustic resonance)
            if (p > 0.28f) {
                val frac3 = ((p - 0.28f) / 0.72f).coerceIn(0f, 1f)
                val r3 = frac3 * maxR * 0.68f
                val a3 = (1f - frac3) * (1f - frac3) * 0.36f
                if (a3 > 0.01f) {
                    drawCircle(
                        color = Color(0xFFC0C0D8).copy(alpha = a3),
                        radius = r3,
                        center = center,
                        style = Stroke(width = max(1.2f, 3.0f * (1f - frac3)))
                    )
                }
            }

            // Ring 4 (Deep ambient echo)
            if (p > 0.42f) {
                val frac4 = ((p - 0.42f) / 0.58f).coerceIn(0f, 1f)
                val r4 = frac4 * maxR * 0.52f
                val a4 = (1f - frac4) * (1f - frac4) * 0.22f
                if (a4 > 0.01f) {
                    drawCircle(
                        color = Color(0xFF9898B0).copy(alpha = a4),
                        radius = r4,
                        center = center,
                        style = Stroke(width = max(1.0f, 2.2f * (1f - frac4)))
                    )
                }
            }
        }

        // LAYER 3: Core crisp porcelain-white "U" (No blur bounding-box, perfectly sculpted)
        Box(
            modifier = Modifier
                .scale(uScale.value)
                .alpha(uAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "U",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 98.sp,
                color = Color.White
            )
        }
    }
}
