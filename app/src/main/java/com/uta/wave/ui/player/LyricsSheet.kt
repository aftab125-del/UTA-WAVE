package com.uta.wave.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.LyricLine
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.lyrics.LyricsClient
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.ui.components.swipeToChangeSong
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.TextMuted

@Composable
fun LyricsSheet(
    track: TrackItem,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    isPlaying: Boolean = true,
    onTogglePlayPause: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences.getInstance(context) }
    val settings by userPrefs.settingsFlow.collectAsState()

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

    // Auto-scroll centering
    LaunchedEffect(activeIndex, settings.lyricsScroll) {
        if (settings.lyricsScroll && activeIndex >= 0 && isSynced && lines.isNotEmpty()) {
            try {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -260
                )
            } catch (e: Exception) {
                listState.scrollToItem(activeIndex)
            }
        }
    }

    val currentAccent = remember(settings.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(settings.accentColorHex))
        } catch (e: Exception) {
            Color(0xFFF2AEC7)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .swipeToChangeSong(
                enabled = settings.swipeLyrics,
                onPrevious = onPrevious,
                onNext = onNext
            )
    ) {
        // -------------------------------------------------------------
        // Interactive Top Header
        // -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close Chevron Button
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Close Lyrics",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(30.dp)
                )
            }

            // Center Track Info & Optional Thumbnail with Play/Pause
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            ) {
                if (settings.enableLyricsThumbnailPlayPause) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF221A26))
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Play/Pause Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.38f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Sync Badge indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isSynced) "SYNCED" else "PLAIN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    ),
                    color = if (isSynced) currentAccent else Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // -------------------------------------------------------------
        // Main Lyrics Content
        // -------------------------------------------------------------
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = currentAccent)
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
                        contentPadding = PaddingValues(vertical = 100.dp, horizontal = 12.dp),
                        horizontalAlignment = when (settings.lyricsPosition) {
                            "CENTER" -> Alignment.CenterHorizontally
                            "RIGHT" -> Alignment.End
                            else -> Alignment.Start
                        }
                    ) {
                        itemsIndexed(
                            items = lines,
                            key = { index, line -> "$index-${line.timestampMs}" }
                        ) { index, line ->
                            val nextTimestamp = lines.getOrNull(index + 1)?.timestampMs

                            LyricsLineRenderer(
                                line = line,
                                index = index,
                                activeIndex = activeIndex,
                                nextTimestampMs = nextTimestamp,
                                currentPositionMs = currentPositionMs,
                                isSynced = isSynced,
                                animationStyle = settings.lyricsAnimationStyle,
                                position = settings.lyricsPosition,
                                textSize = settings.lyricsTextSize,
                                lineSpacing = settings.lyricsLineSpacing,
                                appleMusicBlur = settings.appleMusicLyricsBlur,
                                standardBlur = settings.lyricsStandardBlur,
                                glowEffect = settings.lyricsGlowEffect,
                                accentColor = Color.White,
                                onClick = {
                                    if (settings.lyricsClick && isSynced) {
                                        onSeekTo(line.timestampMs)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
