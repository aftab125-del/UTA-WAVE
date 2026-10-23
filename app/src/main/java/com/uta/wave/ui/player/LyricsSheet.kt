package com.uta.wave.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.innertube.LyricLine
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.lyrics.LyricsClient
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.TextMuted

@Composable
fun LyricsSheet(
    track: TrackItem,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lyricsClient = remember { LyricsClient() }
    var lines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isSynced by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(track.id) {
        isLoading = true
        val result = lyricsClient.getLyrics(track.title, track.artist, track.durationSeconds)
        lines = result.lines
        isSynced = result.isSynced
        isLoading = false
    }

    val activeIndex = remember(lines, currentPositionMs) {
        if (!isSynced || lines.isEmpty()) -1
        else {
            val idx = lines.indexOfLast { it.timestampMs <= currentPositionMs }
            if (idx >= 0) idx else 0
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && isSynced) {
            listState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = -300
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(color = ElectricViolet)
            }
            lines.isEmpty() -> {
                Text(
                    text = "No lyrics available for this track",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 120.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(lines) { index, line ->
                        val isActive = index == activeIndex

                        val animatedScale by animateFloatAsState(
                            targetValue = if (isActive) 1.12f else 1.0f,
                            animationSpec = tween(250),
                            label = "lyricsScale"
                        )

                        val alpha = if (isSynced) {
                            if (isActive) 1.0f else 0.35f
                        } else 0.85f

                        val textShadow = if (isActive) {
                            Shadow(
                                color = Color.White.copy(alpha = 0.6f),
                                blurRadius = 16f
                            )
                        } else null

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable {
                                    if (isSynced) {
                                        onSeekTo(line.timestampMs)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = line.text,
                                color = Color.White.copy(alpha = alpha),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 20.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    shadow = textShadow,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.scale(animatedScale)
                            )
                        }
                    }
                }
            }
        }
    }
}
