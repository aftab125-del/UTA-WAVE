package com.uta.wave.desktop.data

import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.account.DesktopAccountManager
import com.uta.wave.desktop.account.DesktopUserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DesktopSupabaseClient private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: DesktopSupabaseClient? = null

        fun getInstance(): DesktopSupabaseClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DesktopSupabaseClient().also { INSTANCE = it }
            }
        }

        const val SUPABASE_URL = "https://swdpqqfnghpwlimfgzpp.supabase.co"
        const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InN3ZHBxcWZuZ2hwd2xpbWZnenBwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTMwOTM5NzQsImV4cCI6MjEwODY2OTk3NH0.CTTjMlalQk3iqPAWQlvewsaVeQONxeukMd4PDZksNkM"
        const val SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InN3ZHBxcWZuZ2hwd2xpbWZnenBwIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc5MzA5Mzk3NCwiZXhwIjoyMTA4NjY5OTc0fQ.uRfQ4_E6gYOAxASji59ERqG80mFV07InDy01SlDO0Jc"
        const val DEFAULT_USER_ID = "d3be4e1b-af5d-4dc4-80ae-8fb0e3ca969c"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun getHeaders(useServiceKey: Boolean = true): Map<String, String> {
        val key = if (useServiceKey) SUPABASE_SERVICE_KEY else SUPABASE_ANON_KEY
        return mapOf(
            "apikey" to key,
            "Authorization" to "Bearer $key",
            "Content-Type" to "application/json"
        )
    }

    suspend fun fetchUserProfile(userId: String = DEFAULT_USER_ID): DesktopUserProfile? = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/profiles?id=eq.$userId&select=*"
            val reqBuilder = Request.Builder().url(url)
            getHeaders(useServiceKey = true).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val array = JSONArray(body)
                if (array.length() == 0) return@withContext null

                val obj = array.getJSONObject(0)
                val displayName = obj.optString("display_name", "Samurai Gaming")
                val avatarUrl = obj.optString("avatar_url").takeIf { !it.isNullOrBlank() }

                return@withContext DesktopUserProfile(
                    isLoggedIn = true,
                    name = displayName,
                    email = "samurai@uta-wave.cloud",
                    avatarUrl = avatarUrl,
                    handle = "@samurai"
                )
            }
        } catch (e: Exception) {
            println("[DesktopSupabaseClient] fetchUserProfile error: ${e.message}")
            null
        }
    }

    suspend fun fetchPlaylists(userId: String = DEFAULT_USER_ID): List<DesktopPlaylist> = withContext(Dispatchers.IO) {
        val result = mutableListOf<DesktopPlaylist>()
        try {
            val url = "$SUPABASE_URL/rest/v1/playlists?user_id=eq.$userId&select=*&order=created_at.desc"
            val reqBuilder = Request.Builder().url(url)
            getHeaders(useServiceKey = true).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(body)

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val rawName = obj.optString("name", "Untitled")
                    val rawDesc = obj.optString("description", "")
                    val coverUrl = obj.optString("cover_url").takeIf { !it.isNullOrBlank() }

                    // Resolve display title: if name is emojis and description has text like "Namikaze_aftab"
                    val (title, description) = if (rawDesc.isNotBlank() && (rawName.isBlank() || rawName.all { !it.isLetterOrDigit() })) {
                        Pair(rawDesc, rawName)
                    } else {
                        Pair(rawName, rawDesc)
                    }

                    // Fetch tracks for this playlist
                    val tracks = fetchPlaylistTracks(id)

                    result.add(
                        DesktopPlaylist(
                            id = id,
                            name = title,
                            description = description,
                            trackCount = tracks.size,
                            coverUrl = coverUrl ?: tracks.firstOrNull()?.artworkUrl,
                            tracks = tracks
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("[DesktopSupabaseClient] fetchPlaylists error: ${e.message}")
        }
        result
    }

    suspend fun fetchPlaylistTracks(playlistId: String): List<TrackItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackItem>()
        try {
            val url = "$SUPABASE_URL/rest/v1/playlist_tracks?playlist_id=eq.$playlistId&select=*&order=position.asc"
            val reqBuilder = Request.Builder().url(url)
            getHeaders(useServiceKey = true).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(body)

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val trackId = obj.optString("track_id", "")
                    if (trackId.isBlank()) continue

                    val title = obj.optString("title", "Unknown Title")
                    val artist = obj.optString("artist", "Unknown Artist")
                    val artwork = obj.optString("artwork", "")
                    val duration = obj.optInt("duration", 0)

                    tracks.add(
                        TrackItem(
                            id = "youtube:$trackId",
                            videoId = trackId,
                            title = title,
                            artist = artist,
                            album = null,
                            artworkUrl = artwork,
                            durationSeconds = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("[DesktopSupabaseClient] fetchPlaylistTracks error: ${e.message}")
        }
        tracks
    }

    suspend fun fetchLikedTracks(userId: String = DEFAULT_USER_ID): List<TrackItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackItem>()
        try {
            val url = "$SUPABASE_URL/rest/v1/liked_tracks?user_id=eq.$userId&select=*&order=created_at.desc"
            val reqBuilder = Request.Builder().url(url)
            getHeaders(useServiceKey = true).forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(body)

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val trackId = obj.optString("track_id", "")
                    if (trackId.isBlank()) continue

                    val title = obj.optString("title", "Unknown Title")
                    val artist = obj.optString("artist", "Unknown Artist")
                    val artwork = obj.optString("artwork", "")
                    val duration = obj.optInt("duration", 0)

                    tracks.add(
                        TrackItem(
                            id = "youtube:$trackId",
                            videoId = trackId,
                            title = title,
                            artist = artist,
                            album = null,
                            artworkUrl = artwork,
                            durationSeconds = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("[DesktopSupabaseClient] fetchLikedTracks error: ${e.message}")
        }
        tracks
    }

    suspend fun syncAll(userId: String = DEFAULT_USER_ID): DesktopLibrary = withContext(Dispatchers.IO) {
        println("[DesktopSupabaseClient] Syncing user library for: $userId...")
        val profile = fetchUserProfile(userId)
        if (profile != null) {
            DesktopAccountManager.getInstance().saveSession(
                cookieHeader = "",
                userId = userId,
                userName = profile.name,
                userEmail = profile.email,
                avatarUrl = profile.avatarUrl,
                handle = profile.handle
            )
        }

        val playlists = fetchPlaylists(userId)
        val liked = fetchLikedTracks(userId)

        println("[DesktopSupabaseClient] Sync finished: ${playlists.size} playlists, ${liked.size} liked songs.")
        DesktopDatabase.getInstance().saveLibrary(playlists, liked)
        DesktopDatabase.getInstance().library.value
    }
}
