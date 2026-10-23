package com.uta.wave.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground

@Composable
fun AmbientCanvas(
    artworkUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(ElectricViolet) }
    var secondaryColor by remember { mutableStateOf(Color(0xFF4C1D95)) }

    LaunchedEffect(artworkUrl) {
        if (!artworkUrl.isNullOrBlank()) {
            val loader = ImageLoader(context)
            val req = ImageRequest.Builder(context)
                .data(artworkUrl)
                .allowHardware(false)
                .size(64, 64)
                .build()

            val result = (loader.execute(req) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette ->
                    palette?.dominantSwatch?.rgb?.let {
                        dominantColor = Color(it)
                    }
                    palette?.vibrantSwatch?.rgb?.let {
                        secondaryColor = Color(it)
                    } ?: palette?.mutedSwatch?.rgb?.let {
                        secondaryColor = Color(it)
                    }
                }
            }
        }
    }

    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "dominantColor"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "secondaryColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Solid obsidian base
        drawRect(ObsidianBackground)

        val currentScale = if (isPlaying) pulseScale else 1.0f

        // Top primary ambient radial glow
        val topCenter = Offset(width * 0.5f, height * 0.35f)
        val topRadius = (width * 0.85f) * currentScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedDominant.copy(alpha = 0.40f),
                    animatedDominant.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = topCenter,
                radius = topRadius
            ),
            radius = topRadius,
            center = topCenter
        )

        // Bottom secondary ambient radial glow
        val bottomCenter = Offset(width * 0.5f, height * 0.70f)
        val bottomRadius = (width * 0.75f) * currentScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedSecondary.copy(alpha = 0.30f),
                    animatedSecondary.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = bottomCenter,
                radius = bottomRadius
            ),
            radius = bottomRadius,
            center = bottomCenter
        )
    }
}
