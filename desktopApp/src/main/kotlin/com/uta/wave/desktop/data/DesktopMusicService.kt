package com.uta.wave.desktop.data

import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.account.DesktopAccountManager
import com.uta.wave.desktop.account.DesktopUserProfile
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

    // -------------------------------------------------------------------------
    // User Account & Library Cloud Synchronization
    // -------------------------------------------------------------------------

    fun applyAuthHeaders(builder: Request.Builder, cookie: String?, sapisid: String?) {
        if (!cookie.isNullOrBlank()) {
            builder.addHeader("Cookie", cookie)
        }
        val resolvedSapisid = sapisid ?: cookie?.let { extractCookiePart(it, "SAPISID") ?: extractCookiePart(it, "__Secure-3PAPISID") }
        if (!resolvedSapisid.isNullOrBlank()) {
            val timestamp = System.currentTimeMillis() / 1000
            val input = "$timestamp $resolvedSapisid https://music.youtube.com"
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            val sha1 = digest.joinToString("") { b -> "%02x".format(b) }
            builder.addHeader("Authorization", "SAPISIDHASH ${timestamp}_$sha1")
        }
    }

    private fun extractCookiePart(cookie: String, key: String): String? {
        val regex = Regex("(?:^|;\\s*)$key=([^;]+)")
        return regex.find(cookie)?.groupValues?.get(1)
    }

    suspend fun fetchAccountProfile(cookie: String, sapisid: String? = null): DesktopUserProfile? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
            }
            val requestBuilder = Request.Builder()
                .url("$INNERTUBE_BASE_URL/account/account_menu")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", CLIENT_VERSION)

            applyAuthHeaders(requestBuilder, cookie, sapisid)

            val request = requestBuilder.post(payload.toString().toRequestBody(JSON_MEDIA_TYPE)).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val root = JSONObject(body)

                // Locate active account header in account menu
                val actions = root.optJSONArray("actions") ?: return@withContext null
                for (i in 0 until actions.length()) {
                    val action = actions.optJSONObject(i) ?: continue
                    val header = action.optJSONObject("openPopupAction")
                        ?.optJSONObject("popup")
                        ?.optJSONObject("multiPageMenuRenderer")
                        ?.optJSONObject("header")
                        ?.optJSONObject("activeAccountHeaderRenderer") ?: continue

                    val name = header.optJSONObject("accountName")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: header.optJSONObject("accountName")?.optString("simpleText")
                        ?: "Namikaze aftab"

                    val email = header.optJSONObject("email")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: header.optJSONObject("email")?.optString("simpleText")
                        ?: "Google Account Connected"

                    val handle = header.optJSONObject("channelHandle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")

                    val thumbs = header.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
                    var avatarUrl = if (thumbs != null && thumbs.length() > 0) {
                        thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
                    } else null
                    if (avatarUrl != null && avatarUrl.startsWith("//")) {
                        avatarUrl = "https:$avatarUrl"
                    }

                    return@withContext DesktopUserProfile(
                        isLoggedIn = true,
                        name = name,
                        email = email,
                        avatarUrl = avatarUrl,
                        handle = handle,
                        cookie = cookie,
                        sapisid = sapisid
                    )
                }
            }
        } catch (e: Exception) {
            println("[DesktopMusicService] fetchAccountProfile error: ${e.message}")
        }
        null
    }

    suspend fun fetchUserPlaylists(cookie: String, sapisid: String? = null): List<DesktopPlaylist> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<DesktopPlaylist>()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("browseId", "FEmusic_liked_playlists")
            }
            val reqBuilder = Request.Builder()
                .url("$INNERTUBE_BASE_URL/browse")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                    applyAuthHeaders(this, cookie, sapisid)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                parseUserPlaylistsRecursively(root, playlists)
                println("[DesktopMusicService] Discovered ${playlists.size} playlists from YouTube Music")
            }
        } catch (e: Exception) {
            println("[DesktopMusicService] fetchUserPlaylists error: ${e.message}")
        }
        playlists
    }

    private fun parseUserPlaylistsRecursively(json: Any?, outPlaylists: MutableList<DesktopPlaylist>) {
        when (json) {
            is JSONObject -> {
                if (json.has("musicTwoRowItemRenderer")) {
                    val renderer = json.optJSONObject("musicTwoRowItemRenderer")
                    if (renderer != null) {
                        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                        val navEndpoint = renderer.optJSONObject("navigationEndpoint")
                        val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId")
                            ?: navEndpoint?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")

                        val cleanId = browseId?.removePrefix("VL")?.removePrefix("MPSP") ?: ""
                        val ignored = setOf("LM", "VLLM", "LL", "VLLL", "SE", "VLSE", "WL", "VLWL", "RDPN")

                        if (cleanId.isNotBlank() && title.isNotBlank() && !ignored.contains(cleanId.uppercase())) {
                            val thumbUrl = renderer.optJSONObject("thumbnailRenderer")
                                ?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")
                                ?.optJSONArray("thumbnails")
                                ?.let { thumbs ->
                                    thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
                                }

                            val subtitle = renderer.optJSONObject("subtitle")?.optJSONArray("runs")?.let { runs ->
                                (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text") ?: "" }
                            }
                            val count = subtitle?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() } ?: 0

                            if (outPlaylists.none { it.id == cleanId }) {
                                outPlaylists.add(
                                    DesktopPlaylist(
                                        id = cleanId,
                                        name = title,
                                        description = subtitle,
                                        trackCount = count,
                                        coverUrl = thumbUrl
                                    )
                                )
                            }
                        }
                    }
                }
                val keys = json.keys()
                while (keys.hasNext()) {
                    parseUserPlaylistsRecursively(json.opt(keys.next()), outPlaylists)
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    parseUserPlaylistsRecursively(json.opt(i), outPlaylists)
                }
            }
        }
    }

    suspend fun getPlaylist(playlistId: String, cookie: String? = null, sapisid: String? = null): Pair<String, List<TrackItem>> = withContext(Dispatchers.IO) {
        val cleanId = playlistId.removePrefix("VL")
        val browseId = if (cleanId.startsWith("VL")) cleanId else "VL$cleanId"
        val tracks = mutableListOf<TrackItem>()
        var playlistTitle = "Playlist"

        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("browseId", browseId)
            }
            val reqBuilder = Request.Builder()
                .url("$INNERTUBE_BASE_URL/browse")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                    applyAuthHeaders(this, cookie, sapisid)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val root = JSONObject(body)
                        playlistTitle = root.optJSONObject("microformat")
                            ?.optJSONObject("microformatDataRenderer")
                            ?.optString("title", "Playlist") ?: "Playlist"

                        var nextContinuation = parsePlaylistTracks(root, tracks)

                        // Follow continuations up to 500 tracks
                        var pages = 0
                        while (!nextContinuation.isNullOrBlank() && pages < 10) {
                            pages++
                            val contPayload = JSONObject().apply {
                                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                                put("continuation", nextContinuation)
                            }
                            val contReq = Request.Builder()
                                .url("$INNERTUBE_BASE_URL/browse?continuation=$nextContinuation")
                                .apply {
                                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                                    applyAuthHeaders(this, cookie, sapisid)
                                }
                                .post(contPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                                .build()

                            client.newCall(contReq).execute().use { contRes ->
                                if (contRes.isSuccessful) {
                                    val contBody = contRes.body?.string() ?: ""
                                    nextContinuation = parsePlaylistTracks(JSONObject(contBody), tracks)
                                } else {
                                    nextContinuation = null
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("[DesktopMusicService] getPlaylist error: ${e.message}")
        }
        Pair(playlistTitle, tracks)
    }

    private fun parsePlaylistTracks(json: JSONObject, outTracks: MutableList<TrackItem>): String? {
        val contents = json.optJSONObject("contents")
            ?.optJSONObject("twoColumnBrowseResultsRenderer")
            ?.optJSONArray("secondaryContents")
            ?.optJSONObject(0)
            ?.optJSONObject("musicPlaylistShelfRenderer")
            ?.optJSONArray("contents")
            ?: json.optJSONObject("continuationContents")
                ?.optJSONObject("musicPlaylistShelfContinuation")
                ?.optJSONArray("contents")
            ?: return null

        var nextToken: String? = null

        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i) ?: continue
            val renderer = item.optJSONObject("musicResponsiveListItemRenderer")
            if (renderer != null) {
                parseTrackItem(renderer)?.let { track ->
                    if (outTracks.none { it.videoId == track.videoId }) {
                        outTracks.add(track)
                    }
                }
            }
            val contItem = item.optJSONObject("continuationItemRenderer")
            if (contItem != null) {
                nextToken = contItem.optJSONObject("continuationEndpoint")
                    ?.optJSONObject("continuationCommand")
                    ?.optString("token")
            }
        }
        return nextToken
    }

    suspend fun fetchLikedTracks(cookie: String, sapisid: String? = null): List<TrackItem> {
        val (_, tracks) = getPlaylist("LM", cookie, sapisid)
        return tracks
    }

    suspend fun syncFullLibrary(cookie: String, sapisid: String? = null): DesktopLibrary = withContext(Dispatchers.IO) {
        println("[DesktopMusicService] Initiating full library sync...")
        val rawPlaylists = fetchUserPlaylists(cookie, sapisid)
        val hydratedPlaylists = mutableListOf<DesktopPlaylist>()

        for (p in rawPlaylists) {
            println("[DesktopMusicService] Hydrating tracks for playlist: ${p.name} (${p.id})...")
            val (title, tracks) = getPlaylist(p.id, cookie, sapisid)
            hydratedPlaylists.add(
                p.copy(
                    name = if (title.isNotBlank() && title != "Playlist") title else p.name,
                    tracks = tracks,
                    trackCount = if (tracks.isNotEmpty()) tracks.size else p.trackCount,
                    coverUrl = p.coverUrl ?: tracks.firstOrNull()?.artworkUrl
                )
            )
        }

        val likedTracks = fetchLikedTracks(cookie, sapisid)
        println("[DesktopMusicService] Library sync complete: ${hydratedPlaylists.size} playlists, ${likedTracks.size} liked songs.")

        DesktopDatabase.getInstance().saveLibrary(hydratedPlaylists, likedTracks)
        DesktopDatabase.getInstance().library.value
    }
}
