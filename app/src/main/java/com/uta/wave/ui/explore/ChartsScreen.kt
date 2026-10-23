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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

@Composable
fun ChartsScreen(
    currentTrackId: String?,
    isPlaying: Boolean,
    favoritesList: List<TrackItem>,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val innerTubeClient = remember { InnerTubeClient() }
    var chartTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        chartTracks = innerTubeClient.getTrending()
        isLoading = false
    }

    val pageSize = 15
    val totalPages = ((chartTracks.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val pagedTracks = chartTracks.drop((currentPage - 1) * pageSize).take(pageSize)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Top Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                    .clickable { onNavigateToSearch() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = ElectricViolet, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Search library...", color = TextMuted, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            ) {
                Icon(Icons.Default.LibraryMusic, contentDescription = "Library", tint = ElectricViolet, modifier = Modifier.size(22.dp))
            }
        }

        // Section Title: Apple Music Top 100 (System Default)
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            Text(
                text = "Apple Music Top 100",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = TextPrimary
            )
            Text(
                text = "System Default",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricViolet)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                itemsIndexed(pagedTracks) { index, track ->
                    val rank = (currentPage - 1) * pageSize + index + 1
                    val playCount = ((120 - rank * 4).coerceAtLeast(18)) * 10
                    val isCurrent = track.id == currentTrackId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPlayTrack(track, chartTracks) }
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Number
                        Text(
                            text = "$rank",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = if (rank <= 3) ElectricViolet else TextMuted,
                            modifier = Modifier.width(28.dp)
                        )

                        // Album Thumbnail
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(0.5.dp, CardBorder, RoundedCornerShape(10.dp))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = if (isCurrent) ElectricViolet else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play count (e.g. 833k plays)
                        Text(
                            text = "${playCount}k plays",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = { onToggleFavorite(track) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Pagination Bar (< 1 of 6 >)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 140.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { if (currentPage > 1) currentPage-- },
                    enabled = currentPage > 1
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page", tint = if (currentPage > 1) TextPrimary else TextMuted)
                }

                Text(
                    text = "$currentPage of $totalPages",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                IconButton(
                    onClick = { if (currentPage < totalPages) currentPage++ },
                    enabled = currentPage < totalPages
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = if (currentPage < totalPages) TextPrimary else TextMuted)
                }
            }
        }
    }
}
