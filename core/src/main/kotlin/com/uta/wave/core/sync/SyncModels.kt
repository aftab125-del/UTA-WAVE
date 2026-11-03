package com.uta.wave.core.sync

import kotlinx.serialization.Serializable

/**
 * Common data contracts for cross-platform cloud sync between Mobile (Android/iOS)
 * and Windows Desktop via Supabase PostgreSQL + Realtime WebSockets.
 */

@Serializable
data class SyncUserProfile(
    val id: String,
    val displayName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val preferences: SyncUserPreferences = SyncUserPreferences()
)

@Serializable
data class SyncUserPreferences(
    val audioQuality: String = "HIGH",
    val crossfadeSeconds: Int = 0,
    val loudnessNormalization: Boolean = true,
    val theme: String = "DARK",
    val liquidGlassEnabled: Boolean = true
)

@Serializable
data class SyncPlaylist(
    val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val artworkUrl: String? = null,
    val isFavorite: Boolean = false,
    val syncVersion: Long = 1L,
    val isDeleted: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class SyncPlaylistTrack(
    val id: String,
    val playlistId: String,
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val duration: Int = 0,
    val position: Int = 0,
    val addedAt: String
)

@Serializable
data class SyncFavoriteTrack(
    val id: String,
    val userId: String,
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val duration: Int = 0,
    val likedAt: String
)

@Serializable
data class SyncHistoryItem(
    val id: String,
    val userId: String,
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val duration: Int = 0,
    val secondsPlayed: Int = 30,
    val deviceType: String, // "mobile_android" | "desktop_windows" | "mobile_ios"
    val playedAt: String
)

/**
 * Real-time Active Session contract for "UTA Connect" (Remote Control & Handoff).
 */
@Serializable
data class SyncPlaybackSession(
    val deviceId: String,
    val userId: String,
    val deviceName: String,
    val deviceType: String, // "windows" | "android" | "ios"
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTrackJson: String? = null,
    val positionMs: Long = 0L,
    val volume: Float = 1.0f,
    val lastHeartbeat: String
)

/**
 * Local offline-outbox mutation queue entity for offline-first resilience.
 */
@Serializable
data class OfflineMutation(
    val id: Long = 0L,
    val entityType: String, // "playlist" | "playlist_track" | "favorite" | "history" | "preference"
    val action: String,     // "INSERT" | "UPDATE" | "DELETE"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
