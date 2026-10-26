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

        // Ensure InnerTubeClient has active auth headers
        InnerTubeClient.authCookie = profile.cookie
        InnerTubeClient.authSapisid = profile.sapisid

        _syncState.value = AccountSyncState.Syncing("Connecting to Google Account...")
        val localDatabase = LocalDatabase.getInstance(appContext)
        val innerTube = InnerTubeClient()

        var likedCount = 0
        var playlistsCount = 0

        try {
            // 1. Sync Liked Music ("LM")
            _syncState.value = AccountSyncState.Syncing("Syncing Liked Music...")
            try {
                val (_, likedTracks) = innerTube.getPlaylist("LM")
                if (likedTracks.isNotEmpty()) {
                    localDatabase.addFavorites(likedTracks)
                    likedCount = likedTracks.size
                    AppLogger.i("AccountSyncManager: Synced $likedCount liked songs into favorites")
                }
            } catch (e: Exception) {
                AppLogger.w("AccountSyncManager: Liked songs sync warning: ${e.message}")
            }

            // 2. Upload Phase: Scan for local & imported playlists that are not yet on YouTube Music
            _syncState.value = AccountSyncState.Syncing("Uploading imported & local playlists to account...")
            val localPlaylists = localDatabase.getPlaylists()
            for (localPlaylist in localPlaylists) {
                val isCloudPlaylist = localPlaylist.id.startsWith("PL") ||
                        localPlaylist.id.startsWith("VL") ||
                        localPlaylist.id.startsWith("RD") ||
                        localPlaylist.id.startsWith("MP")

                if (!isCloudPlaylist) {
                    try {
                        val tracks = localDatabase.getPlaylistTracks(localPlaylist.id)
                        if (tracks.isNotEmpty()) {
                            AppLogger.i("AccountSyncManager: Uploading local/imported playlist '${localPlaylist.name}' to Google Account...")
                            val remoteId = innerTube.createRemotePlaylist(localPlaylist.name)
                            if (!remoteId.isNullOrBlank()) {
                                val videoIds = tracks.mapNotNull { it.videoId.takeIf { vid -> vid.isNotBlank() } }
                                if (videoIds.isNotEmpty()) {
                                    innerTube.addTracksToRemotePlaylist(remoteId, videoIds)
                                }
                                localDatabase.updatePlaylistId(localPlaylist.id, remoteId)
                                AppLogger.i("AccountSyncManager: Successfully uploaded '${localPlaylist.name}' -> $remoteId with ${videoIds.size} tracks")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.w("AccountSyncManager: Failed to upload local playlist ${localPlaylist.name}: ${e.message}")
                    }
                }
            }

            // 3. Download Phase: Sync Google Account Playlists ("FEmusic_liked_playlists")
            _syncState.value = AccountSyncState.Syncing("Syncing Playlists from Google Account...")
            val remotePlaylists = innerTube.fetchUserPlaylists()
            for (remotePlaylist in remotePlaylists) {
                try {
                    // Create/update playlist in local database
                    localDatabase.createPlaylist(
                        name = remotePlaylist.title,
                        description = remotePlaylist.description ?: "Synced from YouTube Music",
                        customId = remotePlaylist.id
                    )

                    // Fetch tracks for this playlist and populate
                    val (_, tracks) = innerTube.getPlaylist(remotePlaylist.id)
                    if (tracks.isNotEmpty()) {
                        localDatabase.addTracksToPlaylist(remotePlaylist.id, tracks)
                    }
                    playlistsCount++
                } catch (e: Exception) {
                    AppLogger.w("AccountSyncManager: Failed to sync playlist ${remotePlaylist.title}: ${e.message}")
                }
            }

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
        }
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
        val accountManager = AccountManager.getInstance(appContext)
        val profile = accountManager.currentProfile
        val localDatabase = LocalDatabase.getInstance(appContext)

        if (profile.isLoggedIn && !profile.cookie.isNullOrBlank()) {
            try {
                InnerTubeClient.authCookie = profile.cookie
                InnerTubeClient.authSapisid = profile.sapisid

                val remoteId = InnerTubeClient().createRemotePlaylist(name)
                if (!remoteId.isNullOrBlank()) {
                    AppLogger.i("Created cloud playlist linked to Google account: $remoteId")
                    val id = localDatabase.createPlaylist(
                        name = name,
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
        val finalId = localDatabase.createPlaylist(name, description)
        localDatabase.saveAutoBackup()
        finalId
    }
}
