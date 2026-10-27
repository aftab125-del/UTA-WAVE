package com.uta.wave.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
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
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTrackForMenu by remember { mutableStateOf<TrackItem?>(null) }

    val context = LocalContext.current
    val playbackManager = remember { PlaybackManager.getInstance(context) }
    val innerTubeClient = remember { InnerTubeClient() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val popularTags = listOf("Top 100", "Arijit Singh", "Taylor Swift", "Karan Aujla", "The Weeknd", "Lo-Fi Beats", "EDM Hits", "Bollywood")

    BackHandler(enabled = query.isNotEmpty() || results.isNotEmpty()) {
        query = ""
        suggestions = emptyList()
        results = emptyList()
        focusManager.clearFocus()
    }

    fun executeSearch(searchQuery: String) {
        if (searchQuery.isBlank()) return
        query = searchQuery
        suggestions = emptyList()
        isLoading = true
        focusManager.clearFocus()
        scope.launch {
            results = innerTubeClient.search(searchQuery)
            isLoading = false
        }
    }

    LaunchedEffect(query) {
        if (query.trim().length >= 2) {
            searchJob?.cancel()
            searchJob = launch {
                delay(300)
                suggestions = innerTubeClient.getSearchSuggestions(query)
                // Also trigger live search after 600ms if results are not already showing for this exact query
                delay(300)
                if (query.isNotBlank() && results.isEmpty()) {
                    isLoading = true
                    results = innerTubeClient.search(query)
                    isLoading = false
                }
            }
        } else if (query.isEmpty()) {
            suggestions = emptyList()
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
            onValueChange = { 
                query = it
                if (it.isEmpty()) results = emptyList()
            },
            placeholder = { Text("Search songs, artists, albums...", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = ElectricViolet)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        results = emptyList()
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

        Spacer(modifier = Modifier.height(12.dp))

        // Suggestions row
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
                            .padding(horizontal = 14.dp, vertical = 8.dp)
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
                results.isEmpty() && query.isNotBlank() && suggestions.isEmpty() -> {
                    Text("No tracks found for \"$query\"", color = TextMuted)
                }
                results.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
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

                        Spacer(modifier = Modifier.height(36.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMuted.copy(alpha = 0.5f),
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
                        contentPadding = PaddingValues(top = 8.dp, bottom = 150.dp)
                    ) {
                        items(results) { track ->
                            val isFavorite = favoritesList.any { it.id == track.id }
                            TrackRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                isPlaying = isPlaying && track.id == currentTrackId,
                                isFavorite = isFavorite,
                                onTrackClick = { onPlayTrack(track, results) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onMoreClick = { selectedTrackForMenu = track }
                            )
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
                    executeSearch(artistName)
                },
                onDismiss = { selectedTrackForMenu = null }
            )
        }
    }
}
