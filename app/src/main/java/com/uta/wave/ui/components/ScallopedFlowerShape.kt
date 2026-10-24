package com.uta.wave.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 12-lobed scalloped flower / starburst badge shape matching Echo Music & Android 14/15 media player.
 */
class ScallopedFlowerShape(
    private val lobes: Int = 12,
    private val scallopDepthRatio: Float = 0.12f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = min(size.width, size.height) / 2f
        val minRadius = maxRadius * (1f - scallopDepthRatio)
        val avgRadius = (maxRadius + minRadius) / 2f
        val amplitude = (maxRadius - minRadius) / 2f

        val steps = lobes * 16
        for (i in 0 until steps) {
            val angle = (i.toFloat() / steps.toFloat()) * (2f * PI.toFloat())
            val r = avgRadius + amplitude * cos(lobes * angle)
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
