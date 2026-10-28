package com.uta.wave.data.innertube

import android.util.Log
import com.uta.wave.player.AppLogger
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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    companion object {
        private const val TAG = "InnerTubeClient"
        private const val INNERTUBE_BASE_URL = "https://music.youtube.com/youtubei/v1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        // Unencoded base64 string for songs filter
        private const val SONGS_PARAM = "EgWKAQIIAWoKEAkQBRAKEAMQBA=="
        private const val CLIENT_VERSION = "1.20260213.01.00"

        // Verified working initial visitor data seed
        @Volatile
        private var cachedVisitorData: String = "CgtqVExzZXIxN21sZyiX6-zWBjIKCgJJThIEGgAgS2KrAgqoAkN0c0JDQVVTalFFQjhRWnVacVhJUUk4a1psd1ZkR3k4anl6UlVabUhOczhSaDVrSjVvVGduSVExZDc5QnFjenZtVzlFeGRhMzFPeXZLckVwSG4yQXJTT0hVRVpxTTVvRkpjSmx2RnMyaGtWZkxCY0J1WFVoQWRaUHZMcjhHZW5OQ0I0ZlMxVE5lNi1MRDB2V1VEMGNrX2ZXMFJab0VSNXZtUjFhdVhqV2ZEcHJEQmY4UWl6ZGlxalg0T1JFVGZzdm5SaGFuQkVvQlRKRkFkNnBuVS1BUFJDWW9oZXVNajJQUWxGUkZGWU85N0dva2ZmOFV4VENmRzNnLTE4ZElHVmMwVTdkY2Zhb203djNja0xqRDNKdlhyczZtdVFNUzRRTjJ3MGw3cGtG"
        @Volatile
        private var hasRefreshedVisitorData = false

        @Volatile
        var authCookie: String? = null
        @Volatile
        var authSapisid: String? = null
        @Volatile
        var contentCountry: String = "US"
        @Volatile
        var contentLanguage: String = "en"

        fun applyAuthHeaders(builder: okhttp3.Request.Builder) {
            authCookie?.let { builder.addHeader("Cookie", it) }
            authSapisid?.let {
                val timestamp = System.currentTimeMillis() / 1000
                val input = "$timestamp $it https://music.youtube.com"
                val md = java.security.MessageDigest.getInstance("SHA-1")
                val digest = md.digest(input.toByteArray(Charsets.UTF_8))
                val sha1 = digest.joinToString("") { b -> "%02x".format(b) }
                builder.addHeader("Authorization", "SAPISIDHASH ${timestamp}_$sha1")
            }
        }
    }

    private val commonHeaders: Map<String, String> = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Origin" to "https://music.youtube.com",
        "Referer" to "https://music.youtube.com/",
        "X-YouTube-Client-Name" to "67",
        "X-YouTube-Client-Version" to CLIENT_VERSION
    )

    suspend fun refreshVisitorDataIfNeeded() = withContext(Dispatchers.IO) {
        if (hasRefreshedVisitorData) return@withContext
        try {
            val req = Request.Builder()
                .url("https://music.youtube.com/sw.js_data")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val raw = res.body?.string() ?: return@use
                    if (raw.length > 5) {
                        val json = JSONArray(raw.substring(5))
                        // The sw.js_data JSON array has visitorData at data[0][7] or in nested array data[0][2]
                        val array0 = json.optJSONArray(0)
                        val s7 = array0?.optString(7, "")
                        if (!s7.isNullOrBlank() && s7.startsWith("Cg")) {
                            cachedVisitorData = s7
                            hasRefreshedVisitorData = true
                            AppLogger.i("Refreshed visitorData from sw.js_data (${s7.take(20)}...)")
                            return@withContext
                        }

                        val array2 = array0?.optJSONArray(2)
                        if (array2 != null) {
                            for (i in 0 until array2.length()) {
                                val s = array2.optString(i, "")
                                if (s.startsWith("Cg")) {
                                    cachedVisitorData = s
                                    hasRefreshedVisitorData = true
                                    AppLogger.i("Refreshed visitorData from array2 (${s.take(20)}...)")
                                    return@withContext
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to refresh visitorData: ${e.message}")
        }
    }

    private fun getClientContext(clientName: String = "WEB_REMIX", clientVersion: String = CLIENT_VERSION): JSONObject {
        val clientObj = JSONObject().apply {
            put("clientName", clientName)
            put("clientVersion", clientVersion)
            put("hl", contentLanguage)
            put("gl", if (contentCountry == "GLOBAL") "US" else contentCountry)
            put("visitorData", cachedVisitorData)
        }
        val context = JSONObject()
        context.put("client", clientObj)
        return context
    }

    suspend fun search(query: String): List<TrackItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        AppLogger.i("🔍 Searching for: \"$query\"")
        refreshVisitorDataIfNeeded()

        // 1. Try with SONGS_PARAM filter
        var results = searchInternal(query.trim(), SONGS_PARAM)
        if (results.isEmpty()) {
            AppLogger.i("Songs filter returned 0, attempting general search fallback...")
            results = searchInternal(query.trim(), null)
        }
        AppLogger.i("Search for \"$query\" found ${results.size} tracks")
        results
    }

    private fun searchInternal(query: String, filterParam: String?): List<TrackItem> {
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("query", query)
                if (!filterParam.isNullOrBlank()) {
                    put("params", filterParam)
                }
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
                if (!response.isSuccessful) {
                    AppLogger.w("Search HTTP failed: ${response.code} ${response.message}")
                    return emptyList()
                }
                val body = response.body?.string() ?: return emptyList()
                return parseSearchTracks(JSONObject(body))
            }
        } catch (e: Exception) {
            AppLogger.e("Search network error for: $query", e)
            return emptyList()
        }
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("input", query.trim())
            }

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/music/get_search_suggestions")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                }
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
        refreshVisitorDataIfNeeded()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("browseId", "FEmusic_charts")
            }

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/browse")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                    applyAuthHeaders(this)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val parsed = parseBrowseTracks(JSONObject(body))
                        if (parsed.isNotEmpty()) {
                            AppLogger.i("Loaded ${parsed.size} trending tracks from FEmusic_charts")
                            return@withContext parsed
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w("Failed to get trending: ${e.message}")
        }
        search("Top Global Hits 2026")
    }

    suspend fun getMoodSongs(mood: String): List<TrackItem> = withContext(Dispatchers.IO) {
        val query = "$mood music hits"
        search(query)
    }

    fun extractYouTubePlaylistId(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.contains("list=") -> {
                trimmed.substringAfter("list=").substringBefore("&").substringBefore("#").substringBefore("\"").trim()
            }
            trimmed.contains("/browse/VL") -> {
                trimmed.substringAfter("/browse/VL").substringBefore("?").substringBefore("&").substringBefore("/").trim()
            }
            trimmed.contains("/browse/") -> {
                trimmed.substringAfter("/browse/").substringBefore("?").substringBefore("&").substringBefore("/").trim()
            }
            trimmed.startsWith("VL") -> trimmed.removePrefix("VL").trim()
            else -> trimmed
        }
    }

    suspend fun getPlaylist(playlistId: String): Pair<String, List<TrackItem>> = withContext(Dispatchers.IO) {
        refreshVisitorDataIfNeeded()
        val cleanId = extractYouTubePlaylistId(playlistId)
        val browseId = if (cleanId.startsWith("VL")) cleanId else "VL$cleanId"
        AppLogger.i("Fetching playlist cleanId: $cleanId, browseId: $browseId")

        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("browseId", browseId)
            }

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/browse")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                    applyAuthHeaders(this)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val root = JSONObject(body)
                        val tracks = mutableListOf<TrackItem>()
                        var nextContinuation = parsePlaylistTracksScoped(root, tracks)

                        // Follow InnerTube continuation tokens to fetch all pages (100, 200, 500+ tracks)
                        var pagesFetched = 0
                        val maxContinuationPages = 60 // Allows up to 6,000 tracks per playlist

                        while (!nextContinuation.isNullOrBlank() && pagesFetched < maxContinuationPages) {
                            pagesFetched++
                            AppLogger.i("Fetching playlist continuation page $pagesFetched (current: ${tracks.size} tracks)...")
                            try {
                                val contPayload = JSONObject().apply {
                                    put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                                    put("continuation", nextContinuation)
                                }
                                val contRequest = Request.Builder()
                                    .url("$INNERTUBE_BASE_URL/browse?continuation=$nextContinuation")
                                    .apply {
                                        commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                                        addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                                        applyAuthHeaders(this)
                                    }
                                    .post(contPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                                    .build()

                                val contResponse = client.newCall(contRequest).execute()
                                if (!contResponse.isSuccessful) {
                                    AppLogger.w("Playlist continuation page $pagesFetched failed: HTTP ${contResponse.code}")
                                    break
                                }
                                val contBody = contResponse.body?.string() ?: break
                                val contJson = JSONObject(contBody)
                                val initialCount = tracks.size
                                nextContinuation = parsePlaylistTracksScoped(contJson, tracks)
                                val addedCount = tracks.size - initialCount
                                AppLogger.i("Playlist continuation page $pagesFetched added $addedCount tracks (total: ${tracks.size})")

                                if (addedCount == 0) break
                            } catch (e: Exception) {
                                AppLogger.w("Playlist continuation error on page $pagesFetched: ${e.message}")
                                break
                            }
                        }

                        // Extract playlist title
                        var title = ""
                        try {
                            // 1. microformat
                            title = root.optJSONObject("microformat")
                                ?.optJSONObject("microformatDataRenderer")
                                ?.optString("title", "") ?: ""

                            // 2. tabs -> sectionList -> musicResponsiveHeaderRenderer
                            if (title.isBlank()) {
                                val tabs = root.optJSONObject("contents")
                                    ?.optJSONObject("twoColumnBrowseResultsRenderer")
                                    ?.optJSONArray("tabs")
                                val tabContent = tabs?.optJSONObject(0)
                                    ?.optJSONObject("tabRenderer")
                                    ?.optJSONObject("content")
                                    ?.optJSONObject("sectionListRenderer")
                                    ?.optJSONArray("contents")
                                title = tabContent?.optJSONObject(0)
                                    ?.optJSONObject("musicResponsiveHeaderRenderer")
                                    ?.optJSONObject("title")
                                    ?.optJSONArray("runs")
                                    ?.optJSONObject(0)
                                    ?.optString("text", "") ?: ""
                            }

                            // 3. header fallback
                            if (title.isBlank()) {
                                val header = root.optJSONObject("header")
                                    ?.optJSONObject("musicResponsiveHeaderRenderer")
                                    ?: root.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer")
                                title = header?.optJSONObject("title")
                                    ?.optJSONArray("runs")
                                    ?.optJSONObject(0)
                                    ?.optString("text", "") ?: ""
                            }
                        } catch (e: Exception) {
                            AppLogger.w("Could not extract playlist title: ${e.message}")
                        }

                        if (title.isBlank()) {
                            title = "Imported Playlist"
                        }

                        if (tracks.isNotEmpty()) {
                            AppLogger.i("Successfully parsed ${tracks.size} tracks from playlist: $title")
                            return@withContext Pair(title, tracks)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to get playlist: ${e.message}", e)
        }

        // Fallback 2: If WEB_REMIX returned 0, try standard WEB client for standard YouTube playlists
        try {
            AppLogger.i("Trying WEB client fallback for playlist: $browseId")
            val webPayload = JSONObject().apply {
                val clientObj = JSONObject().apply {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20240101.01.00")
                    put("hl", "en")
                    put("gl", "US")
                }
                put("context", JSONObject().put("client", clientObj))
                put("browseId", browseId)
            }
            val webReq = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/browse")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(webPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(webReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val webBody = resp.body?.string()
                    if (webBody != null) {
                        val webRoot = JSONObject(webBody)
                        val webTracks = mutableListOf<TrackItem>()
                        var webCont = parsePlaylistTracksScoped(webRoot, webTracks)

                        // Follow WEB client continuations
                        var webPages = 0
                        while (!webCont.isNullOrBlank() && webPages < 60) {
                            webPages++
                            try {
                                val cPayload = JSONObject().apply {
                                    val clientObj = JSONObject().apply {
                                        put("clientName", "WEB")
                                        put("clientVersion", "2.20240101.01.00")
                                        put("hl", "en")
                                        put("gl", "US")
                                    }
                                    put("context", JSONObject().put("client", clientObj))
                                    put("continuation", webCont)
                                }
                                val cReq = Request.Builder()
                                    .url("https://www.youtube.com/youtubei/v1/browse?continuation=$webCont")
                                    .header("Content-Type", "application/json")
                                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    .post(cPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                                    .build()
                                val cResp = client.newCall(cReq).execute()
                                if (!cResp.isSuccessful) break
                                val cBody = cResp.body?.string() ?: break
                                val cJson = JSONObject(cBody)
                                val initial = webTracks.size
                                webCont = parsePlaylistTracksScoped(cJson, webTracks)
                                val added = webTracks.size - initial
                                if (added == 0) break
                            } catch (e: Exception) {
                                break
                            }
                        }

                        val webTitle = webRoot.optJSONObject("metadata")
                            ?.optJSONObject("playlistMetadataRenderer")
                            ?.optString("title", "YouTube Playlist")
                            ?: "YouTube Playlist"
                        if (webTracks.isNotEmpty()) {
                            AppLogger.i("Successfully parsed ${webTracks.size} tracks via WEB client fallback: $webTitle")
                            return@withContext Pair(webTitle, webTracks)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("WEB fallback failed: ${e.message}", e)
        }

        // Fallback 3: search for playlist title or query
        val searchFallback = search(cleanId)
        Pair("YouTube Playlist", searchFallback)
    }

    suspend fun resolveStreamUrl(videoId: String, qualityTag: String = "LOSSLESS"): String? = withContext(Dispatchers.IO) {
        AppLogger.i("🎯 Resolving audio stream ($qualityTag) for videoId: $videoId")
        refreshVisitorDataIfNeeded()

        // 1. Primary: VISIONOS client (1.02, Client ID 101) - proven to serve whole unthrottled audio stream without 403
        val visionResult = tryResolveVisionOS(videoId, qualityTag)
        if (!visionResult.isNullOrBlank()) {
            AppLogger.i("✅ Resolved stream via VISIONOS client for $videoId")
            return@withContext visionResult
        }

        // 2. Secondary: ANDROID_VR client (1.65.10) with visitorData
        AppLogger.w("VISIONOS returned no stream, falling back to ANDROID_VR...")
        val vrResult = tryResolveVR(videoId, qualityTag)
        if (!vrResult.isNullOrBlank()) {
            AppLogger.i("✅ Resolved stream via ANDROID_VR client for $videoId")
            return@withContext vrResult
        }

        // 3. Tertiary: IOS client (19.45.4, Client ID 5)
        AppLogger.w("ANDROID_VR returned no stream, falling back to IOS...")
        val iosResult = tryResolveIOS(videoId, qualityTag)
        if (!iosResult.isNullOrBlank()) {
            AppLogger.i("✅ Resolved stream via IOS client for $videoId")
            return@withContext iosResult
        }

        AppLogger.e("❌ All stream extractors failed for videoId: $videoId")
        null
    }

    private data class StreamCandidate(
        val url: String,
        val mime: String,
        val bitrate: Long,
        val itag: Int
    )

    private fun extractBestStreamUrl(adaptive: JSONArray, qualityTag: String = "LOSSLESS"): String? {
        val candidates = mutableListOf<StreamCandidate>()
        for (i in 0 until adaptive.length()) {
            val format = adaptive.optJSONObject(i) ?: continue
            val mime = format.optString("mimeType", "")
            val url = format.optString("url", "")
            if (mime.contains("audio/") && url.isNotBlank()) {
                val bitrate = format.optLong("bitrate", 128000L)
                val itag = format.optInt("itag", 0)
                candidates.add(StreamCandidate(url, mime, bitrate, itag))
            }
        }
        if (candidates.isEmpty()) return null

        return when (qualityTag.uppercase()) {
            "AAC", "RINGTONE" -> {
                // ITAG 140 is 128kbps AAC (audio/mp4; codecs="mp4a.40.2"), 141 is 256kbps AAC
                // Prioritize mp4/aac audio format for ringtone/media remuxing
                candidates.filter { it.mime.contains("audio/mp4") || it.mime.contains("mp4a") }
                    .maxByOrNull { it.bitrate }?.url
                    ?: candidates.find { it.itag == 140 }?.url
                    ?: candidates.maxByOrNull { it.bitrate }?.url
            }
            "LOSSLESS" -> {
                // Highest bitrate format available (e.g. 256k AAC or 160k Opus)
                candidates.maxByOrNull { it.bitrate }?.url
            }
            "HIGH" -> {
                candidates.filter { it.bitrate in 140000L..200000L }.maxByOrNull { it.bitrate }?.url
                    ?: candidates.maxByOrNull { it.bitrate }?.url
            }
            "STANDARD" -> {
                candidates.find { it.mime.contains("audio/mp4") && it.bitrate in 100000L..150000L }?.url
                    ?: candidates.find { it.mime.contains("audio/mp4") }?.url
                    ?: candidates.firstOrNull()?.url
            }
            else -> candidates.maxByOrNull { it.bitrate }?.url
        }
    }

    private fun tryResolveVisionOS(videoId: String, qualityTag: String = "LOSSLESS"): String? {
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
                if (!response.isSuccessful) {
                    AppLogger.w("VISIONOS HTTP ${response.code}: ${response.message}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val status = json.optJSONObject("playabilityStatus")?.optString("status", "")
                val reason = json.optJSONObject("playabilityStatus")?.optString("reason", "")
                AppLogger.i("VISIONOS playabilityStatus: $status (reason: $reason)")
                if (status != "OK") return null

                val streamData = json.optJSONObject("streamingData") ?: return null
                val adaptive = streamData.optJSONArray("adaptiveFormats") ?: JSONArray()
                return extractBestStreamUrl(adaptive, qualityTag)
            }
        } catch (e: Exception) {
            AppLogger.w("VISIONOS resolve failed: ${e.message}")
        }
        return null
    }

    private fun tryResolveVR(videoId: String, qualityTag: String = "LOSSLESS"): String? {
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
                if (!response.isSuccessful) {
                    AppLogger.w("ANDROID_VR HTTP ${response.code}: ${response.message}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val status = json.optJSONObject("playabilityStatus")?.optString("status", "")
                val reason = json.optJSONObject("playabilityStatus")?.optString("reason", "")
                AppLogger.i("ANDROID_VR playabilityStatus: $status (reason: $reason)")
                if (status != "OK") return null

                val streamData = json.optJSONObject("streamingData") ?: return null
                val adaptive = streamData.optJSONArray("adaptiveFormats") ?: JSONArray()
                return extractBestStreamUrl(adaptive, qualityTag)
            }
        } catch (e: Exception) {
            AppLogger.w("ANDROID_VR resolve failed: ${e.message}")
        }
        return null
    }

    private fun tryResolveIOS(videoId: String, qualityTag: String = "LOSSLESS"): String? {
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
                val status = json.optJSONObject("playabilityStatus")?.optString("status", "")
                if (status != "OK") return null

                val streamData = json.optJSONObject("streamingData") ?: return null
                val adaptive = streamData.optJSONArray("adaptiveFormats") ?: JSONArray()
                return extractBestStreamUrl(adaptive, qualityTag)
            }
        } catch (e: Exception) {
            AppLogger.w("IOS resolve failed: ${e.message}")
        }
        return null
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
        var artistId: String? = null
        var albumId: String? = null
        var albumName: String? = null

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
                    val runObj = col1.optJSONObject(k) ?: continue
                    val t = runObj.optString("text", "")
                    val bId = runObj.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")
                        ?.optString("browseId")

                    if (bId != null) {
                        if (bId.startsWith("UC") && artistId == null) {
                            artistId = bId
                        } else if ((bId.startsWith("MPREb_") || bId.startsWith("OLAK5uy_")) && albumId == null) {
                            albumId = bId
                            albumName = t
                        }
                    }

                    if (t.contains(":") && t.length <= 8) {
                        duration = parseDuration(t)
                    } else if (t.isNotBlank() && t != "•" && t != " & " && t != ", ") {
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
            album = albumName,
            artworkUrl = thumbnail,
            durationSeconds = duration,
            isSynced = true,
            artistId = artistId,
            albumId = albumId
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

    private fun parsePlaylistVideoRenderer(renderer: JSONObject): TrackItem? {
        val videoId = renderer.optString("videoId")
        if (videoId.length != 11) return null

        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: renderer.optJSONObject("title")?.optString("simpleText")
            ?: "Unknown Title"

        val artist = renderer.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: "YouTube"

        val duration = renderer.optString("lengthSeconds").toIntOrNull() ?: 0
        val thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

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

    fun parsePlaylistPanelVideoRenderer(renderer: JSONObject): TrackItem? {
        val videoId = renderer.optString("videoId")
        if (videoId.length != 11) return null

        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: renderer.optJSONObject("title")?.optString("simpleText")
            ?: "Unknown Title"

        val bylineRuns = renderer.optJSONObject("longBylineText")?.optJSONArray("runs")
            ?: renderer.optJSONObject("shortBylineText")?.optJSONArray("runs")

        var artist = "YouTube"
        var artistId: String? = null
        var album: String? = null
        var albumId: String? = null

        if (bylineRuns != null && bylineRuns.length() > 0) {
            val firstRun = bylineRuns.optJSONObject(0)
            artist = firstRun?.optString("text") ?: "YouTube"
            artistId = firstRun?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId")

            for (i in 1 until bylineRuns.length()) {
                val runObj = bylineRuns.optJSONObject(i) ?: continue
                val text = runObj.optString("text", "")
                val bId = runObj.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                if (!bId.isNullOrBlank() && (bId.startsWith("MPREb_") || bId.startsWith("FEmusic_library_album"))) {
                    album = text
                    albumId = bId
                    break
                }
            }
        }

        val durationText = renderer.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: renderer.optJSONObject("lengthText")?.optString("simpleText") ?: ""
        val duration = parseDuration(durationText)

        val thumbs = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumbnail = if (thumbs != null && thumbs.length() > 0) {
            val url = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
            upgradeThumbnail(url)
        } else {
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        }

        return TrackItem(
            id = "youtube:$videoId",
            videoId = videoId,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = thumbnail,
            durationSeconds = duration,
            isSynced = true,
            artistId = artistId,
            albumId = albumId
        )
    }

    suspend fun getNext(
        videoId: String? = null,
        playlistId: String? = null,
        continuation: String? = null
    ): NextResult? = withContext(Dispatchers.IO) {
        refreshVisitorDataIfNeeded()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                if (!continuation.isNullOrBlank()) {
                    put("continuation", continuation)
                } else {
                    if (!videoId.isNullOrBlank()) {
                        put("videoId", videoId)
                    }
                    val targetPlaylistId = when {
                        !playlistId.isNullOrBlank() -> playlistId
                        !videoId.isNullOrBlank() -> "RDAMVM$videoId"
                        else -> null
                    }
                    if (targetPlaylistId != null) {
                        put("playlistId", targetPlaylistId)
                    }
                    put("isAudioOnly", true)
                }
            }

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/next")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                    applyAuthHeaders(this)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLogger.w("InnerTube /next failed with HTTP ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val root = JSONObject(bodyStr)

                val tracks = mutableListOf<TrackItem>()
                var nextContinuation: String? = null
                var title: String? = null

                if (!continuation.isNullOrBlank()) {
                    val contContents = root.optJSONObject("continuationContents")
                        ?.optJSONObject("playlistPanelContinuation")
                    if (contContents != null) {
                        val contents = contContents.optJSONArray("contents")
                        if (contents != null) {
                            for (i in 0 until contents.length()) {
                                val item = contents.optJSONObject(i)?.optJSONObject("playlistPanelVideoRenderer")
                                if (item != null) {
                                    parsePlaylistPanelVideoRenderer(item)?.let { tracks.add(it) }
                                }
                            }
                        }
                        val contArray = contContents.optJSONArray("continuations")
                        if (contArray != null && contArray.length() > 0) {
                            val contObj = contArray.optJSONObject(0)
                            nextContinuation = contObj?.optJSONObject("nextRadioContinuationData")?.optString("continuation")
                                ?: contObj?.optJSONObject("nextContinuationData")?.optString("continuation")
                        }
                    }
                } else {
                    val tabs = root.optJSONObject("contents")
                        ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                        ?.optJSONObject("tabbedRenderer")
                        ?.optJSONObject("watchNextTabbedResultsRenderer")
                        ?.optJSONArray("tabs")

                    val playlistPanel = tabs?.optJSONObject(0)
                        ?.optJSONObject("tabRenderer")
                        ?.optJSONObject("content")
                        ?.optJSONObject("musicQueueRenderer")
                        ?.optJSONObject("content")
                        ?.optJSONObject("playlistPanelRenderer")

                    if (playlistPanel != null) {
                        title = playlistPanel.optString("title")
                        val contents = playlistPanel.optJSONArray("contents")
                        if (contents != null) {
                            for (i in 0 until contents.length()) {
                                val item = contents.optJSONObject(i)?.optJSONObject("playlistPanelVideoRenderer")
                                if (item != null) {
                                    parsePlaylistPanelVideoRenderer(item)?.let { tracks.add(it) }
                                }
                            }
                        }

                        val contArray = playlistPanel.optJSONArray("continuations")
                        if (contArray != null && contArray.length() > 0) {
                            val contObj = contArray.optJSONObject(0)
                            nextContinuation = contObj?.optJSONObject("nextRadioContinuationData")?.optString("continuation")
                                ?: contObj?.optJSONObject("nextContinuationData")?.optString("continuation")
                        }
                    }

                    if (tracks.isEmpty()) {
                        parseTracksRecursively(root, tracks)
                    }
                }

                AppLogger.i("InnerTube /next resolved ${tracks.size} tracks, nextContinuation=${nextContinuation != null}")
                NextResult(
                    title = title,
                    items = tracks,
                    continuation = nextContinuation,
                    playlistId = playlistId ?: videoId?.let { "RDAMVM$it" }
                )
            }
        } catch (e: Exception) {
            AppLogger.e("InnerTube getNext failed: ${e.message}", e)
            null
        }
    }

    private fun extractBrowseContinuationToken(json: Any?): String? {
        when (json) {
            is JSONObject -> {
                // 1. Direct continuationItemRenderer
                if (json.has("continuationItemRenderer")) {
                    val contItem = json.optJSONObject("continuationItemRenderer")
                    val token = contItem?.optJSONObject("continuationEndpoint")
                        ?.optJSONObject("continuationCommand")
                        ?.optString("token")
                    if (!token.isNullOrBlank()) return token
                }
                // 2. nextContinuationData (in continuations array)
                if (json.has("nextContinuationData")) {
                    val token = json.optJSONObject("nextContinuationData")?.optString("continuation")
                    if (!token.isNullOrBlank()) return token
                }
                // 3. nextRadioContinuationData
                if (json.has("nextRadioContinuationData")) {
                    val token = json.optJSONObject("nextRadioContinuationData")?.optString("continuation")
                    if (!token.isNullOrBlank()) return token
                }
                // 4. continuationContents -> musicPlaylistShelfContinuation -> continuations
                if (json.has("musicPlaylistShelfContinuation")) {
                    val shelfCont = json.optJSONObject("musicPlaylistShelfContinuation")
                    val contArr = shelfCont?.optJSONArray("continuations")
                    if (contArr != null && contArr.length() > 0) {
                        val token = contArr.optJSONObject(0)
                            ?.optJSONObject("nextContinuationData")
                            ?.optString("continuation")
                        if (!token.isNullOrBlank()) return token
                    }
                }
                // 5. SectionListContinuation
                if (json.has("sectionListContinuation")) {
                    val secCont = json.optJSONObject("sectionListContinuation")
                    val contArr = secCont?.optJSONArray("continuations")
                    if (contArr != null && contArr.length() > 0) {
                        val token = contArr.optJSONObject(0)
                            ?.optJSONObject("nextContinuationData")
                            ?.optString("continuation")
                        if (!token.isNullOrBlank()) return token
                    }
                }

                // Recursively check keys, skipping track renderers for speed
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "musicResponsiveListItemRenderer" || key == "playlistVideoRenderer" || key == "playlistPanelVideoRenderer") {
                        continue
                    }
                    val found = extractBrowseContinuationToken(json.opt(key))
                    if (!found.isNullOrBlank()) return found
                }
            }
            is JSONArray -> {
                // Check from end of array because continuation items are at the tail
                for (i in (json.length() - 1) downTo 0) {
                    val found = extractBrowseContinuationToken(json.opt(i))
                    if (!found.isNullOrBlank()) return found
                }
            }
        }
        return null
    }

    /**
     * Parses tracks strictly from the playlist shelf and extracts the continuation token
     * solely belonging to that playlist shelf, preventing recommended/suggested carousels
     * from polluting user playlists.
     */
    private fun parsePlaylistTracksScoped(json: JSONObject, outTracks: MutableList<TrackItem>): String? {
        // 1. Check for continuationContents -> musicPlaylistShelfContinuation / playlistVideoListContinuation
        val contShelf = json.optJSONObject("continuationContents")?.optJSONObject("musicPlaylistShelfContinuation")
            ?: json.optJSONObject("continuationContents")?.optJSONObject("playlistVideoListContinuation")
            ?: json.optJSONObject("musicPlaylistShelfContinuation")
            ?: json.optJSONObject("playlistVideoListContinuation")

        val shelf = contShelf ?: findPlaylistShelf(json)

        if (shelf != null) {
            val contents = shelf.optJSONArray("contents")
            var contToken: String? = null

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val item = contents.optJSONObject(i) ?: continue

                    // Check if item is a track
                    val track = item.optJSONObject("musicResponsiveListItemRenderer")?.let { parseTrackRenderer(it) }
                        ?: item.optJSONObject("playlistVideoRenderer")?.let { parsePlaylistVideoRenderer(it) }
                        ?: item.optJSONObject("playlistPanelVideoRenderer")?.let { parsePlaylistPanelVideoRenderer(it) }

                    if (track != null) {
                        if (outTracks.none { it.videoId == track.videoId }) {
                            outTracks.add(track)
                        }
                    }

                    // Check if item is a continuationItemRenderer
                    val contItem = item.optJSONObject("continuationItemRenderer")
                    if (contItem != null) {
                        val token = contItem.optJSONObject("continuationEndpoint")
                            ?.optJSONObject("continuationCommand")
                            ?.optString("token")
                        if (!token.isNullOrBlank()) {
                            contToken = token
                        }
                    }
                }
            }

            // Also check shelf continuations array
            if (contToken.isNullOrBlank()) {
                val contArr = shelf.optJSONArray("continuations")
                if (contArr != null && contArr.length() > 0) {
                    val token = contArr.optJSONObject(0)
                        ?.optJSONObject("nextContinuationData")
                        ?.optString("continuation")
                    if (!token.isNullOrBlank()) {
                        contToken = token
                    }
                }
            }

            return contToken
        }

        // Fallback for non-standard structures: parse only if no shelf could be identified
        val initialSize = outTracks.size
        parseTracksRecursively(json, outTracks)
        return if (outTracks.size > initialSize) extractBrowseContinuationToken(json) else null
    }

    private fun findPlaylistShelf(obj: JSONObject): JSONObject? {
        if (obj.has("musicPlaylistShelfRenderer")) return obj.optJSONObject("musicPlaylistShelfRenderer")
        if (obj.has("playlistVideoListRenderer")) return obj.optJSONObject("playlistVideoListRenderer")

        // Look specifically in twoColumnBrowseResultsRenderer -> secondaryContents or tabs
        val twoCol = obj.optJSONObject("contents")?.optJSONObject("twoColumnBrowseResultsRenderer")
        if (twoCol != null) {
            val secContents = twoCol.optJSONObject("secondaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            if (secContents != null) {
                for (i in 0 until secContents.length()) {
                    val item = secContents.optJSONObject(i) ?: continue
                    if (item.has("musicPlaylistShelfRenderer")) {
                        return item.optJSONObject("musicPlaylistShelfRenderer")
                    }
                }
            }

            val tabs = twoCol.optJSONArray("tabs")
            if (tabs != null && tabs.length() > 0) {
                val tabContents = tabs.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents")
                if (tabContents != null) {
                    for (i in 0 until tabContents.length()) {
                        val item = tabContents.optJSONObject(i) ?: continue
                        if (item.has("musicPlaylistShelfRenderer")) {
                            return item.optJSONObject("musicPlaylistShelfRenderer")
                        }
                        if (item.has("itemSectionRenderer")) {
                            val isrContents = item.optJSONObject("itemSectionRenderer")?.optJSONArray("contents")
                            if (isrContents != null) {
                                for (j in 0 until isrContents.length()) {
                                    val isrItem = isrContents.optJSONObject(j) ?: continue
                                    if (isrItem.has("playlistVideoListRenderer")) {
                                        return isrItem.optJSONObject("playlistVideoListRenderer")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null
    }

    private fun parseTracksRecursively(json: Any?, outTracks: MutableList<TrackItem>) {
        when (json) {
            is JSONObject -> {
                val panelRenderer = json.optJSONObject("playlistPanelVideoRenderer")
                if (panelRenderer != null) {
                    val track = parsePlaylistPanelVideoRenderer(panelRenderer)
                    if (track != null && outTracks.none { it.videoId == track.videoId }) {
                        outTracks.add(track)
                    }
                }
                val musicRenderer = json.optJSONObject("musicResponsiveListItemRenderer")
                if (musicRenderer != null) {
                    val track = parseTrackRenderer(musicRenderer)
                    if (track != null && outTracks.none { it.videoId == track.videoId }) {
                        outTracks.add(track)
                    }
                }
                val videoRenderer = json.optJSONObject("playlistVideoRenderer")
                if (videoRenderer != null) {
                    val track = parsePlaylistVideoRenderer(videoRenderer)
                    if (track != null && outTracks.none { it.videoId == track.videoId }) {
                        outTracks.add(track)
                    }
                }
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    parseTracksRecursively(json.opt(key), outTracks)
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    parseTracksRecursively(json.opt(i), outTracks)
                }
            }
        }
    }

    suspend fun getArtist(artistBrowseId: String): ArtistPage? = withContext(Dispatchers.IO) {
        refreshVisitorDataIfNeeded()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("browseId", artistBrowseId)
            }

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/browse")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                    applyAuthHeaders(this)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLogger.w("Artist browse HTTP failed: ${response.code} ${response.message}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                return@withContext parseArtistPage(artistBrowseId, JSONObject(body))
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to get artist for $artistBrowseId", e)
            null
        }
    }

    suspend fun getAlbum(albumBrowseId: String): AlbumPage? = withContext(Dispatchers.IO) {
        refreshVisitorDataIfNeeded()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("browseId", albumBrowseId)
            }

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/browse")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                    applyAuthHeaders(this)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLogger.w("Album browse HTTP failed: ${response.code} ${response.message}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                return@withContext parseAlbumPage(albumBrowseId, JSONObject(body))
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to get album for $albumBrowseId", e)
            null
        }
    }

    suspend fun resolveArtistBrowseId(artistName: String): String? = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext null
        refreshVisitorDataIfNeeded()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("query", artistName.trim())
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
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val tabs = json.optJSONObject("contents")
                    ?.optJSONObject("tabbedSearchResultsRenderer")
                    ?.optJSONArray("tabs")
                val sections = tabs?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents") ?: return@withContext null

                for (i in 0 until sections.length()) {
                    val s = sections.optJSONObject(i) ?: continue
                    val card = s.optJSONObject("musicCardShelfRenderer")
                    if (card != null) {
                        val ep = card.optJSONObject("title")
                            ?.optJSONArray("runs")?.optJSONObject(0)
                            ?.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("browseEndpoint")
                            ?.optString("browseId")
                        if (ep != null && ep.startsWith("UC")) return@withContext ep
                    }

                    val shelf = s.optJSONObject("musicShelfRenderer")
                    if (shelf != null) {
                        val items = shelf.optJSONArray("contents") ?: continue
                        for (j in 0 until items.length()) {
                            val item = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                            val flex = item.optJSONArray("flexColumns") ?: continue
                            for (c in 0 until flex.length()) {
                                val runs = flex.optJSONObject(c)
                                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                    ?.optJSONObject("text")?.optJSONArray("runs") ?: continue
                                for (r in 0 until runs.length()) {
                                    val rObj = runs.optJSONObject(r) ?: continue
                                    val bId = rObj.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                                    if (bId != null && bId.startsWith("UC")) {
                                        return@withContext bId
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w("resolveArtistBrowseId failed for $artistName: ${e.message}")
        }
        null
    }

    suspend fun resolveAlbumBrowseId(albumName: String, artistName: String): String? = withContext(Dispatchers.IO) {
        if (albumName.isBlank()) return@withContext null
        refreshVisitorDataIfNeeded()
        val query = "$albumName $artistName".trim()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("query", query)
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
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val tabs = json.optJSONObject("contents")
                    ?.optJSONObject("tabbedSearchResultsRenderer")
                    ?.optJSONArray("tabs")
                val sections = tabs?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents") ?: return@withContext null

                for (i in 0 until sections.length()) {
                    val s = sections.optJSONObject(i) ?: continue
                    val card = s.optJSONObject("musicCardShelfRenderer")
                    if (card != null) {
                        val ep = card.optJSONObject("title")
                            ?.optJSONArray("runs")?.optJSONObject(0)
                            ?.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("browseEndpoint")
                            ?.optString("browseId")
                        if (ep != null && (ep.startsWith("MPREb_") || ep.startsWith("OLAK5uy_"))) return@withContext ep
                    }

                    val shelf = s.optJSONObject("musicShelfRenderer")
                    if (shelf != null) {
                        val items = shelf.optJSONArray("contents") ?: continue
                        for (j in 0 until items.length()) {
                            val item = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                            val flex = item.optJSONArray("flexColumns") ?: continue
                            for (c in 0 until flex.length()) {
                                val runs = flex.optJSONObject(c)
                                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                    ?.optJSONObject("text")?.optJSONArray("runs") ?: continue
                                for (r in 0 until runs.length()) {
                                    val rObj = runs.optJSONObject(r) ?: continue
                                    val bId = rObj.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                                    if (bId != null && (bId.startsWith("MPREb_") || bId.startsWith("OLAK5uy_"))) {
                                        return@withContext bId
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w("resolveAlbumBrowseId failed for $query: ${e.message}")
        }
        null
    }

    private fun parseArtistPage(browseId: String, root: JSONObject): ArtistPage {
        val header = root.optJSONObject("header")
        val imm = header?.optJSONObject("musicImmersiveHeaderRenderer")
            ?: header?.optJSONObject("musicVisualHeaderRenderer")

        val artistName = imm?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: "Artist"
        val bio = imm?.optJSONObject("description")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
        val subscribers = imm?.optJSONObject("subscriptionButton")
            ?.optJSONObject("subscribeButtonRenderer")
            ?.optJSONObject("subscriberCountText")
            ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: imm?.optJSONObject("subtitle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")

        val thumbs = imm?.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val heroUrl = if (thumbs != null && thumbs.length() > 0) {
            val raw = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
            upgradeThumbnail(raw)
        } else null

        val topSongs = mutableListOf<TrackItem>()
        val albums = mutableListOf<AlbumCard>()
        val singles = mutableListOf<AlbumCard>()
        val similarArtists = mutableListOf<ArtistCard>()

        val tabs = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
        val sections = tabs?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: JSONArray()

        for (i in 0 until sections.length()) {
            val sec = sections.optJSONObject(i) ?: continue
            val shelf = sec.optJSONObject("musicShelfRenderer")
                ?: sec.optJSONObject("musicCarouselShelfRenderer") ?: continue

            val titleRuns = shelf.optJSONObject("title")?.optJSONArray("runs")
            val title = (titleRuns?.optJSONObject(0)?.optString("text") ?: "").lowercase()
            val contents = shelf.optJSONArray("contents") ?: continue

            when {
                title.contains("song") -> {
                    for (j in 0 until contents.length()) {
                        val item = contents.optJSONObject(j) ?: continue
                        val renderer = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                        val track = parseTrackRenderer(renderer)
                        if (track != null) topSongs.add(track)
                    }
                }
                title.contains("album") -> {
                    for (j in 0 until contents.length()) {
                        val item = contents.optJSONObject(j) ?: continue
                        val card = parseAlbumCard(item)
                        if (card != null) albums.add(card)
                    }
                }
                title.contains("single") -> {
                    for (j in 0 until contents.length()) {
                        val item = contents.optJSONObject(j) ?: continue
                        val card = parseAlbumCard(item)
                        if (card != null) singles.add(card)
                    }
                }
                title.contains("fan") || title.contains("similar") || title.contains("like") -> {
                    for (j in 0 until contents.length()) {
                        val item = contents.optJSONObject(j) ?: continue
                        val card = parseArtistCard(item)
                        if (card != null) similarArtists.add(card)
                    }
                }
                else -> {
                    if (contents.length() > 0) {
                        val first = contents.optJSONObject(0)
                        val twoRow = first?.optJSONObject("musicTwoRowItemRenderer")
                        val bId = twoRow?.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("browseEndpoint")
                            ?.optString("browseId", "") ?: ""
                        if (bId.startsWith("UC")) {
                            for (j in 0 until contents.length()) {
                                val item = contents.optJSONObject(j) ?: continue
                                val card = parseArtistCard(item)
                                if (card != null) similarArtists.add(card)
                            }
                        } else if (bId.startsWith("MPREb_")) {
                            val sub = twoRow?.optJSONObject("subtitle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                            if (sub.contains("Single", ignoreCase = true)) {
                                for (j in 0 until contents.length()) {
                                    val item = contents.optJSONObject(j) ?: continue
                                    val card = parseAlbumCard(item)
                                    if (card != null) singles.add(card)
                                }
                            } else {
                                for (j in 0 until contents.length()) {
                                    val item = contents.optJSONObject(j) ?: continue
                                    val card = parseAlbumCard(item)
                                    if (card != null) albums.add(card)
                                }
                            }
                        }
                    }
                }
            }
        }

        return ArtistPage(
            id = browseId,
            name = artistName,
            heroArtworkUrl = heroUrl,
            subscriberCount = subscribers,
            bio = bio,
            topSongs = topSongs,
            albums = albums,
            singles = singles,
            similarArtists = similarArtists
        )
    }

    private fun parseAlbumPage(browseId: String, root: JSONObject): AlbumPage {
        var albumTitle = "Album"
        var artistName = "Artist"
        var artistId: String? = null
        var year: String? = null
        var artworkUrl: String? = null

        val twoCol = root.optJSONObject("contents")?.optJSONObject("twoColumnBrowseResultsRenderer")
        val tabs = twoCol?.optJSONArray("tabs")
        val tab0 = tabs?.optJSONObject(0)?.optJSONObject("tabRenderer")?.optJSONObject("content")
        val secContents = tab0?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")

        if (secContents != null && secContents.length() > 0) {
            val headerObj = secContents.optJSONObject(0)?.optJSONObject("musicResponsiveHeaderRenderer")
                ?: secContents.optJSONObject(0)?.optJSONObject("musicDetailHeaderRenderer")

            if (headerObj != null) {
                albumTitle = headerObj.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: albumTitle

                val strapline = headerObj.optJSONObject("straplineTextOne")?.optJSONArray("runs")
                if (strapline != null && strapline.length() > 0) {
                    val sRun = strapline.optJSONObject(0)
                    artistName = sRun?.optString("text", artistName) ?: artistName
                    artistId = sRun?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                }

                val subRuns = headerObj.optJSONObject("subtitle")?.optJSONArray("runs")
                if (subRuns != null) {
                    for (i in 0 until subRuns.length()) {
                        val run = subRuns.optJSONObject(i) ?: continue
                        val ep = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                        if (ep != null && ep.startsWith("UC") && artistId == null) {
                            artistId = ep
                            artistName = run.optString("text", artistName)
                        }
                        val t = run.optString("text", "")
                        if (t.matches(Regex("\\d{4}"))) {
                            year = t
                        }
                    }
                }

                val thumbs = headerObj.optJSONObject("thumbnail")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                    ?.optJSONArray("thumbnails")
                    ?: headerObj.optJSONObject("thumbnail")
                        ?.optJSONObject("croppedSquareThumbnailRenderer")
                        ?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")

                if (thumbs != null && thumbs.length() > 0) {
                    val raw = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
                    artworkUrl = upgradeThumbnail(raw)
                }
            }
        }

        val tracks = mutableListOf<TrackItem>()
        val secondary = twoCol?.optJSONObject("secondaryContents")?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
            ?: secContents

        if (secondary != null) {
            for (i in 0 until secondary.length()) {
                val sec = secondary.optJSONObject(i) ?: continue
                val shelf = sec.optJSONObject("musicShelfRenderer") ?: continue
                val items = shelf.optJSONArray("contents") ?: continue

                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j) ?: continue
                    val renderer = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    val track = parseTrackRenderer(renderer)
                    if (track != null) {
                        val resolvedTrack = track.copy(
                            album = albumTitle,
                            albumId = browseId,
                            artist = if (track.artist == "Unknown Artist") artistName else track.artist,
                            artistId = track.artistId ?: artistId,
                            artworkUrl = if (track.artworkUrl.contains("hqdefault.jpg") && artworkUrl != null) artworkUrl else track.artworkUrl
                        )
                        tracks.add(resolvedTrack)
                    }
                }
            }
        }

        return AlbumPage(
            id = browseId,
            title = albumTitle,
            artist = artistName,
            artistId = artistId,
            year = year,
            artworkUrl = artworkUrl,
            tracks = tracks
        )
    }

    private fun parseAlbumCard(item: JSONObject): AlbumCard? {
        val renderer = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: return null
        val bId = renderer.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
            ?.optString("browseId") ?: return null

        var year: String? = null
        var subText: String? = null
        val subRuns = renderer.optJSONObject("subtitle")?.optJSONArray("runs")
        if (subRuns != null && subRuns.length() > 0) {
            val texts = mutableListOf<String>()
            for (i in 0 until subRuns.length()) {
                val t = subRuns.optJSONObject(i)?.optString("text", "") ?: ""
                if (t.isNotBlank() && t != "•" && t != " • ") {
                    texts.add(t)
                }
            }
            subText = texts.joinToString(" • ")
            year = texts.lastOrNull { it.matches(Regex("\\d{4}")) }
        }

        val thumbs = renderer.optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val artUrl = if (thumbs != null && thumbs.length() > 0) {
            val raw = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
            upgradeThumbnail(raw)
        } else null

        return AlbumCard(
            id = bId,
            title = title,
            subtitle = subText,
            artworkUrl = artUrl,
            year = year
        )
    }

    private fun parseArtistCard(item: JSONObject): ArtistCard? {
        val renderer = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: return null
        val bId = renderer.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
            ?.optString("browseId") ?: return null

        val subText = renderer.optJSONObject("subtitle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")

        val thumbs = renderer.optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val artUrl = if (thumbs != null && thumbs.length() > 0) {
            val raw = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
            upgradeThumbnail(raw)
        } else null

        return ArtistCard(
            id = bId,
            name = title,
            subscriberCount = subText,
            artworkUrl = artUrl
        )
    }

    /**
     * Fetches authenticated YouTube Music account profile details (display name, email, avatar URL).
     */
    suspend fun fetchAccountProfile(cookie: String, sapisid: String?): UserAccountInfo? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
            }

            val reqBuilder = Request.Builder()
                .url("$INNERTUBE_BASE_URL/account/account_menu")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", CLIENT_VERSION)
                .header("Cookie", cookie)

            val resolvedSapisid = sapisid ?: extractCookiePart(cookie, "SAPISID") ?: extractCookiePart(cookie, "__Secure-3PAPISID")
            if (!resolvedSapisid.isNullOrBlank()) {
                val timestamp = System.currentTimeMillis() / 1000
                val input = "$timestamp $resolvedSapisid https://music.youtube.com"
                val md = java.security.MessageDigest.getInstance("SHA-1")
                val digest = md.digest(input.toByteArray(Charsets.UTF_8))
                val sha1 = digest.joinToString("") { b -> "%02x".format(b) }
                reqBuilder.header("Authorization", "SAPISIDHASH ${timestamp}_$sha1")
            }

            val request = reqBuilder
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLogger.w("fetchAccountProfile HTTP failed: ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val root = JSONObject(body)
                val header = findActiveAccountHeader(root)
                if (header != null) {
                    val name = header.optJSONObject("accountName")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: header.optJSONObject("accountName")?.optString("simpleText")
                        ?: "YouTube User"

                    val email = header.optJSONObject("email")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: header.optJSONObject("email")?.optString("simpleText")
                        ?: ""

                    val photoObj = header.optJSONObject("accountPhoto") ?: header.optJSONObject("avatar")
                    val thumbs = photoObj?.optJSONArray("thumbnails")
                    var avatarUrl = if (thumbs != null && thumbs.length() > 0) {
                        thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
                    } else null
                    if (avatarUrl != null && avatarUrl.startsWith("//")) {
                        avatarUrl = "https:$avatarUrl"
                    }

                    val handle = header.optJSONObject("channelHandle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")

                    AppLogger.i("fetchAccountProfile success: name=$name, avatarUrl=$avatarUrl")
                    return@withContext UserAccountInfo(
                        name = name,
                        email = if (email.isNotBlank()) email else (handle ?: "YouTube Connected"),
                        avatarUrl = avatarUrl,
                        handle = handle
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.e("fetchAccountProfile network error", e)
        }
        null
    }

    /**
     * Fetches all playlists owned or saved by the authenticated Google account on YouTube Music.
     */
    suspend fun fetchUserPlaylists(): List<UserPlaylistItem> = withContext(Dispatchers.IO) {
        refreshVisitorDataIfNeeded()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("browseId", "FEmusic_liked_playlists")
            }

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/browse")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                    applyAuthHeaders(this)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLogger.w("fetchUserPlaylists failed: HTTP ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                val playlists = mutableListOf<UserPlaylistItem>()
                parseUserPlaylistsRecursively(root, playlists)
                AppLogger.i("fetchUserPlaylists: Discovered ${playlists.size} playlists from YouTube Music")
                playlists
            }
        } catch (e: Exception) {
            AppLogger.e("fetchUserPlaylists error", e)
            emptyList()
        }
    }

    private fun parseUserPlaylistsRecursively(json: Any?, outPlaylists: MutableList<UserPlaylistItem>) {
        when (json) {
            is JSONObject -> {
                if (json.has("musicTwoRowItemRenderer")) {
                    val renderer = json.optJSONObject("musicTwoRowItemRenderer")
                    if (renderer != null) {
                        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                        val navEndpoint = renderer.optJSONObject("navigationEndpoint")
                        val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId")
                            ?: navEndpoint?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")

                        val cleanId = if (!browseId.isNullOrBlank()) extractYouTubePlaylistId(browseId) else ""
                        val ignoredSystemIds = setOf("LM", "VLLM", "LL", "VLLL", "SE", "VLSE", "WL", "VLWL", "RDPN")
                        val lowerTitle = title.lowercase().trim()
                        val isIgnoredSystemTitle = lowerTitle.contains("liked songs") ||
                                lowerTitle.contains("liked music") ||
                                lowerTitle.contains("liked videos") ||
                                lowerTitle.contains("your likes") ||
                                lowerTitle.contains("sounds from shorts") ||
                                lowerTitle.contains("sounds from short") ||
                                lowerTitle.contains("sound from shorts") ||
                                lowerTitle.contains("episode for later") ||
                                lowerTitle.contains("episodes for later") ||
                                lowerTitle.contains("episodes") ||
                                lowerTitle.contains("watch later")

                        if (!browseId.isNullOrBlank() && title.isNotBlank() &&
                            !ignoredSystemIds.contains(browseId.uppercase()) &&
                            !ignoredSystemIds.contains(cleanId.uppercase()) &&
                            !isIgnoredSystemTitle
                        ) {
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
                                    UserPlaylistItem(
                                        id = cleanId,
                                        title = title,
                                        description = subtitle,
                                        trackCount = count,
                                        artworkUrl = thumbUrl
                                    )
                                )
                            }
                        }
                    }
                } else {
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        parseUserPlaylistsRecursively(json.opt(keys.next()), outPlaylists)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    parseUserPlaylistsRecursively(json.opt(i), outPlaylists)
                }
            }
        }
    }

    /**
     * Creates a new playlist on the authenticated user's YouTube Music / Google account.
     * Optionally attaches initial video IDs directly upon creation to ensure atomic persistence.
     * Returns the created remote playlistId (e.g. "PL...").
     */
    suspend fun createRemotePlaylist(
        title: String,
        privacyStatus: String = "PRIVATE",
        initialVideoIds: List<String> = emptyList()
    ): String? = withContext(Dispatchers.IO) {
        refreshVisitorDataIfNeeded()
        try {
            val payload = JSONObject().apply {
                put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                put("title", title.trim())
                put("privacyStatus", privacyStatus)
                val validIds = initialVideoIds.filter { it.isNotBlank() }.take(50)
                if (validIds.isNotEmpty()) {
                    val arr = JSONArray()
                    validIds.forEach { arr.put(it) }
                    put("videoIds", arr)
                }
            }

            val request = Request.Builder()
                .url("$INNERTUBE_BASE_URL/playlist/create")
                .apply {
                    commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                    addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                    applyAuthHeaders(this)
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val root = JSONObject(body)
                    val playlistId = root.optString("playlistId").takeIf { it.isNotBlank() }
                    AppLogger.i("createRemotePlaylist succeeded: '$title' (initial ${initialVideoIds.size} tracks) -> ID: $playlistId")
                    playlistId
                } else {
                    AppLogger.w("createRemotePlaylist failed: HTTP ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            AppLogger.e("createRemotePlaylist network error", e)
            null
        }
    }

    /**
     * Adds a track video ID to an existing YouTube Music playlist on the user's account.
     */
    suspend fun addTrackToRemotePlaylist(playlistId: String, videoId: String): Boolean = withContext(Dispatchers.IO) {
        addTracksToRemotePlaylist(playlistId, listOf(videoId))
    }

    /**
     * Batch adds multiple track video IDs to an existing YouTube Music playlist on the user's account.
     * Chunks in batches of 50 to prevent payload limits.
     */
    suspend fun addTracksToRemotePlaylist(playlistId: String, videoIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (videoIds.isEmpty()) return@withContext true
        refreshVisitorDataIfNeeded()
        try {
            val cleanPlaylistId = extractYouTubePlaylistId(playlistId)
            var overallSuccess = true

            for (chunk in videoIds.chunked(50)) {
                val payload = JSONObject().apply {
                    put("context", getClientContext("WEB_REMIX", CLIENT_VERSION))
                    put("playlistId", cleanPlaylistId)
                    val actions = JSONArray().apply {
                        chunk.forEach { videoId ->
                            put(JSONObject().apply {
                                put("action", "ACTION_ADD_VIDEO")
                                put("addedVideoId", videoId)
                            })
                        }
                    }
                    put("actions", actions)
                }

                // 1. Primary: YouTube Music endpoint
                val request = Request.Builder()
                    .url("$INNERTUBE_BASE_URL/browse/edit_playlist")
                    .apply {
                        commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                        addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                        applyAuthHeaders(this)
                    }
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                var chunkSuccess = false
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        chunkSuccess = true
                    } else {
                        AppLogger.w("addTracksToRemotePlaylist primary failed: HTTP ${response.code}")
                    }
                }

                // 2. Secondary fallback: YouTube Web endpoint
                if (!chunkSuccess) {
                    try {
                        val webPayload = JSONObject().apply {
                            val clientObj = JSONObject().apply {
                                put("clientName", "WEB")
                                put("clientVersion", "2.20240101.01.00")
                                put("hl", "en")
                                put("gl", "US")
                            }
                            put("context", JSONObject().put("client", clientObj))
                            put("playlistId", cleanPlaylistId)
                            val actions = JSONArray().apply {
                                chunk.forEach { videoId ->
                                    put(JSONObject().apply {
                                        put("action", "ACTION_ADD_VIDEO")
                                        put("addedVideoId", videoId)
                                    })
                                }
                            }
                            put("actions", actions)
                        }
                        val webReq = Request.Builder()
                            .url("https://www.youtube.com/youtubei/v1/browse/edit_playlist")
                            .apply {
                                commonHeaders.forEach { (k, v) -> addHeader(k, v) }
                                addHeader("X-Goog-Visitor-Id", cachedVisitorData)
                                applyAuthHeaders(this)
                            }
                            .post(webPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                            .build()

                        client.newCall(webReq).execute().use { webResp ->
                            if (webResp.isSuccessful) {
                                chunkSuccess = true
                            } else {
                                AppLogger.w("addTracksToRemotePlaylist fallback failed: HTTP ${webResp.code}")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.w("addTracksToRemotePlaylist fallback error: ${e.message}")
                    }
                }

                if (!chunkSuccess) {
                    overallSuccess = false
                }
            }
            AppLogger.i("addTracksToRemotePlaylist: ${videoIds.size} tracks -> $cleanPlaylistId (success=$overallSuccess)")
            overallSuccess
        } catch (e: Exception) {
            AppLogger.e("addTracksToRemotePlaylist network error", e)
            false
        }
    }

    private fun findActiveAccountHeader(obj: JSONObject): JSONObject? {
        if (obj.has("activeAccountHeaderRenderer")) {
            return obj.optJSONObject("activeAccountHeaderRenderer")
        }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val child = obj.opt(key)
            if (child is JSONObject) {
                val res = findActiveAccountHeader(child)
                if (res != null) return res
            } else if (child is JSONArray) {
                for (i in 0 until child.length()) {
                    val item = child.optJSONObject(i)
                    if (item != null) {
                        val res = findActiveAccountHeader(item)
                        if (res != null) return res
                    }
                }
            }
        }
        return null
    }

    private fun extractCookiePart(cookieHeader: String, key: String): String? {
        val parts = cookieHeader.split(";")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("$key=")) {
                return trimmed.substring(key.length + 1)
            }
        }
        return null
    }
}
