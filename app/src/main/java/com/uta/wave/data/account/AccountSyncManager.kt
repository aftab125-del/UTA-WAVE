package com.uta.wave.data.account

import android.content.Context
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

sealed class AccountSyncState {
    data object Idle : AccountSyncState()
    data class Syncing(val message: String) : AccountSyncState()
    data class Success(val playlistsSynced: Int, val likedSynced: Int, val timestamp: Long) : AccountSyncState()
    data class Error(val error: String) : AccountSyncState()
}

/**
 * Enterprise Account Cloud Sync Engine for UTA-WAVE.
 * Synchronizes playlists and Liked Music ("LM") between YouTube Music / Google Account and LocalDatabase.
 * Automatically restores user data upon Gmail sign-in or APK re-installation.
 */
class AccountSyncManager private constructor(private val appContext: Context) {

    companion object {
        private const val PREFS_SYNC = "uta_wave_sync_prefs"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"

        @Volatile
        private var INSTANCE: AccountSyncManager? = null

        fun getInstance(context: Context): AccountSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AccountSyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = appContext.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
    private val syncMutex = Mutex()

    private val _syncState = MutableStateFlow<AccountSyncState>(AccountSyncState.Idle)
    val syncState: StateFlow<AccountSyncState> = _syncState.asStateFlow()

    val lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)

    /**
     * Performs a full bi-directional sync:
     * 1. Fetches Liked Music ("LM") from YouTube Music -> inserts into LocalDatabase favorites.
     * 2. Fetches user playlists ("FEmusic_liked_playlists") -> inserts into LocalDatabase playlists with tracks.
     */
    suspend fun syncAll(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        val accountManager = AccountManager.getInstance(appContext)
        val profile = accountManager.currentProfile

        if (!profile.isLoggedIn || profile.cookie.isNullOrBlank()) {
            AppLogger.w("AccountSyncManager: Skipped sync - user not signed in")
            _syncState.value = AccountSyncState.Error("Sign in with Google to sync library")
            return@withContext Result.failure(IllegalStateException("User not signed in"))
        }

        if (!syncMutex.tryLock()) {
            AppLogger.i("AccountSyncManager: syncAll already in progress, skipping duplicate invocation")
            return@withContext Result.failure(IllegalStateException("Sync already in progress"))
        }

        try {
            // Ensure InnerTubeClient has active auth headers
            InnerTubeClient.authCookie = profile.cookie
            InnerTubeClient.authSapisid = profile.sapisid

            _syncState.value = AccountSyncState.Syncing("Connecting to Google Account...")
            val localDatabase = LocalDatabase.getInstance(appContext)
            val innerTube = InnerTubeClient()

            var likedCount = 0
            var playlistsCount = 0

            // Purge any unwanted YouTube system playlists (Shorts, Episodes, Liked Songs)
            localDatabase.purgeSystemPlaylists()

            // 1. Fetch remote playlists FIRST to know existing cloud playlists
            _syncState.value = AccountSyncState.Syncing("Fetching remote playlists from Google Account...")
            val remotePlaylists = try {
                innerTube.fetchUserPlaylists()
            } catch (e: Exception) {
                AppLogger.w("AccountSyncManager: Failed to fetch remote playlists: ${e.message}")
                emptyList()
            }

            // 2. Upload Phase: Scan for local & imported playlists that are not yet on YouTube Music
            _syncState.value = AccountSyncState.Syncing("Uploading imported & local playlists to account...")
            val localPlaylists = localDatabase.getPlaylists()
            for (localPlaylist in localPlaylists) {
                if (isIgnoredSystemPlaylist(localPlaylist.id, localPlaylist.name)) {
                    continue
                }
                val isCloudPlaylist = localPlaylist.id.startsWith("PL") ||
                        localPlaylist.id.startsWith("VL") ||
                        localPlaylist.id.startsWith("RD") ||
                        localPlaylist.id.startsWith("MP")

                if (!isCloudPlaylist) {
                    try {
                        val tracks = localDatabase.getPlaylistTracks(localPlaylist.id)
                        if (tracks.isNotEmpty()) {
                            // Check if a playlist with this title already exists on YouTube Music
                            val existingRemote = remotePlaylists.find {
                                it.title.trim().equals(localPlaylist.name.trim(), ignoreCase = true)
                            }

                            if (existingRemote != null) {
                                AppLogger.i("AccountSyncManager: Local playlist '${localPlaylist.name}' matches existing remote ${existingRemote.id}, linking instead of creating")
                                localDatabase.updatePlaylistId(localPlaylist.id, existingRemote.id)
                            } else {
                                AppLogger.i("AccountSyncManager: Uploading new playlist '${localPlaylist.name}' to Google Account...")
                                val videoIds = tracks.mapNotNull { it.videoId.takeIf { vid -> vid.isNotBlank() } }
                                val remoteId = innerTube.createRemotePlaylist(localPlaylist.name, "PRIVATE", videoIds)
                                if (!remoteId.isNullOrBlank()) {
                                    if (videoIds.size > 50) {
                                        val remaining = videoIds.drop(50)
                                        innerTube.addTracksToRemotePlaylist(remoteId, remaining)
                                    }
                                    localDatabase.updatePlaylistId(localPlaylist.id, remoteId)
                                    AppLogger.i("AccountSyncManager: Successfully uploaded '${localPlaylist.name}' -> $remoteId with ${videoIds.size} tracks")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.w("AccountSyncManager: Failed to upload local playlist ${localPlaylist.name}: ${e.message}")
                    }
                }
            }

            // 3. Download Phase: Sync Google Account Playlists ("FEmusic_liked_playlists")
            _syncState.value = AccountSyncState.Syncing("Syncing Playlists from Google Account...")
            for (remotePlaylist in remotePlaylists) {
                if (isIgnoredSystemPlaylist(remotePlaylist.id, remotePlaylist.title)) {
                    AppLogger.i("AccountSyncManager: Skipping YouTube system playlist: ${remotePlaylist.title}")
                    continue
                }
                try {
                    // Create/update playlist metadata in local database (automatically deduplicates and merges)
                    localDatabase.createPlaylist(
                        name = remotePlaylist.title,
                        description = remotePlaylist.description ?: "Synced from YouTube Music",
                        customId = remotePlaylist.id
                    )

                    // Fetch tracks for this playlist with scoped pagination
                    val (_, cloudTracks) = innerTube.getPlaylist(remotePlaylist.id)
                    val localTracks = localDatabase.getPlaylistTracks(remotePlaylist.id)

                    if (cloudTracks.isNotEmpty()) {
                        if (localTracks.isNotEmpty() && localTracks.size > cloudTracks.size) {
                            // Local has more tracks than cloud (e.g. tracks added offline or imported from Spotify)
                            val missingOnCloud = localTracks.filter { lt -> cloudTracks.none { ct -> ct.videoId == lt.videoId } }
                            val mergedTracks = cloudTracks.toMutableList().apply { addAll(missingOnCloud) }
                            localDatabase.replacePlaylistTracks(remotePlaylist.id, mergedTracks)

                            // Push the missing tracks to YouTube Music so cloud gets updated
                            val missingVideoIds = missingOnCloud.mapNotNull { it.videoId.takeIf { v -> v.isNotBlank() } }
                            if (missingVideoIds.isNotEmpty()) {
                                innerTube.addTracksToRemotePlaylist(remotePlaylist.id, missingVideoIds)
                            }
                        } else {
                            // Standard clean replace with properly sequenced cloud tracks
                            localDatabase.replacePlaylistTracks(remotePlaylist.id, cloudTracks)
                        }
                    } else if (localTracks.isNotEmpty()) {
                        // Cloud returned 0 tracks (temporary network/API glitch) - DO NOT wipe local tracks!
                        AppLogger.w("AccountSyncManager: Cloud returned 0 tracks for ${remotePlaylist.title}; preserving ${localTracks.size} local tracks")
                        val missingVideoIds = localTracks.mapNotNull { it.videoId.takeIf { v -> v.isNotBlank() } }
                        if (missingVideoIds.isNotEmpty()) {
                            innerTube.addTracksToRemotePlaylist(remotePlaylist.id, missingVideoIds)
                        }
                    }
                    playlistsCount++
                } catch (e: Exception) {
                    AppLogger.w("AccountSyncManager: Failed to sync playlist ${remotePlaylist.title}: ${e.message}")
                }
            }

            // 4. Run automated deduplication cleanup pass
            localDatabase.cleanupDuplicatePlaylists()
            localDatabase.refreshAllFlows()

            // Automatically persist auto-backup snapshot
            localDatabase.saveAutoBackup()

            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_SYNC, now).apply()

            _syncState.value = AccountSyncState.Success(
                playlistsSynced = playlistsCount,
                likedSynced = likedCount,
                timestamp = now
            )
            AppLogger.i("AccountSyncManager: Full sync completed. Playlists: $playlistsCount, Liked: $likedCount")
            Result.success(Pair(playlistsCount, likedCount))
        } catch (e: Exception) {
            AppLogger.e("AccountSyncManager: Full sync error", e)
            _syncState.value = AccountSyncState.Error(e.message ?: "Failed to sync with account")
            Result.failure(e)
        } finally {
            syncMutex.unlock()
        }
    }

    private fun isIgnoredSystemPlaylist(id: String, title: String): Boolean {
        val lowerId = id.uppercase()
        val lowerTitle = title.lowercase().trim()
        val ignoredIds = setOf("LM", "VLLM", "LL", "VLLL", "SE", "VLSE", "WL", "VLWL", "RDPN")
        if (ignoredIds.contains(lowerId)) return true

        val ignoredKeywords = listOf(
            "liked songs",
            "liked music",
            "liked videos",
            "your likes",
            "sounds from shorts",
            "sound from shorts",
            "sounds from short",
            "episode for later",
            "episodes for later",
            "episodes",
            "watch later"
        )
        return ignoredKeywords.any { lowerTitle.contains(it) }
    }

    /**
     * Trigger asynchronous sync in background (non-blocking).
     */
    fun triggerSync() {
        scope.launch {
            syncAll()
        }
    }

    /**
     * Creates a playlist. If the user is logged in with Gmail, creates it on their YouTube Music
     * Google account first and mirrors it locally so it permanently persists across reinstalls.
     */
    suspend fun createPlaylist(name: String, description: String? = null): String = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val accountManager = AccountManager.getInstance(appContext)
        val profile = accountManager.currentProfile
        val localDatabase = LocalDatabase.getInstance(appContext)

        // Check if playlist already exists locally
        val existingLocal = localDatabase.getPlaylistByNameSync(cleanName)
        if (existingLocal != null) {
            AppLogger.i("AccountSyncManager.createPlaylist: Playlist '$cleanName' already exists locally (${existingLocal.id}), returning existing")
            return@withContext existingLocal.id
        }

        if (profile.isLoggedIn && !profile.cookie.isNullOrBlank()) {
            try {
                InnerTubeClient.authCookie = profile.cookie
                InnerTubeClient.authSapisid = profile.sapisid

                val innerTube = InnerTubeClient()
                // Check if already exists on YouTube Music
                val remotePlaylists = innerTube.fetchUserPlaylists()
                val existingRemote = remotePlaylists.find {
                    it.title.trim().equals(cleanName, ignoreCase = true)
                }

                val remoteId = existingRemote?.id ?: innerTube.createRemotePlaylist(cleanName)
                if (!remoteId.isNullOrBlank()) {
                    AppLogger.i("Created/linked cloud playlist linked to Google account: $remoteId")
                    val id = localDatabase.createPlaylist(
                        name = cleanName,
                        description = description,
                        customId = remoteId
                    )
                    localDatabase.saveAutoBackup()
                    return@withContext id
                }
            } catch (e: Exception) {
                AppLogger.w("Could not create remote playlist, creating locally: ${e.message}")
            }
        }

        // Local creation fallback
        val finalId = localDatabase.createPlaylist(cleanName, description)
        localDatabase.saveAutoBackup()
        finalId
    }
}
