package com.uta.wave.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.innertube.LyricLine
import java.text.BreakIterator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsLineRenderer(
    line: LyricLine,
    index: Int,
    activeIndex: Int,
    nextTimestampMs: Long?,
    currentPositionMs: Long,
    isSynced: Boolean,
    animationStyle: String,
    position: String,
    textSize: Float,
    lineSpacing: Float,
    appleMusicBlur: Boolean,
    standardBlur: Boolean,
    glowEffect: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = index == activeIndex
    val distanceFromCurrent = abs(index - activeIndex)

    // Calculate line duration and active window
    val lineDuration = remember(line.timestampMs, nextTimestampMs) {
        if (nextTimestampMs != null && nextTimestampMs > line.timestampMs) {
            nextTimestampMs - line.timestampMs
        } else {
            4000L
        }
    }
    val activeDuration = remember(lineDuration) {
        (lineDuration * 0.95).toLong().coerceAtLeast(300L)
    }

    // Proportional word time slicing
    val wordData = remember(line.text, activeDuration) {
        val words = line.text.split(" ").filter { it.isNotEmpty() }
        if (words.isEmpty()) {
            listOf(Triple(line.text, 0L, activeDuration))
        } else {
            val totalChars = line.text.length
            var accumulated = 0L
            words.mapIndexed { i, word ->
                val wordLength = word.length
                val includeSpace = i < words.size - 1
                val charCount = if (includeSpace) wordLength + 1 else wordLength
                val wordStart = accumulated
                val wordDur = if (totalChars > 0) {
                    (activeDuration * charCount.toFloat() / totalChars).toLong()
                } else activeDuration
                val wordEnd = wordStart + wordDur
                accumulated += wordDur
                Triple(word, wordStart, wordEnd)
            }
        }
    }

    // Relative elapsed time inside this line
    val lineRelTime = (currentPositionMs - line.timestampMs).coerceAtLeast(0L)

    // Dynamic blur calculation (Apple Music distance blur vs Standard blur)
    val blurRadius = when {
        !isSynced -> 0.dp
        appleMusicBlur -> when (distanceFromCurrent) {
            0 -> 0.dp
            1 -> 2.5.dp
            2 -> 4.5.dp
            else -> 7.dp
        }
        standardBlur && !isActive -> 3.5.dp
        else -> 0.dp
    }

    // Dynamic alpha by distance
    val targetAlpha = when {
        !isSynced -> 1.0f
        isActive -> 1.0f
        distanceFromCurrent == 1 -> 0.65f
        distanceFromCurrent == 2 -> 0.45f
        else -> 0.32f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 280),
        label = "lineAlpha"
    )

    // Dynamic scale spring
    val targetScale = if (isActive && animationStyle == "ECHOMUSIC_1") 1.0f else if (isActive) 1.05f else 0.88f
    val animatedScale by animateFloatAsState(
        targetValue = if (!isSynced) 1.0f else targetScale,
        animationSpec = tween(durationMillis = 350),
        label = "lineScale"
    )

    // Alignment
    val textAlign = when (position) {
        "CENTER" -> TextAlign.Center
        "RIGHT" -> TextAlign.Right
        else -> TextAlign.Left
    }
    val horizontalArrangement = when (position) {
        "CENTER" -> Arrangement.Center
        "RIGHT" -> Arrangement.End
        else -> Arrangement.Start
    }

    val itemModifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
            alpha = animatedAlpha
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
        .clip(RoundedCornerShape(12.dp))
        .clickable(enabled = isSynced, onClick = onClick)
        .padding(
            horizontal = 16.dp,
            vertical = (6 * lineSpacing.coerceIn(1.0f, 4.0f)).dp
        )

    val activeGlowShadow = if (glowEffect && isActive) {
        Shadow(
            color = accentColor.copy(alpha = 0.65f),
            offset = Offset.Zero,
            blurRadius = 18f
        )
    } else null

    Box(modifier = itemModifier) {
        when (animationStyle) {
            "APPLE_V2" -> {
                // Letter-by-letter grapheme progression via BreakIterator with tight tracking
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = horizontalArrangement,
                    verticalArrangement = Arrangement.spacedBy(
                        with(LocalDensity.current) {
                            (textSize * (lineSpacing.coerceAtMost(1.3f) - 1f)).sp.toDp()
                        }
                    )
                ) {
                    wordData.forEachIndexed { wordIndex, (wordText, startRel, endRel) ->
                        val wordDuration = (endRel - startRel).coerceAtLeast(1L)
                        val graphemes = remember(wordText) {
                            val iterator = BreakIterator.getCharacterInstance()
                            iterator.setText(wordText)
                            val list = mutableListOf<String>()
                            var start = iterator.first()
                            var end = iterator.next()
                            while (end != BreakIterator.DONE) {
                                list.add(wordText.substring(start, end))
                                start = end
                                end = iterator.next()
                            }
                            list
                        }

                        graphemes.forEachIndexed { charIndex, grapheme ->
                            val charDuration = if (graphemes.isNotEmpty()) wordDuration / graphemes.size else 0L
                            val charStart = startRel + (charIndex * charDuration)
                            val charEnd = charStart + charDuration

                            val charProgress = when {
                                !isActive -> 0f
                                lineRelTime >= charEnd -> 1f
                                lineRelTime < charStart -> 0f
                                else -> if (charDuration <= 0L) 1f else (lineRelTime - charStart).toFloat() / charDuration
                            }

                            val letterAlpha = if (!isActive) 1f else if (charProgress >= 1f) 1f else 0.35f + (0.65f * charProgress)

                            Text(
                                text = grapheme,
                                fontSize = textSize.sp,
                                color = accentColor.copy(alpha = letterAlpha),
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp,
                                style = TextStyle(shadow = activeGlowShadow)
                            )
                        }
                        if (wordIndex < wordData.size - 1) {
                            Text(
                                text = " ",
                                fontSize = textSize.sp,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }
                }
            }

            "APPLE" -> {
                // Word-by-word tracking with Smoothstep cubic Hermite curve and drop shadow
                val styledText = buildAnnotatedString {
                    wordData.forEachIndexed { wordIndex, (wordText, startRel, endRel) ->
                        val wordDur = (endRel - startRel).coerceAtLeast(1L)
                        val isWordActive = isActive && lineRelTime in startRel until endRel
                        val hasWordPassed = isActive && lineRelTime >= endRel

                        val rawProgress = if (isWordActive) {
                            ((lineRelTime - startRel).toFloat() / wordDur).coerceIn(0f, 1f)
                        } else if (hasWordPassed) 1f else 0f

                        val smoothProgress = rawProgress * rawProgress * (3f - 2f * rawProgress)
                        val wordAlpha = when {
                            !isActive -> 0.55f
                            hasWordPassed -> 1f
                            isWordActive -> 0.55f + (0.45f * smoothProgress)
                            else -> 0.40f
                        }
                        val wordWeight = when {
                            !isActive -> FontWeight.SemiBold
                            hasWordPassed -> FontWeight.Bold
                            isWordActive -> FontWeight.ExtraBold
                            else -> FontWeight.Normal
                        }
                        val shadow = if (isWordActive) {
                            Shadow(
                                color = accentColor.copy(alpha = 0.25f + 0.45f * smoothProgress),
                                offset = Offset.Zero,
                                blurRadius = 10f + 12f * smoothProgress
                            )
                        } else activeGlowShadow

                        withStyle(SpanStyle(color = accentColor.copy(alpha = wordAlpha), fontWeight = wordWeight, shadow = shadow)) {
                            append(wordText)
                        }
                        if (wordIndex < wordData.size - 1) append(" ")
                    }
                }
                Text(
                    text = styledText,
                    fontSize = textSize.sp,
                    textAlign = textAlign,
                    lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            "LYRICS_V2" -> {
                // Floating word fluid wave kinematics
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = horizontalArrangement,
                    verticalArrangement = Arrangement.spacedBy(
                        with(LocalDensity.current) {
                            (textSize * (lineSpacing.coerceAtMost(1.3f) - 1f)).sp.toDp()
                        }
                    )
                ) {
                    val density = LocalDensity.current
                    wordData.forEachIndexed { wordIndex, (wordText, startRel, endRel) ->
                        val wordDur = (endRel - startRel).coerceAtLeast(1L)
                        val isWordActive = isActive && lineRelTime in startRel until endRel
                        val isWordComplete = !isActive || lineRelTime >= endRel

                        val progress = when {
                            isWordComplete -> 1f
                            !isActive || lineRelTime <= startRel -> 0f
                            else -> ((lineRelTime - startRel).toFloat() / wordDur).coerceIn(0f, 1f)
                        }

                        val sinProgress = sin(progress * PI).toFloat()
                        val wordScale = 1f + (0.016f * sinProgress)
                        val floatOffset = if (isWordActive) -4f * sinProgress else 0f

                        Text(
                            text = wordText,
                            fontSize = textSize.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = accentColor.copy(alpha = if (isWordComplete || isWordActive) 1f else 0.45f),
                            style = TextStyle(
                                shadow = if (isWordActive) {
                                    Shadow(
                                        color = accentColor.copy(alpha = 0.5f * sinProgress),
                                        offset = Offset.Zero,
                                        blurRadius = 12f * sinProgress
                                    )
                                } else activeGlowShadow
                            ),
                            modifier = Modifier.graphicsLayer {
                                translationY = floatOffset * density.density
                                scaleX = wordScale
                                scaleY = wordScale
                            }
                        )
                        if (wordIndex < wordData.size - 1) {
                            Text(text = " ", fontSize = textSize.sp)
                        }
                    }
                }
            }

            "METRO_LYRICS" -> {
                // Kinetic Windows Phone / Zune typography
                val styledText = buildAnnotatedString {
                    wordData.forEachIndexed { wordIndex, (wordText, startRel, endRel) ->
                        val hasWordPassed = isActive && lineRelTime >= endRel
                        val isWordActive = isActive && lineRelTime in startRel until endRel

                        val wordAlpha = when {
                            !isActive -> 0.40f
                            hasWordPassed -> 1.0f
                            isWordActive -> 0.95f
                            else -> 0.35f
                        }
                        withStyle(
                            SpanStyle(
                                color = accentColor.copy(alpha = wordAlpha),
                                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                                shadow = if (isWordActive) Shadow(color = accentColor.copy(alpha = 0.7f), blurRadius = 16f) else activeGlowShadow
                            )
                        ) {
                            append(wordText.uppercase())
                        }
                        if (wordIndex < wordData.size - 1) append(" ")
                    }
                }
                Text(
                    text = styledText,
                    fontSize = (textSize * 1.05f).sp,
                    textAlign = textAlign,
                    letterSpacing = 0.5.sp,
                    lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f) * 1.1f).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            "KARAOKE" -> {
                // Sweeping gradient brush with leading edge corona
                val styledText = buildAnnotatedString {
                    wordData.forEachIndexed { wordIndex, (wordText, startRel, endRel) ->
                        val wordDur = (endRel - startRel).coerceAtLeast(1L)
                        val isWordActive = isActive && lineRelTime in startRel until endRel
                        val hasWordPassed = isActive && lineRelTime >= endRel

                        if (isWordActive) {
                            val linProg = ((lineRelTime - startRel).toFloat() / wordDur).coerceIn(0f, 1f)
                            val fillProgress = linProg * linProg * (3f - 2f * linProg)
                            val wordBrush = Brush.horizontalGradient(
                                0.0f to accentColor.copy(alpha = 0.4f),
                                (fillProgress * 0.6f).coerceIn(0f, 1f) to accentColor.copy(alpha = 0.75f),
                                fillProgress to accentColor,
                                (fillProgress + 0.08f).coerceIn(0f, 1f) to accentColor.copy(alpha = 0.5f),
                                1.0f to accentColor.copy(alpha = 0.35f)
                            )
                            withStyle(
                                SpanStyle(
                                    brush = wordBrush,
                                    fontWeight = FontWeight.ExtraBold,
                                    shadow = Shadow(color = accentColor.copy(alpha = 0.6f * fillProgress), blurRadius = 16f * fillProgress)
                                )
                            ) {
                                append(wordText)
                            }
                        } else if (hasWordPassed) {
                            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold, shadow = activeGlowShadow)) {
                                append(wordText)
                            }
                        } else {
                            withStyle(SpanStyle(color = accentColor.copy(alpha = if (isActive) 0.4f else 0.35f), fontWeight = FontWeight.Medium)) {
                                append(wordText)
                            }
                        }
                        if (wordIndex < wordData.size - 1) append(" ")
                    }
                }
                Text(
                    text = styledText,
                    fontSize = textSize.sp,
                    textAlign = textAlign,
                    lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            "SLIDE" -> {
                // Horizontal wipe with breathing sine wave
                val breatheValue = (currentPositionMs % 3000) / 3000f
                val breatheEffect = (sin(breatheValue * PI.toFloat() * 2f) * 0.03f).coerceAtLeast(0f)

                val styledText = buildAnnotatedString {
                    wordData.forEachIndexed { wordIndex, (wordText, startRel, endRel) ->
                        val wordDur = (endRel - startRel).coerceAtLeast(1L)
                        val isWordActive = isActive && lineRelTime in startRel until endRel
                        val hasWordPassed = isActive && lineRelTime >= endRel

                        if (isWordActive) {
                            val fillProg = ((lineRelTime - startRel).toFloat() / wordDur).coerceIn(0f, 1f)
                            val slideBrush = Brush.horizontalGradient(
                                0.0f to accentColor,
                                (fillProg * 0.95f).coerceIn(0f, 1f) to accentColor,
                                fillProg to accentColor.copy(alpha = 0.9f),
                                (fillProg + 0.08f).coerceIn(0f, 1f) to accentColor.copy(alpha = 0.35f),
                                1.0f to accentColor.copy(alpha = 0.35f)
                            )
                            withStyle(
                                SpanStyle(
                                    brush = slideBrush,
                                    fontWeight = FontWeight.ExtraBold,
                                    shadow = Shadow(color = accentColor.copy(alpha = 0.4f + breatheEffect), blurRadius = 14f)
                                )
                            ) {
                                append(wordText)
                            }
                        } else if (hasWordPassed) {
                            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold, shadow = activeGlowShadow)) {
                                append(wordText)
                            }
                        } else {
                            withStyle(SpanStyle(color = accentColor.copy(alpha = 0.35f), fontWeight = FontWeight.Medium)) {
                                append(wordText)
                            }
                        }
                        if (wordIndex < wordData.size - 1) append(" ")
                    }
                }
                Text(
                    text = styledText,
                    fontSize = textSize.sp,
                    textAlign = textAlign,
                    lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            "GLOW" -> {
                // Neon pulse and expanding shadow bloom
                val styledText = buildAnnotatedString {
                    wordData.forEachIndexed { wordIndex, (wordText, startRel, endRel) ->
                        val wordDur = (endRel - startRel).coerceAtLeast(1L)
                        val isWordActive = isActive && lineRelTime in startRel until endRel
                        val hasWordPassed = isActive && lineRelTime >= endRel

                        val fillProg = if (isWordActive) {
                            val lin = ((lineRelTime - startRel).toFloat() / wordDur).coerceIn(0f, 1f)
                            lin * lin * (3f - 2f * lin)
                        } else if (hasWordPassed) 1f else 0f

                        val glowIntensity = fillProg * fillProg
                        val brightness = 0.45f + 0.55f * fillProg
                        val wordColor = if (isWordActive || hasWordPassed) accentColor.copy(alpha = brightness) else accentColor.copy(alpha = 0.35f)

                        val shadow = if (isWordActive && glowIntensity > 0.05f) {
                            Shadow(
                                color = accentColor.copy(alpha = 0.5f + 0.3f * glowIntensity),
                                offset = Offset.Zero,
                                blurRadius = 16f + 12f * glowIntensity
                            )
                        } else activeGlowShadow

                        withStyle(SpanStyle(color = wordColor, fontWeight = if (isWordActive) FontWeight.ExtraBold else FontWeight.Bold, shadow = shadow)) {
                            append(wordText)
                        }
                        if (wordIndex < wordData.size - 1) append(" ")
                    }
                }
                Text(
                    text = styledText,
                    fontSize = textSize.sp,
                    textAlign = textAlign,
                    lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            "FADE" -> {
                // Smooth polynomial Hermite cubic fade
                val styledText = buildAnnotatedString {
                    wordData.forEachIndexed { wordIndex, (wordText, startRel, endRel) ->
                        val wordDur = (endRel - startRel).coerceAtLeast(1L)
                        val isWordActive = isActive && lineRelTime in startRel until endRel
                        val hasWordPassed = isActive && lineRelTime >= endRel

                        val fadeProgress = if (isWordActive) {
                            val lin = ((lineRelTime - startRel).toFloat() / wordDur).coerceIn(0f, 1f)
                            lin * lin * (3f - 2f * lin)
                        } else if (hasWordPassed) 1f else 0f

                        val wordAlpha = when {
                            !isActive -> 0.50f
                            hasWordPassed -> 1.0f
                            isWordActive -> 0.4f + (0.6f * fadeProgress)
                            else -> 0.35f
                        }

                        withStyle(
                            SpanStyle(
                                color = accentColor.copy(alpha = wordAlpha),
                                fontWeight = if (isWordActive) FontWeight.ExtraBold else FontWeight.Bold,
                                shadow = activeGlowShadow
                            )
                        ) {
                            append(wordText)
                        }
                        if (wordIndex < wordData.size - 1) append(" ")
                    }
                }
                Text(
                    text = styledText,
                    fontSize = textSize.sp,
                    textAlign = textAlign,
                    lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            "NONE" -> {
                // Instant static highlight
                Text(
                    text = line.text,
                    fontSize = textSize.sp,
                    color = accentColor.copy(alpha = if (isActive) 1.0f else 0.40f),
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    textAlign = textAlign,
                    lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                    style = TextStyle(shadow = activeGlowShadow),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                // Default: ECHOMUSIC_1 Flagship Fluid Scale & Gradient Sweep
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = horizontalArrangement,
                    verticalArrangement = Arrangement.spacedBy(
                        with(LocalDensity.current) {
                            (textSize * (lineSpacing.coerceAtMost(1.3f) - 1f)).sp.toDp()
                        }
                    )
                ) {
                    wordData.forEachIndexed { index, (wordText, startRel, endRel) ->
                        val wordDur = (endRel - startRel).coerceAtLeast(1L)
                        val progress by animateFloatAsState(
                            targetValue = when {
                                !isActive || lineRelTime < startRel -> 0f
                                lineRelTime >= endRel -> 1f
                                else -> ((lineRelTime - startRel).toFloat() / wordDur).coerceIn(0f, 1f)
                            },
                            animationSpec = tween(durationMillis = 150),
                            label = "wordProgress"
                        )

                        Text(
                            text = wordText,
                            fontSize = textSize.sp,
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    0.0f to accentColor,
                                    (progress - 0.05f).coerceAtLeast(0f) to accentColor,
                                    (progress + 0.05f).coerceAtMost(1f) to accentColor.copy(alpha = 0.45f),
                                    1.0f to accentColor.copy(alpha = 0.45f)
                                ),
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                                textAlign = textAlign,
                                shadow = if (isActive) {
                                    Shadow(
                                        color = accentColor.copy(alpha = 0.55f * progress),
                                        offset = Offset.Zero,
                                        blurRadius = (12f * progress).coerceAtLeast(0.1f)
                                    )
                                } else activeGlowShadow
                            )
                        )
                        if (index != wordData.lastIndex) {
                            Text(
                                text = " ",
                                fontSize = textSize.sp,
                                color = accentColor.copy(alpha = if (lineRelTime >= endRel) 1f else 0.45f),
                                lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp
                            )
                        }
                    }
                }
            }
        }
    }
}
