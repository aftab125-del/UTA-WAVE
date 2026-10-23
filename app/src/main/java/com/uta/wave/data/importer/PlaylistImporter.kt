package com.uta.wave.data.importer

import android.content.Context
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object PlaylistImporter {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val innerTubeClient = InnerTubeClient()

    suspend fun importFromYouTubeMusic(
        context: Context,
        playlistUrlOrId: String,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Pair<String, Int> = withContext(Dispatchers.IO) {
        val localDatabase = LocalDatabase.getInstance(context)
        val (title, tracks) = innerTubeClient.getPlaylist(playlistUrlOrId)

        if (tracks.isEmpty()) {
            return@withContext Pair("Empty or invalid playlist", 0)
        }

        val playlistId = localDatabase.createPlaylist(title, "Imported from YouTube Music")
        for (i in tracks.indices) {
            val track = tracks[i]
            localDatabase.addTrackToPlaylist(playlistId, track)
            onProgress(i + 1, tracks.size, track.title)
        }

        Pair(title, tracks.size)
    }

    suspend fun importFromSpotify(
        context: Context,
        spotifyUrlOrId: String,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Pair<String, Int> = withContext(Dispatchers.IO) {
        val localDatabase = LocalDatabase.getInstance(context)

        // Extract spotify playlist ID (supports open.spotify.com/playlist, /intl-*/playlist, spotify.link redirects)
        val playlistId = extractSpotifyPlaylistId(spotifyUrlOrId)
        if (playlistId.isBlank()) {
            return@withContext Pair("Invalid Spotify URL or Playlist ID", 0)
        }

        val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
        AppLogger.i("Fetching Spotify embed: $embedUrl")

        val trackPairs = mutableListOf<Pair<String, String>>()
        var playlistTitle = "Spotify Playlist"

        try {
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""

                // Extract __NEXT_DATA__ JSON script with forgiving regex
                val regex = Pattern.compile("<script[^>]*id=[\"']__NEXT_DATA__[\"'][^>]*>([\\s\\S]*?)</script>")
                val matcher = regex.matcher(html)
                if (matcher.find()) {
                    val jsonStr = matcher.group(1)
                    if (!jsonStr.isNullOrBlank()) {
                        val root = JSONObject(jsonStr)
                        val entity = root.optJSONObject("props")
                            ?.optJSONObject("pageProps")
                            ?.optJSONObject("state")
                            ?.optJSONObject("data")
                            ?.optJSONObject("entity")

                        val extractedName = entity?.optString("name", "")
                        if (!extractedName.isNullOrBlank()) {
                            playlistTitle = extractedName
                        }

                        val trackList = entity?.optJSONArray("trackList")
                        if (trackList != null) {
                            for (i in 0 until trackList.length()) {
                                val item = trackList.optJSONObject(i) ?: continue
                                val title = item.optString("title", "").replace("\u00A0", " ").trim()
                                val artist = item.optString("subtitle", "").replace("\u00A0", " ").trim()
                                if (title.isNotBlank()) {
                                    trackPairs.add(Pair(title, artist))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Spotify embed fetch error: ${e.message}", e)
        }

        if (trackPairs.isEmpty()) {
            return@withContext Pair("Could not extract tracks from Spotify link", 0)
        }

        val createdPlaylistId = localDatabase.createPlaylist(playlistTitle, "Imported from Spotify")
        var addedCount = 0

        for (i in trackPairs.indices) {
            val (title, artist) = trackPairs[i]
            val displayLabel = if (artist.isNotBlank()) "$title - $artist" else title
            onProgress(i + 1, trackPairs.size, displayLabel)

            // Primary search: "Title Artist"
            var searchResults = innerTubeClient.search("$title $artist")

            // Secondary search: stripped features/bracket labels if empty
            if (searchResults.isEmpty()) {
                val simplifiedTitle = title
                    .replace(Regex("\\(feat\\..*?\\)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\(ft\\..*?\\)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\[.*?\\]"), "")
                    .trim()
                if (simplifiedTitle != title) {
                    searchResults = innerTubeClient.search("$simplifiedTitle $artist")
                }
            }

            // Tertiary fallback: title only
            if (searchResults.isEmpty()) {
                searchResults = innerTubeClient.search(title)
            }

            if (searchResults.isNotEmpty()) {
                val matched = searchResults.first()
                localDatabase.addTrackToPlaylist(createdPlaylistId, matched)
                addedCount++
            }
        }

        Pair(playlistTitle, addedCount)
    }

    private fun extractSpotifyPlaylistId(input: String): String {
        var trimmed = input.trim()

        // Handle spotify.link shorteners by following redirect
        if (trimmed.contains("spotify.link/")) {
            try {
                val req = Request.Builder()
                    .url(trimmed)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val resp = httpClient.newCall(req).execute()
                val finalUrl = resp.request.url.toString()
                if (finalUrl.isNotBlank() && finalUrl != trimmed) {
                    trimmed = finalUrl
                }
            } catch (e: Exception) {
                AppLogger.w("Failed to resolve spotify.link redirect: ${e.message}")
            }
        }

        return when {
            trimmed.contains("/playlist/") -> {
                trimmed.substringAfter("/playlist/").substringBefore("?").substringBefore("/").substringBefore("&").trim()
            }
            trimmed.startsWith("spotify:playlist:") -> {
                trimmed.removePrefix("spotify:playlist:").substringBefore("?").trim()
            }
            trimmed.length in 15..35 && !trimmed.contains(" ") && !trimmed.contains("/") && !trimmed.contains(":") -> {
                trimmed
            }
            else -> ""
        }
    }
}
