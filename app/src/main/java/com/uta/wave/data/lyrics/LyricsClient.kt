package com.uta.wave.data.lyrics

import com.uta.wave.data.innertube.LyricLine
import com.uta.wave.data.innertube.LyricsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class LyricsClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val LRCLIB_API_URL = "https://lrclib.net/api/get"
        private val LRC_PATTERN = Pattern.compile("^\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)$")
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        durationSeconds: Int = 0
    ): LyricsResult = withContext(Dispatchers.IO) {
        val cleanTitle = cleanSearchQuery(title)
        val cleanArtist = cleanSearchQuery(artist)

        try {
            val urlBuilder = LRCLIB_API_URL.toHttpUrlOrNull()?.newBuilder() ?: return@withContext emptyLyrics()
            urlBuilder.addQueryParameter("track_name", cleanTitle)
            urlBuilder.addQueryParameter("artist_name", cleanArtist)
            if (durationSeconds > 0) {
                urlBuilder.addQueryParameter("duration", durationSeconds.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .addHeader("User-Agent", "UTA-wave/1.0.0 (Android; https://github.com/aftab125-del/UTA-VERSE)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyLyrics()
                val body = response.body?.string() ?: return@withContext emptyLyrics()
                val json = JSONObject(body)

                val syncedLyrics = json.optString("syncedLyrics", "")
                val plainLyrics = json.optString("plainLyrics", "")

                if (syncedLyrics.isNotBlank()) {
                    val lines = parseLrc(syncedLyrics)
                    if (lines.isNotEmpty()) {
                        return@withContext LyricsResult(
                            lines = lines,
                            isSynced = true,
                            plainText = plainLyrics.ifBlank { syncedLyrics },
                            provider = "LrcLib"
                        )
                    }
                }

                if (plainLyrics.isNotBlank()) {
                    val plainLines = plainLyrics.lines().mapIndexed { idx, line ->
                        LyricLine(timestampMs = idx * 3000L, text = line.trim())
                    }.filter { it.text.isNotBlank() }

                    return@withContext LyricsResult(
                        lines = plainLines,
                        isSynced = false,
                        plainText = plainLyrics,
                        provider = "LrcLib"
                    )
                }
            }
        } catch (e: Exception) {
            // ignore network errors
        }

        emptyLyrics()
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcText.lineSequence().forEach { rawLine ->
            val matcher = LRC_PATTERN.matcher(rawLine.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msRaw = matcher.group(3) ?: "0"
                val ms = if (msRaw.length == 2) msRaw.toLong() * 10 else msRaw.toLong()
                val text = matcher.group(4)?.trim() ?: ""

                val timestampMs = (min * 60 + sec) * 1000 + ms
                lines.add(LyricLine(timestampMs = timestampMs, text = text))
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
            .replace(Regex("(?i)ft\\..*"), "")
            .replace(Regex("(?i)feat\\..*"), "")
            .trim()
    }

    private fun emptyLyrics() = LyricsResult(
        lines = emptyList(),
        isSynced = false,
        plainText = "No lyrics found for this track.",
        provider = "LrcLib"
    )
}
