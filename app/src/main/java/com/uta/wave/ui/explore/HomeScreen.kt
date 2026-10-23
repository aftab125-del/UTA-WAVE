package com.uta.wave.ui.explore

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SoftViolet
import com.uta.wave.ui.theme.SurfaceContainerHigh
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
    modifier: Modifier = Modifier
) {
    val innerTubeClient = remember { InnerTubeClient() }
    val scope = rememberCoroutineScope()

    var trendingTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var moodTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val moods = listOf("Romance", "Relax", "Feel good", "Energise", "Workout", "Commute", "Party", "Focus")

    LaunchedEffect(Unit) {
        trendingTracks = innerTubeClient.getTrending()
        isLoading = false
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground),
        contentPadding = PaddingValues(top = 16.dp, bottom = 150.dp)
    ) {
        // 1. Header (UTA-wave + Top Action Icons: History, Stats, Cast, Profile)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "UTA-wave",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.TrendingUp, contentDescription = "Stats", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Cast, contentDescription = "Connect", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = TextSecondary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // 2. Horizontal Mood Tags
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(moods) { mood ->
                    val isMoodActive = selectedMood == mood
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isMoodActive) ElectricViolet else SurfaceElevated)
                            .border(1.dp, if (isMoodActive) ElectricViolet else CardBorder, RoundedCornerShape(20.dp))
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
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricViolet)
                }
            }
        } else {
            // 3. Hero Carousel Banner
            item {
                if (activeList.isNotEmpty()) {
                    val heroTrack = activeList.first()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .height(170.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                            .clickable { onPlayTrack(heroTrack, activeList) }
                    ) {
                        AsyncImage(
                            model = heroTrack.artworkUrl,
                            contentDescription = heroTrack.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xE60F0B12)
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FEATURED TRACK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ElectricViolet
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = heroTrack.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = heroTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftViolet,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
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

            // 4. "MUSIC VIDEOS FOR YOU" / "QUICK PICKS" Grouped Container
            item {
                val groupTracks = activeList.drop(1).take(4)
                if (groupTracks.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
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
                                    letterSpacing = 1.sp
                                ),
                                color = TextMuted
                            )
                            TextButton(onClick = { onPlayTrack(groupTracks.first(), activeList) }) {
                                Text("Play all", color = ElectricViolet, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        groupTracks.forEach { track ->
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
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (track.id == currentTrackId) ElectricViolet else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = { onToggleFavorite(track) }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            // 5. "SPEED DIAL" 3-Column Square Grid
            item {
                val speedDialItems = activeList.drop(5).take(9)
                if (speedDialItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "SPEED DIAL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = TextPrimary
                        )

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
                                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                            .clickable { onPlayTrack(item, activeList) }
                                    ) {
                                        AsyncImage(
                                            model = item.artworkUrl,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )

                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(Color.Black.copy(alpha = 0.28f))
                                        )

                                        // Centered play pill
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Title overlay at bottom
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
                                // Fill empty slots if last row has less than 3
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 6. FORGOTTEN FAVORITES / RECENT HISTORY
            item {
                val bottomList = if (recentHistory.isNotEmpty()) recentHistory else activeList.drop(14)
                if (bottomList.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = if (recentHistory.isNotEmpty()) "Forgotten Favorites" else "Recommended Tracks",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(bottomList.take(8)) { track ->
                                Column(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { onPlayTrack(track, bottomList) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(SurfaceDark)
                                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
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
            }
        }
    }
}
