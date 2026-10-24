package com.uta.wave.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.uta.wave.ui.components.GlassBackgroundBrush
import com.uta.wave.ui.components.GlassBorderBrush
import com.uta.wave.ui.recognizer.BlushRose

// Dynamic global theme tokens tracked by Compose Snapshot State
var ObsidianBackground by mutableStateOf(Color(0xFF0F0B12))
var SurfaceDark by mutableStateOf(Color(0xFF18131E))
var SurfaceElevated by mutableStateOf(Color(0xFF231C2A))
var SurfaceContainerHigh by mutableStateOf(Color(0xFF2C2333))
var CardBorder by mutableStateOf(Color(0xFF382D42))
var ElectricViolet by mutableStateOf(Color(0xFFF2AEC7)) // Dynamic primary accent
var SoftViolet by mutableStateOf(Color(0xFFF3C2E5))
var DeepViolet by mutableStateOf(Color(0xFF9E4C84))
var AccentPink by mutableStateOf(Color(0xFFFF75B5))
var TextPrimary by mutableStateOf(Color(0xFFF8F4F9))
var TextSecondary by mutableStateOf(Color(0xFFC7B8CC))
var TextMuted by mutableStateOf(Color(0xFF8E7D96))

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
            Color(0xFFF2AEC7)
        }
    }

    val brandFont = remember(selectedFont, customFontPath) {
        resolveFontFamily(selectedFont, customFontPath)
    }
    val typography = remember(brandFont) {
        getTypography(brandFont = brandFont, plainFont = brandFont)
    }

    // Synchronously update the global theme state so all screens immediately reflect changes
    val colorScheme = remember(isDark, pureBlack, primaryAccent) {
        if (isDark) {
            if (pureBlack) {
                // AMOLED Pure Black Scheme (000000 true black for OLED battery efficiency)
                ObsidianBackground = Color.Black
                SurfaceDark = Color.Black
                SurfaceElevated = Color(0xFF121212)
                SurfaceContainerHigh = Color(0xFF181818)
                CardBorder = Color(0xFF262626)
                TextPrimary = Color(0xFFF8F4F9)
                TextSecondary = Color(0xFFB0A4B5)
                TextMuted = Color(0xFF7A6F7F)

                GlassBackgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF0A0A0A), Color(0xFF000000))
                )
                GlassBorderBrush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
                )

                darkColorScheme(
                    primary = primaryAccent,
                    secondary = primaryAccent.copy(alpha = 0.85f),
                    tertiary = primaryAccent.copy(alpha = 0.70f),
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212),
                    surfaceContainer = Color(0xFF0A0A0A),
                    surfaceContainerHigh = Color(0xFF181818),
                    surfaceContainerLow = Color(0xFF050505),
                    onPrimary = Color.Black,
                    onSecondary = Color.Black,
                    onBackground = Color(0xFFF8F4F9),
                    onSurface = Color(0xFFF8F4F9),
                    onSurfaceVariant = Color(0xFFB0A4B5),
                    outline = Color(0xFF262626),
                    outlineVariant = Color(0xFF1E1E1E)
                )
            } else {
                // Obsidian / Charcoal Dark Scheme
                ObsidianBackground = Color(0xFF0F0B12)
                SurfaceDark = Color(0xFF18131E)
                SurfaceElevated = Color(0xFF231C2A)
                SurfaceContainerHigh = Color(0xFF2C2333)
                CardBorder = Color(0xFF382D42)
                TextPrimary = Color(0xFFF8F4F9)
                TextSecondary = Color(0xFFC7B8CC)
                TextMuted = Color(0xFF8E7D96)

                GlassBackgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xD922182B), Color(0xBF140D1C))
                )
                GlassBorderBrush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                )

                darkColorScheme(
                    primary = primaryAccent,
                    secondary = SoftViolet,
                    tertiary = DeepViolet,
                    background = Color(0xFF0F0B12),
                    surface = Color(0xFF18131E),
                    surfaceVariant = Color(0xFF231C2A),
                    surfaceContainer = Color(0xFF140F1A),
                    surfaceContainerHigh = Color(0xFF2C2333),
                    surfaceContainerLow = Color(0xFF100B14),
                    onPrimary = Color(0xFF0F0B12),
                    onSecondary = Color(0xFF0F0B12),
                    onBackground = Color(0xFFF8F4F9),
                    onSurface = Color(0xFFF8F4F9),
                    onSurfaceVariant = Color(0xFFC7B8CC),
                    outline = Color(0xFF382D42),
                    outlineVariant = Color(0xFF261E2E)
                )
            }
        } else {
            // Elegant Light Theme Scheme
            ObsidianBackground = Color(0xFFFAF7FA)
            SurfaceDark = Color(0xFFFFFFFF)
            SurfaceElevated = Color(0xFFF3EDF5)
            SurfaceContainerHigh = Color(0xFFE8DFEB)
            CardBorder = Color(0xFFD6CBD8)
            TextPrimary = Color(0xFF1E1A22)
            TextSecondary = Color(0xFF4C4452)
            TextMuted = Color(0xFF7E7385)

            GlassBackgroundBrush = Brush.verticalGradient(
                listOf(Color(0xF5FFFFFF), Color(0xEEF2EDF5))
            )
            GlassBorderBrush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.05f))
            )

            lightColorScheme(
                primary = primaryAccent,
                secondary = primaryAccent.copy(alpha = 0.85f),
                tertiary = DeepViolet,
                background = Color(0xFFFAF7FA),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFF3EDF5),
                surfaceContainer = Color(0xFFEDE5F0),
                surfaceContainerHigh = Color(0xFFE8DFEB),
                surfaceContainerLow = Color(0xFFF7F2F8),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF1E1A22),
                onSurface = Color(0xFF1E1A22),
                onSurfaceVariant = Color(0xFF4C4452),
                outline = Color(0xFFD6CBD8),
                outlineVariant = Color(0xFFE2D9E6)
            )
        }
    }

    ElectricViolet = primaryAccent
    SoftViolet = primaryAccent.copy(alpha = 0.85f)
    DeepViolet = primaryAccent.copy(alpha = 0.65f)
    AccentPink = primaryAccent
    BlushRose = primaryAccent

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        // App-wide font family provider: every single Text(...) across all screens inherits brandFont
        CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.bodyLarge.copy(
                fontFamily = brandFont,
                color = TextPrimary
            )
        ) {
            content()
        }
    }
}
