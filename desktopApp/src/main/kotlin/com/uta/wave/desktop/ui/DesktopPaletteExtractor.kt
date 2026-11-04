package com.uta.wave.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

/**
 * Palette colors extracted from the current song's album artwork for dynamic theme synchronization.
 * Refined for sophisticated, atmospheric mood aesthetics (no blinding neon/fluorescent tones).
 */
data class AlbumArtPalette(
    val dominantColor: Color,
    val accentColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val onAccentTextColor: Color
) {
    companion object {
        val Default = AlbumArtPalette(
            dominantColor = Color(0xFF161922),
            accentColor = Color(0xFF8A84A3),
            secondaryColor = Color(0xFF6B6584),
            glowColor = Color(0x338A84A3),
            onAccentTextColor = Color.White
        )
    }
}

private object PaletteCache {
    val cache = ConcurrentHashMap<String, AlbumArtPalette>()
}

/**
 * Remember and extract the album artwork color palette asynchronously with caching.
 */
@Composable
fun rememberAlbumArtPalette(artworkUrl: String?): AlbumArtPalette {
    if (artworkUrl.isNullOrBlank()) return AlbumArtPalette.Default

    var palette by remember(artworkUrl) {
        mutableStateOf(PaletteCache.cache[artworkUrl] ?: AlbumArtPalette.Default)
    }

    LaunchedEffect(artworkUrl) {
        if (!PaletteCache.cache.containsKey(artworkUrl)) {
            val extracted = withContext(Dispatchers.IO) {
                extractPaletteFromUrl(artworkUrl)
            }
            PaletteCache.cache[artworkUrl] = extracted
            palette = extracted
        }
    }

    return palette
}

private fun extractPaletteFromUrl(url: String): AlbumArtPalette {
    try {
        val bytes = URI(url).toURL().openStream().use { it.readBytes() }
        val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return AlbumArtPalette.Default

        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return AlbumArtPalette.Default

        // Sample in a 24x24 grid
        val stepX = max(1, width / 24)
        val stepY = max(1, height / 24)

        var bestVibrantScore = -1f
        var bestVibrantR = 138
        var bestVibrantG = 132
        var bestVibrantB = 163

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var sampledCount = 0

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF

                // Skip very dark or almost pure white pixels
                val brightness = (max(r, max(g, b)) + min(r, min(g, b))) / 510f
                val delta = (max(r, max(g, b)) - min(r, min(g, b))) / 255f

                if (brightness < 0.15f || brightness > 0.92f) continue
                if (delta < 0.12f) continue // Skip muddy grayscale

                totalR += r
                totalG += g
                totalB += b
                sampledCount++

                // Score vibrancy: moderate saturation with pleasant mid brightness
                val saturation = if (brightness > 0f) delta / (1f - kotlin.math.abs(2f * brightness - 1f) + 0.001f) else 0f
                val score = saturation * 0.6f + (1f - kotlin.math.abs(brightness - 0.55f)) * 0.4f

                if (score > bestVibrantScore) {
                    bestVibrantScore = score
                    bestVibrantR = r
                    bestVibrantG = g
                    bestVibrantB = b
                }
            }
        }

        if (bestVibrantScore > 0f) {
            // Refine color to calm atmospheric tone (no blinding neon)
            val accent = refineAtmosphericColor(bestVibrantR, bestVibrantG, bestVibrantB)
            val domR = if (sampledCount > 0) (totalR / sampledCount).toInt() else bestVibrantR
            val domG = if (sampledCount > 0) (totalG / sampledCount).toInt() else bestVibrantG
            val domB = if (sampledCount > 0) (totalB / sampledCount).toInt() else bestVibrantB
            val dominant = Color(domR, domG, domB).copy(alpha = 1f)

            return AlbumArtPalette(
                dominantColor = dominant,
                accentColor = accent,
                secondaryColor = accent.copy(alpha = 0.65f),
                glowColor = accent.copy(alpha = 0.25f),
                onAccentTextColor = Color.White
            )
        }
    } catch (_: Exception) {}

    return AlbumArtPalette.Default
}

/**
 * Normalizes raw color into a calm, sophisticated atmospheric tone.
 * Caps saturation strictly between 0.25 and 0.42 to ensure colors harmonize with the dark cinematic vibe.
 */
private fun refineAtmosphericColor(r: Int, g: Int, b: Int): Color {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f

    val max = max(rf, max(gf, bf))
    val min = min(rf, min(gf, bf))
    val d = max - min

    val l = (max + min) / 2f
    val s = if (l > 0.5f) d / (2f - max - min + 0.001f) else d / (max + min + 0.001f)

    // Gentle, tasteful saturation for dark UI
    val targetS = (s * 0.60f).coerceIn(0.24f, 0.42f)
    val targetL = l.coerceIn(0.56f, 0.68f)

    val h = when {
        d == 0f -> 0f
        max == rf -> ((gf - bf) / d + (if (gf < bf) 6f else 0f)) / 6f
        max == gf -> ((bf - rf) / d + 2f) / 6f
        else -> ((rf - gf) / d + 4f) / 6f
    }

    return hslToColor(h, targetS, targetL)
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q

    fun hueToRgb(t: Float): Float {
        var tc = t
        if (tc < 0f) tc += 1f
        if (tc > 1f) tc -= 1f
        if (tc < 1f / 6f) return p + (q - p) * 6f * tc
        if (tc < 1f / 2f) return q
        if (tc < 2f / 3f) return p + (q - p) * (2f / 3f - tc) * 6f
        return p
    }

    val r = hueToRgb(h + 1f / 3f)
    val g = hueToRgb(h)
    val b = hueToRgb(h - 1f / 3f)

    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
}
