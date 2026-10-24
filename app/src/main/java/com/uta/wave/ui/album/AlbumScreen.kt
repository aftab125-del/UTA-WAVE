package com.uta.wave.ui.album

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.AlbumPage
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.ui.components.AmbientCanvas
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

@Composable
fun AlbumScreen(
    albumIdOrName: String,
    artistNameHint: String?,
    currentTrackId: String?,
    isPlaying: Boolean,
    favoritesList: List<TrackItem>,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onOpenArtist: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val innerTubeClient = remember { InnerTubeClient() }
    var albumPage by remember { mutableStateOf<AlbumPage?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(albumIdOrName) {
        isLoading = true
        errorMsg = null
        val resolvedId = if (albumIdOrName.startsWith("MPREb_") || albumIdOrName.startsWith("OLAK5uy_")) {
            albumIdOrName
        } else {
            innerTubeClient.resolveAlbumBrowseId(albumIdOrName, artistNameHint ?: "") ?: albumIdOrName
        }

        val page = innerTubeClient.getAlbum(resolvedId)
        if (page != null) {
            albumPage = page
        } else {
            errorMsg = "Could not load album tracklist"
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0A12))
    ) {
        // Dynamic Ambient Glow from album artwork
        if (albumPage?.artworkUrl != null) {
            AmbientCanvas(
                artworkUrl = albumPage?.artworkUrl ?: "",
                isPlaying = isPlaying
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFFF3D7EC),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading album...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            errorMsg != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = errorMsg ?: "Error",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3D7EC))
                        ) {
                            Text(text = "Go Back", color = Color(0xFF3B1E38))
                        }
                    }
                }
            }

            albumPage != null -> {
                val page = albumPage!!

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // 1. Top Bar with Back Button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Album",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }

                    // 2. Album Artwork & Metadata Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Rounded Album Artwork
                            Box(
                                modifier = Modifier
                                    .size(210.dp)
                                    .shadow(20.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1E1724))
                            ) {
                                if (!page.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = page.artworkUrl,
                                        contentDescription = page.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Album Title
                            Text(
                                text = page.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Clickable Artist Link Chip (Directly links to ArtistScreen!)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF261D2B))
                                    .clickable {
                                        val target = page.artistId ?: page.artist
                                        onOpenArtist(target)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFFF3D7EC),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = page.artist,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFFF3D7EC)
                                )
                            }

                            // Year & Track Count Subtitle
                            Spacer(modifier = Modifier.height(6.dp))
                            val subtitleText = buildString {
                                if (!page.year.isNullOrBlank()) append("${page.year} • ")
                                append("${page.tracks.size} songs")
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action Buttons Row: Play All, Shuffle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (page.tracks.isNotEmpty()) {
                                            onPlayTrack(page.tracks[0], page.tracks)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3D7EC)),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF3B1E38),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Play All",
                                        color = Color(0xFF3B1E38),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF261D2B))
                                        .clickable {
                                            if (page.tracks.isNotEmpty()) {
                                                val shuffled = page.tracks.shuffled()
                                                onPlayTrack(shuffled[0], shuffled)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Tracklist Divider & Header
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.06f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 4. Numbered Tracklist
                    itemsIndexed(page.tracks) { index, track ->
                        val isCurrent = track.id == currentTrackId
                        val isFav = favoritesList.any { it.id == track.id }

                        AlbumTrackRow(
                            index = index + 1,
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            isFavorite = isFav,
                            onPlay = { onPlayTrack(track, page.tracks) },
                            onToggleFavorite = { onToggleFavorite(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumTrackRow(
    index: Int,
    track: TrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.Equalizer,
                contentDescription = "Playing",
                tint = Color(0xFFF3D7EC),
                modifier = Modifier
                    .size(20.dp)
                    .width(28.dp)
            )
        } else {
            Text(
                text = "$index",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isCurrent) Color(0xFFF3D7EC) else TextMuted,
                modifier = Modifier.width(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = if (isCurrent) Color(0xFFF3D7EC) else TextPrimary,
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

        if (track.durationSeconds > 0) {
            Text(
                text = formatDuration(track.durationSeconds),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Color(0xFFE53935) else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
