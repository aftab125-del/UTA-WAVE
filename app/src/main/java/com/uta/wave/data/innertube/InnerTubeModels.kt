package com.uta.wave.data.innertube

data class TrackItem(
    val id: String,                     // e.g. "youtube:dQw4w9WgXcQ"
    val videoId: String,                // e.g. "dQw4w9WgXcQ"
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String,
    val durationSeconds: Int = 0,
    val isSynced: Boolean = false
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
