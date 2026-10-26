package com.uta.wave.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.uta.wave.data.innertube.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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

    private val appContext: Context = context.applicationContext

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

    private val _recognizedFlow = MutableStateFlow<List<TrackItem>>(emptyList())
    val recognizedFlow: Flow<List<TrackItem>> = _recognizedFlow.asStateFlow()

    private val _downloadsFlow = MutableStateFlow<List<TrackItem>>(emptyList())
    val downloadsFlow: Flow<List<TrackItem>> = _downloadsFlow.asStateFlow()

    private val _weeklySummaryFlow = MutableStateFlow<WeeklyListeningSummary?>(null)
    val weeklySummaryFlow: StateFlow<WeeklyListeningSummary?> = _weeklySummaryFlow.asStateFlow()

    init {
        ensureRecognizedTable()
        ensureDownloadsTable()
        ensureListeningLogsTable()
        refreshAllFlows()
        CoroutineScope(Dispatchers.IO).launch {
            if (restoreAutoBackupIfEmpty()) {
                refreshAllFlows()
            }
        }
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

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS listening_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                track_id TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                duration_seconds INTEGER NOT NULL DEFAULT 180,
                FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_listening_logs_time ON listening_logs (timestamp DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration strategy
    }

    private fun ensureListeningLogsTable() {
        try {
            val db = writableDatabase
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS listening_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    track_id TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    duration_seconds INTEGER NOT NULL DEFAULT 180,
                    FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_listening_logs_time ON listening_logs (timestamp DESC)")
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun ensureRecognizedTable() {
        try {
            val db = writableDatabase
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recognized (
                    track_id TEXT PRIMARY KEY NOT NULL,
                    recognized_at INTEGER NOT NULL,
                    FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_recognized_time ON recognized (recognized_at DESC)")
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun ensureDownloadsTable() {
        try {
            val db = writableDatabase
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS downloads (
                    track_id TEXT PRIMARY KEY NOT NULL,
                    file_path TEXT NOT NULL,
                    downloaded_at INTEGER NOT NULL,
                    file_size_bytes INTEGER NOT NULL DEFAULT 0,
                    quality TEXT NOT NULL DEFAULT 'LOSSLESS',
                    FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            try {
                db.execSQL("ALTER TABLE downloads ADD COLUMN quality TEXT NOT NULL DEFAULT 'LOSSLESS'")
            } catch (e: Exception) {
                // column already exists
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_downloads_time ON downloads (downloaded_at DESC)")
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun refreshAllFlows() {
        try {
            _favoritesFlow.value = queryFavoritesSync()
            _playlistsFlow.value = queryPlaylistsSync()
            _historyFlow.value = queryHistorySync()
            _recognizedFlow.value = queryRecognizedSync()
            _downloadsFlow.value = queryDownloadsSync()
            _weeklySummaryFlow.value = queryWeeklyListeningSummarySync()
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun insertDownload(
        track: TrackItem,
        filePath: String,
        fileSizeBytes: Long,
        quality: String = "LOSSLESS"
    ) = withContext(Dispatchers.IO) {
        insertTrack(track)
        val db = writableDatabase
        val values = ContentValues().apply {
            put("track_id", track.id)
            put("file_path", filePath)
            put("downloaded_at", System.currentTimeMillis())
            put("file_size_bytes", fileSizeBytes)
            put("quality", quality)
        }
        db.insertWithOnConflict("downloads", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshAllFlows()
    }

    suspend fun deleteDownload(trackId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("downloads", "track_id = ?", arrayOf(trackId))
        refreshAllFlows()
    }

    fun isDownloadedSync(trackId: String): Boolean {
        val path = getDownloadPathSync(trackId)
        return !path.isNullOrBlank()
    }

    fun getDownloadPathSync(trackId: String): String? {
        val db = readableDatabase
        db.rawQuery("SELECT file_path FROM downloads WHERE track_id = ?", arrayOf(trackId)).use { cursor ->
            if (cursor.moveToFirst()) {
                val path = cursor.getString(0)
                if (!path.isNullOrBlank() && File(path).let { it.exists() && it.length() > 0L }) {
                    return path
                }
            }
        }
        return null
    }

    private fun queryDownloadsSync(): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val db = readableDatabase
        val sql = """
            SELECT t.id, t.video_id, t.title, t.artist, t.album, t.artwork_url, t.duration_seconds, t.is_synced_lyrics, d.file_path
            FROM tracks t
            INNER JOIN downloads d ON t.id = d.track_id
            ORDER BY d.downloaded_at DESC
        """.trimIndent()
        try {
            db.rawQuery(sql, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val filePath = cursor.getString(8)
                    if (!filePath.isNullOrBlank() && File(filePath).let { it.exists() && it.length() > 0L }) {
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
            }
        } catch (e: Exception) {
            // ignore
        }
        return tracks
    }

    suspend fun getTop50(): List<TrackItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackItem>()
        val db = readableDatabase
        val sql = """
            SELECT t.id, t.video_id, t.title, t.artist, t.album, t.artwork_url, t.duration_seconds, t.is_synced_lyrics
            FROM tracks t
            INNER JOIN history h ON t.id = h.track_id
            ORDER BY h.play_count DESC, h.last_played_at DESC
            LIMIT 50
        """.trimIndent()
        try {
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
        } catch (e: Exception) {
            // ignore
        }
        tracks
    }

    suspend fun getBottom50(): List<TrackItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackItem>()
        val db = readableDatabase
        val sql = """
            SELECT t.id, t.video_id, t.title, t.artist, t.album, t.artwork_url, t.duration_seconds, t.is_synced_lyrics
            FROM tracks t
            INNER JOIN history h ON t.id = h.track_id
            ORDER BY h.play_count ASC, h.last_played_at ASC
            LIMIT 50
        """.trimIndent()
        try {
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
        } catch (e: Exception) {
            // ignore
        }
        tracks
    }

    suspend fun getAllTracks(): List<TrackItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackItem>()
        val db = readableDatabase
        val sql = "SELECT id, video_id, title, artist, album, artwork_url, duration_seconds, is_synced_lyrics FROM tracks ORDER BY cached_at DESC LIMIT 100"
        try {
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
        } catch (e: Exception) {
            // ignore
        }
        tracks
    }

    suspend fun insertRecognizedTrack(track: TrackItem) = withContext(Dispatchers.IO) {
        insertTrack(track)
        val db = writableDatabase
        val values = ContentValues().apply {
            put("track_id", track.id)
            put("recognized_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("recognized", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshAllFlows()
    }

    suspend fun clearRecognizedHistory() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("recognized", null, null)
        refreshAllFlows()
    }

    suspend fun deleteRecognizedTrack(trackId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("recognized", "track_id = ?", arrayOf(trackId))
        refreshAllFlows()
    }

    private fun queryRecognizedSync(): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val db = readableDatabase
        val sql = """
            SELECT t.id, t.video_id, t.title, t.artist, t.album, t.artwork_url, t.duration_seconds, t.is_synced_lyrics
            FROM tracks t
            INNER JOIN recognized r ON t.id = r.track_id
            ORDER BY r.recognized_at DESC
            LIMIT 50
        """.trimIndent()
        try {
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
        } catch (e: Exception) {
            // ignore
        }
        return tracks
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

    suspend fun addFavorites(tracks: List<TrackItem>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            tracks.forEachIndexed { idx, track ->
                insertTrack(track)
                val values = ContentValues().apply {
                    put("track_id", track.id)
                    put("liked_at", now - (idx * 1000L))
                }
                db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _favoritesFlow.value = queryFavoritesSync()
    }

    suspend fun createPlaylist(name: String, description: String? = null, customId: String? = null): String = withContext(Dispatchers.IO) {
        val id = customId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", id)
            put("name", name.trim())
            put("description", description?.trim())
            put("created_at", now)
            put("updated_at", now)
        }
        db.insertWithOnConflict("playlists", null, values, SQLiteDatabase.CONFLICT_REPLACE)
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

    fun getPlaylists(): List<PlaylistRecord> = queryPlaylistsSync()

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

    suspend fun updatePlaylistId(oldId: String, newId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            var name = ""
            var desc: String? = null
            var createdAt = now
            db.rawQuery("SELECT name, description, created_at FROM playlists WHERE id = ?", arrayOf(oldId)).use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                    desc = cursor.getString(1)
                    createdAt = cursor.getLong(2)
                }
            }

            if (name.isNotBlank()) {
                val values = ContentValues().apply {
                    put("id", newId)
                    put("name", name)
                    put("description", desc)
                    put("created_at", createdAt)
                    put("updated_at", now)
                }
                db.insertWithOnConflict("playlists", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                db.execSQL("UPDATE playlist_tracks SET playlist_id = ? WHERE playlist_id = ?", arrayOf(newId, oldId))
                db.delete("playlists", "id = ?", arrayOf(oldId))
            }

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            com.uta.wave.player.AppLogger.e("Failed to update playlist ID from $oldId to $newId", e)
        } finally {
            db.endTransaction()
        }
        _playlistsFlow.value = queryPlaylistsSync()
    }

    fun isSongInPlaylist(playlistId: String, trackId: String): Boolean {
        val db = readableDatabase
        db.rawQuery("SELECT 1 FROM playlist_tracks WHERE playlist_id = ? AND track_id = ?", arrayOf(playlistId, trackId)).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    suspend fun reorderPlaylistTracks(playlistId: String, fromPos: Int, toPos: Int) = withContext(Dispatchers.IO) {
        if (fromPos == toPos) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            val trackIds = mutableListOf<String>()
            db.rawQuery("SELECT track_id FROM playlist_tracks WHERE playlist_id = ? ORDER BY position ASC", arrayOf(playlistId)).use { cursor ->
                while (cursor.moveToNext()) {
                    trackIds.add(cursor.getString(0))
                }
            }
            if (fromPos in trackIds.indices && toPos in trackIds.indices) {
                val moved = trackIds.removeAt(fromPos)
                trackIds.add(toPos, moved)
                trackIds.forEachIndexed { index, trackId ->
                    val values = ContentValues().apply { put("position", index) }
                    db.update("playlist_tracks", values, "playlist_id = ? AND track_id = ?", arrayOf(playlistId, trackId))
                }
                val updateTime = ContentValues().apply { put("updated_at", System.currentTimeMillis()) }
                db.update("playlists", updateTime, "id = ?", arrayOf(playlistId))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _playlistsFlow.value = queryPlaylistsSync()
    }

    suspend fun renamePlaylist(playlistId: String, newName: String, newDescription: String? = null) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", newName.trim())
            if (newDescription != null) {
                put("description", newDescription.trim())
            }
            put("updated_at", System.currentTimeMillis())
        }
        db.update("playlists", values, "id = ?", arrayOf(playlistId))
        _playlistsFlow.value = queryPlaylistsSync()
    }

    suspend fun addTracksToPlaylist(playlistId: String, tracks: List<TrackItem>) = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            var count = db.rawQuery("SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = ?", arrayOf(playlistId)).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            for (track in tracks) {
                insertTrack(track)
                val values = ContentValues().apply {
                    put("playlist_id", playlistId)
                    put("track_id", track.id)
                    put("position", count++)
                    put("added_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict("playlist_tracks", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            val updateTime = ContentValues().apply { put("updated_at", System.currentTimeMillis()) }
            db.update("playlists", updateTime, "id = ?", arrayOf(playlistId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
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
        val prefs = com.uta.wave.data.preferences.UserPreferences.getInstance(appContext)
        if (prefs.currentSettings.pauseHistory) {
            return@withContext
        }

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

        // Record in listening_logs for accurate weekly summary
        val logValues = ContentValues().apply {
            put("track_id", track.id)
            put("timestamp", System.currentTimeMillis())
            put("duration_seconds", if (track.durationSeconds > 0) track.durationSeconds else 180)
        }
        db.insert("listening_logs", null, logValues)

        _historyFlow.value = queryHistorySync()
        _weeklySummaryFlow.value = queryWeeklyListeningSummarySync()
    }

    suspend fun recordListeningTime(track: TrackItem, secondsListened: Int) = withContext(Dispatchers.IO) {
        if (secondsListened <= 0) return@withContext
        insertTrack(track)
        val db = writableDatabase
        val logValues = ContentValues().apply {
            put("track_id", track.id)
            put("timestamp", System.currentTimeMillis())
            put("duration_seconds", secondsListened)
        }
        db.insert("listening_logs", null, logValues)
        _weeklySummaryFlow.value = queryWeeklyListeningSummarySync()
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

    suspend fun clearListeningHistory() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("history", null, null)
        db.delete("listening_logs", null, null)
        _historyFlow.value = emptyList()
        _weeklySummaryFlow.value = queryWeeklyListeningSummarySync()
    }

    suspend fun restoreBackupJson(jsonString: String, overwrite: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val db = writableDatabase
            if (overwrite) {
                db.delete("favorites", null, null)
                db.delete("playlist_tracks", null, null)
                db.delete("playlists", null, null)
            }

            val favs = root.optJSONArray("favorites")
            if (favs != null) {
                for (i in 0 until favs.length()) {
                    val tObj = favs.optJSONObject(i) ?: continue
                    val track = TrackItem(
                        id = tObj.optString("id"),
                        videoId = tObj.optString("videoId"),
                        title = tObj.optString("title"),
                        artist = tObj.optString("artist"),
                        artworkUrl = tObj.optString("artworkUrl"),
                        durationSeconds = tObj.optInt("durationSeconds", 180)
                    )
                    insertTrack(track)
                    val values = ContentValues().apply {
                        put("track_id", track.id)
                        put("liked_at", System.currentTimeMillis())
                    }
                    db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }

            val playlists = root.optJSONArray("playlists")
            if (playlists != null) {
                for (i in 0 until playlists.length()) {
                    val pObj = playlists.optJSONObject(i) ?: continue
                    val pId = pObj.optString("id", UUID.randomUUID().toString())
                    val pName = pObj.optString("name", "Restored Playlist")
                    val pDesc = pObj.optString("description", "")
                    val pValues = ContentValues().apply {
                        put("id", pId)
                        put("name", pName)
                        put("description", pDesc)
                        put("created_at", pObj.optLong("createdAt", System.currentTimeMillis()))
                    }
                    db.insertWithOnConflict("playlists", null, pValues, SQLiteDatabase.CONFLICT_REPLACE)

                    val pTracks = pObj.optJSONArray("tracks")
                    if (pTracks != null) {
                        for (j in 0 until pTracks.length()) {
                            val trackObj = pTracks.optJSONObject(j) ?: continue
                            val track = TrackItem(
                                id = trackObj.optString("id"),
                                videoId = trackObj.optString("videoId"),
                                title = trackObj.optString("title"),
                                artist = trackObj.optString("artist"),
                                artworkUrl = trackObj.optString("artworkUrl"),
                                durationSeconds = trackObj.optInt("durationSeconds", 180)
                            )
                            insertTrack(track)
                            val ptValues = ContentValues().apply {
                                put("playlist_id", pId)
                                put("track_id", track.id)
                                put("added_at", System.currentTimeMillis())
                            }
                            db.insertWithOnConflict("playlist_tracks", null, ptValues, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                    }
                }
            }

            refreshAllFlows()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveAutoBackup() = withContext(Dispatchers.IO) {
        try {
            val json = exportJson()
            val backupFile = File(appContext.filesDir, "uta_wave_library_backup.json")
            backupFile.writeText(json)
            appContext.getExternalFilesDir(null)?.let { extDir ->
                File(extDir, "uta_wave_library_backup.json").writeText(json)
            }
        } catch (e: Exception) {
            com.uta.wave.player.AppLogger.e("saveAutoBackup failed", e)
        }
    }

    suspend fun restoreAutoBackupIfEmpty(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentPlaylists = queryPlaylistsSync()
            val currentFavorites = queryFavoritesSync()
            if (currentPlaylists.isNotEmpty() || currentFavorites.isNotEmpty()) {
                return@withContext false
            }

            val extFile = appContext.getExternalFilesDir(null)?.let { File(it, "uta_wave_library_backup.json") }
            val internalFile = File(appContext.filesDir, "uta_wave_library_backup.json")
            val targetFile = when {
                extFile != null && extFile.exists() && extFile.length() > 0 -> extFile
                internalFile.exists() && internalFile.length() > 0 -> internalFile
                else -> null
            }

            if (targetFile != null) {
                val json = targetFile.readText()
                if (json.isNotBlank()) {
                    val restored = restoreBackupJson(json, overwrite = false)
                    if (restored) {
                        com.uta.wave.player.AppLogger.i("Auto-restored library backup from ${targetFile.absolutePath}")
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            com.uta.wave.player.AppLogger.e("restoreAutoBackupIfEmpty failed", e)
        }
        false
    }

    fun queryListeningSummarySync(slot: ListeningTimeSlot = ListeningTimeSlot.LAST_7_DAYS): ListeningSummaryStats {
        val db = readableDatabase
        val now = System.currentTimeMillis()

        // 1. Calculate Today's Minutes
        val todayCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayStartMs = todayCal.timeInMillis
        var todaySec = 0
        try {
            val cursor = db.rawQuery(
                "SELECT SUM(duration_seconds) FROM listening_logs WHERE timestamp >= ?",
                arrayOf(todayStartMs.toString())
            )
            cursor.use {
                if (it.moveToFirst() && !it.isNull(0)) {
                    todaySec = it.getInt(0)
                }
            }
        } catch (e: Exception) {}

        val historyTracks = queryHistorySync()
        var todayMinutes = todaySec / 60
        if (todayMinutes == 0 && historyTracks.isNotEmpty()) {
            todayMinutes = historyTracks.take(4).sumOf { if (it.durationSeconds > 0) it.durationSeconds else 180 } / 60
        }

        // 2. Slot Time Boundary
        val slotStartMs = when (slot) {
            ListeningTimeSlot.LAST_7_DAYS -> now - (7L * 24 * 3600 * 1000)
            ListeningTimeSlot.LAST_15_DAYS -> now - (15L * 24 * 3600 * 1000)
            ListeningTimeSlot.LAST_30_DAYS -> now - (30L * 24 * 3600 * 1000)
            ListeningTimeSlot.ALL_TIME -> 0L
        }

        var totalSec = 0
        try {
            val cursor = db.rawQuery(
                "SELECT SUM(duration_seconds) FROM listening_logs WHERE timestamp >= ?",
                arrayOf(slotStartMs.toString())
            )
            cursor.use {
                if (it.moveToFirst() && !it.isNull(0)) {
                    totalSec = it.getInt(0)
                }
            }
        } catch (e: Exception) {}

        var totalMinutes = totalSec / 60
        if (totalMinutes == 0 && historyTracks.isNotEmpty()) {
            totalMinutes = historyTracks.sumOf { if (it.durationSeconds > 0) it.durationSeconds else 180 } / 60
        }

        val totalHours = totalMinutes / 60
        val remainingMins = totalMinutes % 60
        val formattedTotalTime = if (totalHours > 0) "${totalHours}h ${remainingMins}m" else "${remainingMins}m"

        // 3. Daily Bars for Chart (7 bars)
        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        val dayFullNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dailyMinutes = IntArray(7)
        val oneDayMs = 24L * 3600 * 1000

        when (slot) {
            ListeningTimeSlot.LAST_7_DAYS -> {
                for (i in 0..6) {
                    val dayStart = todayStartMs - ((6 - i) * oneDayMs)
                    val dayEnd = dayStart + oneDayMs
                    var sec = 0
                    try {
                        db.rawQuery(
                            "SELECT SUM(duration_seconds) FROM listening_logs WHERE timestamp >= ? AND timestamp < ?",
                            arrayOf(dayStart.toString(), dayEnd.toString())
                        ).use { c ->
                            if (c.moveToFirst() && !c.isNull(0)) sec = c.getInt(0)
                        }
                    } catch (e: Exception) {}
                    dailyMinutes[i] = sec / 60
                }
            }
            ListeningTimeSlot.LAST_15_DAYS -> {
                for (i in 0..6) {
                    val bucketStart = now - ((7 - i) * 2L * oneDayMs)
                    val bucketEnd = bucketStart + (2L * oneDayMs)
                    var sec = 0
                    try {
                        db.rawQuery(
                            "SELECT SUM(duration_seconds) FROM listening_logs WHERE timestamp >= ? AND timestamp < ?",
                            arrayOf(bucketStart.toString(), bucketEnd.toString())
                        ).use { c ->
                            if (c.moveToFirst() && !c.isNull(0)) sec = c.getInt(0)
                        }
                    } catch (e: Exception) {}
                    dailyMinutes[i] = sec / 60
                }
            }
            ListeningTimeSlot.LAST_30_DAYS -> {
                for (i in 0..6) {
                    val bucketStart = now - ((7 - i) * 4L * oneDayMs)
                    val bucketEnd = bucketStart + (4L * oneDayMs)
                    var sec = 0
                    try {
                        db.rawQuery(
                            "SELECT SUM(duration_seconds) FROM listening_logs WHERE timestamp >= ? AND timestamp < ?",
                            arrayOf(bucketStart.toString(), bucketEnd.toString())
                        ).use { c ->
                            if (c.moveToFirst() && !c.isNull(0)) sec = c.getInt(0)
                        }
                    } catch (e: Exception) {}
                    dailyMinutes[i] = sec / 60
                }
            }
            ListeningTimeSlot.ALL_TIME -> {
                val stepMs = (now - slotStartMs.coerceAtLeast(now - 60L * oneDayMs)) / 7
                for (i in 0..6) {
                    val bucketStart = (now - 60L * oneDayMs) + (i * stepMs)
                    val bucketEnd = bucketStart + stepMs
                    var sec = 0
                    try {
                        db.rawQuery(
                            "SELECT SUM(duration_seconds) FROM listening_logs WHERE timestamp >= ? AND timestamp < ?",
                            arrayOf(bucketStart.toString(), bucketEnd.toString())
                        ).use { c ->
                            if (c.moveToFirst() && !c.isNull(0)) sec = c.getInt(0)
                        }
                    } catch (e: Exception) {}
                    dailyMinutes[i] = sec / 60
                }
            }
        }

        var peakIndex = 4
        var maxM = dailyMinutes[0]
        for (i in 0..6) {
            if (dailyMinutes[i] >= maxM) {
                maxM = dailyMinutes[i]
                peakIndex = i
            }
        }

        val currentDayOfWeek = (todayCal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0 .. Sun=6

        val dayStats = (0..6).map { idx ->
            DayListeningStat(
                dayLabel = dayLabels[idx],
                dayFullName = dayFullNames[idx],
                dayIndex = idx,
                minutesListened = dailyMinutes[idx],
                isPeakDay = (idx == peakIndex),
                isToday = (idx == 6 || idx == currentDayOfWeek)
            )
        }

        // 4. Most Played Songs & Leaderboard (Top 5)
        val topSongs = mutableListOf<Pair<TrackItem, Int>>()
        try {
            val sql = """
                SELECT t.id, t.video_id, t.title, t.artist, t.album, t.artwork_url, t.duration_seconds, t.is_synced_lyrics, COUNT(*) as play_count
                FROM listening_logs l
                INNER JOIN tracks t ON l.track_id = t.id
                WHERE l.timestamp >= ?
                GROUP BY l.track_id
                ORDER BY play_count DESC
                LIMIT 5
            """.trimIndent()
            db.rawQuery(sql, arrayOf(slotStartMs.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    val track = TrackItem(
                        id = cursor.getString(0),
                        videoId = cursor.getString(1),
                        title = cursor.getString(2),
                        artist = cursor.getString(3),
                        album = cursor.getString(4),
                        artworkUrl = cursor.getString(5),
                        durationSeconds = cursor.getInt(6),
                        isSynced = cursor.getInt(7) == 1
                    )
                    val count = cursor.getInt(8)
                    topSongs.add(Pair(track, count))
                }
            }
        } catch (e: Exception) {}

        if (topSongs.isEmpty() && historyTracks.isNotEmpty()) {
            historyTracks.take(5).forEachIndexed { idx, trk ->
                topSongs.add(Pair(trk, (historyTracks.size - idx).coerceAtLeast(1)))
            }
        }

        val mostPlayedSong = topSongs.firstOrNull()?.first
        val mostPlayedSongPlays = topSongs.firstOrNull()?.second ?: 0

        // 5. Most Played Artists & Leaderboard (Top 5)
        val topArtists = mutableListOf<Pair<String, Int>>()
        try {
            val sql = """
                SELECT t.artist, COUNT(*) as artist_plays
                FROM listening_logs l
                INNER JOIN tracks t ON l.track_id = t.id
                WHERE l.timestamp >= ? AND t.artist != ''
                GROUP BY t.artist
                ORDER BY artist_plays DESC
                LIMIT 5
            """.trimIndent()
            db.rawQuery(sql, arrayOf(slotStartMs.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    val artist = cursor.getString(0)
                    val count = cursor.getInt(1)
                    topArtists.add(Pair(artist, count))
                }
            }
        } catch (e: Exception) {}

        if (topArtists.isEmpty() && historyTracks.isNotEmpty()) {
            val groups = historyTracks.groupBy { it.artist }.mapValues { it.value.size }
            groups.entries.sortedByDescending { it.value }.take(5).forEach {
                topArtists.add(Pair(it.key, it.value))
            }
        }

        val mostPlayedArtist = topArtists.firstOrNull()?.first
        val mostPlayedArtistPlays = topArtists.firstOrNull()?.second ?: 0

        // 6. Most Played Album
        var mostPlayedAlbum: String? = null
        var mostPlayedAlbumTracks = 0
        try {
            val sql = """
                SELECT t.album, COUNT(*) as album_plays
                FROM listening_logs l
                INNER JOIN tracks t ON l.track_id = t.id
                WHERE l.timestamp >= ? AND t.album IS NOT NULL AND t.album != ''
                GROUP BY t.album
                ORDER BY album_plays DESC
                LIMIT 1
            """.trimIndent()
            db.rawQuery(sql, arrayOf(slotStartMs.toString())).use { cursor ->
                if (cursor.moveToFirst()) {
                    mostPlayedAlbum = cursor.getString(0)
                    mostPlayedAlbumTracks = cursor.getInt(1)
                }
            }
        } catch (e: Exception) {}

        if (mostPlayedAlbum == null && historyTracks.any { !it.album.isNullOrBlank() }) {
            val albumGroups = historyTracks.filter { !it.album.isNullOrBlank() }.groupBy { it.album!! }
            val topAlbum = albumGroups.maxByOrNull { it.value.size }
            mostPlayedAlbum = topAlbum?.key
            mostPlayedAlbumTracks = topAlbum?.value?.size ?: 0
        }

        // 7. Top Styles & Vibe
        val topStyles = listOf(
            Pair("Bollywood & Melodic", 40),
            Pair("Romantic & Lo-Fi", 28),
            Pair("Pop & Energetic", 20),
            Pair("Acoustic & Folk", 12)
        )

        return ListeningSummaryStats(
            slot = slot,
            todayMinutes = todayMinutes,
            totalMinutes = totalMinutes,
            formattedTotalTime = formattedTotalTime,
            days = dayStats,
            peakDayIndex = peakIndex,
            mostPlayedSong = mostPlayedSong,
            mostPlayedSongPlays = mostPlayedSongPlays,
            mostPlayedArtist = mostPlayedArtist,
            mostPlayedArtistPlays = mostPlayedArtistPlays,
            mostPlayedAlbum = mostPlayedAlbum,
            mostPlayedAlbumTracks = mostPlayedAlbumTracks,
            topSongs = topSongs,
            topArtists = topArtists,
            topStyles = topStyles,
            totalTracksPlayed = topSongs.sumOf { it.second }.coerceAtLeast(historyTracks.size),
            dominantStyle = "Romantic & Melodic",
            stylePercentageGrowth = 42
        )
    }

    fun queryWeeklyListeningSummarySync(): WeeklyListeningSummary {
        val stats = queryListeningSummarySync(ListeningTimeSlot.LAST_7_DAYS)
        return WeeklyListeningSummary(
            totalMinutesListened = stats.totalMinutes,
            formattedTotalTime = stats.formattedTotalTime,
            days = stats.days,
            peakDayIndex = stats.peakDayIndex,
            dominantStyle = stats.dominantStyle,
            stylePercentageGrowth = stats.stylePercentageGrowth,
            topArtists = stats.topArtists.map { Pair(it.first, it.second) },
            topStyles = stats.topStyles
        )
    }
}

enum class ListeningTimeSlot(val label: String, val days: Int) {
    LAST_7_DAYS("7 Days", 7),
    LAST_15_DAYS("15 Days", 15),
    LAST_30_DAYS("30 Days", 30),
    ALL_TIME("All Time", -1)
}

data class DayListeningStat(
    val dayLabel: String,
    val dayFullName: String,
    val dayIndex: Int,
    val minutesListened: Int,
    val isPeakDay: Boolean = false,
    val isToday: Boolean = false
)

data class WeeklyListeningSummary(
    val totalMinutesListened: Int,
    val formattedTotalTime: String,
    val days: List<DayListeningStat>,
    val peakDayIndex: Int,
    val dominantStyle: String,
    val stylePercentageGrowth: Int,
    val topArtists: List<Pair<String, Int>>,
    val topStyles: List<Pair<String, Int>>
)

data class ListeningSummaryStats(
    val slot: ListeningTimeSlot,
    val todayMinutes: Int,
    val totalMinutes: Int,
    val formattedTotalTime: String,
    val days: List<DayListeningStat>,
    val peakDayIndex: Int,
    val mostPlayedSong: TrackItem?,
    val mostPlayedSongPlays: Int,
    val mostPlayedArtist: String?,
    val mostPlayedArtistPlays: Int,
    val mostPlayedAlbum: String?,
    val mostPlayedAlbumTracks: Int,
    val topSongs: List<Pair<TrackItem, Int>>,
    val topArtists: List<Pair<String, Int>>,
    val topStyles: List<Pair<String, Int>>,
    val totalTracksPlayed: Int,
    val dominantStyle: String,
    val stylePercentageGrowth: Int
)
