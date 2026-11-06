package com.uta.wave.desktop.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import java.util.Random

private data class MatrixRainColumn(
    val xFraction: Float,
    var yFraction: Float,
    val speed: Float,
    val length: Int,
    val glyphs: CharArray
)

private val MATRIX_GLYPHS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZｦｱｳｴｵｶｷｹｺｻｼｽｾｿﾀﾂﾃﾅﾆﾇﾈﾊﾋﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ".toCharArray()

/**
 * Authentic Matrix Digital Code Rain Background for UTA-WAVE Ambient Mode.
 * Replicates the `黑客` layer from Hope麻匪 Wallpaper Engine workshop (#2974757317).
 *
 * Renders vertical streams of falling digital code glyphs across the background
 * with bright leading heads and gradual trailing fade.
 */
@Composable
fun DesktopMatrixRainCanvas(
    modifier: Modifier = Modifier,
    columnCount: Int = 50,
    accentGlowColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF38BDF8)
) {
    val columns = remember(columnCount) {
        val rng = Random(1337)
        val list = mutableListOf<MatrixRainColumn>()
        for (i in 0 until columnCount) {
            val length = 12 + rng.nextInt(14)
            val chars = CharArray(length) { MATRIX_GLYPHS[rng.nextInt(MATRIX_GLYPHS.size)] }
            list.add(
                MatrixRainColumn(
                    xFraction = (i.toFloat() + rng.nextFloat() * 0.4f) / columnCount.toFloat(),
                    yFraction = rng.nextFloat(),
                    speed = 0.0018f + rng.nextFloat() * 0.0032f,
                    length = length,
                    glyphs = chars
                )
            )
        }
        list
    }

    val font = remember { Font().apply { size = 13.5f } }
    val headPaint = remember {
        Paint().apply {
            color = 0xFFFFFFFF.toInt()
            isAntiAlias = true
        }
    }
    val tailPaint = remember {
        Paint().apply {
            isAntiAlias = true
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "matrixRainLoop")
    val timeTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainTick"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val charSpacing = 16f

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas

            for (col in columns) {
                // Update vertical position
                col.yFraction = (col.yFraction + col.speed) % 1.0f
                val headX = col.xFraction * w
                val headY = col.yFraction * (h + col.length * charSpacing)

                // Occasionally mutate glyphs
                if (System.nanoTime() % 17 == 0L) {
                    val rndIdx = (col.yFraction * col.length).toInt().coerceIn(0, col.length - 1)
                    col.glyphs[rndIdx] = MATRIX_GLYPHS[(col.yFraction * 1000).toInt() % MATRIX_GLYPHS.size]
                }

                for (j in 0 until col.length) {
                    val glyphY = headY - (j * charSpacing)
                    if (glyphY < -20f || glyphY > h + 20f) continue

                    val glyphStr = col.glyphs[j].toString()

                    if (j == 0) {
                        // Head character: Pure radiant white/cyan
                        native.drawString(glyphStr, headX, glyphY, font, headPaint)
                    } else {
                        // Tail character: Fading opacity
                        val trailFraction = 1.0f - (j.toFloat() / col.length.toFloat())
                        val alphaInt = ((trailFraction * trailFraction) * 160f).toInt().coerceIn(0, 255)
                        // Soft code cyan/violet glow
                        val tailColor = (alphaInt shl 24) or 0x0088CCFF
                        tailPaint.color = tailColor
                        native.drawString(glyphStr, headX, glyphY, font, tailPaint)
                    }
                }
            }
        }
    }
}
