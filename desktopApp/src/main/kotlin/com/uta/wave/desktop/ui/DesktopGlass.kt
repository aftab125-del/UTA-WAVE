package com.uta.wave.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter

/**
 * Backdrop layer coordinator for Compose Multiplatform Desktop.
 * Captures underlying scene graphics so floating surfaces can blur and refract them.
 */
@Stable
class DesktopBackdrop(val graphicsLayer: GraphicsLayer) {
    var layerCoordinates: LayoutCoordinates? by mutableStateOf(null)
}

@Composable
fun rememberDesktopBackdrop(): DesktopBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { DesktopBackdrop(layer) }
}

/**
 * Records the child content into the [DesktopBackdrop] graphics layer so that
 * any floating liquid glass surfaces on top can sample and refract it.
 */
fun Modifier.desktopBackdropLayer(backdrop: DesktopBackdrop): Modifier =
    this.drawWithContent {
        backdrop.graphicsLayer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(backdrop.graphicsLayer)
    }.onGloballyPositioned { coordinates ->
        backdrop.layerCoordinates = coordinates
    }

// Specular Glass Border Gradients (Light refraction angles)
val GlassSpecularBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.55f),  // Bright top-left specular highlight
        Color.White.copy(alpha = 0.20f),  // Mid-edge transmission
        Color.White.copy(alpha = 0.05f),  // Bottom-edge ambient falloff
        Color.White.copy(alpha = 0.15f)   // Bottom-right faint reflection
    ),
    start = Offset(0f, 0f),
    end = Offset(400f, 100f)
)

val GlassSubtleBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.35f),
        Color.White.copy(alpha = 0.08f)
    )
)

// Active Tab Pill Glass Styling
val GlassActivePillBackground = Color.White.copy(alpha = 0.22f)
val GlassActivePillBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.45f),
        Color.White.copy(alpha = 0.15f)
    )
)

/**
 * Authentic Liquid Glass Modifier for Desktop Compose.
 *
 * 1. Samples the underlying [DesktopBackdrop] at the exact coordinates of this element.
 * 2. Applies native Skia Gaussian Blur ([ImageFilter.makeBlur]) to the captured backdrop.
 * 3. Clips the blurred reflection to [shape].
 * 4. Overlays a subtle, translucent theme-adaptive glass tint.
 * 5. Applies physical specular rim highlights and ambient drop shadow.
 */
@Composable
fun Modifier.desktopLiquidGlass(
    backdrop: DesktopBackdrop? = null,
    shape: CornerBasedShape = RoundedCornerShape(percent = 50),
    blurRadius: Float = 32f,
    surfaceTint: Color = Color(0x38120A1E),   // Deep translucent amethyst glass
    frostSheen: Color = Color.White.copy(alpha = 0.06f),
    borderBrush: Brush = GlassSpecularBorderBrush,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 22.dp,
    spotShadowColor: Color = Color.Black.copy(alpha = 0.65f),
    ambientShadowColor: Color = Color(0x408B5CF6) // Ethereal purple aura
): Modifier {
    val glassLayer = rememberGraphicsLayer()
    var elementCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Pre-create the Skia blur render effect
    val blurRenderEffect = remember(blurRadius) {
        if (blurRadius > 0f) {
            val skiaBlur = ImageFilter.makeBlur(blurRadius, blurRadius, FilterTileMode.CLAMP, null, null)
            skiaBlur.asComposeRenderEffect()
        } else null
    }

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = spotShadowColor,
            ambientColor = ambientShadowColor
        )
        .onGloballyPositioned { coords ->
            elementCoordinates = coords
        }
        .clip(shape)
        .drawWithContent {
            val backdropLayer = backdrop?.graphicsLayer
            val backdropCoords = backdrop?.layerCoordinates
            val elemCoords = elementCoordinates

            if (backdropLayer != null && backdropCoords != null && elemCoords != null && elemCoords.isAttached && backdropCoords.isAttached) {
                val elemPos = elemCoords.positionInWindow()
                val bgPos = backdropCoords.positionInWindow()
                val offsetX = -(elemPos.x - bgPos.x)
                val offsetY = -(elemPos.y - bgPos.y)

                glassLayer.renderEffect = blurRenderEffect
                glassLayer.record(size.toIntSize()) {
                    translate(offsetX, offsetY) {
                        drawLayer(backdropLayer)
                    }
                }
                drawLayer(glassLayer)
            } else {
                // Fallback translucent glass gradient if backdrop not yet attached
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xD9221832), Color(0xBF140D20))
                    )
                )
            }

            // Surface Glass Tint (Translucent amethyst)
            drawRect(color = surfaceTint)

            // Frosted Sheen (Top-to-bottom light sheen)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.02f),
                        Color.Transparent
                    )
                )
            )

            // Draw content (tab icons, labels)
            drawContent()
        }
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}
