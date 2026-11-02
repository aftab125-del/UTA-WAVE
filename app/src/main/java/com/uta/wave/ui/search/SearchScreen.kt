package com.uta.wave.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.AlbumCard
import com.uta.wave.data.innertube.ArtistCard
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.SearchCategory
import com.uta.wave.data.innertube.SearchSummary
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.innertube.UserPlaylistItem
import com.uta.wave.player.PlaybackManager
import com.uta.wave.ui.components.SongMenuBottomSheet
import com.uta.wave.ui.components.TrackRow
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    currentTrackId: String?,
    isPlaying: Boolean,
    favoritesList: List<TrackItem>,
    onPlayTrack: (TrackItem, List<TrackItem>) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var lastSearchedQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    var searchSummary by remember { mutableStateOf<SearchSummary?>(null) }
    var trackResults by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var artistResults by remember { mutableStateOf<List<ArtistCard>>(emptyList()) }
    var albumResults by remember { mutableStateOf<List<AlbumCard>>(emptyList()) }
    var playlistResults by remember { mutableStateOf<List<UserPlaylistItem>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var selectedTrackForMenu by remember { mutableStateOf<TrackItem?>(null) }

    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val innerTubeClient = remember { InnerTubeClient() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val popularTags = listOf("Top 100", "Arijit Singh", "Taylor Swift", "Karan Aujla", "The Weeknd", "Lo-Fi Beats", "EDM Hits", "Bollywood")

    val hasAnyResult = when (selectedCategory) {
        SearchCategory.ALL -> (searchSummary?.topArtist != null || searchSummary?.topAlbum != null ||
                trackResults.isNotEmpty() || artistResults.isNotEmpty() ||
                albumResults.isNotEmpty() || playlistResults.isNotEmpty())
        SearchCategory.SONGS -> trackResults.isNotEmpty()
        SearchCategory.ARTISTS -> artistResults.isNotEmpty()
        SearchCategory.ALBUMS -> albumResults.isNotEmpty()
        SearchCategory.PLAYLISTS -> playlistResults.isNotEmpty()
    }

    fun clearAllResults() {
        searchSummary = null
        trackResults = emptyList()
        artistResults = emptyList()
        albumResults = emptyList()
        playlistResults = emptyList()
    }

    BackHandler(enabled = query.isNotEmpty() || hasAnyResult) {
        query = ""
        lastSearchedQuery = ""
        suggestions = emptyList()
        clearAllResults()
        focusManager.clearFocus()
    }

    fun executeSearch(searchQuery: String, category: SearchCategory = selectedCategory) {
        if (searchQuery.isBlank()) return
        query = searchQuery
        lastSearchedQuery = searchQuery
        suggestions = emptyList()
        isLoading = true
        focusManager.clearFocus()

        scope.launch {
            when (category) {
                SearchCategory.ALL -> {
                    val summary = innerTubeClient.searchAll(searchQuery)
                    searchSummary = summary
                    trackResults = summary.tracks
                    artistResults = summary.artists
                    albumResults = summary.albums
                    playlistResults = summary.playlists
                }
                SearchCategory.SONGS -> {
                    trackResults = innerTubeClient.search(searchQuery)
                }
                SearchCategory.ARTISTS -> {
                    artistResults = innerTubeClient.searchArtists(searchQuery)
                }
                SearchCategory.ALBUMS -> {
                    albumResults = innerTubeClient.searchAlbums(searchQuery)
                }
                SearchCategory.PLAYLISTS -> {
                    playlistResults = innerTubeClient.searchPlaylists(searchQuery)
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(query) {
        if (query.trim().length >= 2) {
            searchJob?.cancel()
            searchJob = launch {
                delay(300)
                suggestions = innerTubeClient.getSearchSuggestions(query)
                delay(300)
                if (query.isNotBlank() && query != lastSearchedQuery) {
                    executeSearch(query, selectedCategory)
                }
            }
        } else if (query.isEmpty()) {
            suggestions = emptyList()
            lastSearchedQuery = ""
            clearAllResults()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(top = 16.dp)
    ) {
        // Search Input Bar
        TextField(
            value = query,
            onValueChange = { newQuery ->
                query = newQuery
                if (newQuery.isEmpty()) {
                    clearAllResults()
                    suggestions = emptyList()
                    lastSearchedQuery = ""
                }
            },
            placeholder = { Text("Search songs, artists, albums, playlists...", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = ElectricViolet)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        lastSearchedQuery = ""
                        clearAllResults()
                        suggestions = emptyList()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { executeSearch(query) }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = ElectricViolet,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Search Category Filter Chips (All, Songs, Artists, Albums, Playlists)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            items(SearchCategory.values()) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) ElectricViolet else SurfaceElevated,
                    border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (selectedCategory != category) {
                                selectedCategory = category
                                if (query.isNotBlank()) {
                                    executeSearch(query, category)
                                }
                            }
                        }
                ) {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Search Suggestions Row
        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(suggestions) { item ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceElevated)
                            .clickable { executeSearch(item) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Search Results / Empty State
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = ElectricViolet)
                }
                query.isNotBlank() && !hasAnyResult && suggestions.isEmpty() -> {
                    Text("No ${selectedCategory.label.lowercase()} found for \"$query\"", color = TextMuted)
                }
                !hasAnyResult && query.isBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "QUICK DISCOVERY",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ElectricViolet
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(popularTags) { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(SurfaceElevated)
                                        .clickable { executeSearch(tag) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        ),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Search the open universe of music",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 150.dp)
                    ) {
                        when (selectedCategory) {
                            SearchCategory.ALL -> {
                                // 1. Top Artist Hero Card
                                searchSummary?.topArtist?.let { artist ->
                                    item(key = "top_artist_${artist.id}") {
                                        TopArtistHeroCard(
                                            artist = artist,
                                            onClick = { onOpenArtist(artist.id) }
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                }

                                // 2. Songs Section (First 5)
                                if (trackResults.isNotEmpty()) {
                                    item(key = "header_songs") {
                                        SearchSectionHeader(
                                            title = "Songs",
                                            onSeeAllClick = {
                                                selectedCategory = SearchCategory.SONGS
                                                executeSearch(query, SearchCategory.SONGS)
                                            }
                                        )
                                    }
                                    items(trackResults.take(5), key = { "song_${it.id}" }) { track ->
                                        val isFavorite = favoritesList.any { it.id == track.id }
                                        TrackRow(
                                            track = track,
                                            isCurrent = track.id == currentTrackId,
                                            isPlaying = isPlaying && track.id == currentTrackId,
                                            isFavorite = isFavorite,
                                            onTrackClick = { onPlayTrack(track, trackResults) },
                                            onToggleFavorite = { onToggleFavorite(track) },
                                            onMoreClick = { selectedTrackForMenu = track }
                                        )
                                    }
                                }

                                // 3. Artists Section (Horizontal Carousel)
                                if (artistResults.isNotEmpty()) {
                                    item(key = "header_artists") {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        SearchSectionHeader(
                                            title = "Artists",
                                            onSeeAllClick = {
                                                selectedCategory = SearchCategory.ARTISTS
                                                executeSearch(query, SearchCategory.ARTISTS)
                                            }
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            items(artistResults, key = { "artist_carousel_${it.id}" }) { artist ->
                                                SearchArtistAvatarCard(
                                                    artist = artist,
                                                    onClick = { onOpenArtist(artist.id) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 4. Albums Section (Horizontal Carousel)
                                if (albumResults.isNotEmpty()) {
                                    item(key = "header_albums") {
                                        Spacer(modifier = Modifier.height(18.dp))
                                        SearchSectionHeader(
                                            title = "Albums & Singles",
                                            onSeeAllClick = {
                                                selectedCategory = SearchCategory.ALBUMS
                                                executeSearch(query, SearchCategory.ALBUMS)
                                            }
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            items(albumResults, key = { "album_carousel_${it.id}" }) { album ->
                                                SearchAlbumCardItem(
                                                    album = album,
                                                    onClick = { onOpenAlbum(album.id) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 5. Playlists Section (Horizontal Carousel)
                                if (playlistResults.isNotEmpty()) {
                                    item(key = "header_playlists") {
                                        Spacer(modifier = Modifier.height(18.dp))
                                        SearchSectionHeader(
                                            title = "Playlists",
                                            onSeeAllClick = {
                                                selectedCategory = SearchCategory.PLAYLISTS
                                                executeSearch(query, SearchCategory.PLAYLISTS)
                                            }
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            items(playlistResults, key = { "playlist_carousel_${it.id}" }) { playlist ->
                                                SearchPlaylistCardItem(
                                                    playlist = playlist,
                                                    onClick = {
                                                        scope.launch {
                                                            isLoading = true
                                                            val (_, plTracks) = innerTubeClient.getPlaylist(playlist.id)
                                                            if (plTracks.isNotEmpty()) {
                                                                onPlayTrack(plTracks.first(), plTracks)
                                                            }
                                                            isLoading = false
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            SearchCategory.SONGS -> {
                                items(trackResults, key = { "song_${it.id}" }) { track ->
                                    val isFavorite = favoritesList.any { it.id == track.id }
                                    TrackRow(
                                        track = track,
                                        isCurrent = track.id == currentTrackId,
                                        isPlaying = isPlaying && track.id == currentTrackId,
                                        isFavorite = isFavorite,
                                        onTrackClick = { onPlayTrack(track, trackResults) },
                                        onToggleFavorite = { onToggleFavorite(track) },
                                        onMoreClick = { selectedTrackForMenu = track }
                                    )
                                }
                            }

                            SearchCategory.ARTISTS -> {
                                items(artistResults, key = { "artist_${it.id}" }) { artist ->
                                    ArtistCardRow(
                                        artist = artist,
                                        onClick = { onOpenArtist(artist.id) }
                                    )
                                }
                            }

                            SearchCategory.ALBUMS -> {
                                items(albumResults, key = { "album_${it.id}" }) { album ->
                                    AlbumCardRow(
                                        album = album,
                                        onClick = { onOpenAlbum(album.id) }
                                    )
                                }
                            }

                            SearchCategory.PLAYLISTS -> {
                                items(playlistResults, key = { "playlist_${it.id}" }) { playlist ->
                                    PlaylistCardRow(
                                        playlist = playlist,
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                val (_, plTracks) = innerTubeClient.getPlaylist(playlist.id)
                                                if (plTracks.isNotEmpty()) {
                                                    onPlayTrack(plTracks.first(), plTracks)
                                                }
                                                isLoading = false
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

        selectedTrackForMenu?.let { menuTrack ->
            SongMenuBottomSheet(
                track = menuTrack,
                isFavorite = favoritesList.any { it.id == menuTrack.id },
                onToggleFavorite = { onToggleFavorite(it) },
                onPlayNext = { track -> playbackManager.addToQueue(track, insertNext = true) },
                onAddToQueue = { track -> playbackManager.addToQueue(track, insertNext = false) },
                onStartRadio = { track -> playbackManager.playRadio(track) },
                onOpenArtist = { artistName ->
                    selectedTrackForMenu = null
                    onOpenArtist(menuTrack.artistId ?: artistName)
                },
                onDismiss = { selectedTrackForMenu = null }
            )
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            color = TextPrimary
        )
        Text(
            text = "See all",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = ElectricViolet,
            modifier = Modifier.clickable(onClick = onSeeAllClick)
        )
    }
}

@Composable
private fun TopArtistHeroCard(
    artist: ArtistCard,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .border(1.dp, ElectricViolet.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1724))
        ) {
            if (!artist.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artist.artworkUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(ElectricViolet.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "TOP RESULT • ARTIST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    color = ElectricViolet
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!artist.subscriberCount.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artist.subscriberCount,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View Artist",
            tint = ElectricViolet,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SearchArtistAvatarCard(
    artist: ArtistCard,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .shadow(6.dp, CircleShape)
                .background(Color(0xFF1E1724))
        ) {
            if (!artist.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artist.artworkUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Artist",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun SearchAlbumCardItem(
    album: AlbumCard,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(12.dp))
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1724))
        ) {
            if (!album.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = album.artworkUrl,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.subtitle ?: (album.year ?: "Album"),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchPlaylistCardItem(
    playlist: UserPlaylistItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(12.dp))
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1724))
        ) {
            if (!playlist.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.artworkUrl,
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.description ?: "Playlist",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistCardRow(
    artist: ArtistCard,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1724))
        ) {
            if (!artist.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artist.artworkUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (!artist.subscriberCount.isNullOrBlank()) "Artist • ${artist.subscriberCount}" else "Artist",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AlbumCardRow(
    album: AlbumCard,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1724))
        ) {
            if (!album.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = album.artworkUrl,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = album.subtitle ?: (album.year?.let { "Album • $it" } ?: "Album"),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PlaylistCardRow(
    playlist: UserPlaylistItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1724))
        ) {
            if (!playlist.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.artworkUrl,
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = playlist.description ?: "Playlist",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play Playlist",
            tint = ElectricViolet,
            modifier = Modifier.size(22.dp)
        )
    }
}
