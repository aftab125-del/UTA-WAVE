package com.uta.wave.data.innertube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class InnerTubeClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    companion object {
        private const val INNERTUBE_BASE_URL = "https://music.youtube.com/youtubei/v1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val SONGS_PARAM = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
        private const val CLIENT_VERSION = "1.20260213.01.00"
    }

    private val commonHeaders: Map<String, String> = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Origin" to "https://music.youtube.com",
        "Referer" to "https://music.youtube.com/",
        "X-YouTube-Client-Name" to "67",
        "X-YouTube-Client-Version" to CLIENT_VERSION
    )

    private fun getClientContext(): JSONObject {
        val clientObj = JSONObject()
        clientObj.put("clientName", "WEB_REMIX")
        clientObj.put("clientVersion", CLIENT_VERSION)
        clientObj.put("hl", "en")
        clientObj.put("gl", "US")

        val context = JSONObject()
        context.put("client", clientObj)
        return context
    }

    suspend fun search(query: String): List<TrackItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = JSONObject()
            payload.put("context", getClientContext())
            payload.put("query", query.trim())
            payload.put("params", SONGS_PARAM)

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/search")
                .apply { commonHeaders.forEach { (k, v) -> addHeader(k, v) } }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseSearchTracks(JSONObject(body))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = JSONObject()
            payload.put("context", getClientContext())
            payload.put("input", query.trim())

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/music/get_search_suggestions")
                .apply { commonHeaders.forEach { (k, v) -> addHeader(k, v) } }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseSuggestions(JSONObject(body))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTrending(): List<TrackItem> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
            payload.put("context", getClientContext())
            payload.put("browseId", "FEmusic_charts")

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/browse")
                .apply { commonHeaders.forEach { (k, v) -> addHeader(k, v) } }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext search("Top Global Hits 2026")
                val body = response.body?.string() ?: return@withContext emptyList()
                val parsed = parseBrowseTracks(JSONObject(body))
                if (parsed.isNotEmpty()) parsed else search("Top Global Hits 2026")
            }
        } catch (e: Exception) {
            search("Top Global Hits 2026")
        }
    }

    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val clients = listOf(
            // VISIONOS client profile (bypasses 1MB throttling, serves direct audio stream)
            Triple("VISIONOS", "1.0.0", "Apple Vision Pro"),
            // ANDROID client profile fallback
            Triple("ANDROID", "19.09.37", "Android 11")
        )

        for ((clientName, clientVersion, deviceModel) in clients) {
            try {
                val clientObj = JSONObject().apply {
                    put("clientName", clientName)
                    put("clientVersion", clientVersion)
                    put("hl", "en")
                    put("gl", "US")
                }
                val payload = JSONObject().apply {
                    put("context", JSONObject().put("client", clientObj))
                    put("videoId", videoId)
                    put("playbackContext", JSONObject().put("contentPlaybackContext", JSONObject().put("signatureTimestamp", 19800)))
                }

                val request = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/player")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use null
                        val json = JSONObject(body)
                        val streamData = json.optJSONObject("streamingData") ?: return@use null

                        // 1. Check adaptive formats (audio/mp4, webm)
                        val adaptive = streamData.optJSONArray("adaptiveFormats") ?: JSONArray()
                        for (i in 0 until adaptive.length()) {
                            val format = adaptive.optJSONObject(i) ?: continue
                            val mime = format.optString("mimeType", "")
                            val url = format.optString("url", "")
                            if (mime.contains("audio/mp4") && url.isNotBlank()) {
                                return@withContext url
                            }
                        }

                        // 2. Fallback to any audio format with url
                        for (i in 0 until adaptive.length()) {
                            val format = adaptive.optJSONObject(i) ?: continue
                            val mime = format.optString("mimeType", "")
                            val url = format.optString("url", "")
                            if (mime.contains("audio/") && url.isNotBlank()) {
                                return@withContext url
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // try next client
            }
        }
        null
    }

    private fun parseSuggestions(root: JSONObject): List<String> {
        val results = mutableListOf<String>()
        val contents = root.optJSONArray("contents") ?: return results
        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i) ?: continue
            val runs = item.optJSONObject("searchSuggestionRenderer")
                ?.optJSONObject("suggestion")
                ?.optJSONArray("runs") ?: continue

            val text = StringBuilder()
            for (j in 0 until runs.length()) {
                text.append(runs.optJSONObject(j)?.optString("text", "") ?: "")
            }
            if (text.isNotBlank()) results.add(text.toString())
        }
        return results
    }

    private fun parseSearchTracks(root: JSONObject): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return tracks

        for (i in 0 until contents.length()) {
            val section = contents.optJSONObject(i) ?: continue
            val shelfContents = section.optJSONObject("musicShelfRenderer")?.optJSONArray("contents")
                ?: section.optJSONObject("itemSectionRenderer")?.optJSONArray("contents")
                ?: continue

            for (j in 0 until shelfContents.length()) {
                val item = shelfContents.optJSONObject(j) ?: continue
                val renderer = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                val track = parseTrackRenderer(renderer)
                if (track != null) tracks.add(track)
            }
        }
        return tracks
    }

    private fun parseBrowseTracks(root: JSONObject): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val tabs = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs") ?: return tracks

        val tabContent = tabs.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return tracks

        for (i in 0 until tabContent.length()) {
            val shelf = tabContent.optJSONObject(i)?.optJSONObject("musicCarouselShelfRenderer") ?: continue
            val items = shelf.optJSONArray("contents") ?: continue
            for (j in 0 until items.length()) {
                val item = items.optJSONObject(j) ?: continue
                val renderer = item.optJSONObject("musicResponsiveListItemRenderer")
                    ?: item.optJSONObject("musicTwoRowItemRenderer") ?: continue
                val track = parseTrackRenderer(renderer)
                if (track != null) tracks.add(track)
            }
        }
        return tracks
    }

    private fun parseTrackRenderer(renderer: JSONObject): TrackItem? {
        val videoId = renderer.optJSONObject("playlistItemData")?.optString("videoId")
            ?: renderer.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
            ?: renderer.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
            ?: return null

        if (videoId.length != 11) return null

        val flexColumns = renderer.optJSONArray("flexColumns") ?: JSONArray()
        var title = "Unknown Title"
        var artist = "Unknown Artist"
        var duration = 0

        if (flexColumns.length() > 0) {
            val col0 = flexColumns.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")

            if (col0 != null && col0.length() > 0) {
                title = col0.optJSONObject(0)?.optString("text", title) ?: title
            }
        }

        if (flexColumns.length() > 1) {
            val col1 = flexColumns.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")

            if (col1 != null) {
                val artistRuns = mutableListOf<String>()
                for (k in 0 until col1.length()) {
                    val t = col1.optJSONObject(k)?.optString("text", "") ?: ""
                    if (t.contains(":") && t.length <= 8) {
                        duration = parseDuration(t)
                    } else if (t.isNotBlank() && t != "•" && t != " & ") {
                        artistRuns.add(t)
                    }
                }
                if (artistRuns.isNotEmpty()) {
                    artist = artistRuns[0]
                }
            }
        }

        val thumbnail = renderer.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { thumbs ->
                if (thumbs.length() > 0) {
                    val url = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
                    upgradeThumbnail(url)
                } else null
            } ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return TrackItem(
            id = "youtube:$videoId",
            videoId = videoId,
            title = title,
            artist = artist,
            artworkUrl = thumbnail,
            durationSeconds = duration,
            isSynced = true
        )
    }

    private fun upgradeThumbnail(url: String): String {
        return when {
            url.contains("googleusercontent.com") -> url.replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544-l90-rj")
            url.contains("i.ytimg.com") -> url.replace(Regex("hqdefault\\.jpg.*"), "hqdefault.jpg")
            else -> url
        }
    }

    private fun parseDuration(text: String): Int {
        val parts = text.trim().split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }
}
