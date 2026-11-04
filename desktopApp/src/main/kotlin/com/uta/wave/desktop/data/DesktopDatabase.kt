package com.uta.wave.desktop.data

import com.uta.wave.core.model.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object TextSanitizer {
    private val WINDOWS_1252: Charset = try {
        Charset.forName("windows-1252")
    } catch (_: Exception) {
        StandardCharsets.ISO_8859_1
    }

    fun fixMojibake(input: String?): String {
        if (input.isNullOrBlank()) return ""
        var current: String = input

        // Specific known emojis or double corrupted patterns
        if (current.contains("Ã°Å¸â€™Å’") || current.contains("ðŸ’Œ")) {
            current = "💌🫶🏻"
        }

        // Iterative CP1252 / ISO-8859-1 to UTF-8 decoding
        for (step in 0 until 3) {
            val hasHighAscii = current.any { it.code in 0x80..0xFF }
            if (!hasHighAscii) break

            var repaired: String? = null
            for (charset in listOf(WINDOWS_1252, StandardCharsets.ISO_8859_1)) {
                try {
                    val bytes = current.toByteArray(charset)
                    val cand = String(bytes, StandardCharsets.UTF_8)
                    if (!cand.contains('\uFFFD') && cand.length < current.length) {
                        repaired = cand
                        break
                    }
                } catch (_: Exception) {}
            }
            if (repaired != null) {
                current = repaired
            } else {
                break
            }
        }

        return current.replace("\u00A0", " ").replace("\u200B", "").trim()
    }
}

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

    fun loadLibrary(): DesktopLibrary {
        try {
            if (!libraryFile.exists()) return DesktopLibrary()
            val text = libraryFile.readText(Charsets.UTF_8).removePrefix("\uFEFF").trim()
            if (text.isBlank()) return DesktopLibrary()

            val root = JSONObject(text)
            val playlistsArray = root.optJSONArray("playlists") ?: JSONArray()
            val playlists = mutableListOf<DesktopPlaylist>()
            var hadMojibake = false

            for (i in 0 until playlistsArray.length()) {
                val pObj = playlistsArray.optJSONObject(i) ?: continue
                val id = pObj.optString("id", "")
                val rawName = pObj.optString("name", "Untitled Playlist")
                val name = TextSanitizer.fixMojibake(rawName)
                if (name != rawName) hadMojibake = true

                val rawDesc = pObj.optString("description").takeIf { !it.isNullOrBlank() }
                val desc = rawDesc?.let { TextSanitizer.fixMojibake(it) }
                if (desc != rawDesc) hadMojibake = true

                val cover = pObj.optString("cover_url").takeIf { !it.isNullOrBlank() }
                val trackCount = pObj.optInt("track_count", 0)

                val tracksArray = pObj.optJSONArray("tracks") ?: JSONArray()
                val tracks = mutableListOf<TrackItem>()
                for (j in 0 until tracksArray.length()) {
                    val tObj = tracksArray.optJSONObject(j) ?: continue
                    val rawTitle = tObj.optString("title", "Unknown")
                    val rawArtist = tObj.optString("artist", "Unknown")
                    val rawAlbum = tObj.optString("album").takeIf { !it.isNullOrBlank() }

                    val cleanTitle = TextSanitizer.fixMojibake(rawTitle)
                    val cleanArtist = TextSanitizer.fixMojibake(rawArtist)
                    val cleanAlbum = rawAlbum?.let { TextSanitizer.fixMojibake(it) }

                    if (cleanTitle != rawTitle || cleanArtist != rawArtist || cleanAlbum != rawAlbum) {
                        hadMojibake = true
                    }

                    tracks.add(
                        TrackItem(
                            id = tObj.optString("id", ""),
                            videoId = tObj.optString("video_id", ""),
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = cleanAlbum,
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
                val rawTitle = tObj.optString("title", "Unknown")
                val rawArtist = tObj.optString("artist", "Unknown")
                val rawAlbum = tObj.optString("album").takeIf { !it.isNullOrBlank() }

                val cleanTitle = TextSanitizer.fixMojibake(rawTitle)
                val cleanArtist = TextSanitizer.fixMojibake(rawArtist)
                val cleanAlbum = rawAlbum?.let { TextSanitizer.fixMojibake(it) }

                if (cleanTitle != rawTitle || cleanArtist != rawArtist || cleanAlbum != rawAlbum) {
                    hadMojibake = true
                }

                liked.add(
                    TrackItem(
                        id = tObj.optString("id", ""),
                        videoId = tObj.optString("video_id", ""),
                        title = cleanTitle,
                        artist = cleanArtist,
                        album = cleanAlbum,
                        artworkUrl = tObj.optString("artwork_url", ""),
                        durationSeconds = tObj.optInt("duration_seconds", 0)
                    )
                )
            }

            val lastSynced = root.optLong("last_synced_at", 0L)
            val result = DesktopLibrary(playlists = playlists, likedSongs = liked, lastSyncedAt = lastSynced)

            if (hadMojibake) {
                println("[DesktopDatabase] Mojibake repaired across library. Persisting sanitized library.json...")
                try {
                    saveLibrary(playlists, liked)
                } catch (_: Exception) {}
            }

            return result
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

    fun clearLibrary() {
        try {
            if (libraryFile.exists()) libraryFile.delete()
        } catch (_: Exception) {}
        _library.value = DesktopLibrary()
    }

    fun isFavorite(trackIdOrVideoId: String): Boolean {
        return _library.value.likedSongs.any { it.id == trackIdOrVideoId || it.videoId == trackIdOrVideoId }
    }

    fun toggleFavorite(track: TrackItem): Boolean {
        val currentLiked = _library.value.likedSongs.toMutableList()
        val exists = currentLiked.any { it.id == track.id || it.videoId == track.videoId }
        val isNowFavorite = if (exists) {
            currentLiked.removeAll { it.id == track.id || it.videoId == track.videoId }
            false
        } else {
            currentLiked.add(0, track)
            true
        }
        saveLibrary(_library.value.playlists, currentLiked)
        return isNowFavorite
    }

    fun addTrackToPlaylist(playlistId: String, track: TrackItem): Boolean {
        val currentPlaylists = _library.value.playlists.toMutableList()
        val idx = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (idx != -1) {
            val p = currentPlaylists[idx]
            val currentTracks = p.tracks.toMutableList()
            if (currentTracks.none { it.id == track.id || it.videoId == track.videoId }) {
                currentTracks.add(track)
                currentPlaylists[idx] = p.copy(
                    tracks = currentTracks,
                    trackCount = currentTracks.size,
                    coverUrl = p.coverUrl ?: track.artworkUrl
                )
                saveLibrary(currentPlaylists, _library.value.likedSongs)
                return true
            }
        }
        return false
    }

    fun removeTrackFromPlaylist(playlistId: String, trackIdOrVideoId: String): Boolean {
        val currentPlaylists = _library.value.playlists.toMutableList()
        val idx = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (idx != -1) {
            val p = currentPlaylists[idx]
            val currentTracks = p.tracks.toMutableList()
            val removed = currentTracks.removeAll { it.id == trackIdOrVideoId || it.videoId == trackIdOrVideoId }
            if (removed) {
                currentPlaylists[idx] = p.copy(
                    tracks = currentTracks,
                    trackCount = currentTracks.size
                )
                saveLibrary(currentPlaylists, _library.value.likedSongs)
                return true
            }
        }
        return false
    }

    fun createPlaylist(name: String, initialTrack: TrackItem?): DesktopPlaylist {
        val currentPlaylists = _library.value.playlists.toMutableList()
        val newId = "local_pl_" + System.currentTimeMillis()
        val tracks = if (initialTrack != null) listOf(initialTrack) else emptyList()
        val newPlaylist = DesktopPlaylist(
            id = newId,
            name = name.ifBlank { "New Playlist" },
            description = "Created on UTA-WAVE Desktop",
            trackCount = tracks.size,
            coverUrl = initialTrack?.artworkUrl,
            tracks = tracks
        )
        currentPlaylists.add(0, newPlaylist)
        saveLibrary(currentPlaylists, _library.value.likedSongs)
        return newPlaylist
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

    fun importMobileBackupJson(jsonString: String): Boolean {
        try {
            val root = JSONObject(jsonString)
            val newPlaylists = mutableListOf<DesktopPlaylist>()
            val newLiked = mutableListOf<TrackItem>()

            val favsArray = root.optJSONArray("favorites")
            if (favsArray != null) {
                for (i in 0 until favsArray.length()) {
                    val obj = favsArray.optJSONObject(i) ?: continue
                    val vid = obj.optString("videoId").ifBlank { obj.optString("id").removePrefix("youtube:") }
                    if (vid.isNotBlank()) {
                        newLiked.add(
                            TrackItem(
                                id = "youtube:$vid",
                                videoId = vid,
                                title = TextSanitizer.fixMojibake(obj.optString("title", "Unknown")),
                                artist = TextSanitizer.fixMojibake(obj.optString("artist", "Unknown")),
                                artworkUrl = obj.optString("artworkUrl", ""),
                                durationSeconds = obj.optInt("durationSeconds", 0)
                            )
                        )
                    }
                }
            }

            val playlistsArray = root.optJSONArray("playlists")
            if (playlistsArray != null) {
                for (i in 0 until playlistsArray.length()) {
                    val pObj = playlistsArray.optJSONObject(i) ?: continue
                    val pId = pObj.optString("id", java.util.UUID.randomUUID().toString())
                    val pName = TextSanitizer.fixMojibake(pObj.optString("name", "Mobile Playlist"))
                    val pDesc = pObj.optString("description").takeIf { !it.isNullOrBlank() }?.let { TextSanitizer.fixMojibake(it) }

                    val tracksArray = pObj.optJSONArray("tracks") ?: JSONArray()
                    val pTracks = mutableListOf<TrackItem>()
                    for (j in 0 until tracksArray.length()) {
                        val tObj = tracksArray.optJSONObject(j) ?: continue
                        val vid = tObj.optString("videoId").ifBlank { tObj.optString("id").removePrefix("youtube:") }
                        if (vid.isNotBlank()) {
                            pTracks.add(
                                TrackItem(
                                    id = "youtube:$vid",
                                    videoId = vid,
                                    title = TextSanitizer.fixMojibake(tObj.optString("title", "Unknown")),
                                    artist = TextSanitizer.fixMojibake(tObj.optString("artist", "Unknown")),
                                    artworkUrl = tObj.optString("artworkUrl", ""),
                                    durationSeconds = tObj.optInt("durationSeconds", 0)
                                )
                            )
                        }
                    }

                    newPlaylists.add(
                        DesktopPlaylist(
                            id = pId,
                            name = pName,
                            description = pDesc,
                            trackCount = pTracks.size,
                            coverUrl = pTracks.firstOrNull()?.artworkUrl,
                            tracks = pTracks
                        )
                    )
                }
            }

            if (newPlaylists.isNotEmpty() || newLiked.isNotEmpty()) {
                saveLibrary(newPlaylists, newLiked)
                return true
            }
        } catch (e: Exception) {
            println("[DesktopDatabase] Failed to import mobile backup: ${e.message}")
        }
        return false
    }
}
