package com.uta.wave.desktop.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.desktop.data.DesktopLyricLine
import com.uta.wave.desktop.data.DesktopLyricsData
import kotlin.math.abs

/**
 * Subpixel horizontal clipping shape for smooth letter-by-letter karaoke progression.
 * Produces crisp antialiased letter glyph filling with ZERO bounding-box artifacts.
 */
private class FractionalWidthShape(private val fraction: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val f = fraction.coerceIn(0f, 1f)
        return Outline.Rectangle(
            Rect(
                offset = Offset.Zero,
                size = Size(width = size.width * f, height = size.height)
            )
        )
    }
}

/**
 * Continuous drift-corrected master clock synchronized with hardware VSync via withFrameNanos.
 * Does NOT reset or cancel on periodic 150ms audio ticks; gently filters toward reported playback position.
 */
@Composable
private fun rememberSmoothPlaybackPosition(
    reportedPositionMs: Long,
    isPlaying: Boolean
): Float {
    val currentReported by rememberUpdatedState(reportedPositionMs)
    var smoothPos by remember { mutableStateOf(reportedPositionMs.toFloat()) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastTimeNano = System.nanoTime()
        while (isPlaying) {
            withFrameNanos { nowNano ->
                val dtMs = (nowNano - lastTimeNano) / 1_000_000f
                lastTimeNano = nowNano
                if (dtMs in 0.1f..100f) {
                    val target = currentReported.toFloat()
                    val dist = target - smoothPos
                    if (abs(dist) > 400f) {
                        smoothPos = target
                    } else {
                        smoothPos += dtMs + dist * 0.08f
                    }
                }
            }
        }
    }

    LaunchedEffect(reportedPositionMs) {
        if (!isPlaying || abs(reportedPositionMs - smoothPos) > 400f) {
            smoothPos = reportedPositionMs.toFloat()
        }
    }

    return smoothPos
}

/**
 * High-performance synchronized word-by-word & alphabet-by-alphabet karaoke lyrics renderer for Windows Desktop.
 * Pure white styling, zero box-halo artifacts, buttery-smooth flow, and auto-centering scroll.
 */
@Composable
fun DesktopSyncedLyricsView(
    lyrics: DesktopLyricsData,
    currentPositionMs: Long,
    isPlaying: Boolean,
    palette: AlbumArtPalette,
    isLoading: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = Color.White.copy(alpha = 0.85f),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Synchronizing lyrics...",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    if (lyrics.lines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Lyrics not available for this track",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enjoy the music · Lossless Audio",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.40f)
                )
            }
        }
        return
    }

    val smoothPositionMs = rememberSmoothPlaybackPosition(
        reportedPositionMs = currentPositionMs,
        isPlaying = isPlaying
    )

    val listState = rememberLazyListState()

    val activeIndex = remember(lyrics.lines, smoothPositionMs) {
        if (!lyrics.isSynced || lyrics.lines.isEmpty()) -1
        else {
            val idx = lyrics.lines.indexOfLast { it.timestampMs <= smoothPositionMs }
            if (idx >= 0) idx else 0
        }
    }

    // Auto-scroll centering as the song advances
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lyrics.isSynced && lyrics.lines.isNotEmpty()) {
            try {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -220
                )
            } catch (_: Exception) {
                listState.scrollToItem(activeIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 180.dp, bottom = 260.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val nextTimestampMs = lyrics.lines.getOrNull(index + 1)?.timestampMs
            DesktopLyricLineRow(
                line = line,
                index = index,
                activeIndex = activeIndex,
                nextTimestampMs = nextTimestampMs,
                smoothPositionMs = smoothPositionMs,
                isSynced = lyrics.isSynced,
                onClick = {
                    if (lyrics.isSynced) {
                        onSeekTo(line.timestampMs)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopLyricLineRow(
    line: DesktopLyricLine,
    index: Int,
    activeIndex: Int,
    nextTimestampMs: Long?,
    smoothPositionMs: Float,
    isSynced: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = isSynced && index == activeIndex
    val distanceFromCurrent = if (isSynced && activeIndex >= 0) abs(index - activeIndex) else 0

    // Dynamic Alpha Transition
    val targetAlpha = when {
        !isSynced -> 0.95f
        isActive -> 1.0f
        distanceFromCurrent == 1 -> 0.55f
        distanceFromCurrent == 2 -> 0.32f
        else -> 0.16f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 260),
        label = "lyricLineAlpha"
    )

    // Dynamic Scale Transition (Active line subtly springs larger)
    val targetScale = if (isActive) 1.04f else 0.96f
    val animatedScale by animateFloatAsState(
        targetValue = if (!isSynced) 1.0f else targetScale,
        animationSpec = tween(durationMillis = 300),
        label = "lyricLineScale"
    )

    // Distance Blur effect (Apple Music style)
    val blurDp = when {
        !isSynced || isActive -> 0.dp
        distanceFromCurrent == 1 -> 1.5.dp
        distanceFromCurrent == 2 -> 3.dp
        else -> 5.dp
    }

    // Line duration and active window
    val lineDuration = remember(line.timestampMs, nextTimestampMs) {
        if (nextTimestampMs != null && nextTimestampMs > line.timestampMs) {
            (nextTimestampMs - line.timestampMs).coerceIn(1000L, 12000L)
        } else {
            3600L
        }
    }

    // Word breakdown
    val words = remember(line.text) {
        line.text.split(" ").filter { it.isNotBlank() }
    }

    // Proportional word time distribution (allocating 92% of line duration to singing)
    val wordTimeRanges = remember(words, lineDuration) {
        var elapsed = 0f
        val singingDuration = lineDuration * 0.92f
        val totalWeight = words.sumOf { it.length.toDouble() + 1.0 }.toFloat().coerceAtLeast(1f)
        words.map { word ->
            val weight = word.length.toFloat() + 1.0f
            val slice = (singingDuration * (weight / totalWeight)).coerceAtLeast(180f)
            val start = elapsed
            val end = elapsed + slice
            elapsed = end
            Triple(word, start, end)
        }
    }

    val elapsedInLine = (smoothPositionMs - line.timestampMs.toFloat()).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .then(if (blurDp > 0.dp) Modifier.blur(blurDp) else Modifier)
            .clip(RoundedCornerShape(10.dp))
            .pointerHoverIcon(if (isSynced) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                enabled = isSynced,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        if (!isActive || words.isEmpty()) {
            // Inactive / Unsynced Line: Crisp pure white with distance alpha (NO box glow)
            Text(
                text = line.text,
                fontSize = if (isActive) 28.sp else 22.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                color = Color.White.copy(alpha = animatedAlpha),
                lineHeight = 36.sp
            )
        } else {
            // Active Line: Buttery Smooth Alphabet-to-Alphabet Continuous Karaoke Fill (NO BOX SHADOWS)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                wordTimeRanges.forEach { (word, wordStart, wordEnd) ->
                    val wordProgress = when {
                        elapsedInLine >= wordEnd -> 1.0f
                        elapsedInLine <= wordStart -> 0.0f
                        else -> ((elapsedInLine - wordStart) / (wordEnd - wordStart)).coerceIn(0f, 1f)
                    }

                    Box(
                        modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // 1. Base Layer: Unsung semi-transparent white (35% alpha)
                        Text(
                            text = word,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.35f),
                            lineHeight = 40.sp
                        )

                        // 2. Overlay Layer: Pure solid White glyphs clipped subpixel across letters (ZERO BOX SHADOW)
                        if (wordProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(FractionalWidthShape(wordProgress))
                            ) {
                                Text(
                                    text = word,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 40.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
