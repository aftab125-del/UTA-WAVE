package com.uta.wave.ui.components

import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.uta.wave.data.preferences.UserPreferences
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.uta.wave.player.WavePlayerState
import com.uta.wave.ui.components.GlassComponent
import com.uta.wave.ui.components.LocalGlassEffectConfig
import com.uta.wave.ui.components.isGlassSupported
import com.uta.wave.ui.components.liquidGlass
import com.uta.wave.ui.recognizer.BlushRose
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.SoftViolet
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asynchronously extracts vibrant and muted palette colors from the track artwork.
 */
@Composable
fun rememberMiniPlayerPalette(thumbnailUrl: String?): List<Color> {
    val context = LocalContext.current
    var colors by remember(thumbnailUrl) { mutableStateOf<List<Color>>(emptyList()) }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl.isNullOrBlank()) {
            colors = emptyList()
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val imageLoader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .build()
                val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(8)
                        .resizeBitmapArea(100 * 100)
                        .generate()
                    val extracted = listOfNotNull(
                        palette.vibrantSwatch?.rgb?.let { Color(it) },
                        palette.lightVibrantSwatch?.rgb?.let { Color(it) },
                        palette.darkVibrantSwatch?.rgb?.let { Color(it) },
                        palette.mutedSwatch?.rgb?.let { Color(it) },
                        palette.lightMutedSwatch?.rgb?.let { Color(it) },
                        palette.darkMutedSwatch?.rgb?.let { Color(it) }
                    )
                    withContext(Dispatchers.Main) {
                        colors = if (extracted.isNotEmpty()) extracted else listOf(Color(0xFF6B21A8), Color(0xFFF2AEC7))
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    colors = listOf(Color(0xFF6B21A8), Color(0xFFF2AEC7))
                }
            }
        }
    }
    return colors
}

/**
 * Renders the chosen dynamic background behind the miniplayer controls.
 * Supported styles:
 * - BLUR: Gaussian blurred album art with 0.45f dimming scrim (Android 12+)
 * - GLOW_ANIMATED: 20s sinusoidal swirling radial gradient blobs from extracted palette
 * - LIVE_MESH: 60s rotating 360deg artwork, 1.6x saturated, 1.5x scaled, 40dp blur
 */
@Composable
fun MiniPlayerBackgroundLayer(
    style: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp)
) {
    val context = LocalContext.current
    val paletteColors = rememberMiniPlayerPalette(artworkUrl)

    Box(
        modifier = modifier.clip(shape)
    ) {
        when (style) {
            "BLUR" -> {
                val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(30.dp)
                } else {
                    Modifier
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .size(128, 128)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(blurModifier)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }

            "GLOW_ANIMATED" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "MiniPlayerGlow")
                val progress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 20000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "GlowProgress"
                )

                val colors = if (paletteColors.isNotEmpty()) {
                    paletteColors
                } else {
                    listOf(Color(0xFF8B5CF6), Color(0xFFF2AEC7), Color(0xFF38BDF8))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val p = progress
                            val width = size.width
                            val height = size.height

                            fun oscillate(min: Float, max: Float, phase: Float): Float {
                                val v = kotlin.math.sin(2f * kotlin.math.PI.toFloat() * (p + phase))
                                return min + (max - min) * ((v + 1f) * 0.5f)
                            }

                            val c1 = colors[0 % colors.size]
                            val c2 = colors[1 % colors.size]

                            val o1x = oscillate(0.0f, 1.0f, 0.0f)
                            val o1y = oscillate(0.0f, 0.5f, 0.1f)
                            val o2x = oscillate(1.0f, 0.0f, 0.2f)
                            val o2y = oscillate(0.5f, 1.0f, 0.3f)

                            val b1 = Brush.radialGradient(
                                colors = listOf(c1.copy(alpha = 0.8f), Color.Transparent),
                                center = Offset(width * o1x, height * o1y),
                                radius = width * 1.2f
                            )
                            val b2 = Brush.radialGradient(
                                colors = listOf(c2.copy(alpha = 0.7f), Color.Transparent),
                                center = Offset(width * o2x, height * o2y),
                                radius = width * 1.0f
                            )

                            drawRect(Color(0xFF0A070E))
                            drawRect(b1)
                            drawRect(b2)
                            drawRect(Color.Black.copy(alpha = 0.25f))
                        }
                )
            }

            "LIVE_MESH" -> {
                val infiniteTransition = rememberInfiniteTransition(label = "MiniPlayerLiveMesh")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 60000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "LiveMeshRotation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.5f
                            scaleY = 1.5f
                        }
                ) {
                    val matrix = remember { ColorMatrix().apply { setToSaturation(1.6f) } }
                    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(40.dp)
                    } else {
                        Modifier
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artworkUrl)
                            .size(128, 128)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(matrix),
                        modifier = Modifier
                            .fillMaxSize()
                            .then(blurModifier)
                            .graphicsLayer { rotationZ = rotation }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(
    playerState: WavePlayerState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences.getInstance(context) }
    val settings by userPrefs.settingsFlow.collectAsState()
    val isPureBlack = settings.pureBlack

    val glassConfig = LocalGlassEffectConfig.current
    val miniPlayerBackground = settings.miniplayerBackgroundStyle
    val isGlassSelected = miniPlayerBackground == "LIQUID_GLASS" || glassConfig.isEnabledFor(GlassComponent.MINI_PLAYER)
    val useGlass = isGlassSelected && isGlassSupported() && !isPureBlack
    val isDynamicBackground = !useGlass && miniPlayerBackground in listOf("BLUR", "GLOW_ANIMATED", "LIVE_MESH", "ACRYLIC_BLUR")

    val adaptiveTextColor =
        if (glassConfig.textColor.isSpecified) {
            glassConfig.textColor
        } else if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
            Color.Black
        } else {
            Color.White
        }

    val contentAccentColor = when {
        useGlass -> adaptiveTextColor
        isDynamicBackground -> Color.White
        else -> BlushRose
    }

    val primaryTextColor = when {
        useGlass -> adaptiveTextColor
        isDynamicBackground -> Color.White
        else -> TextPrimary
    }

    val secondaryTextColor = when {
        useGlass -> adaptiveTextColor.copy(alpha = 0.7f)
        isDynamicBackground -> Color.White.copy(alpha = 0.75f)
        else -> SoftViolet
    }

    val skipIconTint = when {
        useGlass -> adaptiveTextColor.copy(alpha = 0.8f)
        isDynamicBackground -> Color.White.copy(alpha = 0.85f)
        else -> TextSecondary
    }

    // Infinite rotation for the vinyl disk when playing
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiskRotation"
    )


    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val containerModifier = if (useGlass) {
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .liquidGlass(config = glassConfig, shape = RoundedCornerShape(28.dp))
                .clickable(onClick = onExpand)
                .swipeToChangeSong(
                    enabled = settings.enableSwipeToChangeSong,
                    onPrevious = onPrevious,
                    onNext = onNext
                )
        } else {
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .shadow(
                    elevation = if (isPureBlack) 8.dp else 18.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    when {
                        isPureBlack -> Brush.linearGradient(listOf(Color.Black, Color.Black))
                        isDynamicBackground -> Brush.linearGradient(listOf(Color(0xFF0C0911), Color(0xFF0C0911)))
                        else -> Brush.verticalGradient(
                            colors = listOf(
                                Color(0xD9281C30),
                                Color(0xBF181120)
                            )
                        )
                    }
                )
                .border(
                    1.dp,
                    when {
                        isPureBlack -> Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                        isDynamicBackground -> Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.32f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        )
                        else -> Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        )
                    },
                    RoundedCornerShape(28.dp)
                )
                .clickable(onClick = onExpand)
                .swipeToChangeSong(
                    enabled = settings.enableSwipeToChangeSong,
                    onPrevious = onPrevious,
                    onNext = onNext
                )
        }

        Box(
            modifier = containerModifier
        ) {
            if (isDynamicBackground) {
                MiniPlayerBackgroundLayer(
                    style = miniPlayerBackground,
                    artworkUrl = track.artworkUrl,
                    modifier = Modifier.matchParentSize(),
                    shape = RoundedCornerShape(28.dp)
                )
            }

            Column {
                if (playerState.error != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE53935))
                            .clickable(onClick = onOpenDiagnostics)
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "⚠️ ${playerState.error} • Tap for details",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spinning Circular Vinyl Artwork Disc
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .border(1.5.dp, contentAccentColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .rotate(if (playerState.isPlaying) rotation else 0f)
                        )
                        // Center vinyl hole dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B1522))
                                .border(1.dp, contentAccentColor, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track Title & Artist
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = primaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val syncManager = remember { com.uta.wave.sync.WaveSyncManager.getInstance(context) }
                        val roomState by syncManager.roomState.collectAsState()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (roomState != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ElectricViolet.copy(alpha = 0.25f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "SYNC",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = ElectricViolet
                                    )
                                }
                            }
                        }
                    }

                    // Play/Pause Action Button with Glass/BlushRose Accent
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(contentAccentColor.copy(alpha = if (useGlass || isDynamicBackground) 0.16f else 0.2f))
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            playerState.isResolving || playerState.isBuffering -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = contentAccentColor,
                                    strokeWidth = 2.dp
                                )
                            }
                            playerState.isPlaying -> {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = contentAccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = contentAccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Skip Next Button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = skipIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Progress track at bottom edge of pill
                val progress = if (playerState.durationMs > 0) {
                    (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                val progressStyle = settings.miniplayerProgressStyle
                if (progressStyle != "HIDDEN") {
                    val trackHeight = if (progressStyle == "WAVE") 3.dp else 2.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        val progressBrush = when {
                            progressStyle == "NEON_TRACK" -> Brush.horizontalGradient(
                                colors = listOf(ElectricViolet, BlushRose)
                            )
                            useGlass -> Brush.horizontalGradient(
                                colors = listOf(adaptiveTextColor, adaptiveTextColor.copy(alpha = 0.35f))
                            )
                            isDynamicBackground -> Brush.horizontalGradient(
                                colors = listOf(Color.White, Color.White.copy(alpha = 0.45f))
                            )
                            else -> Brush.horizontalGradient(
                                colors = listOf(BlushRose, ElectricViolet)
                            )
                        }

                        if (progressStyle == "WAVE" && playerState.isPlaying) {
                            // Animated mini waveform track
                            val wavePhase by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = (2 * Math.PI).toFloat(),
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "MiniWaveProgress"
                            )
                            val accentClr = if (useGlass) adaptiveTextColor else BlushRose
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progress)
                                    .height(trackHeight)
                            ) {
                                val w = size.width
                                val h = size.height
                                val midY = h / 2f
                                val path = androidx.compose.ui.graphics.Path()
                                val step = 4f
                                var x = 0f
                                path.moveTo(0f, midY)
                                while (x <= w) {
                                    val y = midY + kotlin.math.sin(x * 0.15f + wavePhase) * (h * 0.4f)
                                    path.lineTo(x, y)
                                    x += step
                                }
                                drawPath(
                                    path = path,
                                    color = accentClr,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progress)
                                    .height(trackHeight)
                                    .background(progressBrush)
                            )
                        }
                    }
                }
            }
        }
    }
}
