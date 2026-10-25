package com.uta.wave.ui.player

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
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
import com.uta.wave.sync.WaveSyncManager
import com.uta.wave.player.RepeatMode as PlayerRepeatMode
import com.uta.wave.player.WavePlayerState
import com.uta.wave.ui.components.AmbientCanvas
import com.uta.wave.ui.components.ArtworkThumbnailView
import com.uta.wave.ui.components.AudioDeviceBottomSheet
import com.uta.wave.ui.components.CanvasArtworkPlayer
import com.uta.wave.ui.components.DownloadQualityBottomSheet
import com.uta.wave.ui.components.EqualizerSheet
import com.uta.wave.ui.components.GlassComponent
import com.uta.wave.ui.components.LocalGlassEffectConfig
import com.uta.wave.ui.components.PLAYER_BLUR_MULTIPLIER
import com.uta.wave.ui.components.PlaybackLogsDialog
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

    val syncManager = remember { WaveSyncManager.getInstance(context) }
    val playbackManager = remember { com.uta.wave.player.PlaybackManager.getInstance(context) }
    val roomState by syncManager.roomState.collectAsState()

    // Dialog & Sheet States
    var showLyrics by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSongDetails by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showTrimRingtone by remember { mutableStateOf(false) }
    var showDownloadQuality by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showAudioDevices by remember { mutableStateOf(false) }
    var showPlaybackLogs by remember { mutableStateOf(false) }
    var showListenTogether by remember { mutableStateOf(false) }

    // Modal Sheets and Dialogs Back Handlers
    BackHandler(enabled = showLyrics) { showLyrics = false }
    BackHandler(enabled = showQueue) { showQueue = false }
    BackHandler(enabled = showEqualizer) { showEqualizer = false }
    BackHandler(enabled = showMoreOptions) { showMoreOptions = false }
    BackHandler(enabled = showAudioDevices) { showAudioDevices = false }
    BackHandler(enabled = showSleepTimer) { showSleepTimer = false }
    BackHandler(enabled = showSongDetails) { showSongDetails = false }
    BackHandler(enabled = showPlaylistPicker) { showPlaylistPicker = false }
    BackHandler(enabled = showTrimRingtone) { showTrimRingtone = false }
    BackHandler(enabled = showDownloadQuality) { showDownloadQuality = false }
    BackHandler(enabled = showPlaybackLogs) { showPlaybackLogs = false }
    BackHandler(enabled = showListenTogether) { showListenTogether = false }

    val hasAnyPlayerSheetOpen = showLyrics || showQueue || showEqualizer || showMoreOptions ||
            showAudioDevices || showSleepTimer || showSongDetails || showPlaylistPicker ||
            showTrimRingtone || showDownloadQuality || showPlaybackLogs || showListenTogether

    BackHandler(enabled = !hasAnyPlayerSheetOpen) {
        onCollapse()
    }

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

    // Seek tracking & Progressive seek (+5s incrementally if within 1500ms)
    var lastSeekBackTime by remember { mutableLongStateOf(0L) }
    var consecutiveBackCount by remember { mutableIntStateOf(0) }
    var currentBackSec by remember(settings.seekStepSeconds) { mutableIntStateOf(settings.seekStepSeconds.coerceAtLeast(5)) }

    var lastSeekForwardTime by remember { mutableLongStateOf(0L) }
    var consecutiveForwardCount by remember { mutableIntStateOf(0) }
    var currentForwardSec by remember(settings.seekStepSeconds) { mutableIntStateOf(settings.seekStepSeconds.coerceAtLeast(5)) }

    val handleSeekBackward: () -> Unit = {
        val now = System.currentTimeMillis()
        val baseSec = settings.seekStepSeconds.coerceAtLeast(5)
        val sec = if (settings.seekExtraSeconds && (now - lastSeekBackTime <= 1500L)) {
            consecutiveBackCount += 1
            baseSec + (consecutiveBackCount * 5)
        } else {
            consecutiveBackCount = 0
            baseSec
        }
        lastSeekBackTime = now
        currentBackSec = sec
        val target = (playerState.positionMs - (sec * 1000L)).coerceAtLeast(0L)
        onSeekTo(target)
        seekFeedbackDirection = "-${sec}s"
    }

    val handleSeekForward: () -> Unit = {
        val now = System.currentTimeMillis()
        val baseSec = settings.seekStepSeconds.coerceAtLeast(5)
        val sec = if (settings.seekExtraSeconds && (now - lastSeekForwardTime <= 1500L)) {
            consecutiveForwardCount += 1
            baseSec + (consecutiveForwardCount * 5)
        } else {
            consecutiveForwardCount = 0
            baseSec
        }
        lastSeekForwardTime = now
        currentForwardSec = sec
        val target = (playerState.positionMs + (sec * 1000L)).coerceAtMost(playerState.durationMs)
        onSeekTo(target)
        seekFeedbackDirection = "+${sec}s"
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

    // Swipe-down-to-collapse animation mechanics (EchoMusic / iOS style spring physics)
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 130.dp.toPx() }
    val dismissVelocityThreshold = with(density) { 800.dp.toPx() }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(track.id) {
        dragOffsetY = 0f
    }

    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "fullscreenPlayerSwipeDown"
    )

    val swipeCollapseModifier = Modifier.pointerInput(Unit) {
        val velocityTracker = VelocityTracker()
        detectVerticalDragGestures(
            onDragStart = {
                velocityTracker.resetTracking()
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
            },
            onDragEnd = {
                val velocityY = velocityTracker.calculateVelocity().y
                if (dragOffsetY > dismissThresholdPx || velocityY > dismissVelocityThreshold) {
                    onCollapse()
                } else {
                    dragOffsetY = 0f
                }
            },
            onDragCancel = {
                dragOffsetY = 0f
            }
        )
    }

    val sheetNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (dragOffsetY > 0f && available.y < 0f) {
                    val consumedY = available.y.coerceAtLeast(-dragOffsetY)
                    dragOffsetY += consumedY
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    dragOffsetY += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (available.y > dismissVelocityThreshold || dragOffsetY > dismissThresholdPx) {
                    onCollapse()
                    return available
                } else if (dragOffsetY > 0f) {
                    dragOffsetY = 0f
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val currentDrag = animatedOffsetY
    val progress = (currentDrag / (density.density * 320f)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (!showLyrics) swipeCollapseModifier else Modifier)
            .nestedScroll(sheetNestedScrollConnection)
            .graphicsLayer {
                translationY = currentDrag
                scaleX = 1f - (progress * 0.08f)
                scaleY = 1f - (progress * 0.08f)
                alpha = 1f - (progress * 0.40f)
                shape = RoundedCornerShape(topStart = (progress * 28).dp, topEnd = (progress * 28).dp)
                clip = progress > 0.005f
            }
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
                        )
                        .then(swipeCollapseModifier),
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
                        onSeekBackward = handleSeekBackward,
                        onSeekForward = handleSeekForward,
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
                                        handleSeekBackward()
                                    } else if (isRight) {
                                        handleSeekForward()
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
                        )
                        .then(swipeCollapseModifier),
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
            LyricsSheet(
                track = track,
                currentPositionMs = playerState.positionMs,
                onSeekTo = onSeekTo,
                isPlaying = playerState.isPlaying,
                onTogglePlayPause = onTogglePlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onClose = { showLyrics = false },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            )
        } else if (isAppleMusic) {
            // -------------------------------------------------------------
            // APPLE MUSIC INSPIRED FULLSCREEN PLAYER LAYOUT
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(swipeCollapseModifier)
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: Down Chevron, subtle drag handle, Cast icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(swipeCollapseModifier)
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
                            .width(42.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.45f))
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showListenTogether = true }) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Listen Together",
                                tint = if (roomState != null) com.uta.wave.ui.theme.ElectricViolet else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = {
                            showAudioDevices = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Cast",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(if (roomState != null) RoundedCornerShape(19.dp) else CircleShape)
                                .background(if (roomState != null) com.uta.wave.ui.theme.ElectricViolet.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.16f))
                                .border(
                                    1.dp,
                                    if (roomState != null) com.uta.wave.ui.theme.ElectricViolet.copy(alpha = 0.6f) else Color.Transparent,
                                    if (roomState != null) RoundedCornerShape(19.dp) else CircleShape
                                )
                                .clickable { showListenTogether = true }
                                .padding(horizontal = if (roomState != null) 10.dp else 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Listen Together",
                                    tint = if (roomState != null) com.uta.wave.ui.theme.ElectricViolet else Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                                if (roomState != null) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = roomState?.roomCode ?: "",
                                        color = com.uta.wave.ui.theme.ElectricViolet,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

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
                        .padding(horizontal = if (settings.showSeekButtons) 14.dp else 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (settings.showSeekButtons) {
                        SeekButton(
                            isForward = false,
                            stepSeconds = currentBackSec,
                            isAppleMusic = true,
                            onClick = handleSeekBackward
                        )
                    }

                    // Previous (<<)
                    Box(
                        modifier = Modifier
                            .size(if (settings.showSeekButtons) 52.dp else 60.dp)
                            .clickable(onClick = onPrevious),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.apple_skip_previous),
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(if (settings.showSeekButtons) 40.dp else 46.dp)
                        )
                    }

                    // Play / Pause (|| or ▶)
                    Box(
                        modifier = Modifier
                            .size(if (settings.showSeekButtons) 76.dp else 84.dp)
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
                                modifier = Modifier.size(if (settings.showSeekButtons) 60.dp else 68.dp)
                            )
                        }
                    }

                    // Next (>>)
                    Box(
                        modifier = Modifier
                            .size(if (settings.showSeekButtons) 52.dp else 60.dp)
                            .clickable(onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.apple_skip_next),
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(if (settings.showSeekButtons) 40.dp else 46.dp)
                        )
                    }

                    if (settings.showSeekButtons) {
                        SeekButton(
                            isForward = true,
                            stepSeconds = currentForwardSec,
                            isAppleMusic = true,
                            onClick = handleSeekForward
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
                                    showAudioDevices = true
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
                    .then(swipeCollapseModifier)
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(swipeCollapseModifier),
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
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.45f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showListenTogether = true }) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Listen Together",
                                tint = if (roomState != null) com.uta.wave.ui.theme.ElectricViolet else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = {
                            showAudioDevices = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Cast",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Center Content: Artwork + Single Line Preview
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(swipeCollapseModifier),
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
                        onSeekBackward = handleSeekBackward,
                        onSeekForward = handleSeekForward,
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

                // Playback Controls Row: Scalloped play button + optional Seek buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (settings.showSeekButtons) 12.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (settings.showSeekButtons) {
                        SeekButton(
                            isForward = false,
                            stepSeconds = currentBackSec,
                            isAppleMusic = false,
                            onClick = handleSeekBackward
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(if (settings.showSeekButtons) 50.dp else 56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3D7EC))
                            .clickable(onClick = onPrevious),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color(0xFF3B1E38),
                            modifier = Modifier.size(if (settings.showSeekButtons) 26.dp else 30.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(if (settings.showSeekButtons) 72.dp else 78.dp)
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
                            .size(if (settings.showSeekButtons) 50.dp else 56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3D7EC))
                            .clickable(onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color(0xFF3B1E38),
                            modifier = Modifier.size(if (settings.showSeekButtons) 26.dp else 30.dp)
                        )
                    }

                    if (settings.showSeekButtons) {
                        SeekButton(
                            isForward = true,
                            stepSeconds = currentForwardSec,
                            isAppleMusic = false,
                            onClick = handleSeekForward
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
                showListenTogether = true
            },
            onShowDetails = {
                showSongDetails = true
            },
            onRadio = {
                Toast.makeText(context, "Starting radio station for ${track.title}...", Toast.LENGTH_SHORT).show()
            },
            onAddToPlaylist = {
                showPlaylistPicker = true
            },
            onOpenEqualizer = {
                showEqualizer = true
            },
            onOpenAudioDevices = {
                showAudioDevices = true
            },
            onOpenDiagnostics = {
                showPlaybackLogs = true
            }
        )
    }

    // Modal Bottom Sheet: Now Playing Queue
    if (showQueue) {
        QueueSheet(
            queue = playerState.queue,
            currentIndex = playerState.queueIndex,
            isPlaying = playerState.isPlaying,
            queueTitle = playerState.queueTitle ?: queueTitle,
            isRadioActive = playerState.isRadioActive,
            currentTrack = playerState.currentTrack,
            onSelectTrack = { selected ->
                onSelectQueueTrack?.invoke(selected)
            },
            onReorderQueue = { from, to ->
                playbackManager.reorderQueue(from, to)
            },
            onRemoveFromQueue = { id ->
                playbackManager.removeFromQueue(id)
            },
            onInsertTrackNext = { trackItem ->
                playbackManager.addToQueue(trackItem, insertNext = true)
            },
            onAddToQueue = { trackItem ->
                playbackManager.addToQueue(trackItem, insertNext = false)
            },
            onStartRadio = {
                playbackManager.startRadioFromCurrentTrack()
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

    // Modal Bottom Sheet: Equalizer
    if (showEqualizer) {
        EqualizerSheet(
            onDismiss = { showEqualizer = false }
        )
    }

    // Modal Bottom Sheet: Audio Output Devices
    if (showAudioDevices) {
        AudioDeviceBottomSheet(
            onDismiss = { showAudioDevices = false }
        )
    }

    // Modal Bottom Sheet: Playback Logs & Telemetry HUD
    if (showPlaybackLogs) {
        PlaybackLogsDialog(
            playerState = playerState,
            onDismiss = { showPlaybackLogs = false }
        )
    }

    // Modal Bottom Sheet: Listen Together
    if (showListenTogether) {
        ListenTogetherSheet(
            onDismiss = { showListenTogether = false }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%d:%02d", m, s)
}

@Composable
private fun SeekButton(
    isForward: Boolean,
    stepSeconds: Int,
    isAppleMusic: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isAppleMusic) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color(0xFFF3D7EC).copy(alpha = 0.70f)
    }
    val contentColor = if (isAppleMusic) {
        Color.White.copy(alpha = 0.90f)
    } else {
        Color(0xFF3B1E38)
    }

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = if (isForward) "Seek Forward $stepSeconds seconds" else "Seek Backward $stepSeconds seconds",
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = "${if (isForward) "+" else "-"}${stepSeconds}s",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor
            )
        }
    }
}
