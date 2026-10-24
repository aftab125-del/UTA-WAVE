package com.uta.wave.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Android 13/14 / Echo Music signature squiggly wavy audio progress bar.
 */
@Composable
fun SquigglySlider(
    progress: Float, // 0f..1f
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFF3D7EC),
    inactiveColor: Color = Color.White.copy(alpha = 0.22f),
    thumbColor: Color = Color(0xFFF3D7EC)
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val effectiveProgress = (if (isDragging) dragProgress else progress).coerceIn(0f, 1f)

    // Continuous wave animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "squigglyWave")
    val phase by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavePhase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newP = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(newP)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        onSeek(dragProgress)
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val strokeWidthPx = 4.dp.toPx()
        val waveLengthPx = 28.dp.toPx()
        val amplitudePx = 3.8.dp.toPx()
        val thumbWidthPx = 4.5.dp.toPx()
        val thumbHeightPx = 18.dp.toPx()

        val thumbX = effectiveProgress * width

        // 1. Draw played squiggly wave (from 0 to thumbX)
        if (thumbX > 0f) {
            val wavePath = Path()
            wavePath.moveTo(0f, centerY)

            val stepPx = 2f
            var x = 0f
            while (x <= thumbX) {
                // Diminish wave amplitude near the start and near the thumb for a smooth taper
                val taper = if (thumbX > 30f) {
                    val distFromEnd = (thumbX - x).coerceAtMost(x)
                    (distFromEnd / 18f).coerceIn(0.2f, 1f)
                } else 0.5f

                val y = centerY + amplitudePx * taper * sin((x / waveLengthPx) * 2f * PI.toFloat() - phase)
                wavePath.lineTo(x, y)
                x += stepPx
            }
            // Ensure connection up to thumb position
            wavePath.lineTo(thumbX, centerY)

            drawPath(
                path = wavePath,
                color = activeColor,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // 2. Draw unplayed straight track (from thumbX to width)
        if (thumbX < width) {
            drawLine(
                color = inactiveColor,
                start = Offset(thumbX, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx * 0.9f,
                cap = StrokeCap.Round
            )
        }

        // 3. Draw vertical pill / capsule playhead thumb
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(thumbX - (thumbWidthPx / 2f), centerY - (thumbHeightPx / 2f)),
            size = androidx.compose.ui.geometry.Size(thumbWidthPx, thumbHeightPx),
            cornerRadius = CornerRadius(thumbWidthPx / 2f, thumbWidthPx / 2f)
        )
    }
}
