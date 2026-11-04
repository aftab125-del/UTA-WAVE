package com.uta.wave.desktop.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class DesktopLyricLine(
    val timestampMs: Long,
    val text: String
)

data class DesktopLyricsData(
    val lines: List<DesktopLyricLine>,
    val isSynced: Boolean,
    val plainText: String,
    val provider: String = "LrcLib"
)

class DesktopLyricsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val LRCLIB_GET_URL = "https://lrclib.net/api/get"
        private const val LRCLIB_SEARCH_URL = "https://lrclib.net/api/search"
        private val LRC_PATTERN = Pattern.compile("^\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)$")
        private val memoryCache = ConcurrentHashMap<String, DesktopLyricsData>()
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        durationSeconds: Int = 0
    ): DesktopLyricsData = withContext(Dispatchers.IO) {
        val cleanTitle = cleanSearchQuery(title)
        val cleanArtist = cleanSearchQuery(artist)
        val cacheKey = "${cleanTitle.lowercase()}::${cleanArtist.lowercase()}"

        memoryCache[cacheKey]?.let { return@withContext it }

        // Tier 1: Try exact match on LRCLIB api/get
        try {
            val urlBuilder = LRCLIB_GET_URL.toHttpUrlOrNull()?.newBuilder() ?: return@withContext emptyLyrics()
            urlBuilder.addQueryParameter("track_name", cleanTitle)
            urlBuilder.addQueryParameter("artist_name", cleanArtist)
            if (durationSeconds > 0) {
                urlBuilder.addQueryParameter("duration", durationSeconds.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .addHeader("User-Agent", "UTA-wave/1.0.0 (Windows; https://github.com/aftab125-del/UTA-VERSE)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val parsed = parseJsonResponse(body)
                    if (parsed != null) {
                        memoryCache[cacheKey] = parsed
                        return@withContext parsed
                    }
                }
            }
        } catch (e: Exception) {
            println("[DesktopLyricsService] Tier 1 lookup error: ${e.message}")
        }

        // Tier 2: Fallback to LRCLIB Search api/search?q=
        try {
            val searchUrl = LRCLIB_SEARCH_URL.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("q", "$cleanTitle $cleanArtist")
                ?.build()

            if (searchUrl != null) {
                val request = Request.Builder()
                    .url(searchUrl)
                    .addHeader("User-Agent", "UTA-wave/1.0.0 (Windows; https://github.com/aftab125-del/UTA-VERSE)")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val array = JSONArray(body)
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            val parsed = parseJsonObject(item)
                            if (parsed != null && parsed.isSynced) {
                                memoryCache[cacheKey] = parsed
                                return@withContext parsed
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("[DesktopLyricsService] Tier 2 search error: ${e.message}")
        }

        emptyLyrics()
    }

    private fun parseJsonResponse(body: String): DesktopLyricsData? {
        if (body.isBlank()) return null
        return try {
            val json = JSONObject(body)
            parseJsonObject(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJsonObject(json: JSONObject): DesktopLyricsData? {
        val syncedLyrics = json.optString("syncedLyrics", "")
        val plainLyrics = json.optString("plainLyrics", "")

        if (syncedLyrics.isNotBlank()) {
            val lines = parseLrc(syncedLyrics)
            if (lines.isNotEmpty()) {
                return DesktopLyricsData(
                    lines = lines,
                    isSynced = true,
                    plainText = plainLyrics.ifBlank { syncedLyrics },
                    provider = "LrcLib"
                )
            }
        }

        if (plainLyrics.isNotBlank()) {
            val plainLines = plainLyrics.lines().mapIndexed { idx, line ->
                DesktopLyricLine(timestampMs = idx * 3500L, text = line.trim())
            }.filter { it.text.isNotBlank() }

            return DesktopLyricsData(
                lines = plainLines,
                isSynced = false,
                plainText = plainLyrics,
                provider = "LrcLib"
            )
        }

        return null
    }

    private fun parseLrc(lrcText: String): List<DesktopLyricLine> {
        val lines = mutableListOf<DesktopLyricLine>()
        lrcText.lineSequence().forEach { rawLine ->
            val matcher = LRC_PATTERN.matcher(rawLine.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msRaw = matcher.group(3) ?: "0"
                val ms = if (msRaw.length == 2) msRaw.toLong() * 10 else msRaw.toLong()
                val text = matcher.group(4)?.trim() ?: ""

                val timestampMs = (min * 60 + sec) * 1000 + ms
                if (text.isNotBlank()) {
                    lines.add(DesktopLyricLine(timestampMs = timestampMs, text = text))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    private fun cleanSearchQuery(text: String): String {
        return text
            .replace(Regex("(?i)\\[official.*?video\\]"), "")
            .replace(Regex("(?i)\\(official.*?video\\)"), "")
            .replace(Regex("(?i)\\[official.*?audio\\]"), "")
            .replace(Regex("(?i)\\(official.*?audio\\)"), "")
            .replace(Regex("(?i)\\[lyrics?\\]"), "")
            .replace(Regex("(?i)\\(lyrics?\\)"), "")
            .replace(Regex("(?i)\\[remastered.*?\\]"), "")
            .replace(Regex("(?i)\\(remastered.*?\\)"), "")
            .replace(Regex("(?i)ft\\..*"), "")
            .replace(Regex("(?i)feat\\..*"), "")
            .trim()
    }

    private fun emptyLyrics() = DesktopLyricsData(
        lines = emptyList(),
        isSynced = false,
        plainText = "No lyrics found for this track.",
        provider = "LrcLib"
    )
}
