package com.uta.wave.desktop.data

import com.uta.wave.core.model.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Desktop Music Service for YouTube Music search and stream resolution on Windows.
 * Modelled after UTA-wave's Echo Music InnerTube architecture.
 */
class DesktopMusicService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    companion object {
        private const val INNERTUBE_BASE_URL = "https://music.youtube.com/youtubei/v1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val SONGS_PARAM = "EgWKAQIIAWoKEAkQBRAKEAMQBA=="
        private const val CLIENT_VERSION = "1.20260213.01.00"

        @Volatile
        private var cachedVisitorData: String = "CgtqVExzZXIxN21sZyiX6-zWBjIKCgJJThIEGgAgS2KrAgqoAkN0c0JDQVVTalFFQjhRWnVacVhJUUk4a1psd1ZkR3k4anl6UlVabUhOczhSaDVrSjVvVGduSVExZDc5QnFjenZtVzlFeGRhMzFPeXZLckVwSG4yQXJTT0hVRVpxTTVvRkpjSmx2RnMyaGtWZkxCY0J1WFVoQWRaUHZMcjhHZW5OQ0I0ZlMxVE5lNi1MRDB2V1VEMGNrX2ZXMFJab0VSNXZtUjFhdVhqV2ZEcHJEQmY4UWl6ZGlxalg0T1JFVGZzdm5SaGFuQkVvQlRKRkFkNnBuVS1BUFJDWW9oZXVNajJQUWxGUkZGWU85N0dva2ZmOFV4VENmRzNnLTE4ZElHVmMwVTdkY2Zhb203djNja0xqRDNKdlhyczZtdVFNUzRRTjJ3MGw3cGtG"

        private val commonHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Referer" to "https://music.youtube.com/",
            "Origin" to "https://music.youtube.com",
            "X-YouTube-Client-Name" to "67",
            "X-YouTube-Client-Version" to CLIENT_VERSION
        )
    }

    private fun getClientContext(clientName: String = "WEB_REMIX", clientVersion: String = CLIENT_VERSION): JSONObject {
        val clientObj = JSONObject().apply {
            put("clientName", clientName)
            put("clientVersion", clientVersion)
            put("hl", "en")
            put("gl", "US")
            put("visitorData", cachedVisitorData)
        }
        return JSONObject().apply { put("client", clientObj) }
    }

    suspend fun searchSongs(query: String): List<TrackItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext())
                put("query", query.trim())
                put("params", SONGS_PARAM)
            }
            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/search")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseSearchTracks(JSONObject(body))
            }
        } catch (e: Exception) {
            println("[DesktopMusicService] Search error: ${e.message}")
            emptyList()
        }
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
                val item = shelfContents.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer")
                    ?: continue
                parseTrackItem(item)?.let { tracks.add(it) }
            }
        }
        return tracks
    }

    private fun parseTrackItem(item: JSONObject): TrackItem? {
        val videoId = item.optJSONObject("playlistItemData")?.optString("videoId", "")
            ?.takeIf { it.isNotBlank() }
            ?: item.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId", "")
            ?: item.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId", "")
            ?: return null

        if (videoId.isBlank()) return null

        val flexColumns = item.optJSONArray("flexColumns") ?: return null
        val col0 = flexColumns.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")

        val title = col0?.optJSONObject(0)?.optString("text", "Unknown Title") ?: "Unknown Title"

        val col1 = flexColumns.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")

        var artist = "Unknown Artist"
        var album: String? = null
        var durationSec = 0

        if (col1 != null) {
            val texts = mutableListOf<String>()
            for (k in 0 until col1.length()) {
                val text = col1.optJSONObject(k)?.optString("text", "") ?: ""
                if (text != " • " && text.isNotBlank()) texts.add(text)
            }
            if (texts.isNotEmpty()) artist = texts[0]
            if (texts.size >= 2 && !texts[1].contains(":")) album = texts[1]
            val lastText = texts.lastOrNull() ?: ""
            if (lastText.contains(":")) durationSec = parseDurationSeconds(lastText)
        }

        val thumbnails = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")

        var artworkUrl = thumbnails?.optJSONObject(thumbnails.length() - 1)?.optString("url", "") ?: ""
        if (artworkUrl.contains("googleusercontent.com")) {
            artworkUrl = artworkUrl.replace(Regex("=w\\d+-h\\d+.*$"), "=w544-h544-l90-rj")
        }

        return TrackItem(
            id = "youtube:$videoId",
            videoId = videoId,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artworkUrl,
            durationSeconds = durationSec
        )
    }

    private fun parseDurationSeconds(durationStr: String): Int {
        val parts = durationStr.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }

    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        // Level 1: Primary VISIONOS / ANDROID_VR InnerTube player resolution
        val primaryStream = tryResolveVisionOS(videoId) ?: tryResolveVR(videoId) ?: tryResolveIOS(videoId)
        if (!primaryStream.isNullOrBlank()) {
            return@withContext primaryStream
        }

        // Level 2: Local yt-dlp extraction fallback if installed on host Windows PC
        val ytDlpStream = tryResolveYtDlp(videoId)
        if (!ytDlpStream.isNullOrBlank()) {
            return@withContext ytDlpStream
        }

        // Level 3: Piped instances fallback
        return@withContext tryResolvePiped(videoId)
    }

    private fun tryResolveVisionOS(videoId: String): String? {
        try {
            val payload = JSONObject().apply {
                val clientObj = JSONObject().apply {
                    put("clientName", "VISIONOS")
                    put("clientVersion", "1.02")
                    put("osName", "visionOS")
                    put("osVersion", "1.0")
                    put("deviceMake", "Apple")
                    put("deviceModel", "RealityDevice14,1")
                    put("hl", "en")
                    put("gl", "US")
                    put("visitorData", cachedVisitorData)
                }
                put("context", JSONObject().put("client", clientObj))
                put("videoId", videoId)
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15")
                .addHeader("X-YouTube-Client-Name", "101")
                .addHeader("X-YouTube-Client-Version", "1.02")
                .addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                if (json.optJSONObject("playabilityStatus")?.optString("status", "") != "OK") return null
                val adaptive = json.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats") ?: return null
                return extractBestAudioUrl(adaptive)
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun tryResolveVR(videoId: String): String? {
        try {
            val payload = JSONObject().apply {
                val clientObj = JSONObject().apply {
                    put("clientName", "ANDROID_VR")
                    put("clientVersion", "1.65.10")
                    put("hl", "en")
                    put("gl", "US")
                    put("visitorData", cachedVisitorData)
                }
                put("context", JSONObject().put("client", clientObj))
                put("videoId", videoId)
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                if (json.optJSONObject("playabilityStatus")?.optString("status", "") != "OK") return null
                val adaptive = json.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats") ?: return null
                return extractBestAudioUrl(adaptive)
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun tryResolveIOS(videoId: String): String? {
        try {
            val payload = JSONObject().apply {
                val clientObj = JSONObject().apply {
                    put("clientName", "IOS")
                    put("clientVersion", "19.45.4")
                    put("deviceMake", "Apple")
                    put("deviceModel", "iPhone16,2")
                    put("hl", "en")
                    put("gl", "US")
                    put("visitorData", cachedVisitorData)
                }
                put("context", JSONObject().put("client", clientObj))
                put("videoId", videoId)
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1 like Mac OS X; en_US)")
                .addHeader("X-YouTube-Client-Name", "5")
                .addHeader("X-YouTube-Client-Version", "19.45.4")
                .addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                if (json.optJSONObject("playabilityStatus")?.optString("status", "") != "OK") return null
                val adaptive = json.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats") ?: return null
                return extractBestAudioUrl(adaptive)
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun tryResolveYtDlp(videoId: String): String? {
        val commands = listOf(
            listOf("py", "-m", "yt_dlp", "-g", "-f", "140/bestaudio[ext=m4a]/bestaudio", "https://www.youtube.com/watch?v=$videoId"),
            listOf("yt-dlp", "-g", "-f", "140/bestaudio[ext=m4a]/bestaudio", "https://www.youtube.com/watch?v=$videoId")
        )
        for (cmd in commands) {
            try {
                val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText().trim()
                process.waitFor(10, TimeUnit.SECONDS)
                if (process.exitValue() == 0 && output.startsWith("http")) {
                    val lines = output.lines().filter { it.startsWith("http") }
                    if (lines.isNotEmpty()) return lines[0].trim()
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun tryResolvePiped(videoId: String): String? {
        val instances = listOf("https://pipedapi.kavin.rocks", "https://api.piped.privacydev.net")
        for (base in instances) {
            try {
                val req = Request.Builder().url("$base/streams/$videoId").build()
                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    val audioStreams = json.optJSONArray("audioStreams") ?: return@use
                    if (audioStreams.length() > 0) {
                        val stream = audioStreams.optJSONObject(0)?.optString("url", "")
                        if (!stream.isNullOrBlank()) return stream
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun extractBestAudioUrl(adaptive: JSONArray): String? {
        val candidates = mutableListOf<Triple<String, String, Int>>()

        for (i in 0 until adaptive.length()) {
            val format = adaptive.optJSONObject(i) ?: continue
            val mimeType = format.optString("mimeType", "")
            if (!mimeType.contains("audio")) continue
            val url = format.optString("url", "")
            if (url.isBlank()) continue

            val bitrate = format.optInt("bitrate", 0)
            candidates.add(Triple(url, mimeType, bitrate))
        }

        if (candidates.isEmpty()) return null

        // 1. Windows native Media Foundation strongly requires AAC in MP4 container (audio/mp4)
        val mp4Candidates = candidates.filter { it.second.contains("audio/mp4") || it.second.contains("mp4a") }
        if (mp4Candidates.isNotEmpty()) {
            // Pick highest bitrate AAC (typically ITAG 140 ~128kbps or ITAG 141 ~256kbps)
            return mp4Candidates.maxByOrNull { it.third }?.first
        }

        // Fallback to whatever audio format is available
        return candidates.maxByOrNull { it.third }?.first
    }
}
