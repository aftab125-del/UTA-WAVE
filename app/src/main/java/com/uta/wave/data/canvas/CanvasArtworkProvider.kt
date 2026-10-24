package com.uta.wave.data.canvas

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object CanvasArtworkProvider {

    private const val TIDAL_BASE_URL = "https://api.tidal.com/v1/"
    private const val TIDAL_TOKEN = "vNVdglQOjFJJGG2U"
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .build()
    }

    private val memCache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val artwork: CanvasArtwork?,
        val expiresAtMs: Long
    )

    fun normalizeCanvasSongTitle(raw: String): String {
        return raw
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s*[\(\[\{]?(?:feat|featuring|ft)\.?\s+[^\)\]\}]+[\)\]\}]?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*[\(\[\{]?(?:remastered|remaster|edition|deluxe|bonus|anniversary)[^\)\]\}]*[\)\]\}]?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*[\(\[\{]?(?:official\s*(?:audio|video|music\s*video|visualizer|lyric\s*video))[^\)\]\}]*[\)\]\}]?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*(?:official\s*(?:audio|video|music\s*video|visualizer)|remastered|bonus).*$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun normalizeCanvasArtistName(raw: String): String {
        return raw
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s*[\(\[\{]?(?:topic|vevo)[^\)\]\}]*[\)\]\}]?""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    suspend fun getCanvas(
        context: Context,
        mediaId: String,
        title: String,
        artist: String,
        album: String? = null
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        val cacheKey = "canvas_${mediaId.ifBlank { "${title}_${artist}" }}"

        // 1. Memory Cache
        memCache[cacheKey]?.let { entry ->
            if (entry.expiresAtMs > System.currentTimeMillis()) {
                return@withContext entry.artwork
            }
        }

        // 2. Disk Cache
        try {
            val diskFile = File(context.cacheDir, "$cacheKey.json")
            if (diskFile.exists() && diskFile.length() > 0) {
                val jsonStr = diskFile.readText()
                val json = JSONObject(jsonStr)
                val artwork = CanvasArtwork(
                    name = json.optString("name").takeIf { it.isNotBlank() },
                    artist = json.optString("artist").takeIf { it.isNotBlank() },
                    albumName = json.optString("albumName").takeIf { it.isNotBlank() },
                    videoUrl = json.optString("videoUrl").takeIf { it.isNotBlank() },
                    animated = json.optString("animated").takeIf { it.isNotBlank() }
                )
                memCache[cacheKey] = CacheEntry(artwork, System.currentTimeMillis() + CACHE_TTL_MS)
                return@withContext artwork
            }
        } catch (e: Exception) {
            // continue to remote fetch
        }

        // 3. Remote Search: Tidal Video Covers
        val found = searchTidalCanvas(title, artist, album)

        if (found != null) {
            // Save to memory cache
            memCache[cacheKey] = CacheEntry(found, System.currentTimeMillis() + CACHE_TTL_MS)
            // Save to disk cache
            try {
                val diskFile = File(context.cacheDir, "$cacheKey.json")
                val json = JSONObject().apply {
                    put("name", found.name.orEmpty())
                    put("artist", found.artist.orEmpty())
                    put("albumName", found.albumName.orEmpty())
                    put("videoUrl", found.videoUrl.orEmpty())
                    put("animated", found.animated.orEmpty())
                }
                diskFile.writeText(json.toString())
            } catch (e: Exception) {
                // ignore disk write errors
            }
        }

        found
    }

    private fun searchTidalCanvas(
        song: String,
        artist: String,
        album: String?
    ): CanvasArtwork? {
        val normSong = normalizeCanvasSongTitle(song)
        val normArtist = normalizeCanvasArtistName(artist)

        val query = if (!album.isNullOrBlank()) "$album $artist $song" else "$artist $song"
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "${TIDAL_BASE_URL}search?query=$encodedQuery&limit=10&types=TRACKS&countryCode=US"

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Tidal-Token", TIDAL_TOKEN)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val root = JSONObject(bodyStr)
                val tracks = root.optJSONObject("tracks") ?: return null
                val items = tracks.optJSONArray("items") ?: return null

                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val title = item.optString("title", "")

                    // Check title similarity
                    val normResultTitle = normalizeCanvasSongTitle(title)
                    val titleMatches = normResultTitle.contains(normSong) || normSong.contains(normResultTitle)

                    val artistsArray = item.optJSONArray("artists")
                    var primaryArtist = item.optJSONObject("artist")?.optString("name", "") ?: ""
                    if (primaryArtist.isBlank() && artistsArray != null && artistsArray.length() > 0) {
                        primaryArtist = artistsArray.optJSONObject(0)?.optString("name", "") ?: ""
                    }
                    val normResultArtist = normalizeCanvasArtistName(primaryArtist)
                    val artistMatches = normArtist.isBlank() ||
                            normResultArtist.contains(normArtist) ||
                            normArtist.contains(normResultArtist)

                    val albumObj = item.optJSONObject("album")
                    val videoCover = albumObj?.optString("videoCover", "")?.takeIf { it.isNotBlank() }
                        ?: item.optString("videoCover", "").takeIf { it.isNotBlank() }

                    if (!videoCover.isNullOrBlank() && (titleMatches || artistMatches)) {
                        val videoUrl = formatTidalVideoUrl(videoCover)
                        if (videoUrl != null) {
                            return CanvasArtwork(
                                name = title,
                                artist = primaryArtist,
                                albumName = albumObj?.optString("title"),
                                videoUrl = videoUrl,
                                animated = videoUrl
                            )
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatTidalVideoUrl(id: String): String? {
        val parts = id.split("-")
        if (parts.size != 5) return null
        return "https://resources.tidal.com/videos/${parts[0]}/${parts[1]}/${parts[2]}/${parts[3]}/${parts[4]}/1280x1280.mp4"
    }
}
