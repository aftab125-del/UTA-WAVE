package com.uta.wave.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Reusable Swipe Gesture modifier matching EchoMusic's Modifier.SwipeGesture.
 * Supports interactive dragging with spring return physics and haptic ticks on threshold trigger.
 */
fun Modifier.swipeToChangeSong(
    enabled: Boolean = true,
    swipeThreshold: Float = 80f,
    onPrevious: () -> Unit,
    onNext: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    this
        .graphicsLayer {
            translationX = offsetX.value
        }
        .pointerInput(Unit) {
            var totalDrag = 0f

            detectHorizontalDragGestures(
                onDragStart = {
                    totalDrag = 0f
                },
                onDragEnd = {
                    if (abs(totalDrag) > swipeThreshold) {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        } catch (_: Exception) {}

                        if (totalDrag > 0) {
                            onPrevious()
                        } else {
                            onNext()
                        }
                    }
                    coroutineScope.launch {
                        offsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    totalDrag = 0f
                },
                onDragCancel = {
                    coroutineScope.launch {
                        offsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    totalDrag = 0f
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                    coroutineScope.launch {
                        offsetX.snapTo(offsetX.value + dragAmount * 0.7f)
                    }
                }
            )
        }
}
