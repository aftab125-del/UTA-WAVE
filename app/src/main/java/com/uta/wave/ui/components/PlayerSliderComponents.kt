package com.uta.wave.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Universal Player Seeker that dynamically renders the active slider style:
 * - BAR (Standard Material 3)
 * - WAVY (Expressive sinusoidal wave with circular thumb)
 * - SLIM (Sleek line with spring bouncy expansion)
 * - SQUIGGLY (Android 13/14 Tiramisu cubic Bézier waveform)
 */
@Composable
fun PlayerSeeker(
    sliderStyle: String,
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFF3D7EC),
    inactiveColor: Color = Color.White.copy(alpha = 0.22f),
    thumbColor: Color = Color(0xFFF3D7EC),
    enableHaptics: Boolean = true
) {
    when (sliderStyle.uppercase()) {
        "BAR" -> {
            BarSlider(
                progress = progress,
                onSeek = onSeek,
                modifier = modifier,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                thumbColor = thumbColor,
                enableHaptics = enableHaptics
            )
        }
        "WAVY" -> {
            WavySlider(
                progress = progress,
                isPlaying = isPlaying,
                onSeek = onSeek,
                modifier = modifier,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                thumbColor = thumbColor,
                enableHaptics = enableHaptics
            )
        }
        "SLIM" -> {
            SlimSlider(
                progress = progress,
                onSeek = onSeek,
                modifier = modifier,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                enableHaptics = enableHaptics
            )
        }
        else -> {
            // "SQUIGGLY" Default
            SquigglySlider(
                progress = progress,
                isPlaying = isPlaying,
                onSeek = onSeek,
                modifier = modifier,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                thumbColor = thumbColor
            )
        }
    }
}

/**
 * 1. Bar Slider: Standard Material 3 Slider with predictable round thumb.
 */
@Composable
fun BarSlider(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFF3D7EC),
    inactiveColor: Color = Color.White.copy(alpha = 0.22f),
    thumbColor: Color = Color(0xFFF3D7EC),
    enableHaptics: Boolean = true,
    enabled: Boolean = true
) {
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val effectiveProgress = (if (isDragging) dragProgress else progress).coerceIn(0f, 1f)

    Slider(
        value = effectiveProgress,
        onValueChange = {
            if (enableHaptics && Math.abs(it - dragProgress) > 0.04f) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            isDragging = true
            dragProgress = it
            onSeek(it)
        },
        onValueChangeFinished = {
            isDragging = false
        },
        enabled = enabled,
        modifier = modifier.height(34.dp),
        colors = SliderDefaults.colors(
            thumbColor = thumbColor,
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveColor
        )
    )
}

/**
 * 2. Wavy Slider: Expressive sine-wave seeker that oscillates forward while playing
 * and flattens into a straight bar when paused or scrubbed, with a custom circular thumb.
 */
@Composable
fun WavySlider(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFF3D7EC),
    inactiveColor: Color = Color.White.copy(alpha = 0.22f),
    thumbColor: Color = Color(0xFFF3D7EC),
    enableHaptics: Boolean = true,
    enabled: Boolean = true
) {
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val effectiveProgress = (if (isDragging) dragProgress else progress).coerceIn(0f, 1f)

    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying && !isDragging) 1f else 0f,
        animationSpec = tween(250, easing = LinearEasing),
        label = "wavyAmplitude"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wavyPhase")
    val phase by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavyPhaseFloat"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val newP = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                if (enableHaptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSeek(newP)
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    if (enableHaptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                },
                                onDragEnd = {
                                    onSeek(dragProgress)
                                    isDragging = false
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { change, _ ->
                                    val nextP = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    if (enableHaptics && Math.abs(nextP - dragProgress) > 0.04f) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                    dragProgress = nextP
                                    onSeek(dragProgress)
                                }
                            )
                        }
                } else Modifier
            )
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val strokeWidthPx = 4.dp.toPx()
        val waveLengthPx = 32.dp.toPx()
        val amplitudePx = 4.dp.toPx() * animatedAmplitude
        val thumbRadiusPx = 6.5.dp.toPx()
        val thumbX = effectiveProgress * width

        // 1. Played Wavy Track
        if (thumbX > 0f) {
            val wavePath = Path()
            wavePath.moveTo(0f, centerY)

            val stepPx = 2f
            var x = 0f
            while (x <= thumbX) {
                val y = centerY + amplitudePx * sin((x / waveLengthPx) * 2f * PI.toFloat() - phase)
                wavePath.lineTo(x, y)
                x += stepPx
            }
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

        // 2. Unplayed Solid Track
        if (thumbX < width) {
            drawLine(
                color = inactiveColor,
                start = Offset(thumbX, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx * 0.9f,
                cap = StrokeCap.Round
            )
        }

        // 3. Thumb Circle
        val thumbY = centerY + (if (amplitudePx > 0f) amplitudePx * sin((thumbX / waveLengthPx) * 2f * PI.toFloat() - phase) else 0f)
        drawCircle(
            color = thumbColor,
            radius = thumbRadiusPx,
            center = Offset(thumbX, thumbY)
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = thumbRadiusPx * 0.45f,
            center = Offset(thumbX, thumbY)
        )
    }
}

/**
 * 3. Slim Slider: Minimalist, clean bar without thumb circle that dynamically
 * expands with spring physics from 4.dp to 10.dp when touched or dragged.
 */
@Composable
fun SlimSlider(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFF3D7EC),
    inactiveColor: Color = Color.White.copy(alpha = 0.22f),
    enableHaptics: Boolean = true,
    enabled: Boolean = true
) {
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val effectiveProgress = (if (isDragging) dragProgress else progress).coerceIn(0f, 1f)

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "slimTrackHeight"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val newP = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                if (enableHaptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSeek(newP)
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    if (enableHaptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                },
                                onDragEnd = {
                                    onSeek(dragProgress)
                                    isDragging = false
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { change, _ ->
                                    val nextP = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    if (enableHaptics && Math.abs(nextP - dragProgress) > 0.04f) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                    dragProgress = nextP
                                    onSeek(dragProgress)
                                }
                            )
                        }
                } else Modifier
            )
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val strokePx = trackHeight.toPx()
        val thumbX = effectiveProgress * width

        // Inactive Base Track
        drawLine(
            color = inactiveColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // Active Track
        if (thumbX > 0f) {
            drawLine(
                color = activeColor,
                start = Offset(0f, centerY),
                end = Offset(thumbX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 9-Sided Parametric Wavy Cookie Shape for Play/Pause buttons.
 * Morphs between a smooth circle (indent = 0f) and a 9-petaled wavy cookie (indent > 0f).
 */
class WavyShape(
    val sides: Int = 9,
    val indent: Float = 0.14f,
    val rotationDegrees: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (indent <= 0.001f) {
            val path = Path().apply {
                addOval(Rect(Offset.Zero, size))
            }
            return Outline.Generic(path)
        }
        val path = Path()
        val maxRadiusX = size.width / 2f
        val maxRadiusY = size.height / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        val steps = 120
        val rotationRad = rotationDegrees * PI / 180.0
        for (i in 0..steps) {
            val angle = i * PI * 2 / steps
            val bumpAngle = angle - rotationRad
            val r = 1f - indent + indent * cos(sides * bumpAngle)
            val x = cx + maxRadiusX * r * cos(angle)
            val y = cy + maxRadiusY * r * sin(angle)
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * 2x2 Grid Dialog for choosing the Player Slider Style with live non-interactive mini-previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderStyleDialog(
    currentStyle: String,
    onSelectStyle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1424),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Player slider style",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                // 2x2 Grid of Previews
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Bar & Wavy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SliderPreviewCard(
                            title = "Bar",
                            isSelected = currentStyle.equals("BAR", ignoreCase = true),
                            onClick = {
                                onSelectStyle("BAR")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            BarSlider(
                                progress = 0.45f,
                                onSeek = {},
                                enabled = false,
                                activeColor = Color(0xFFF3D7EC),
                                inactiveColor = Color.White.copy(alpha = 0.2f),
                                thumbColor = Color(0xFFF3D7EC)
                            )
                        }

                        SliderPreviewCard(
                            title = "Wavy",
                            isSelected = currentStyle.equals("WAVY", ignoreCase = true),
                            onClick = {
                                onSelectStyle("WAVY")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            WavySlider(
                                progress = 0.5f,
                                isPlaying = true,
                                onSeek = {},
                                enabled = false,
                                activeColor = Color(0xFFF3D7EC),
                                inactiveColor = Color.White.copy(alpha = 0.2f),
                                thumbColor = Color(0xFFF3D7EC)
                            )
                        }
                    }

                    // Row 2: Slim & Squiggly
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SliderPreviewCard(
                            title = "Slim",
                            isSelected = currentStyle.equals("SLIM", ignoreCase = true),
                            onClick = {
                                onSelectStyle("SLIM")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            SlimSlider(
                                progress = 0.65f,
                                onSeek = {},
                                enabled = false,
                                activeColor = Color(0xFFF3D7EC),
                                inactiveColor = Color.White.copy(alpha = 0.2f)
                            )
                        }

                        SliderPreviewCard(
                            title = "Squiggly",
                            isSelected = currentStyle.equals("SQUIGGLY", ignoreCase = true),
                            onClick = {
                                onSelectStyle("SQUIGGLY")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            SquigglySlider(
                                progress = 0.5f,
                                isPlaying = true,
                                onSeek = {},
                                activeColor = Color(0xFFF3D7EC),
                                inactiveColor = Color.White.copy(alpha = 0.2f),
                                thumbColor = Color(0xFFF3D7EC)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFFF2AEC7))
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderPreviewCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFF2AEC7) else Color.White.copy(alpha = 0.12f)
    val bgColor = if (isSelected) Color(0xFFF2AEC7).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = if (isSelected) Color(0xFFF2AEC7) else Color.White.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
