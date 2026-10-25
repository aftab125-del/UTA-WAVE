package com.uta.wave.data.recognizer

import com.uta.wave.player.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class ShazamResult(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val coverArtUrl: String? = null,
    val coverArtHqUrl: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val label: String? = null,
    val lyrics: String? = null,
    val shazamUrl: String? = null,
    val appleMusicUrl: String? = null,
    val spotifyUrl: String? = null,
    val isrc: String? = null,
    val youtubeVideoId: String? = null
)

object ShazamApiClient {

    private const val MAX_RETRIES = 3
    private const val INITIAL_RETRY_DELAY_MS = 1500L
    private const val MIN_REQUEST_INTERVAL_MS = 800L
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val userAgents = listOf(
        "Dalvik/2.1.0 (Linux; U; Android 14; Pixel 8 Build/UD1A.230803.041)",
        "Dalvik/2.1.0 (Linux; U; Android 13; SM-S918B Build/TP1A.220624.014)",
        "Dalvik/2.1.0 (Linux; U; Android 12; Pixel 6 Build/SQ3A.220705.004)",
        "Dalvik/2.1.0 (Linux; U; Android 11; SM-G998B Build/RP1A.200720.012)"
    )

    private val timezones = listOf(
        "Europe/London", "America/New_York", "Asia/Tokyo", "Europe/Paris", "America/Los_Angeles"
    )

    private val requestMutex = Mutex()
    private var lastRequestTime = 0L

    private val resultCache = ConcurrentHashMap<String, CachedResult>()

    private data class CachedResult(val timestamp: Long, val result: ShazamResult)

    suspend fun recognize(signature: String, durationMs: Long): Result<ShazamResult> =
        withContext(Dispatchers.IO) {
            val cacheKey = signature.hashCode().toString()
            getCachedResult(cacheKey)?.let {
                return@withContext Result.success(it)
            }

            var lastException: Exception? = null

            for (attempt in 0 until MAX_RETRIES) {
                try {
                    enforceRateLimit()

                    val result = performRecognition(signature, durationMs)
                    cacheResult(cacheKey, result)
                    return@withContext Result.success(result)
                } catch (e: Exception) {
                    lastException = e
                    val msg = e.message ?: ""
                    if (msg.contains("429") || msg.contains("Too many requests", ignoreCase = true)) {
                        if (attempt < MAX_RETRIES - 1) {
                            val backoff = INITIAL_RETRY_DELAY_MS * (1 shl attempt)
                            delay(backoff)
                            continue
                        }
                    } else if (msg.contains("No match", ignoreCase = true)) {
                        return@withContext Result.failure(e)
                    } else {
                        if (attempt < MAX_RETRIES - 1) {
                            delay(1000L)
                            continue
                        }
                    }
                }
            }

            Result.failure(lastException ?: Exception("Recognition failed after $MAX_RETRIES attempts"))
        }

    private suspend fun performRecognition(signature: String, durationMs: Long): ShazamResult {
        val timestamp = System.currentTimeMillis() / 1000
        val uuid1 = UUID.randomUUID().toString().uppercase()
        val uuid2 = UUID.randomUUID().toString()

        val jsonBody = JSONObject().apply {
            put("geolocation", JSONObject().apply {
                put("altitude", Random.nextDouble() * 300.0 + 100.0)
                put("latitude", Random.nextDouble() * 140.0 - 70.0)
                put("longitude", Random.nextDouble() * 340.0 - 170.0)
            })
            put("signature", JSONObject().apply {
                put("samplems", durationMs)
                put("timestamp", timestamp)
                put("uri", signature)
            })
            put("timestamp", timestamp)
            put("timezone", timezones.random())
        }

        val url = "https://amp.shazam.com/discovery/v5/en/US/android/-/tag/$uuid1/$uuid2" +
                "?sync=true&webv3=true&sampling=true&connected=&shazamapiversion=v3&sharehub=true&video=v3"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", userAgents.random())
            .addHeader("Content-Language", "en_US")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val code = response.code
        val bodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            when (code) {
                429 -> throw Exception("Too many requests (429)")
                404 -> throw Exception("No match found")
                in 500..599 -> throw Exception("Shazam service temporarily unavailable ($code)")
                else -> throw Exception("Recognition failed with HTTP error $code")
            }
        }

        if (bodyString.isBlank()) {
            throw Exception("Empty response from Shazam")
        }

        val json = JSONObject(bodyString)
        val track = json.optJSONObject("track") ?: throw Exception("No match found")

        val trackId = track.optString("key", json.optString("tagid", ""))
        val title = track.optString("title", "")
        val artist = track.optString("subtitle", "")

        if (title.isBlank()) {
            throw Exception("No match found")
        }

        // Parse sections for Album, Label, Released, Lyrics
        var album: String? = null
        var label: String? = null
        var releaseDate: String? = null
        var lyrics: String? = null

        val sections = track.optJSONArray("sections")
        if (sections != null) {
            for (i in 0 until sections.length()) {
                val sec = sections.optJSONObject(i) ?: continue
                val type = sec.optString("type", "")
                if (type.equals("SONG", ignoreCase = true)) {
                    val metadata = sec.optJSONArray("metadata")
                    if (metadata != null) {
                        for (m in 0 until metadata.length()) {
                            val item = metadata.optJSONObject(m) ?: continue
                            val metaTitle = item.optString("title", "")
                            val metaText = item.optString("text", "")
                            when {
                                metaTitle.equals("Album", ignoreCase = true) -> album = metaText
                                metaTitle.equals("Label", ignoreCase = true) -> label = metaText
                                metaTitle.equals("Released", ignoreCase = true) -> releaseDate = metaText
                            }
                        }
                    }
                } else if (type.equals("LYRICS", ignoreCase = true)) {
                    val textArray = sec.optJSONArray("text")
                    if (textArray != null) {
                        val lines = mutableListOf<String>()
                        for (l in 0 until textArray.length()) {
                            lines.add(textArray.optString(l, ""))
                        }
                        lyrics = lines.joinToString("\n")
                    }
                }
            }
        }

        // Images
        val images = track.optJSONObject("images")
        val coverArtUrl = images?.optString("coverart")?.takeIf { it.isNotBlank() }
        val coverArtHqUrl = images?.optString("coverarthq")?.takeIf { it.isNotBlank() }

        // Genres
        val genres = track.optJSONObject("genres")
        val primaryGenre = genres?.optString("primary")?.takeIf { it.isNotBlank() }

        // Hub (streaming options & YouTube video)
        var appleMusicUrl: String? = null
        var spotifyUrl: String? = null
        var youtubeVideoId: String? = null

        val hub = track.optJSONObject("hub")
        if (hub != null) {
            val options = hub.optJSONArray("options")
            if (options != null) {
                for (o in 0 until options.length()) {
                    val opt = options.optJSONObject(o) ?: continue
                    val providerName = opt.optString("providername", "")
                    val optType = opt.optString("type", "")

                    if (providerName.contains("apple", ignoreCase = true)) {
                        val actions = opt.optJSONArray("actions")
                        appleMusicUrl = actions?.optJSONObject(0)?.optString("uri")
                    }

                    if (optType.contains("video", ignoreCase = true)) {
                        val actions = opt.optJSONArray("actions")
                        val uri = actions?.optJSONObject(0)?.optString("uri")
                        if (!uri.isNullOrBlank()) {
                            youtubeVideoId = when {
                                uri.contains("v=") -> uri.substringAfterLast("v=").take(11)
                                uri.contains("/") -> uri.substringAfterLast("/").takeIf { it.length == 11 }
                                else -> null
                            }
                        }
                    }
                }
            }

            val providers = hub.optJSONArray("providers")
            if (providers != null) {
                for (p in 0 until providers.length()) {
                    val prov = providers.optJSONObject(p) ?: continue
                    val caption = prov.optString("caption", "")
                    if (caption.contains("spotify", ignoreCase = true)) {
                        val actions = prov.optJSONArray("actions")
                        spotifyUrl = actions?.optJSONObject(0)?.optString("uri")
                    }
                }
            }
        }

        val shazamUrl = track.optString("url").takeIf { it.isNotBlank() }
        val isrc = track.optString("isrc").takeIf { it.isNotBlank() }

        return ShazamResult(
            trackId = trackId,
            title = title,
            artist = artist,
            album = album,
            coverArtUrl = coverArtUrl,
            coverArtHqUrl = coverArtHqUrl,
            genre = primaryGenre,
            releaseDate = releaseDate,
            label = label,
            lyrics = lyrics,
            shazamUrl = shazamUrl,
            appleMusicUrl = appleMusicUrl,
            spotifyUrl = spotifyUrl,
            isrc = isrc,
            youtubeVideoId = youtubeVideoId
        )
    }

    private suspend fun enforceRateLimit() {
        requestMutex.withLock {
            val now = System.currentTimeMillis()
            val diff = now - lastRequestTime
            if (diff < MIN_REQUEST_INTERVAL_MS) {
                delay(MIN_REQUEST_INTERVAL_MS - diff)
            }
            lastRequestTime = System.currentTimeMillis()
        }
    }

    private fun getCachedResult(key: String): ShazamResult? {
        val cached = resultCache[key] ?: return null
        if (System.currentTimeMillis() - cached.timestamp > CACHE_DURATION_MS) {
            resultCache.remove(key)
            return null
        }
        return cached.result
    }

    private fun cacheResult(key: String, result: ShazamResult) {
        resultCache[key] = CachedResult(System.currentTimeMillis(), result)
        if (resultCache.size > 80) {
            val now = System.currentTimeMillis()
            val iter = resultCache.entries.iterator()
            while (iter.hasNext()) {
                val e = iter.next()
                if (now - e.value.timestamp > CACHE_DURATION_MS) {
                    iter.remove()
                }
            }
        }
    }
}
