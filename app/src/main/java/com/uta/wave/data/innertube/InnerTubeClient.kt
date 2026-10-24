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
            put("hl", "en")
            put("gl", "US")
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
                }
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val root = JSONObject(body)
                        val tracks = mutableListOf<TrackItem>()
                        parseTracksRecursively(root, tracks)

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
                        parseTracksRecursively(webRoot, webTracks)
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

    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        AppLogger.i("🎯 Resolving audio stream for videoId: $videoId")
        refreshVisitorDataIfNeeded()

        // 1. Primary: VISIONOS client (1.02, Client ID 101) - proven to serve whole unthrottled audio stream without 403
        val visionResult = tryResolveVisionOS(videoId)
        if (!visionResult.isNullOrBlank()) {
            AppLogger.i("✅ Resolved stream via VISIONOS client for $videoId")
            return@withContext visionResult
        }

        // 2. Secondary: ANDROID_VR client (1.65.10) with visitorData
        AppLogger.w("VISIONOS returned no stream, falling back to ANDROID_VR...")
        val vrResult = tryResolveVR(videoId)
        if (!vrResult.isNullOrBlank()) {
            AppLogger.i("✅ Resolved stream via ANDROID_VR client for $videoId")
            return@withContext vrResult
        }

        // 3. Tertiary: IOS client (19.45.4, Client ID 5)
        AppLogger.w("ANDROID_VR returned no stream, falling back to IOS...")
        val iosResult = tryResolveIOS(videoId)
        if (!iosResult.isNullOrBlank()) {
            AppLogger.i("✅ Resolved stream via IOS client for $videoId")
            return@withContext iosResult
        }

        AppLogger.e("❌ All stream extractors failed for videoId: $videoId")
        null
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
                
                // Prefer audio/mp4 (m4a)
                for (i in 0 until adaptive.length()) {
                    val format = adaptive.optJSONObject(i) ?: continue
                    val mime = format.optString("mimeType", "")
                    val url = format.optString("url", "")
                    if (mime.contains("audio/mp4") && url.isNotBlank()) {
                        return url
                    }
                }

                // Fallback to any audio url (webm/opus)
                for (i in 0 until adaptive.length()) {
                    val format = adaptive.optJSONObject(i) ?: continue
                    val mime = format.optString("mimeType", "")
                    val url = format.optString("url", "")
                    if (mime.contains("audio/") && url.isNotBlank()) {
                        return url
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w("VISIONOS resolve failed: ${e.message}")
        }
        return null
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
                
                for (i in 0 until adaptive.length()) {
                    val format = adaptive.optJSONObject(i) ?: continue
                    val mime = format.optString("mimeType", "")
                    val url = format.optString("url", "")
                    if (mime.contains("audio/mp4") && url.isNotBlank()) {
                        return url
                    }
                }

                for (i in 0 until adaptive.length()) {
                    val format = adaptive.optJSONObject(i) ?: continue
                    val mime = format.optString("mimeType", "")
                    val url = format.optString("url", "")
                    if (mime.contains("audio/") && url.isNotBlank()) {
                        return url
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w("ANDROID_VR resolve failed: ${e.message}")
        }
        return null
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
                val status = json.optJSONObject("playabilityStatus")?.optString("status", "")
                if (status != "OK") return null

                val streamData = json.optJSONObject("streamingData") ?: return null
                val adaptive = streamData.optJSONArray("adaptiveFormats") ?: JSONArray()
                for (i in 0 until adaptive.length()) {
                    val format = adaptive.optJSONObject(i) ?: continue
                    val mime = format.optString("mimeType", "")
                    val url = format.optString("url", "")
                    if (mime.contains("audio/") && url.isNotBlank()) {
                        return url
                    }
                }
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

    private fun parseTracksRecursively(json: Any?, outTracks: MutableList<TrackItem>) {
        when (json) {
            is JSONObject -> {
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
}
