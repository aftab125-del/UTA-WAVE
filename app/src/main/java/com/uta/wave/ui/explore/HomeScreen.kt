package com.uta.wave.ui.explore

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.ui.components.TrackRow
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

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
    var trendingTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        trendingTracks = innerTubeClient.getTrending()
        isLoading = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground),
        contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
    ) {
        // App Header
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "LIVE DISCOVERY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ElectricViolet
                )
                Text(
                    text = "UTA-wave",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = TextPrimary
                )
            }
        }

        // Trending Carousel
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Global Trending Charts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricViolet)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(trendingTracks.take(8)) { track ->
                            TrendingCard(
                                track = track,
                                onClick = { onPlayTrack(track, trendingTracks) }
                            )
                        }
                    }
                }
            }
        }

        // Quick Picks Section (History or Trending remainder)
        item {
            Spacer(modifier = Modifier.height(24.dp))
            val sectionTracks = if (recentHistory.isNotEmpty()) recentHistory else trendingTracks.drop(8)
            val sectionTitle = if (recentHistory.isNotEmpty()) "Recently Played" else "Top Hits"

            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Column {
                sectionTracks.take(12).forEach { track ->
                    val isFavorite = favoritesList.any { it.id == track.id }
                    TrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying && track.id == currentTrackId,
                        isFavorite = isFavorite,
                        onTrackClick = { onPlayTrack(track, sectionTracks) },
                        onToggleFavorite = { onToggleFavorite(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingCard(
    track: TrackItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceElevated)
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
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
}
