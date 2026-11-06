package com.uta.wave.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.exp
import kotlin.math.max

private const val DISTORTION_SKSL = """
    uniform shader content;
    uniform vec2 u_mouse;
    uniform vec2 u_velocity;
    uniform float u_strength;
    uniform float u_radius;
    uniform vec2 u_resolution;

    vec4 main(vec2 coord) {
        if (u_strength <= 0.001) {
            return content.eval(coord);
        }

        vec2 toMouse = coord - u_mouse;
        float dist = length(toMouse);

        if (dist < u_radius && dist > 0.1) {
            // Smooth cubic bell curve decay from cursor
            float normDist = dist / u_radius;
            float falloff = (1.0 - normDist) * (1.0 - normDist);

            // Dynamic directional distortion:
            // 1. Radial displacement away from cursor (bulge / refraction)
            vec2 radialDir = normalize(toMouse);
            // 2. Velocity-driven shear / drag displacement (rubber/fluid drag)
            vec2 velocityOffset = u_velocity * 0.12;

            vec2 offset = (radialDir * 32.0 + velocityOffset) * (falloff * u_strength);

            // Scale UV offset to content bounds
            vec2 distortedCoord = coord + offset;
            distortedCoord = clamp(distortedCoord, vec2(0.0), u_resolution);

            return content.eval(distortedCoord);
        }

        return content.eval(coord);
    }
"""

/**
 * Authentic Interactive Texture Distortion Surface for UTA-WAVE Desktop Ambient Mode.
 *
 * Direct replica of the Hope麻匪 Wallpaper Engine cursor ripple effect (`cursorripple_combine.frag`):
 * - Real GPU UV displacement: moving, hovering, or dragging the mouse warps and stretches
 *   the album artwork pixels dynamically under the cursor.
 * - Physics-based viscous relaxation: once the cursor stops or exits, the image snaps smoothly
 *   back to its undisturbed shape via exponential spring damping.
 * - At rest: exactly 0% distortion, displaying 100% crystal-clear original album art.
 */
@Composable
fun DesktopDistortionAlbumArt(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    topCornerRadius: Dp = 22.dp
) {
    var artworkBitmap by remember(artworkUrl) {
        mutableStateOf<ImageBitmap?>(artworkUrl?.let { DesktopImageCache.get(it) })
    }

    LaunchedEffect(artworkUrl) {
        if (!artworkUrl.isNullOrBlank() && artworkBitmap == null) {
            val loaded = loadDesktopImageBitmapSafe(artworkUrl)
            if (loaded != null) {
                artworkBitmap = loaded
            }
        }
    }

    // Compile SkSL RuntimeEffect once
    val runtimeEffect = remember {
        try {
            RuntimeEffect.makeForShader(DISTORTION_SKSL)
        } catch (_: Exception) {
            null
        }
    }

    // Mouse interactive physics state
    var mousePos by remember { mutableStateOf(Offset(-1000f, -1000f)) }
    var velocity by remember { mutableStateOf(Offset.Zero) }
    var strength by remember { mutableStateOf(0f) }
    var lastInteractionTime by remember { mutableStateOf(0L) }
    var lastPointerPos by remember { mutableStateOf(Offset.Zero) }

    fun registerMouseMovement(pos: Offset) {
        val now = System.currentTimeMillis()
        val dt = max(1L, now - lastInteractionTime)
        lastInteractionTime = now

        val delta = pos - lastPointerPos
        lastPointerPos = pos
        mousePos = pos

        // Calculate kinetic velocity vector
        val speedX = (delta.x / dt.toFloat()) * 16.0f
        val speedY = (delta.y / dt.toFloat()) * 16.0f
        velocity = Offset(speedX.coerceIn(-60f, 60f), speedY.coerceIn(-60f, 60f))
        strength = 1.0f
    }

    // VSYNC physics simulation loop (runs while strength > 0.001)
    LaunchedEffect(strength > 0.001f) {
        var lastNano = System.nanoTime()
        while (strength > 0.001f) {
            withFrameNanos { now ->
                val dt = ((now - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNano = now

                // Viscous exponential damping: snaps pixels back to rest
                strength += (0f - strength) * (1.0f - exp(-dt * 5.5f))
                velocity = Offset(
                    velocity.x + (0f - velocity.x) * (1.0f - exp(-dt * 7.0f)),
                    velocity.y + (0f - velocity.y) * (1.0f - exp(-dt * 7.0f))
                )

                if (strength < 0.005f) {
                    strength = 0f
                    velocity = Offset.Zero
                }
            }
        }
    }

    val cornerShape = RoundedCornerShape(
        topStart = topCornerRadius,
        topEnd = topCornerRadius,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    Box(
        modifier = modifier
            .clip(cornerShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Enter -> {
                                registerMouseMovement(change.position)
                            }
                            PointerEventType.Press -> {
                                registerMouseMovement(change.position)
                                strength = 1.25f
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        registerMouseMovement(change.position)
                        strength = 1.25f
                    }
                )
            }
    ) {
        if (artworkBitmap != null && runtimeEffect != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawIntoCanvas { composeCanvas ->
                    val skiaCanvas = composeCanvas.nativeCanvas

                    val skiaImage = Image.makeFromBitmap(artworkBitmap!!.asSkiaBitmap())
                    val imageShader = skiaImage.makeShader()

                    val builder = RuntimeShaderBuilder(runtimeEffect)
                    builder.child("content", imageShader)
                    builder.uniform("u_mouse", mousePos.x, mousePos.y)
                    builder.uniform("u_velocity", velocity.x, velocity.y)
                    builder.uniform("u_strength", strength)
                    builder.uniform("u_radius", max(w, h) * 0.55f)
                    builder.uniform("u_resolution", w, h)

                    val shader = builder.makeShader()
                    val paint = Paint().apply {
                        this.shader = shader
                        isAntiAlias = true
                    }

                    skiaCanvas.drawRect(Rect.makeWH(w, h), paint)
                }
            }
        } else if (artworkBitmap != null) {
            // Fallback standard image if GPU runtime shader not supported on host driver
            androidx.compose.foundation.Image(
                bitmap = artworkBitmap!!,
                contentDescription = "Album Artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Neutral dark placeholder while artwork loads
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF2A1B4E), Color(0xFF110C24))
                        )
                    )
            )
        }
    }
}
