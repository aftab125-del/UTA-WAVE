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
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Paint
import java.util.Random

private data class MatrixRainColumn(
    val xFraction: Float,
    var yFraction: Float,
    val speed: Float,
    val length: Int,
    val glyphs: CharArray
)

// Authentic Matrix Rain character set: Alphanumeric + Japanese Katakana
private val MATRIX_GLYPHS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZｦｱｳｴｵｶｷｹｺｻｼｽｾｿﾀﾂﾃﾅﾆﾇﾈﾊﾋﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ".toCharArray()

/**
 * Authentic Matrix Digital Code Rain Background for UTA-WAVE Ambient Mode.
 * Replicates the `黑客` layer from Hope麻匪 Wallpaper Engine workshop (#2974757317).
 *
 * Renders large, crisp, legible vertical streams of falling digital code glyphs
 * matching the exact font size and column density of the real wallpaper.
 */
@Composable
fun DesktopMatrixRainCanvas(
    modifier: Modifier = Modifier,
    columnCount: Int = 24
) {
    val columns = remember(columnCount) {
        val rng = Random(42)
        val list = mutableListOf<MatrixRainColumn>()
        for (i in 0 until columnCount) {
            val length = 10 + rng.nextInt(12)
            val chars = CharArray(length) { MATRIX_GLYPHS[rng.nextInt(MATRIX_GLYPHS.size)] }
            // Distribute columns across the screen width with slight natural jitter
            val x = (i.toFloat() + 0.5f + (rng.nextFloat() - 0.5f) * 0.4f) / columnCount.toFloat()
            list.add(
                MatrixRainColumn(
                    xFraction = x.coerceIn(0.02f, 0.98f),
                    yFraction = rng.nextFloat(),
                    speed = 0.0012f + rng.nextFloat() * 0.0022f,
                    length = length,
                    glyphs = chars
                )
            )
        }
        list
    }

    // Try to load MS Gothic or Segoe UI Symbol for authentic Katakana + numbers support on Windows
    val font = remember {
        val typeface = try {
            FontMgr.default.matchFamilyStyle("MS Gothic", FontStyle.NORMAL)
                ?: FontMgr.default.matchFamilyStyle("Segoe UI Symbol", FontStyle.NORMAL)
        } catch (_: Exception) {
            null
        }
        if (typeface != null) {
            Font(typeface, 22f)
        } else {
            Font().apply { size = 22f }
        }
    }

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
        val charSpacing = 26f

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas

            for (col in columns) {
                // Update vertical falling position
                col.yFraction = (col.yFraction + col.speed) % 1.0f
                val headX = col.xFraction * w
                val headY = col.yFraction * (h + col.length * charSpacing)

                // Subtle random glyph mutation
                if (System.nanoTime() % 23 == 0L) {
                    val rndIdx = (col.yFraction * col.length).toInt().coerceIn(0, col.length - 1)
                    col.glyphs[rndIdx] = MATRIX_GLYPHS[(col.yFraction * 1000).toInt() % MATRIX_GLYPHS.size]
                }

                for (j in 0 until col.length) {
                    val glyphY = headY - (j * charSpacing)
                    if (glyphY < -30f || glyphY > h + 30f) continue

                    val glyphStr = col.glyphs[j].toString()

                    if (j == 0) {
                        // Head character: Radiant bright white
                        headPaint.color = 0xFFFFFFFF.toInt()
                        native.drawString(glyphStr, headX, glyphY, font, headPaint)
                    } else {
                        // Tail character: Soft fading opacity matching Screenshot 1
                        val trailFraction = 1.0f - (j.toFloat() / col.length.toFloat())
                        val alpha = ((trailFraction * trailFraction) * 200f).toInt().coerceIn(0, 255)
                        val colorInt = (alpha shl 24) or (210 shl 16) or (230 shl 8) or 255
                        tailPaint.color = colorInt
                        native.drawString(glyphStr, headX, glyphY, font, tailPaint)
                    }
                }
            }
        }
    }
}
