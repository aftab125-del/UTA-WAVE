package com.uta.wave.core.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * State representing the background synchronization status.
 */
sealed class SyncState {
    data object Idle : SyncState()
    data class Syncing(val stage: String) : SyncState()
    data class Success(val playlistsMerged: Int, val favoritesMerged: Int, val timestamp: Long) : SyncState()
    data class Failed(val reason: String) : SyncState()
}

/**
 * Enterprise Multiplatform Cloud Sync Engine for UTA-WAVE.
 * Handles bi-directional synchronization between local SQLite database and Supabase Cloud.
 */
interface SyncEngine {
    val state: StateFlow<SyncState>

    /**
     * Executes a full bi-directional library sync:
     * 1. Flushes any pending local mutations in the offline outbox.
     * 2. Pulls remote updates since last_sync_timestamp from Supabase.
     * 3. Merges remote playlists, tracks, favorites, and history into local database.
     */
    suspend fun syncAll(): Result<Pair<Int, Int>>

    /**
     * Enqueues a local mutation to be synced to the cloud immediately if online,
     * or stored in the offline outbox if disconnected.
     */
    suspend fun enqueueMutation(mutation: OfflineMutation)

    /**
     * Connects to Supabase Realtime WebSocket channels to receive live deltas
     * from other active devices (e.g. PC receives song added on Phone).
     */
    suspend fun subscribeRealtime(userId: String)

    /**
     * Unsubscribes from Realtime channels on app background or sign-out.
     */
    suspend fun unsubscribeRealtime()
}
