package com.uta.wave.ui.splash

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.ui.theme.OutfitFontFamily
import kotlinx.coroutines.delay

/**
 * Cinematic opening splash screen for UTA-WAVE.
 * Orchestrates:
 * 1. The gravitational drop of the porcelain-white "U" (0.0s -> 0.52s)
 * 2. Water surface impact, volume squash & stretch, and expanding concentric ripples (0.52s -> 1.4s)
 * 3. Fluid dissipation and smooth crossfade transition directly into HomeScreen (1.4s -> 2.0s)
 */
@Composable
fun SplashWaterSurface(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val physicsEngine = remember { WaterPhysicsEngine() }
    var centerOffset by remember { mutableStateOf(Offset.Zero) }

    // Physics Animation Drivers
    val dropProgress = remember { Animatable(0f) }
    val squashX = remember { Animatable(1f) }
    val squashY = remember { Animatable(1f) }
    val uGlowAlpha = remember { Animatable(0f) }
    val surfaceAlpha = remember { Animatable(1f) }
    var currentClockMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val startNs = System.nanoTime()

        // 1. Gravitational acceleration: Drop U from top into water surface (0.0s -> 0.52s)
        dropProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 520, easing = FastOutLinearInEasing)
        )

        // 2. Impact point at 520ms: Trigger concentric liquid ripples
        val impactTimeMs = (System.nanoTime() - startNs) / 1_000_000L
        physicsEngine.triggerImpact(impactTimeMs)

        // 3. Elastic Squash & Stretch (Incompressible volume preservation on liquid impact)
        // Impact compression
        squashY.animateTo(0.72f, tween(75, easing = LinearOutSlowInEasing))
        squashX.animateTo(1.24f, tween(75, easing = LinearOutSlowInEasing))
        uGlowAlpha.animateTo(0.6f, tween(100))

        // Elastic rebound spring
        squashY.animateTo(1.08f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMedium))
        squashX.animateTo(0.95f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMedium))
        squashY.animateTo(1.0f, tween(120))
        squashX.animateTo(1.0f, tween(120))
        uGlowAlpha.animateTo(0.15f, tween(300))

        // 4. Wave resonance expands while U floats peacefully in water (0.6s -> 1.4s)
        delay(700)

        // 5. Smooth Dissolve / Transition into HomeScreen (1.4s -> 1.95s)
        surfaceAlpha.animateTo(0f, tween(durationMillis = 520, easing = FastOutSlowInEasing))

        onAnimationComplete()
    }

    // High-precision frame ticker for fluid wave physics (runs at 60/120Hz)
    LaunchedEffect(Unit) {
        val startNs = System.nanoTime()
        while (surfaceAlpha.value > 0f) {
            withFrameNanos { frameNs ->
                currentClockMs = (frameNs - startNs) / 1_000_000L
            }
        }
    }

    val activeRings = remember(currentClockMs) {
        physicsEngine.getActiveRings(currentClockMs)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(surfaceAlpha.value)
            .onGloballyPositioned { coordinates ->
                centerOffset = Offset(
                    coordinates.size.width / 2f,
                    coordinates.size.height / 2f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // LAYER 1: Concentric Circular Ripple Waves Canvas
        WaterRippleCanvasRenderer(
            rings = activeRings,
            centerOffset = centerOffset
        )

        // LAYER 2: Epicenter Water Caustic Pool Glow
        if (uGlowAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .blur(28.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6DE8F2).copy(alpha = uGlowAlpha.value * 0.40f),
                                Color(0xFFFFB060).copy(alpha = uGlowAlpha.value * 0.20f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // LAYER 3: Dropping & Floating Porcelain White "U"
        val startYOffset = with(density) { -260.dp.toPx() }
        val currentYOffset = startYOffset * (1f - dropProgress.value)

        Box(
            modifier = Modifier
                .offset(y = with(density) { currentYOffset.toDp() })
                .scale(scaleX = squashX.value, scaleY = squashY.value),
            contentAlignment = Alignment.Center
        ) {
            // Subtle 3D Depth Shadow on water
            Text(
                text = "U",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 94.sp,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .offset(y = 4.dp)
                    .blur(6.dp)
            )

            // Pure Porcelain White Letter U (Matching Apple-Style Core Inlay)
            Text(
                text = "U",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 94.sp,
                color = Color.White
            )
        }
    }
}
