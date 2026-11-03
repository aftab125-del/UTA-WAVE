package com.uta.wave.core.model

import kotlinx.serialization.Serializable

/**
 * Common music models shared across Mobile (Android/iOS) and Windows Desktop.
 */

@Serializable
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

@Serializable
data class PlaylistRecord(
    val id: String,
    val name: String,
    val description: String? = null,
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ArtistCard(
    val id: String,                     // browseId, e.g. "UC..."
    val name: String,
    val subscriberCount: String? = null,
    val artworkUrl: String? = null
)

@Serializable
data class AlbumCard(
    val id: String,                     // browseId, e.g. "MPREb_..."
    val title: String,
    val subtitle: String? = null,       // e.g. "2026" or "Single"
    val artworkUrl: String? = null,
    val year: String? = null
)

@Serializable
data class ArtistPage(
    val id: String,
    val name: String,
    val heroArtworkUrl: String? = null,
    val subscriberCount: String? = null,
    val bio: String? = null,
    val topSongs: List<TrackItem> = emptyList(),
    val albums: List<AlbumCard> = emptyList(),
    val singles: List<AlbumCard> = emptyList(),
    val similarArtists: List<ArtistCard> = emptyList()
)

@Serializable
data class AlbumPage(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val year: String? = null,
    val artworkUrl: String? = null,
    val tracks: List<TrackItem> = emptyList()
)

enum class SearchCategory(val label: String, val param: String?) {
    ALL("All", null),
    SONGS("Songs", "EgWKAQIIAWoKEAkQBRAKEAMQBA=="),
    ARTISTS("Artists", "EgWKAQIgAWoKEAkQChAFEAMQBA=="),
    ALBUMS("Albums", "EgWKAQIYAWoKEAkQChAFEAMQBA=="),
    PLAYLISTS("Playlists", "EgeKAQQoAEABagoQAxAEEAoQCRAF")
}

@Serializable
data class SearchSummary(
    val topArtist: ArtistCard? = null,
    val topAlbum: AlbumCard? = null,
    val tracks: List<TrackItem> = emptyList(),
    val artists: List<ArtistCard> = emptyList(),
    val albums: List<AlbumCard> = emptyList()
)

@Serializable
data class StreamInfo(
    val url: String,
    val videoId: String,
    val itag: Int = 140,
    val mimeType: String = "audio/mp4",
    val bitrate: Long = 128000
)

@Serializable
data class LyricLine(
    val timestampMs: Long,
    val text: String
)

@Serializable
data class LyricsResult(
    val lines: List<LyricLine> = emptyList(),
    val isSynced: Boolean = false,
    val plainText: String = "",
    val provider: String = "LrcLib"
)
