package com.uta.wave.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

val ObsidianBackground = Color(0xFF0F0B12)
val SurfaceDark = Color(0xFF18131E)
val SurfaceElevated = Color(0xFF231C2A)
val SurfaceContainerHigh = Color(0xFF2C2333)
val CardBorder = Color(0xFF382D42)
val ElectricViolet = Color(0xFFE5A4D0) // Echo Music Soft Rose/Violet
val SoftViolet = Color(0xFFF3C2E5)
val DeepViolet = Color(0xFF9E4C84)
val AccentPink = Color(0xFFFF75B5)
val TextPrimary = Color(0xFFF8F4F9)
val TextSecondary = Color(0xFFC7B8CC)
val TextMuted = Color(0xFF8E7D96)

private val WaveDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    secondary = SoftViolet,
    background = ObsidianBackground,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevated,
    surfaceContainerHigh = SurfaceContainerHigh,
    onPrimary = ObsidianBackground,
    onSecondary = ObsidianBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun UtaWaveTheme(
    selectedFont: String = "SYSTEM",
    customFontPath: String? = null,
    content: @Composable () -> Unit
) {
    val brandFont = remember(selectedFont, customFontPath) {
        resolveFontFamily(selectedFont, customFontPath)
    }
    val typography = remember(brandFont) {
        getTypography(brandFont = brandFont, plainFont = brandFont)
    }

    MaterialTheme(
        colorScheme = WaveDarkColorScheme,
        typography = typography,
        content = content
    )
}
