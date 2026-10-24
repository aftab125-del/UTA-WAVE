package com.uta.wave.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

val ObsidianBackground = Color(0xFF0F0B12)
val SurfaceDark = Color(0xFF18131E)
val SurfaceElevated = Color(0xFF231C2A)
val SurfaceContainerHigh = Color(0xFF2C2333)
val CardBorder = Color(0xFF382D42)
val ElectricViolet = Color(0xFFF2AEC7) // Echo Music Soft Rose/Blush Pink default (#F2AEC7)
val SoftViolet = Color(0xFFF3C2E5)
val DeepViolet = Color(0xFF9E4C84)
val AccentPink = Color(0xFFFF75B5)
val TextPrimary = Color(0xFFF8F4F9)
val TextSecondary = Color(0xFFC7B8CC)
val TextMuted = Color(0xFF8E7D96)

@Composable
fun UtaWaveTheme(
    darkMode: String = "ON",
    pureBlack: Boolean = false,
    accentColorHex: String = "#F2AEC7",
    isDynamicPalette: Boolean = true,
    selectedFont: String = "SYSTEM",
    customFontPath: String? = null,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        "ON" -> true
        "OFF" -> false
        else -> systemInDark // "AUTO"
    }

    val primaryAccent = remember(accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(accentColorHex))
        } catch (e: Exception) {
            ElectricViolet
        }
    }

    val colorScheme = remember(isDark, pureBlack, primaryAccent) {
        if (isDark) {
            if (pureBlack) {
                // AMOLED Pure Black Scheme (000000 true black for OLED battery efficiency)
                darkColorScheme(
                    primary = primaryAccent,
                    secondary = primaryAccent.copy(alpha = 0.85f),
                    tertiary = SoftViolet,
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF141414),
                    surfaceContainer = Color(0xFF0A0A0A),
                    surfaceContainerHigh = Color(0xFF1F1F1F),
                    surfaceContainerLow = Color(0xFF050505),
                    onPrimary = Color.Black,
                    onSecondary = Color.Black,
                    onBackground = Color(0xFFF8F4F9),
                    onSurface = Color(0xFFF8F4F9),
                    onSurfaceVariant = Color(0xFFB0A4B5),
                    outline = Color(0xFF333333),
                    outlineVariant = Color(0xFF222222)
                )
            } else {
                // Obsidian / Charcoal Dark Scheme
                darkColorScheme(
                    primary = primaryAccent,
                    secondary = SoftViolet,
                    tertiary = DeepViolet,
                    background = ObsidianBackground,
                    surface = SurfaceDark,
                    surfaceVariant = SurfaceElevated,
                    surfaceContainer = Color(0xFF140F1A),
                    surfaceContainerHigh = SurfaceContainerHigh,
                    surfaceContainerLow = Color(0xFF100B14),
                    onPrimary = ObsidianBackground,
                    onSecondary = ObsidianBackground,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary,
                    onSurfaceVariant = TextSecondary,
                    outline = CardBorder,
                    outlineVariant = Color(0xFF261E2E)
                )
            }
        } else {
            // Elegant Light Theme Scheme
            lightColorScheme(
                primary = primaryAccent,
                secondary = primaryAccent.copy(alpha = 0.85f),
                tertiary = DeepViolet,
                background = Color(0xFFFAF7FA),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFF3EDF5),
                surfaceContainer = Color(0xFFEDE5F0),
                surfaceContainerHigh = Color(0xFFE5DCE8),
                surfaceContainerLow = Color(0xFFF7F2F8),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF1E1A22),
                onSurface = Color(0xFF1E1A22),
                onSurfaceVariant = Color(0xFF4C4452),
                outline = Color(0xFFCCC2D1),
                outlineVariant = Color(0xFFE2D9E6)
            )
        }
    }

    val brandFont = remember(selectedFont, customFontPath) {
        resolveFontFamily(selectedFont, customFontPath)
    }
    val typography = remember(brandFont) {
        getTypography(brandFont = brandFont, plainFont = brandFont)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
