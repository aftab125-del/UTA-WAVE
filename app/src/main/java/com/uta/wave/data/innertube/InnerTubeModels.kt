package com.uta.wave.data.innertube

data class TrackItem(
    val id: String,                     // e.g. "youtube:dQw4w9WgXcQ"
    val videoId: String,                // e.g. "dQw4w9WgXcQ"
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String,
    val durationSeconds: Int = 0,
    val isSynced: Boolean = false,
    val artistId: String? = null,        // e.g. "UCDxKh1gFWeYsqePvgVzmPoQ"
    val albumId: String? = null         // e.g. "MPREb_n4bQGzZvAyb"
)

data class ArtistCard(
    val id: String,                     // browseId, e.g. "UC..."
    val name: String,
    val subscriberCount: String? = null,
    val artworkUrl: String? = null
)

data class AlbumCard(
    val id: String,                     // browseId, e.g. "MPREb_..."
    val title: String,
    val subtitle: String? = null,       // e.g. "2026" or "Single"
    val artworkUrl: String? = null,
    val year: String? = null
)

data class ArtistPage(
    val id: String,                     // browseId, e.g. "UCDxKh1gFWeY..."
    val name: String,
    val heroArtworkUrl: String? = null,
    val subscriberCount: String? = null,
    val bio: String? = null,
    val topSongs: List<TrackItem> = emptyList(),
    val albums: List<AlbumCard> = emptyList(),
    val singles: List<AlbumCard> = emptyList(),
    val similarArtists: List<ArtistCard> = emptyList()
)

data class AlbumPage(
    val id: String,                     // browseId, e.g. "MPREb_..."
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val year: String? = null,
    val artworkUrl: String? = null,
    val tracks: List<TrackItem> = emptyList()
)

data class SearchResult(
    val tracks: List<TrackItem> = emptyList(),
    val suggestions: List<String> = emptyList()
)

data class StreamInfo(
    val url: String,
    val videoId: String,
    val itag: Int = 140,
    val mimeType: String = "audio/mp4",
    val bitrate: Long = 128000
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class LyricsResult(
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
    val plainText: String = "",
    val provider: String = "LrcLib"
)

data class UserAccountInfo(
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val handle: String? = null
)

data class NextResult(
    val title: String? = null,
    val items: List<TrackItem> = emptyList(),
    val currentIndex: Int? = null,
    val continuation: String? = null,
    val playlistId: String? = null
)
