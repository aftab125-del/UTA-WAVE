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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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

// Authentic Matrix Rain character set: Alphanumeric + Japanese Katakana (matches Wallpaper Engine spritesheet 72)
private val MATRIX_GLYPHS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZｦｱｳｴｵｶｷｹｺｻｼｽｾｿﾀﾂﾃﾅﾆﾇﾈﾊﾋﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ".toCharArray()

/**
 * Authentic Beat-Reactive Matrix Digital Code Rain Background for UTA-WAVE Ambient Mode.
 * Exact replication of the `黑客` layer from Hope麻匪 Wallpaper Engine workshop (#2974757317):
 * - Spawns vertical streaming columns from `matrix spritesheet 72`.
 * - Replicates script `09_Matrix_Code_Rain_instanceoverride_colorn_script.js`:
 *   Falling glyph tails synchronize with `secondaryColor` extracted from the current song's album art.
 * - Replicates script `09_Matrix_Code_Rain_instanceoverride_rate_script.js`:
 *   Falling speed and head radiance surge in real time on musical bass kicks (bassPulse).
 * - Replicates particle operator `matrix_trail_copy1.json`:
 *   Tail glyphs undergo progressive cubic alpha dissolution, dissolving into the dark background.
 * - Font linkage to Windows native `MS Gothic` / `Segoe UI Symbol` for full Katakana & Unicode sharpness.
 */
@Composable
fun DesktopMatrixRainCanvas(
    modifier: Modifier = Modifier,
    columnCount: Int = 28,
    secondaryColor: Color = Color(0xFF38BDF8),
    bassPulse: Float = 0f
) {
    val columns = remember(columnCount) {
        val rng = Random(42)
        val list = mutableListOf<MatrixRainColumn>()
        for (i in 0 until columnCount) {
            val length = 12 + rng.nextInt(14)
            val chars = CharArray(length) { MATRIX_GLYPHS[rng.nextInt(MATRIX_GLYPHS.size)] }
            // Stagger columns horizontally across full viewport
            val x = (i.toFloat() + 0.5f + (rng.nextFloat() - 0.5f) * 0.45f) / columnCount.toFloat()
            list.add(
                MatrixRainColumn(
                    xFraction = x.coerceIn(0.01f, 0.99f),
                    yFraction = rng.nextFloat(),
                    speed = 0.08f + rng.nextFloat() * 0.12f,
                    length = length,
                    glyphs = chars
                )
            )
        }
        list
    }

    // Load MS Gothic or Segoe UI Symbol for authentic Katakana + numbers support on Windows
    val font = remember {
        val typeface = try {
            FontMgr.default.matchFamilyStyle("MS Gothic", FontStyle.NORMAL)
                ?: FontMgr.default.matchFamilyStyle("Segoe UI Symbol", FontStyle.NORMAL)
        } catch (_: Exception) {
            null
        }
        if (typeface != null) {
            Font(typeface, 24f)
        } else {
            Font().apply { size = 24f }
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
    val framePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "matrixFramePhase"
    )

    var lastNanoTime by remember { mutableStateOf(System.nanoTime()) }

    val secColorArgb = remember(secondaryColor) { secondaryColor.toArgb() }
    val secR = (secColorArgb shr 16) and 0xFF
    val secG = (secColorArgb shr 8) and 0xFF
    val secB = secColorArgb and 0xFF

    Canvas(modifier = modifier.fillMaxSize()) {
        // READ framePhase to force continuous re-draw every VSYNC frame!
        val _phase = framePhase

        val now = System.nanoTime()
        val dt = ((now - lastNanoTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastNanoTime = now

        val w = size.width
        val h = size.height
        val charSpacing = 28f

        // Audio beat surge speed multiplier (matches 09_Matrix_Code_Rain_instanceoverride_rate_script.js)
        val speedMultiplier = 1.0f + (bassPulse * 0.85f)

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas

            for (col in columns) {
                // Update vertical falling position with delta time & audio beat surge
                col.yFraction = (col.yFraction + col.speed * speedMultiplier * dt) % 1.0f
                val headX = col.xFraction * w
                val headY = col.yFraction * (h + col.length * charSpacing)

                // Subtle random glyph mutation (matching original particle animation)
                if (System.nanoTime() % 19 == 0L) {
                    val rndIdx = (col.yFraction * col.length).toInt().coerceIn(0, col.length - 1)
                    col.glyphs[rndIdx] = MATRIX_GLYPHS[(col.yFraction * 1000).toInt() % MATRIX_GLYPHS.size]
                }

                for (j in 0 until col.length) {
                    val glyphY = headY - (j * charSpacing)
                    if (glyphY < -30f || glyphY > h + 30f) continue

                    val glyphStr = col.glyphs[j].toString()

                    if (j == 0) {
                        // Head character: Radiant bright white, pulses with audio kick
                        val headAlpha = ((0.88f + bassPulse * 0.12f) * 255f).toInt().coerceIn(0, 255)
                        headPaint.color = (headAlpha shl 24) or 0x00FFFFFF
                        native.drawString(glyphStr, headX, glyphY, font, headPaint)
                    } else {
                        // Tail character: Progressive cubic alpha dissolution into the dark background
                        // Replicates matrix_trail_copy1.json alphachange operator
                        val trailFraction = 1.0f - (j.toFloat() / col.length.toFloat())
                        // Cubic fade creates smooth atmospheric dissolve rather than harsh clipping
                        val dissolveFade = trailFraction * trailFraction * trailFraction
                        val alpha = (dissolveFade * (195f + bassPulse * 45f)).toInt().coerceIn(0, 255)

                        // Blend between bright glyph tint and extracted secondaryColor
                        val r = ((secR * 0.7f) + (230 * 0.3f)).toInt().coerceIn(0, 255)
                        val g = ((secG * 0.7f) + (240 * 0.3f)).toInt().coerceIn(0, 255)
                        val b = ((secB * 0.7f) + (255 * 0.3f)).toInt().coerceIn(0, 255)
                        val colorInt = (alpha shl 24) or (r shl 16) or (g shl 8) or b
                        tailPaint.color = colorInt
                        native.drawString(glyphStr, headX, glyphY, font, tailPaint)
                    }
                }
            }
        }
    }
}
