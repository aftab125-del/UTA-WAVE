package com.uta.wave.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.LyricLine
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.lyrics.LyricsClient
import com.uta.wave.player.WavePlayerState
import com.uta.wave.ui.components.AmbientCanvas
import com.uta.wave.ui.components.ScallopedFlowerShape
import com.uta.wave.ui.components.SquigglySlider
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * 100% visual and functional replica of Echo Music's Ambient Mode.
 * Features:
 * - Edge-to-edge cinematic dynamic mesh glow (AmbientCanvas)
 * - Auto-scrolling synchronized kinetic typography lyrics
 * - Vertical Ambient Mode (Portrait) & Split-Screen Ambient Mode (Landscape / Wide)
 * - Auto-hiding floating translucent playback controls (fades after 4s inactivity)
 * - Toggle to show/hide album artwork (when hidden, lyrics center in a 65% width block)
 * - Tap anywhere to toggle controls / Back button overlay to exit
 */
@Composable
fun AmbientModeScreen(
    track: TrackItem,
    playerState: WavePlayerState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isArtworkVisible by remember { mutableStateOf(true) }

    // Auto-hide controls after 4 seconds of inactivity if playing
    LaunchedEffect(showControls, lastInteractionTime, playerState.isPlaying) {
        if (showControls && playerState.isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Lyrics resolution
    val lyricsClient = remember { LyricsClient() }
    var lyricLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isLyricsSynced by remember { mutableStateOf(false) }
    var isLyricsLoading by remember { mutableStateOf(true) }

    val lyricsListState = rememberLazyListState()

    LaunchedEffect(track.id) {
        isLyricsLoading = true
        val result = lyricsClient.getLyrics(track.title, track.artist, track.durationSeconds)
        lyricLines = result.lines
        isLyricsSynced = result.isSynced
        isLyricsLoading = false
    }

    // Active lyric index based on player position
    val activeIndex = remember(lyricLines, playerState.positionMs, isLyricsSynced) {
        if (!isLyricsSynced || lyricLines.isEmpty()) -1
        else {
            val idx = lyricLines.indexOfLast { it.timestampMs <= playerState.positionMs }
            if (idx >= 0) idx else 0
        }
    }

    // Auto-scroll lyrics to keep active line centered
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && isLyricsSynced) {
            lyricsListState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = -220
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A060E))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                        lastInteractionTime = System.currentTimeMillis()
                    }
                )
            }
    ) {
        // 1. Cinematic Ambient Dynamic Mesh Glow
        AmbientCanvas(
            artworkUrl = track.artworkUrl,
            isPlaying = playerState.isPlaying
        )

        // 2. Main Content (Adapts to orientation)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                // Split-Screen Ambient Mode (Landscape / Wide)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Artwork + Title + Artist
                    if (isArtworkVisible) {
                        Column(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .shadow(28.dp, RoundedCornerShape(26.dp))
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(Color(0xFF1E1724))
                            ) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right Column: Synchronized Kinetic Lyrics
                    Box(
                        modifier = Modifier
                            .weight(if (isArtworkVisible) 0.58f else 1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        AmbientLyricsColumn(
                            lyricLines = lyricLines,
                            activeIndex = activeIndex,
                            isSynced = isLyricsSynced,
                            isLoading = isLyricsLoading,
                            listState = lyricsListState,
                            onSeekTo = onSeekTo,
                            modifier = Modifier.fillMaxWidth(if (!isArtworkVisible) 0.65f else 1f)
                        )
                    }
                }
            } else {
                // Vertical Ambient Mode (Portrait)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Floating Artwork (if enabled)
                    if (isArtworkVisible) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .shadow(24.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1E1724))
                        ) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Spacer(modifier = Modifier.height(50.dp))
                    }

                    // Auto-scrolling Kinetic Lyrics Column
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(if (!isArtworkVisible) 0.70f else 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AmbientLyricsColumn(
                            lyricLines = lyricLines,
                            activeIndex = activeIndex,
                            isSynced = isLyricsSynced,
                            isLoading = isLyricsLoading,
                            listState = lyricsListState,
                            onSeekTo = onSeekTo,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // 3. Top Action Bar Overlay: Back Button & Artwork Toggle
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit / Back Button
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit Ambient Mode",
                        tint = Color.White
                    )
                }

                // Toggle Artwork Visibility Button
                IconButton(
                    onClick = {
                        isArtworkVisible = !isArtworkVisible
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isArtworkVisible) Icons.Default.Image else Icons.Outlined.Image,
                        contentDescription = "Toggle Artwork",
                        tint = if (isArtworkVisible) Color(0xFFF3D7EC) else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // 4. Floating Glass Controls Bar Overlay at the Bottom
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seeker Bar
                val currentRatio = if (playerState.durationMs > 0) {
                    (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                SquigglySlider(
                    progress = currentRatio,
                    isPlaying = playerState.isPlaying,
                    onSeek = { ratio ->
                        val targetMs = (ratio * playerState.durationMs).toLong()
                        onSeekTo(targetMs)
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    activeColor = Color(0xFFF3D7EC),
                    inactiveColor = Color.White.copy(alpha = 0.22f),
                    thumbColor = Color(0xFFF3D7EC)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Translucent Floating Pill with Playback Controls
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Track
                    IconButton(
                        onClick = {
                            onPrevious()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Scalloped Play/Pause Button in Pastel Lavender (#F3D7EC)
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(ScallopedFlowerShape(lobes = 12, scallopDepthRatio = 0.12f))
                            .background(Color(0xFFF3D7EC))
                            .clickable {
                                onTogglePlayPause()
                                lastInteractionTime = System.currentTimeMillis()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (playerState.isBuffering || playerState.isResolving) {
                            CircularProgressIndicator(
                                color = Color(0xFF3B1E38),
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = Color(0xFF3B1E38),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Next Track
                    IconButton(
                        onClick = {
                            onNext()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientLyricsColumn(
    lyricLines: List<LyricLine>,
    activeIndex: Int,
    isSynced: Boolean,
    isLoading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFF3D7EC), strokeWidth = 2.5.dp)
            }
        }

        lyricLines.isEmpty() -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = "Lyrics unavailable for this track",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier,
                contentPadding = PaddingValues(vertical = 140.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lyricLines) { index, line ->
                    val isActive = index == activeIndex

                    val animatedScale by animateFloatAsState(
                        targetValue = if (isActive) 1.15f else 1.0f,
                        animationSpec = tween(300),
                        label = "ambientLyricScale"
                    )

                    val alpha = if (isSynced) {
                        if (isActive) 1.0f else 0.28f
                    } else 0.85f

                    val textShadow = if (isActive) {
                        Shadow(
                            color = Color.White.copy(alpha = 0.55f),
                            blurRadius = 18f
                        )
                    } else null

                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = if (isActive) 22.sp else 17.sp,
                            shadow = textShadow
                        ),
                        color = Color.White.copy(alpha = alpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(animatedScale)
                            .clickable {
                                if (isSynced) {
                                    onSeekTo(line.timestampMs)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
