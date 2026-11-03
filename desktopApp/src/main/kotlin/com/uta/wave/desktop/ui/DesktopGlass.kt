package com.uta.wave.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// UTA-WAVE Liquid Glass Color Tokens
val GlassBgTop = Color(0xEE1E152B)       // Frosted deep violet amethyst top (93% opacity)
val GlassBgBottom = Color(0xD9120D1C)    // Obsidian violet-slate bottom (85% opacity)

val GlassBorderTop = Color.White.copy(alpha = 0.28f)    // Specular light reflection on top edge
val GlassBorderBottom = Color.White.copy(alpha = 0.06f) // Faint ambient rim on bottom edge

val GlassActivePill = Color(0xFF8B5CF6)  // Vibrant Accent Purple
val GlassActivePillGlow = Color(0x408B5CF6)
val GlassTextActive = Color(0xFFFFFFFF)
val GlassTextMuted = Color(0xFF9CA3AF)

// Liquid Glass Gradients
val DesktopGlassBackgroundBrush = Brush.verticalGradient(
    colors = listOf(GlassBgTop, GlassBgBottom)
)

val DesktopGlassBorderBrush = Brush.verticalGradient(
    colors = listOf(GlassBorderTop, GlassBorderBottom)
)

val DesktopGlassHoverBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF281C38),
        Color(0xFF181024)
    )
)

/**
 * Applies the signature UTA-WAVE Liquid Glass effect on Compose Desktop.
 * Combines deep ambient drop shadow, translucent glass fill, and specular edge highlight.
 */
fun Modifier.desktopLiquidGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 18.dp,
    borderWidth: Dp = 1.dp,
    backgroundBrush: Brush = DesktopGlassBackgroundBrush,
    borderBrush: Brush = DesktopGlassBorderBrush,
    spotColor: Color = Color.Black.copy(alpha = 0.65f),
    ambientColor: Color = Color(0x338B5CF6)
): Modifier {
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = spotColor,
            ambientColor = ambientColor
        )
        .clip(shape)
        .background(backgroundBrush)
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}
