package com.uta.wave.ui.explore

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.player.PlaybackManager
import com.uta.wave.ui.components.SongMenuBottomSheet
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SoftViolet
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    currentTrackId: String?,
    isPlaying: Boolean,
    favoritesList: List<TrackItem>,
    recentHistory: List<TrackItem>,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onNavigateToCharts: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onOpenListeningSummary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val innerTubeClient = remember { InnerTubeClient() }
    val accountManager = remember { com.uta.wave.data.account.AccountManager.getInstance(context) }
    val userProfile by accountManager.profileFlow.collectAsState()
    val userPrefs = remember { com.uta.wave.data.preferences.UserPreferences.getInstance(context) }
    val userSettings by userPrefs.settingsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var trendingTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var moodTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var similarTracks1 by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var similarTracks2 by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTrackForMenu by remember { mutableStateOf<TrackItem?>(null) }

    BackHandler(enabled = selectedTrackForMenu != null) {
        selectedTrackForMenu = null
    }
    BackHandler(enabled = selectedMood != null) {
        selectedMood = null
        moodTracks = emptyList()
    }

    val moods = listOf("Romance", "Relax", "Feel good", "Energise", "Workout", "Commute", "Party", "Focus", "Sad", "Sleep")

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

    LaunchedEffect(Unit) {
        val trending = innerTubeClient.getTrending()
        trendingTracks = trending
        isLoading = false

        // Load similar artist recommendations in background
        if (trending.isNotEmpty()) {
            val artist1 = trending.firstOrNull()?.artist ?: "The Weeknd"
            val artist2 = trending.getOrNull(4)?.artist ?: "Dua Lipa"
            scope.launch {
                similarTracks1 = innerTubeClient.search("$artist1 hits")
            }
            scope.launch {
                similarTracks2 = innerTubeClient.search("$artist2 hits")
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
                moodTracks = innerTubeClient.getMoodSongs(mood)
                isLoading = false
            }
        }
    }

    val activeList = if (selectedMood != null && moodTracks.isNotEmpty()) moodTracks else trendingTracks

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp)
        ) {
        // -------------------------------------------------------------
        // 1. TOP BAR: "UTA-wave" + Utility Actions (History, Stats, Cast, Avatar)
        // -------------------------------------------------------------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val customIconFile = remember(userSettings.customIconPath) {
                        userSettings.customIconPath?.let { java.io.File(it) }?.takeIf { it.exists() }
                    }
                    if (userSettings.useCustomIconInApp && customIconFile != null) {
                        AsyncImage(
                            model = customIconFile,
                            contentDescription = "Custom Icon",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Text(
                        text = if (userSettings.useCustomIconInApp && userSettings.customIconLabel.isNotBlank()) {
                            userSettings.customIconLabel
                        } else {
                            "UTA-wave"
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            letterSpacing = (-0.6).sp
                        ),
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onOpenListeningSummary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Listening Summary & History",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Listen Together room ready", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cast,
                            contentDescription = "Cast & Listen Together",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (!userProfile.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userProfile.avatarUrl,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, ElectricViolet.copy(alpha = 0.8f), CircleShape)
                                .clickable(onClick = onOpenAccount)
                        )
                    } else {
                        IconButton(
                            onClick = onOpenAccount,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Profile",
                                tint = ElectricViolet,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. HORIZONTAL MOOD FILTER CHIPS
        // -------------------------------------------------------------
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(moods) { mood ->
                    val isMoodActive = selectedMood == mood
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isMoodActive) ElectricViolet else Color(0xFF241C2B))
                            .border(
                                1.dp,
                                if (isMoodActive) ElectricViolet else CardBorder.copy(alpha = 0.7f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectMood(mood) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = mood,
                            color = if (isMoodActive) ObsidianBackground else TextPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isMoodActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
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
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricViolet)
                }
            }
        } else {
            // -------------------------------------------------------------
            // 3. HORIZONTAL CENTERED HERO CAROUSEL
            // -------------------------------------------------------------
            item {
                val heroItems = activeList.take(5)
                if (heroItems.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { heroItems.size })

                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            pageSpacing = 12.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) { page ->
                            val track = heroItems[page]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(8.dp, RoundedCornerShape(24.dp))
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                                    .clickable { onPlayTrack(track, activeList) }
                            ) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )

                                // Gradient Scrim
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color(0xB3150F1C),
                                                    Color(0xF0100A15)
                                                )
                                            )
                                        )
                                )

                                // Pill Tag at Top Left
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(14.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = selectedMood?.uppercase() ?: "FEATURED MIX",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        color = ElectricViolet
                                    )
                                }

                                // Bottom Track Information & Play Button
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            ),
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = SoftViolet,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .shadow(8.dp, CircleShape, spotColor = ElectricViolet)
                                            .clip(CircleShape)
                                            .background(ElectricViolet),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = ObsidianBackground,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. "STATION / LISTEN TOGETHER" Grouped Card (4 Tracks + Play all)
            // -------------------------------------------------------------
            item {
                val stationTracks = activeList.drop(5).take(4)
                if (stationTracks.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .shadow(6.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "STATION / LISTEN TOGETHER",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 12.sp
                                ),
                                color = TextMuted
                            )
                            TextButton(
                                onClick = { onPlayTrack(stationTracks.first(), activeList) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Play all", color = ElectricViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        stationTracks.forEach { track ->
                            val isCurrent = track.id == currentTrackId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPlayTrack(track, activeList) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (isCurrent) ElectricViolet else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • Station",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = { selectedTrackForMenu = track }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. "MUSIC VIDEOS FOR YOU" (Verified Badge + 4 Rows + Play all)
            // -------------------------------------------------------------
            item {
                val videoTracks = activeList.drop(9).take(4)
                if (videoTracks.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .shadow(6.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MUSIC VIDEOS FOR YOU",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 12.sp
                                ),
                                color = TextMuted
                            )
                            TextButton(
                                onClick = { onPlayTrack(videoTracks.first(), activeList) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Play all", color = ElectricViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        videoTracks.forEach { track ->
                            val isCurrent = track.id == currentTrackId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPlayTrack(track, activeList) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (isCurrent) ElectricViolet else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified Artist",
                                            tint = ElectricViolet,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                IconButton(onClick = { selectedTrackForMenu = track }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 6. "SPEED DIAL" 3-Column Square Grid with Frosted Center Play Button
            // -------------------------------------------------------------
            item {
                val speedDialItems = activeList.drop(13).take(9).ifEmpty { activeList.take(9) }
                if (speedDialItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
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
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val chunked = speedDialItems.chunked(3)
                        chunked.forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(SurfaceDark)
                                            .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                            .clickable { onPlayTrack(item, activeList) }
                                    ) {
                                        AsyncImage(
                                            model = item.artworkUrl,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )

                                        // Dark scrim
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(Color.Black.copy(alpha = 0.32f))
                                        )

                                        // Centered Frosted Glass Play Button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Song title overlay at bottom
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                        )
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
            // 7. "Your YouTube Playlists" Rich Gradient Cards
            // -------------------------------------------------------------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
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
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your YouTube playlists",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = TextPrimary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Liked music Card (Hot Pink to Deep Violet)
                        item {
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(130.dp)
                                    .shadow(8.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFF2E93), Color(0xFF7928CA))
                                        )
                                    )
                                    .clickable {
                                        if (favoritesList.isNotEmpty()) {
                                            onPlayTrack(favoritesList.first(), favoritesList)
                                        } else {
                                            Toast.makeText(context, "No liked songs yet", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                    Column {
                                        Text("Liked music", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                                        Text("Auto playlist • ${favoritesList.size.coerceAtLeast(1)} songs", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                    }
                                }
                            }
                        }

                        // Sounds from Shorts Card (Yellow to Coral)
                        item {
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(130.dp)
                                    .shadow(8.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFFB703), Color(0xFFFF006E))
                                        )
                                    )
                                    .clickable {
                                        if (activeList.isNotEmpty()) {
                                            onPlayTrack(activeList.random(), activeList)
                                        }
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                    Column {
                                        Text("Sounds from Shorts", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                                        Text("Auto playlist • 12 songs", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                    }
                                }
                            }
                        }

                        // Episodes for Later Card (Indigo to Cyan)
                        item {
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(130.dp)
                                    .shadow(8.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF3A0CA3), Color(0xFF4CC9F0))
                                        )
                                    )
                                    .clickable {
                                        Toast.makeText(context, "Episodes playlist loaded", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                                    Column {
                                        Text("Episodes for Later", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                                        Text("Auto playlist • 6 episodes", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                    }
                                }
                            }
                        }

                        // UTA-wave Favorites Card (Electric Violet to Blue)
                        item {
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(130.dp)
                                    .shadow(8.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ElectricViolet, Color(0xFF4361EE))
                                        )
                                    )
                                    .clickable {
                                        if (favoritesList.isNotEmpty()) {
                                            onPlayTrack(favoritesList.first(), favoritesList)
                                        }
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                    Column {
                                        Text("UTA-wave Favorites", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                                        Text("${favoritesList.size} custom tracks", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 8. "Similar to [Artist]" Horizontal Carousels
            // -------------------------------------------------------------
            val artist1Name = activeList.firstOrNull()?.artist ?: "The Weeknd"
            val artist1Tracks = if (similarTracks1.isNotEmpty()) similarTracks1 else activeList.drop(2).take(8)

            item {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(artist1Tracks.firstOrNull()?.artistId ?: artist1Name) }
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = artist1Tracks.firstOrNull()?.artworkUrl,
                                contentDescription = artist1Name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, ElectricViolet, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Similar to $artist1Name",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = TextPrimary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(artist1Tracks) { track ->
                            Column(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { onPlayTrack(track, artist1Tracks) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(135.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                ) {
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Second "Similar to [Artist 2]" Carousel
            val artist2Name = activeList.getOrNull(4)?.artist ?: "Dua Lipa"
            val artist2Tracks = if (similarTracks2.isNotEmpty()) similarTracks2 else activeList.drop(6).take(8)

            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(artist2Tracks.firstOrNull()?.artistId ?: artist2Name) }
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = artist2Tracks.firstOrNull()?.artworkUrl,
                                contentDescription = artist2Name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, ElectricViolet, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Similar to $artist2Name",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = TextPrimary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(artist2Tracks) { track ->
                            Column(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { onPlayTrack(track, artist2Tracks) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(135.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                                ) {
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 9. "MOOD AND GENRES" 2-Column Grid
            // -------------------------------------------------------------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "MOOD AND GENRES",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
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
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
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
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(accentColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = genreName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
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

    if (selectedTrackForMenu != null) {
        val item = selectedTrackForMenu!!
        val playbackManager = remember { PlaybackManager.getInstance(context) }
        SongMenuBottomSheet(
            track = item,
            isFavorite = favoritesList.any { it.id == item.id },
            onToggleFavorite = onToggleFavorite,
            onPlayNext = { track -> playbackManager.addToQueue(track, insertNext = true) },
            onAddToQueue = { track -> playbackManager.addToQueue(track, insertNext = false) },
            onStartRadio = { track -> playbackManager.playRadio(track) },
            onOpenArtist = onOpenArtist,
            onDismiss = { selectedTrackForMenu = null }
        )
    }
}
}
