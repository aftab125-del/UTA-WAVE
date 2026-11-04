package com.uta.wave.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.account.DesktopUserProfile
import com.uta.wave.desktop.data.DesktopLibrary
import com.uta.wave.desktop.data.DesktopMusicService
import kotlinx.coroutines.launch

// Color Tokens matching mobile theme
private val CardBorderColor = Color(0xFF262035)
private val CardSurfaceColor = Color(0xFF140F1E)
private val ElectricViolet = Color(0xFF8B5CF6)
private val SoftViolet = Color(0xFFC084FC)
private val TextPrimary = Color(0xFFF3F4F6)
private val TextSecondary = Color(0xFF9CA3AF)
private val TextMuted = Color(0xFF6B7280)

/**
 * Authentic UTA-WAVE Home Screen for Windows Desktop.
 *
 * Full parity with mobile `HomeScreen.kt`:
 * 1. Horizontal Mood & Vibe filter chips.
 * 2. Featured Mix Hero Carousel with gradient scrim and Play FAB.
 * 3. Station / Listen Together grouped shelf.
 * 4. Music Videos For You with Verified Artist badge.
 * 5. Speed Dial 3x3 square grid with frosted play buttons.
 * 6. "Your YouTube Playlists" rich gradient cards.
 * 7. "Similar to [Artist]" recommendation carousels.
 * 8. "Mood and Genres" multi-column grid.
 */
@Composable
fun DesktopHomeScreen(
    musicService: DesktopMusicService,
    library: DesktopLibrary,
    userProfile: DesktopUserProfile,
    currentTrackId: String?,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onMoreClick: ((TrackItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var trendingTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var moodTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var similarTracks1 by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var similarTracks2 by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val moods = listOf(
        "Romance", "Relax", "Feel good", "Energise", "Workout",
        "Commute", "Party", "Focus", "Sad", "Sleep"
    )

    val genres = listOf(
        Pair("African", Color(0xFFFF5722)),
        Pair("Pop", Color(0xFFE91E63)),
        Pair("Hindi", Color(0xFFFF9800)),
        Pair("2000s", Color(0xFF9C27B0)),
        Pair("Folk and acoustic", Color(0xFF4CAF50)),
        Pair("Country and Americana", Color(0xFF795548)),
        Pair("1990s", Color(0xFF00BCD4)),
        Pair("1970s", Color(0xFFFFC107)),
        Pair("Rock", Color(0xFFF44336)),
        Pair("Hip Hop", Color(0xFF673AB7))
    )

    // Initial load: trending charts & recommendations
    LaunchedEffect(Unit) {
        if (trendingTracks.isEmpty()) {
            isLoading = true
            val trending = musicService.getTrending()
            trendingTracks = trending
            isLoading = false

            // Load similar artist tracks in background
            if (trending.isNotEmpty()) {
                val artist1 = trending.firstOrNull()?.artist ?: "The Weeknd"
                val artist2 = trending.getOrNull(4)?.artist ?: "Dua Lipa"
                scope.launch {
                    similarTracks1 = musicService.getArtistHits(artist1)
                }
                scope.launch {
                    similarTracks2 = musicService.getArtistHits(artist2)
                }
            }
        }
    }

    fun selectMood(mood: String) {
        if (selectedMood == mood) {
            selectedMood = null
            moodTracks = emptyList()
        } else {
            selectedMood = mood
            isLoading = true
            scope.launch {
                moodTracks = musicService.getMoodSongs(mood)
                isLoading = false
            }
        }
    }

    val activeList = if (selectedMood != null && moodTracks.isNotEmpty()) moodTracks else trendingTracks

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp)
    ) {
        // -------------------------------------------------------------
        // 1. MOOD & VIBE FILTER CHIPS ROW
        // -------------------------------------------------------------
        item {
            LazyRow(
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(moods) { mood ->
                    val isMoodActive = selectedMood == mood
                    val chipBg by animateColorAsState(
                        if (isMoodActive) ElectricViolet else Color(0xFF1E1728)
                    )
                    val chipText by animateColorAsState(
                        if (isMoodActive) Color.White else TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(chipBg)
                            .border(
                                width = 1.dp,
                                color = if (isMoodActive) ElectricViolet else CardBorderColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { selectMood(mood) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = mood,
                            color = chipText,
                            fontSize = 13.sp,
                            fontWeight = if (isMoodActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricViolet)
                }
            }
        } else {
            // -------------------------------------------------------------
            // 2. FEATURED HERO CAROUSEL (Top 5 Active Tracks)
            // -------------------------------------------------------------
            val heroItems = activeList.take(5)
            if (heroItems.isNotEmpty()) {
                item {
                    val pagerState = rememberPagerState(pageCount = { heroItems.size })

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        HorizontalPager(
                            state = pagerState,
                            pageSpacing = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        ) { page ->
                            val track = heroItems[page]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(12.dp, RoundedCornerShape(24.dp))
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(CardSurfaceColor)
                                    .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { onPlayTrack(track, activeList) }
                            ) {
                                DesktopNetworkImage(
                                    url = track.artworkUrl,
                                    contentDescription = track.title,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Dark Cinematic Gradient Scrim
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color(0xB3150F1C),
                                                    Color(0xF2100A15)
                                                )
                                            )
                                        )
                                )

                                // Pill Tag in Top Left
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = selectedMood?.uppercase()?.let { "$it MIX" } ?: "FEATURED MIX",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = ElectricViolet
                                    )
                                }

                                // 3-Dots Action Button in Top Right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                        .clickable { onMoreClick?.invoke(track) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Bottom Track Information & Play FAB
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = track.artist,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SoftViolet,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .shadow(10.dp, CircleShape, spotColor = ElectricViolet)
                                            .clip(CircleShape)
                                            .background(ElectricViolet),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Pager Indicator Dots
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(heroItems.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (isSelected) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) ElectricViolet else Color.White.copy(alpha = 0.25f))
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3 & 4. STATION / LISTEN TOGETHER + MUSIC VIDEOS CARDS (Responsive Row)
            // -------------------------------------------------------------
            val stationTracks = activeList.drop(5).take(4)
            val videoTracks = activeList.drop(9).take(4)

            if (stationTracks.isNotEmpty() || videoTracks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 3. Station Card
                        if (stationTracks.isNotEmpty()) {
                            GroupedTrackCard(
                                title = "STATION / LISTEN TOGETHER",
                                tracks = stationTracks,
                                currentTrackId = currentTrackId,
                                isVideoSection = false,
                                onPlayTrack = { track -> onPlayTrack(track, activeList) },
                                onPlayAll = { onPlayTrack(stationTracks.first(), activeList) },
                                onMoreClick = onMoreClick,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 4. Music Videos For You Card
                        if (videoTracks.isNotEmpty()) {
                            GroupedTrackCard(
                                title = "MUSIC VIDEOS FOR YOU",
                                tracks = videoTracks,
                                currentTrackId = currentTrackId,
                                isVideoSection = true,
                                onPlayTrack = { track -> onPlayTrack(track, activeList) },
                                onPlayAll = { onPlayTrack(videoTracks.first(), activeList) },
                                onMoreClick = onMoreClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. "⚡ SPEED DIAL" 3-Column Square Grid
            // -------------------------------------------------------------
            val speedDialItems = activeList.drop(13).take(9).ifEmpty { activeList.take(9) }
            if (speedDialItems.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "SPEED DIAL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val chunked = speedDialItems.chunked(3)
                        chunked.forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(CardSurfaceColor)
                                            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable { onPlayTrack(item, activeList) }
                                    ) {
                                        DesktopNetworkImage(
                                            url = item.artworkUrl,
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Dark scrim
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.35f))
                                        )

                                        // Top-Right 3-Dots Menu Button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .clickable { onMoreClick?.invoke(item) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Song options",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Centered Frosted Glass Play Button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        // Bottom Song Title Overlay
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .fillMaxWidth()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color(0xCC000000))
                                                    )
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 6. "YOUR YOUTUBE PLAYLISTS" Rich Gradient Cards
            // -------------------------------------------------------------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ElectricViolet.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your YouTube playlists",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Liked Music Card (Hot Pink to Deep Violet)
                        item {
                            PlaylistGradientCard(
                                title = "Liked music",
                                subtitle = "Auto playlist • ${library.likedSongs.size} songs",
                                gradient = listOf(Color(0xFFFF2E93), Color(0xFF7928CA)),
                                icon = Icons.Default.Favorite,
                                onClick = {
                                    if (library.likedSongs.isNotEmpty()) {
                                        onPlayTrack(library.likedSongs.first(), library.likedSongs)
                                    }
                                }
                            )
                        }

                        // 2. Sounds from Shorts Card (Amber to Coral)
                        item {
                            PlaylistGradientCard(
                                title = "Sounds from Shorts",
                                subtitle = "Auto playlist • Trending hits",
                                gradient = listOf(Color(0xFFFFB703), Color(0xFFFF006E)),
                                icon = Icons.Default.MusicNote,
                                onClick = {
                                    if (activeList.isNotEmpty()) {
                                        onPlayTrack(activeList.random(), activeList)
                                    }
                                }
                            )
                        }

                        // 3. Episodes for Later Card (Indigo to Cyan)
                        item {
                            PlaylistGradientCard(
                                title = "Episodes for Later",
                                subtitle = "Saved audio episodes",
                                gradient = listOf(Color(0xFF3A0CA3), Color(0xFF4CC9F0)),
                                icon = Icons.Default.Bookmark,
                                onClick = {}
                            )
                        }

                        // 4. UTA-wave Favorites Card (Violet to Royal Blue)
                        item {
                            PlaylistGradientCard(
                                title = "UTA-wave Favorites",
                                subtitle = "${library.likedSongs.size} custom tracks",
                                gradient = listOf(ElectricViolet, Color(0xFF4361EE)),
                                icon = Icons.Default.Star,
                                onClick = {
                                    if (library.likedSongs.isNotEmpty()) {
                                        onPlayTrack(library.likedSongs.first(), library.likedSongs)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 7. "SIMILAR TO [ARTIST 1]" & "[ARTIST 2]" Recommendation Carousels
            // -------------------------------------------------------------
            val artist1Name = activeList.firstOrNull()?.artist ?: "The Weeknd"
            val artist1Tracks = if (similarTracks1.isNotEmpty()) similarTracks1 else activeList.drop(2).take(8)

            if (artist1Tracks.isNotEmpty()) {
                item {
                    SimilarArtistCarousel(
                        artistName = artist1Name,
                        tracks = artist1Tracks,
                        onPlayTrack = { track -> onPlayTrack(track, artist1Tracks) },
                        onMoreClick = onMoreClick
                    )
                }
            }

            val artist2Name = activeList.getOrNull(4)?.artist ?: "Dua Lipa"
            val artist2Tracks = if (similarTracks2.isNotEmpty()) similarTracks2 else activeList.drop(6).take(8)

            if (artist2Tracks.isNotEmpty()) {
                item {
                    SimilarArtistCarousel(
                        artistName = artist2Name,
                        tracks = artist2Tracks,
                        onPlayTrack = { track -> onPlayTrack(track, artist2Tracks) },
                        onMoreClick = onMoreClick
                    )
                }
            }

            // -------------------------------------------------------------
            // 8. "MOOD AND GENRES" Multi-Column Grid
            // -------------------------------------------------------------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = "MOOD AND GENRES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val chunkedGenres = genres.chunked(2)
                    chunkedGenres.forEach { rowPair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowPair.forEach { (genreName, accentColor) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(CardSurfaceColor)
                                        .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable {
                                            selectMood(genreName)
                                            scope.launch {
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(24.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(accentColor)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = genreName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (rowPair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER COMPOSABLES
// -------------------------------------------------------------

@Composable
private fun GroupedTrackCard(
    title: String,
    tracks: List<TrackItem>,
    currentTrackId: String?,
    isVideoSection: Boolean,
    onPlayTrack: (TrackItem) -> Unit,
    onPlayAll: () -> Unit,
    onMoreClick: ((TrackItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
        color = CardSurfaceColor,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
                TextButton(
                    onClick = onPlayAll,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Play all",
                        color = ElectricViolet,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            tracks.forEach { track ->
                val isCurrent = track.id == currentTrackId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onPlayTrack(track) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DesktopNetworkImage(
                        url = track.artworkUrl,
                        contentDescription = track.title,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (isCurrent) ElectricViolet else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.artist,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isVideoSection) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onMoreClick?.invoke(track) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistGradientCard(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(130.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SimilarArtistCarousel(
    artistName: String,
    tracks: List<TrackItem>,
    onPlayTrack: (TrackItem) -> Unit,
    onMoreClick: ((TrackItem) -> Unit)? = null
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DesktopNetworkImage(
                    url = tracks.firstOrNull()?.artworkUrl ?: "",
                    contentDescription = artistName,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, ElectricViolet, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Similar to $artistName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(tracks) { track ->
                Column(
                    modifier = Modifier
                        .width(135.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onPlayTrack(track) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(135.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardSurfaceColor)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
                    ) {
                        DesktopNetworkImage(
                            url = track.artworkUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 3-dots button at top right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { onMoreClick?.invoke(track) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = track.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
