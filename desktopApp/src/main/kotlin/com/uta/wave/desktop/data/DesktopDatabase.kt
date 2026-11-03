package com.uta.wave.desktop.data

import com.uta.wave.core.model.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class DesktopPlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val trackCount: Int = 0,
    val coverUrl: String? = null,
    val tracks: List<TrackItem> = emptyList()
)

data class DesktopLibrary(
    val playlists: List<DesktopPlaylist> = emptyList(),
    val likedSongs: List<TrackItem> = emptyList(),
    val lastSyncedAt: Long = 0L
)

class DesktopDatabase private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: DesktopDatabase? = null

        fun getInstance(): DesktopDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DesktopDatabase().also { INSTANCE = it }
            }
        }

        private fun getLibraryFile(): File {
            val localAppData = System.getenv("LOCALAPPDATA")
                ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local")
            val dir = File(localAppData, "UTA-wave")
            if (!dir.exists()) dir.mkdirs()
            return File(dir, "library.json")
        }
    }

    private val libraryFile = getLibraryFile()
    private val _library = MutableStateFlow(loadLibrary())
    val library: StateFlow<DesktopLibrary> = _library.asStateFlow()

    private fun loadLibrary(): DesktopLibrary {
        try {
            if (!libraryFile.exists()) return DesktopLibrary()
            val text = libraryFile.readText(Charsets.UTF_8)
            if (text.isBlank()) return DesktopLibrary()

            val root = JSONObject(text)
            val playlistsArray = root.optJSONArray("playlists") ?: JSONArray()
            val playlists = mutableListOf<DesktopPlaylist>()

            for (i in 0 until playlistsArray.length()) {
                val pObj = playlistsArray.optJSONObject(i) ?: continue
                val id = pObj.optString("id", "")
                val name = pObj.optString("name", "Untitled Playlist")
                val desc = pObj.optString("description").takeIf { !it.isNullOrBlank() }
                val cover = pObj.optString("cover_url").takeIf { !it.isNullOrBlank() }
                val trackCount = pObj.optInt("track_count", 0)

                val tracksArray = pObj.optJSONArray("tracks") ?: JSONArray()
                val tracks = mutableListOf<TrackItem>()
                for (j in 0 until tracksArray.length()) {
                    val tObj = tracksArray.optJSONObject(j) ?: continue
                    tracks.add(
                        TrackItem(
                            id = tObj.optString("id", ""),
                            videoId = tObj.optString("video_id", ""),
                            title = tObj.optString("title", "Unknown"),
                            artist = tObj.optString("artist", "Unknown"),
                            album = tObj.optString("album").takeIf { !it.isNullOrBlank() },
                            artworkUrl = tObj.optString("artwork_url", ""),
                            durationSeconds = tObj.optInt("duration_seconds", 0)
                        )
                    )
                }

                playlists.add(
                    DesktopPlaylist(
                        id = id,
                        name = name,
                        description = desc,
                        trackCount = if (tracks.isNotEmpty()) tracks.size else trackCount,
                        coverUrl = cover ?: tracks.firstOrNull()?.artworkUrl,
                        tracks = tracks
                    )
                )
            }

            val likedArray = root.optJSONArray("liked_songs") ?: JSONArray()
            val liked = mutableListOf<TrackItem>()
            for (k in 0 until likedArray.length()) {
                val tObj = likedArray.optJSONObject(k) ?: continue
                liked.add(
                    TrackItem(
                        id = tObj.optString("id", ""),
                        videoId = tObj.optString("video_id", ""),
                        title = tObj.optString("title", "Unknown"),
                        artist = tObj.optString("artist", "Unknown"),
                        album = tObj.optString("album").takeIf { !it.isNullOrBlank() },
                        artworkUrl = tObj.optString("artwork_url", ""),
                        durationSeconds = tObj.optInt("duration_seconds", 0)
                    )
                )
            }

            val lastSynced = root.optLong("last_synced_at", 0L)
            return DesktopLibrary(playlists = playlists, likedSongs = liked, lastSyncedAt = lastSynced)
        } catch (e: Exception) {
            println("[DesktopDatabase] Failed to load library.json: ${e.message}")
            return DesktopLibrary()
        }
    }

    fun saveLibrary(playlists: List<DesktopPlaylist>, likedSongs: List<TrackItem>) {
        val lastSynced = System.currentTimeMillis()
        val newLib = DesktopLibrary(playlists = playlists, likedSongs = likedSongs, lastSyncedAt = lastSynced)

        try {
            val root = JSONObject().apply {
                put("last_synced_at", lastSynced)

                val pArray = JSONArray()
                for (p in playlists) {
                    val pObj = JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("description", p.description ?: "")
                        put("cover_url", p.coverUrl ?: "")
                        put("track_count", p.trackCount)

                        val tArray = JSONArray()
                        for (t in p.tracks) {
                            tArray.put(
                                JSONObject().apply {
                                    put("id", t.id)
                                    put("video_id", t.videoId)
                                    put("title", t.title)
                                    put("artist", t.artist)
                                    put("album", t.album ?: "")
                                    put("artwork_url", t.artworkUrl)
                                    put("duration_seconds", t.durationSeconds)
                                }
                            )
                        }
                        put("tracks", tArray)
                    }
                    pArray.put(pObj)
                }
                put("playlists", pArray)

                val lArray = JSONArray()
                for (t in likedSongs) {
                    lArray.put(
                        JSONObject().apply {
                            put("id", t.id)
                            put("video_id", t.videoId)
                            put("title", t.title)
                            put("artist", t.artist)
                            put("album", t.album ?: "")
                            put("artwork_url", t.artworkUrl)
                            put("duration_seconds", t.durationSeconds)
                        }
                    )
                }
                put("liked_songs", lArray)
            }

            libraryFile.writeText(root.toString(2), Charsets.UTF_8)
            println("[DesktopDatabase] Saved ${playlists.size} playlists & ${likedSongs.size} liked songs to library.json")
        } catch (e: Exception) {
            println("[DesktopDatabase] Failed to write library.json: ${e.message}")
        }

        _library.value = newLib
    }

    fun updatePlaylistTracks(playlistId: String, tracks: List<TrackItem>) {
        val current = _library.value.playlists.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val p = current[index]
            current[index] = p.copy(
                tracks = tracks,
                trackCount = tracks.size,
                coverUrl = p.coverUrl ?: tracks.firstOrNull()?.artworkUrl
            )
            saveLibrary(current, _library.value.likedSongs)
        }
    }
}
