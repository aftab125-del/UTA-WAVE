package com.uta.wave.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.uta.wave.data.innertube.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PlaylistRecord(
    val id: String,
    val name: String,
    val description: String?,
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

class LocalDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "utaverse_wave.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDatabase(context).also { INSTANCE = it }
            }
        }
    }

    private val _favoritesFlow = MutableStateFlow<List<TrackItem>>(emptyList())
    val favoritesFlow: Flow<List<TrackItem>> = _favoritesFlow.asStateFlow()

    private val _playlistsFlow = MutableStateFlow<List<PlaylistRecord>>(emptyList())
    val playlistsFlow: Flow<List<PlaylistRecord>> = _playlistsFlow.asStateFlow()

    private val _historyFlow = MutableStateFlow<List<TrackItem>>(emptyList())
    val historyFlow: Flow<List<TrackItem>> = _historyFlow.asStateFlow()

    init {
        refreshAllFlows()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tracks (
                id TEXT PRIMARY KEY NOT NULL,
                video_id TEXT NOT NULL,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                artwork_url TEXT NOT NULL,
                duration_seconds INTEGER NOT NULL DEFAULT 0,
                is_synced_lyrics INTEGER NOT NULL DEFAULT 0,
                cached_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tracks_video_id ON tracks (video_id)")

        db.execSQL(
            """
            CREATE TABLE playlists (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE playlist_tracks (
                playlist_id TEXT NOT NULL,
                track_id TEXT NOT NULL,
                position INTEGER NOT NULL DEFAULT 0,
                added_at INTEGER NOT NULL,
                PRIMARY KEY (playlist_id, track_id),
                FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE,
                FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_playlist_tracks_pos ON playlist_tracks (playlist_id, position)")

        db.execSQL(
            """
            CREATE TABLE favorites (
                track_id TEXT PRIMARY KEY NOT NULL,
                liked_at INTEGER NOT NULL,
                FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_favorites_liked_at ON favorites (liked_at DESC)")

        db.execSQL(
            """
            CREATE TABLE history (
                track_id TEXT PRIMARY KEY NOT NULL,
                last_played_at INTEGER NOT NULL,
                play_count INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_history_last_played ON history (last_played_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration strategy
    }

    private fun refreshAllFlows() {
        try {
            _favoritesFlow.value = queryFavoritesSync()
            _playlistsFlow.value = queryPlaylistsSync()
            _historyFlow.value = queryHistorySync()
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun insertTrack(track: TrackItem) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", track.id)
            put("video_id", track.videoId)
            put("title", track.title)
            put("artist", track.artist)
            put("album", track.album)
            put("artwork_url", track.artworkUrl)
            put("duration_seconds", track.durationSeconds)
            put("is_synced_lyrics", if (track.isSynced) 1 else 0)
            put("cached_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            "tracks",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun toggleFavorite(track: TrackItem): Boolean = withContext(Dispatchers.IO) {
        insertTrack(track)
        val db = writableDatabase
        val isFav = isFavoriteSync(track.id)
        if (isFav) {
            db.delete("favorites", "track_id = ?", arrayOf(track.id))
        } else {
            val values = ContentValues().apply {
                put("track_id", track.id)
                put("liked_at", System.currentTimeMillis())
            }
            db.insert("favorites", null, values)
        }
        _favoritesFlow.value = queryFavoritesSync()
        !isFav
    }

    fun isFavoriteSync(trackId: String): Boolean {
        val db = readableDatabase
        db.rawQuery("SELECT 1 FROM favorites WHERE track_id = ?", arrayOf(trackId)).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    suspend fun createPlaylist(name: String, description: String? = null): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", id)
            put("name", name.trim())
            put("description", description?.trim())
            put("created_at", now)
            put("updated_at", now)
        }
        db.insert("playlists", null, values)
        _playlistsFlow.value = queryPlaylistsSync()
        id
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: TrackItem) = withContext(Dispatchers.IO) {
        insertTrack(track)
        val db = writableDatabase
        val count = db.rawQuery("SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = ?", arrayOf(playlistId)).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val values = ContentValues().apply {
            put("playlist_id", playlistId)
            put("track_id", track.id)
            put("position", count)
            put("added_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("playlist_tracks", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        _playlistsFlow.value = queryPlaylistsSync()
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("playlist_tracks", "playlist_id = ? AND track_id = ?", arrayOf(playlistId, trackId))
        _playlistsFlow.value = queryPlaylistsSync()
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("playlist_tracks", "playlist_id = ?", arrayOf(playlistId))
        db.delete("playlists", "id = ?", arrayOf(playlistId))
        _playlistsFlow.value = queryPlaylistsSync()
    }

    suspend fun getPlaylistTracks(playlistId: String): List<TrackItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackItem>()
        val db = readableDatabase
        val sql = """
            SELECT t.id, t.video_id, t.title, t.artist, t.album, t.artwork_url, t.duration_seconds, t.is_synced_lyrics
            FROM tracks t
            INNER JOIN playlist_tracks pt ON t.id = pt.track_id
            WHERE pt.playlist_id = ?
            ORDER BY pt.position ASC
        """.trimIndent()

        db.rawQuery(sql, arrayOf(playlistId)).use { cursor ->
            while (cursor.moveToNext()) {
                tracks.add(
                    TrackItem(
                        id = cursor.getString(0),
                        videoId = cursor.getString(1),
                        title = cursor.getString(2),
                        artist = cursor.getString(3),
                        album = cursor.getString(4),
                        artworkUrl = cursor.getString(5),
                        durationSeconds = cursor.getInt(6),
                        isSynced = cursor.getInt(7) == 1
                    )
                )
            }
        }
        tracks
    }

    suspend fun recordHistory(track: TrackItem) = withContext(Dispatchers.IO) {
        insertTrack(track)
        val db = writableDatabase
        val existingCount = db.rawQuery("SELECT play_count FROM history WHERE track_id = ?", arrayOf(track.id)).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val values = ContentValues().apply {
            put("track_id", track.id)
            put("last_played_at", System.currentTimeMillis())
            put("play_count", existingCount + 1)
        }
        db.insertWithOnConflict("history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _historyFlow.value = queryHistorySync()
    }

    private fun queryFavoritesSync(): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val db = readableDatabase
        val sql = """
            SELECT t.id, t.video_id, t.title, t.artist, t.album, t.artwork_url, t.duration_seconds, t.is_synced_lyrics
            FROM tracks t
            INNER JOIN favorites f ON t.id = f.track_id
            ORDER BY f.liked_at DESC
        """.trimIndent()
        db.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                tracks.add(
                    TrackItem(
                        id = cursor.getString(0),
                        videoId = cursor.getString(1),
                        title = cursor.getString(2),
                        artist = cursor.getString(3),
                        album = cursor.getString(4),
                        artworkUrl = cursor.getString(5),
                        durationSeconds = cursor.getInt(6),
                        isSynced = cursor.getInt(7) == 1
                    )
                )
            }
        }
        return tracks
    }

    private fun queryPlaylistsSync(): List<PlaylistRecord> {
        val list = mutableListOf<PlaylistRecord>()
        val db = readableDatabase
        val sql = """
            SELECT p.id, p.name, p.description, p.created_at, COUNT(pt.track_id)
            FROM playlists p
            LEFT JOIN playlist_tracks pt ON p.id = pt.playlist_id
            GROUP BY p.id
            ORDER BY p.updated_at DESC
        """.trimIndent()
        db.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    PlaylistRecord(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        description = cursor.getString(2),
                        createdAt = cursor.getLong(3),
                        trackCount = cursor.getInt(4)
                    )
                )
            }
        }
        return list
    }

    private fun queryHistorySync(): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val db = readableDatabase
        val sql = """
            SELECT t.id, t.video_id, t.title, t.artist, t.album, t.artwork_url, t.duration_seconds, t.is_synced_lyrics
            FROM tracks t
            INNER JOIN history h ON t.id = h.track_id
            ORDER BY h.last_played_at DESC
            LIMIT 50
        """.trimIndent()
        db.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                tracks.add(
                    TrackItem(
                        id = cursor.getString(0),
                        videoId = cursor.getString(1),
                        title = cursor.getString(2),
                        artist = cursor.getString(3),
                        album = cursor.getString(4),
                        artworkUrl = cursor.getString(5),
                        durationSeconds = cursor.getInt(6),
                        isSynced = cursor.getInt(7) == 1
                    )
                )
            }
        }
        return tracks
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val favs = JSONArray()
        queryFavoritesSync().forEach { t ->
            favs.put(JSONObject().apply {
                put("id", t.id)
                put("videoId", t.videoId)
                put("title", t.title)
                put("artist", t.artist)
                put("artworkUrl", t.artworkUrl)
                put("durationSeconds", t.durationSeconds)
            })
        }
        root.put("favorites", favs)

        val playlistsArr = JSONArray()
        queryPlaylistsSync().forEach { p ->
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description)
                put("createdAt", p.createdAt)
            }
            val pTracks = JSONArray()
            getPlaylistTracks(p.id).forEach { t ->
                pTracks.put(JSONObject().apply {
                    put("id", t.id)
                    put("videoId", t.videoId)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("artworkUrl", t.artworkUrl)
                    put("durationSeconds", t.durationSeconds)
                })
            }
            pObj.put("tracks", pTracks)
            playlistsArr.put(pObj)
        }
        root.put("playlists", playlistsArr)

        root.toString(2)
    }
}
