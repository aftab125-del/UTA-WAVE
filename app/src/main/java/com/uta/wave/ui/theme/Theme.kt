package com.uta.wave.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ObsidianBackground = Color(0xFF030305)
val SurfaceDark = Color(0xFF090A0F)
val SurfaceElevated = Color(0xFF13141F)
val ElectricViolet = Color(0xFF8B5CF6)
val SoftViolet = Color(0xFFA78BFA)
val DeepViolet = Color(0xFF6D28D9)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

private val WaveDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    secondary = SoftViolet,
    background = ObsidianBackground,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun UtaWaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WaveDarkColorScheme,
        content = content
    )
}
