package com.uta.wave.ui.player

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.R
import com.uta.wave.data.canvas.CanvasArtwork
import com.uta.wave.data.canvas.CanvasArtworkProvider
import com.uta.wave.data.downloads.AudioDownloadManager
import com.uta.wave.data.downloads.AudioQuality
import com.uta.wave.data.innertube.LyricLine
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.PlaylistRecord
import com.uta.wave.data.lyrics.LyricsClient
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.player.RepeatMode as PlayerRepeatMode
import com.uta.wave.player.WavePlayerState
import com.uta.wave.ui.components.AmbientCanvas
import com.uta.wave.ui.components.ArtworkThumbnailView
import com.uta.wave.ui.components.CanvasArtworkPlayer
import com.uta.wave.ui.components.DownloadQualityBottomSheet
import com.uta.wave.ui.components.GlassComponent
import com.uta.wave.ui.components.LocalGlassEffectConfig
import com.uta.wave.ui.components.PLAYER_BLUR_MULTIPLIER
import com.uta.wave.ui.components.PlaylistSelectionSheet
import com.uta.wave.ui.components.ScallopedFlowerShape
import com.uta.wave.ui.components.SongDetailsDialog
import com.uta.wave.ui.components.liquidGlass
import com.uta.wave.ui.components.SquigglySlider
import com.uta.wave.ui.components.PlayerSeeker
import com.uta.wave.ui.components.WavyShape
import com.uta.wave.ui.components.swipeToChangeSong
import com.uta.wave.ui.components.TrimRingtoneBottomSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 100% visual and functional replica of Echo Music's Fullscreen Player screen.
 * Supports:
 * - Apple Music Inspired mode (dissolve top artwork, Apple double-chevron glyphs, iOS volume scrub, AirPlay pill)
 * - Wave / Squiggly mode (12-lobed scalloped flower button, pastel lavender badges, bottom 6-item toolbar)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenPlayerSheet(
    playerState: WavePlayerState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    queueTitle: String = "Now Playing",
    onSelectQueueTrack: ((TrackItem) -> Unit)? = null,
    onClearQueue: (() -> Unit)? = null,
    onViewArtist: ((String) -> Unit)? = null,
    onViewAlbum: ((String) -> Unit)? = null,
    playlists: List<PlaylistRecord> = emptyList(),
    onCreatePlaylist: ((String) -> Unit)? = null,
    onAddToPlaylist: ((String, TrackItem) -> Unit)? = null,
    onLaunchAmbientMode: (() -> Unit)? = null
) {
    val track = playerState.currentTrack ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPrefs = remember { UserPreferences.getInstance(context) }
    val settings by userPrefs.settingsFlow.collectAsState()
    val isAppleMusic = settings.appleMusicPlayerEnabled || settings.playerStyle == "APPLE_MUSIC"

    // Dialog & Sheet States
    var showLyrics by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSongDetails by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showTrimRingtone by remember { mutableStateOf(false) }
    var showDownloadQuality by remember { mutableStateOf(false) }

    // Sleep Timer Job
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }

    // Download Management
    val downloadManager = remember { AudioDownloadManager.getInstance(context) }
    val downloadingIds by downloadManager.downloadingIds.collectAsState()
    val isDownloading = downloadingIds.contains(track.id)
    var isDownloaded by remember(track.id) { mutableStateOf(downloadManager.isDownloaded(track.id)) }

    // Synchronized Lyrics Cache for single-line live preview
    val lyricsClient = remember { LyricsClient() }
    var lyricLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isLyricsLoading by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        isLyricsLoading = true
        isDownloaded = downloadManager.isDownloaded(track.id)
        val result = lyricsClient.getLyrics(track.title, track.artist, track.durationSeconds)
        lyricLines = result.lines
        isLyricsLoading = false
    }

    // Live active lyric line
    val activeLyricText = remember(playerState.positionMs, lyricLines) {
        if (lyricLines.isNotEmpty()) {
            val matching = lyricLines.lastOrNull { it.timestampMs <= playerState.positionMs }
            matching?.text?.takeIf { it.isNotBlank() }
        } else null
    }

    // Audio stream volume management
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVol by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol.toFloat())
    }

    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.isEnabledFor(GlassComponent.PLAYER)

    val currentRatio = if (playerState.durationMs > 0) {
        (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val clearArtworkAlpha by animateFloatAsState(
        targetValue = if (showLyrics) 0f else 1f,
        animationSpec = tween(500),
        label = "clearArtworkAlpha"
    )

    // Canvas artwork video state (precedence: rotating thumbnail disables canvas)
    var canvasArtwork by remember(track.id) { mutableStateOf<CanvasArtwork?>(null) }
    LaunchedEffect(track.id, settings.canvasThumbnailAnimation, settings.rotatingThumbnail) {
        if (settings.canvasThumbnailAnimation && !settings.rotatingThumbnail) {
            canvasArtwork = CanvasArtworkProvider.getCanvas(
                context = context,
                mediaId = track.id,
                title = track.title,
                artist = track.artist,
                album = null
            )
        } else {
            canvasArtwork = null
        }
    }

    // Double-tap seek feedback overlay
    var seekFeedbackDirection by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(seekFeedbackDirection) {
        if (seekFeedbackDirection != null) {
            delay(850)
            seekFeedbackDirection = null
        }
    }

    // Wavy cookie play/pause morph & rotation
    val cookieIndent by animateFloatAsState(
        targetValue = if (settings.wavyPlayPause && playerState.isPlaying) 0.14f else 0f,
        animationSpec = tween(400),
        label = "cookieIndent"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "cookieRotationTransition")
    val cookieRotation by if (settings.wavyPlayPause && playerState.isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cookieRotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val defaultPlayPauseShape = if (settings.wavyPlayPause && cookieIndent > 0.001f) {
        WavyShape(sides = 9, indent = cookieIndent, rotationDegrees = cookieRotation)
    } else {
        CircleShape
    }

    val playPauseInteractionSource = remember { MutableInteractionSource() }
    val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
    val playPauseScale by animateFloatAsState(
        targetValue = if (isPlayPausePressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "playPauseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0A12))
    ) {
        if (isAppleMusic) {
            // LAYER 1: Deep atmospheric blurred background (150dp blur)
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(150.dp)
            } else {
                Modifier
            }
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
            )

            // LAYER 2: Apple-Music Top Artwork (or 20s spinning vinyl turntable if rotating thumbnail enabled)
            if (settings.rotatingThumbnail) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.58f)
                        .alpha(clearArtworkAlpha)
                        .padding(top = 40.dp, bottom = 12.dp)
                        .swipeToChangeSong(
                            enabled = settings.enableSwipeToChangeSong,
                            onPrevious = onPrevious,
                            onNext = onNext
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ArtworkThumbnailView(
                        artworkUrl = track.artworkUrl,
                        songId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = null,
                        isPlaying = playerState.isPlaying,
                        isRotatingEnabled = true,
                        isCanvasEnabled = false,
                        onSeekDelta = { deltaMs ->
                            val target = (playerState.positionMs + deltaMs).coerceIn(0L, playerState.durationMs)
                            onSeekTo(target)
                        },
                        onTogglePlayPause = onTogglePlayPause,
                        modifier = Modifier
                            .size(260.dp)
                            .shadow(elevation = 28.dp, shape = CircleShape)
                    )
                }
            } else {
                val layoutDirection = LocalLayoutDirection.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.58f)
                        .alpha(clearArtworkAlpha)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Black,
                                        0.68f to Color.Black,
                                        0.88f to Color.Black.copy(alpha = 0.4f),
                                        1.00f to Color.Transparent
                                    )
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val leftBound = size.width * 0.33f
                                    val rightBound = size.width * 0.67f
                                    val isLeft = (layoutDirection == LayoutDirection.Ltr && offset.x < leftBound) ||
                                            (layoutDirection == LayoutDirection.Rtl && offset.x > rightBound)
                                    val isRight = (layoutDirection == LayoutDirection.Ltr && offset.x > rightBound) ||
                                            (layoutDirection == LayoutDirection.Rtl && offset.x < leftBound)

                                    if (isLeft) {
                                        val target = (playerState.positionMs - 10000L).coerceAtLeast(0L)
                                        onSeekTo(target)
                                        seekFeedbackDirection = "-10s"
                                    } else if (isRight) {
                                        val target = (playerState.positionMs + 10000L).coerceAtMost(playerState.durationMs)
                                        onSeekTo(target)
                                        seekFeedbackDirection = "+10s"
                                    } else {
                                        onTogglePlayPause()
                                    }
                                }
                            )
                        }
                        .swipeToChangeSong(
                            enabled = settings.enableSwipeToChangeSong,
                            onPrevious = onPrevious,
                            onNext = onNext
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Canvas Looping Video overlay
                    if (settings.canvasThumbnailAnimation && canvasArtwork?.preferredAnimationUrl != null) {
                        CanvasArtworkPlayer(
                            primaryUrl = canvasArtwork?.preferredAnimationUrl,
                            fallbackUrl = canvasArtwork?.videoUrl,
                            isPlaying = playerState.isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Seek Feedback Overlay (-10s / +10s)
                    AnimatedVisibility(
                        visible = seekFeedbackDirection != null,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(250)),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        val direction = seekFeedbackDirection
                        if (direction != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (direction.startsWith("-")) Icons.Default.FastRewind else Icons.Default.FastForward,
                                        contentDescription = direction,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = direction,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // LAYER 3: Dark contrast scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.50f)
                            )
                        )
                    )
            )
        } else {
            // Standard / Dynamic Ambient Glow
            AmbientCanvas(
                artworkUrl = track.artworkUrl,
                isPlaying = playerState.isPlaying
            )
        }

        if (showLyrics) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showLyrics = false }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Close Lyrics",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = "LYRICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 11.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.Transparent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LyricsSheet(
                    track = track,
                    currentPositionMs = playerState.positionMs,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (isAppleMusic) {
            // -------------------------------------------------------------
            // APPLE MUSIC INSPIRED FULLSCREEN PLAYER LAYOUT
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: Down Chevron, subtle drag handle, Cast icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                    )

                    IconButton(onClick = {
                        Toast.makeText(context, "Ready to cast UTA-wave audio", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Cast",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 1. Live Synced Lyric Preview Line (Above Title)
                if (!activeLyricText.isNullOrBlank()) {
                    Text(
                        text = activeLyricText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = Color.White.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable { showLyrics = true }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 2. Track Title & Artist with [E] Explicit Badge + Right Actions (⋮, ♡)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = "E",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable { showMoreOptions = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable(onClick = onToggleFavorite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFEF4444) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Player Seeker & Timestamps
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    PlayerSeeker(
                        sliderStyle = settings.sliderStyle,
                        progress = currentRatio,
                        isPlaying = playerState.isPlaying,
                        onSeek = { ratio ->
                            val targetMs = (ratio * playerState.durationMs).toLong()
                            onSeekTo(targetMs)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        activeColor = Color.White.copy(alpha = 0.9f),
                        inactiveColor = Color.White.copy(alpha = 0.22f),
                        thumbColor = Color.White,
                        enableHaptics = settings.enableSliderHaptics
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(playerState.positionMs),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = formatMs(playerState.durationMs),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Apple Playback Transport Controls (<< , || / ▶ , >>)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous (<<)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clickable(onClick = onPrevious),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.apple_skip_previous),
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    // Play / Pause (|| or ▶)
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playerState.isBuffering || playerState.isResolving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(if (playerState.isPlaying) R.drawable.pause_applemusic else R.drawable.play_applemusic),
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }

                    // Next (>>)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clickable(onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.apple_skip_next),
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                // 5. On-Screen Volume Slider Row
                if (!settings.hideVolumeSlider) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = "Volume Down",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                        Slider(
                            value = currentVol,
                            onValueChange = { newVol ->
                                currentVol = newVol
                                val step = (newVol * maxVol).toInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, step, 0)
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Transparent,
                                activeTrackColor = Color.White.copy(alpha = 0.85f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                            )
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume Up",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Bottom Action Bar: Queue, Segmented Pill [ 🎛️ | ⏱ ], and Lyrics [ 💬" ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (showQueue) Color(0xFFDEC5DE) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Center Segmented Capsule Pill
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Audio Output: Phone Speaker / Bluetooth", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speaker,
                                    contentDescription = "Audio Route",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(Color.White.copy(alpha = 0.25f))
                            )
                            IconButton(
                                onClick = { showSleepTimer = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerJob?.isActive == true) Color(0xFFDEC5DE) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            painter = painterResource(R.drawable.apple_lyrics),
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) Color(0xFFDEC5DE) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        } else {
            // -------------------------------------------------------------
            // CLASSIC / WAVE FULLSCREEN PLAYER LAYOUT
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFFDEC5DE)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = queueTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = {
                        Toast.makeText(context, "Ready to cast UTA-wave audio", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Cast",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Center Content: Artwork + Single Line Preview
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ArtworkThumbnailView(
                        artworkUrl = track.artworkUrl,
                        songId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = null,
                        isPlaying = playerState.isPlaying,
                        isRotatingEnabled = settings.rotatingThumbnail,
                        isCanvasEnabled = settings.canvasThumbnailAnimation,
                        onSeekDelta = { deltaMs ->
                            val target = (playerState.positionMs + deltaMs).coerceIn(0L, playerState.durationMs)
                            onSeekTo(target)
                        },
                        onTogglePlayPause = onTogglePlayPause,
                        modifier = Modifier
                            .size(300.dp)
                            .shadow(
                                elevation = 28.dp,
                                shape = if (settings.rotatingThumbnail) CircleShape else RoundedCornerShape(26.dp)
                            )
                            .swipeToChangeSong(
                                enabled = settings.enableSwipeToChangeSong,
                                onPrevious = onPrevious,
                                onNext = onNext
                            )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showLyrics = true }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeLyricText
                                ?: if (isLyricsLoading) "Loading synchronized lyrics..." else "Tap for full synchronized lyrics",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = if (activeLyricText != null) Color.White else Color.White.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Title, Artist & Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            ),
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3D7EC))
                                .clickable {
                                    if (isDownloaded) {
                                        downloadManager.deleteDownload(track.id)
                                        isDownloaded = false
                                        Toast.makeText(context, "Download removed", Toast.LENGTH_SHORT).show()
                                    } else if (!isDownloading) {
                                        showDownloadQuality = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF3B1E38)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.FileDownload,
                                    contentDescription = if (isDownloaded) "Downloaded" else "Download",
                                    tint = Color(0xFF3B1E38),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3D7EC))
                                .clickable(onClick = onToggleFavorite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFE53935) else Color(0xFF3B1E38),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Seeker
                Column(modifier = Modifier.fillMaxWidth()) {
                    PlayerSeeker(
                        sliderStyle = settings.sliderStyle,
                        progress = currentRatio,
                        isPlaying = playerState.isPlaying,
                        onSeek = { ratio ->
                            val targetMs = (ratio * playerState.durationMs).toLong()
                            onSeekTo(targetMs)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        activeColor = Color(0xFFF3D7EC),
                        inactiveColor = Color.White.copy(alpha = 0.22f),
                        thumbColor = Color(0xFFF3D7EC),
                        enableHaptics = settings.enableSliderHaptics
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(playerState.positionMs),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatMs(playerState.durationMs),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playback Controls Row: Scalloped play button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3D7EC))
                            .clickable(onClick = onPrevious),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color(0xFF3B1E38),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .graphicsLayer {
                                scaleX = playPauseScale
                                scaleY = playPauseScale
                            }
                            .clip(defaultPlayPauseShape)
                            .background(Color(0xFFF3D7EC))
                            .clickable(
                                interactionSource = playPauseInteractionSource,
                                indication = null,
                                onClick = onTogglePlayPause
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playerState.isBuffering || playerState.isResolving) {
                            CircularProgressIndicator(
                                color = Color(0xFF3B1E38),
                                modifier = Modifier.size(34.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = Color(0xFF3B1E38),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3D7EC))
                            .clickable(onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color(0xFF3B1E38),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom 6-Item Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (showQueue) Color(0xFFF3D7EC) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { showSleepTimer = true }) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerJob?.isActive == true) Color(0xFFF3D7EC) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) Color(0xFFF3D7EC) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.isShuffle) Color(0xFFF3D7EC) else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = when (playerState.repeatMode) {
                                PlayerRepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = when (playerState.repeatMode) {
                                PlayerRepeatMode.OFF -> Color.White.copy(alpha = 0.45f)
                                else -> Color(0xFFF3D7EC)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3D7EC))
                            .clickable { showMoreOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color(0xFF3B1E38),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }



    // Modal Bottom Sheet: More Options
    if (showMoreOptions) {
        PlayerMoreOptionsSheet(
            track = track,
            isDownloaded = isDownloaded,
            isLyricsVisible = showLyrics,
            onDismiss = { showMoreOptions = false },
            onViewArtist = { artistName ->
                onViewArtist?.invoke(artistName)
            },
            onViewAlbum = { albumName ->
                onViewAlbum?.invoke(albumName)
            },
            onAddToLibrary = {
                onToggleFavorite()
                Toast.makeText(context, "Added to library", Toast.LENGTH_SHORT).show()
            },
            onCast = {
                Toast.makeText(context, "Ready to cast UTA-wave audio", Toast.LENGTH_SHORT).show()
            },
            onToggleAmbientMode = {
                showMoreOptions = false
                onLaunchAmbientMode?.invoke()
            },
            onToggleLyrics = {
                showLyrics = !showLyrics
            },
            onClearQueue = {
                onClearQueue?.invoke()
                Toast.makeText(context, "Queue cleared", Toast.LENGTH_SHORT).show()
            },
            onToggleDownload = {
                if (isDownloaded) {
                    downloadManager.deleteDownload(track.id)
                    isDownloaded = false
                    Toast.makeText(context, "Download removed", Toast.LENGTH_SHORT).show()
                } else {
                    showDownloadQuality = true
                }
            },
            onSetRingtone = {
                showTrimRingtone = true
            },
            onListenTogether = {
                Toast.makeText(context, "Listen Together room created: #UTA-WAVE-SYNC", Toast.LENGTH_SHORT).show()
            },
            onShowDetails = {
                showSongDetails = true
            },
            onRadio = {
                Toast.makeText(context, "Starting radio station for ${track.title}...", Toast.LENGTH_SHORT).show()
            },
            onAddToPlaylist = {
                showPlaylistPicker = true
            }
        )
    }

    // Modal Bottom Sheet: Now Playing Queue
    if (showQueue) {
        QueueSheet(
            queue = playerState.queue,
            currentIndex = playerState.queueIndex,
            isPlaying = playerState.isPlaying,
            onSelectTrack = { selected ->
                onSelectQueueTrack?.invoke(selected)
            },
            onClearQueue = {
                onClearQueue?.invoke()
                showQueue = false
            },
            onDismiss = { showQueue = false }
        )
    }

    // Dialog: Sleep Timer
    if (showSleepTimer) {
        SleepTimerDialog(
            onDismiss = { showSleepTimer = false },
            onSetTimerMinutes = { minutes ->
                sleepTimerJob?.cancel()
                if (minutes > 0) {
                    sleepTimerJob = scope.launch {
                        delay(minutes * 60 * 1000L)
                        if (playerState.isPlaying) {
                            onTogglePlayPause()
                        }
                    }
                }
                showSleepTimer = false
            }
        )
    }

    // Dialog: Song Details
    if (showSongDetails) {
        SongDetailsDialog(
            track = track,
            durationMs = playerState.durationMs,
            onDismiss = { showSongDetails = false }
        )
    }

    // Bottom Sheet: Add to Playlist
    if (showPlaylistPicker) {
        PlaylistSelectionSheet(
            track = track,
            playlists = playlists,
            onAddToPlaylist = { playlistId ->
                onAddToPlaylist?.invoke(playlistId, track)
                Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
            },
            onCreatePlaylist = { playlistName ->
                onCreatePlaylist?.invoke(playlistName)
            },
            onDismiss = { showPlaylistPicker = false }
        )
    }

    // Modal Bottom Sheet: Trim Ringtone
    if (showTrimRingtone) {
        TrimRingtoneBottomSheet(
            track = track,
            durationSeconds = (playerState.durationMs / 1000).toInt(),
            onDismiss = { showTrimRingtone = false }
        )
    }

    // Modal Bottom Sheet: Download Quality Selection
    if (showDownloadQuality) {
        DownloadQualityBottomSheet(
            track = track,
            onSelectQuality = { quality ->
                Toast.makeText(context, "Downloading \"${track.title}\" (${quality.badge})...", Toast.LENGTH_SHORT).show()
                downloadManager.downloadTrack(track, quality) { success ->
                    if (success) {
                        isDownloaded = true
                        Toast.makeText(context, "Downloaded \"${track.title}\" (${quality.badge})", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showDownloadQuality = false }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%d:%02d", m, s)
}
