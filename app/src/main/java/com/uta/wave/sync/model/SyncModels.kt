package com.uta.wave.sync.model

object SyncMessageTypes {
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LEAVE_ROOM = "leave_room"
    const val APPROVE_JOIN = "approve_join"
    const val REJECT_JOIN = "reject_join"
    const val PLAYBACK_ACTION = "playback_action"
    const val BUFFER_READY = "buffer_ready"
    const val KICK_USER = "kick_user"
    const val TRANSFER_HOST = "transfer_host"
    const val PING = "ping"
    const val CHAT = "chat"
    const val REQUEST_SYNC = "request_sync"
    const val RECONNECT = "reconnect"
    const val SUGGEST_TRACK = "suggest_track"
    const val APPROVE_SUGGESTION = "approve_suggestion"
    const val REJECT_SUGGESTION = "reject_suggestion"
    const val UPDATE_ROOM_SETTINGS = "update_room_settings"

    const val ROOM_CREATED = "room_created"
    const val JOIN_REQUEST = "join_request"
    const val JOIN_APPROVED = "join_approved"
    const val JOIN_REJECTED = "join_rejected"
    const val USER_JOINED = "user_joined"
    const val USER_LEFT = "user_left"
    const val SYNC_PLAYBACK = "sync_playback"
    const val BUFFER_WAIT = "buffer_wait"
    const val BUFFER_COMPLETE = "buffer_complete"
    const val ERROR = "error"
    const val PONG = "pong"
    const val HOST_CHANGED = "host_changed"
    const val KICKED = "kicked"
    const val SYNC_STATE = "sync_state"
    const val RECONNECTED = "reconnected"
    const val USER_RECONNECTED = "user_reconnected"
    const val USER_DISCONNECTED = "user_disconnected"
    const val SUGGESTION_RECEIVED = "suggestion_received"
    const val SUGGESTION_APPROVED = "suggestion_approved"
    const val SUGGESTION_REJECTED = "suggestion_rejected"
    const val ROOM_SETTINGS_CHANGED = "room_settings_changed"
}

object SyncPlaybackActions {
    const val PLAY = "play"
    const val PAUSE = "pause"
    const val SEEK = "seek"
    const val SKIP_NEXT = "skip_next"
    const val SKIP_PREV = "skip_prev"
    const val CHANGE_TRACK = "change_track"
    const val QUEUE_ADD = "queue_add"
    const val QUEUE_REMOVE = "queue_remove"
    const val QUEUE_CLEAR = "queue_clear"
    const val SYNC_QUEUE = "sync_queue"
    const val SET_VOLUME = "set_volume"
}

enum class SyncConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

enum class SyncRoomRole {
    HOST,
    GUEST,
    NONE
}

data class SyncTrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long = 0L,
    val thumbnail: String? = null,
    val suggestedBy: String? = null
)

data class SyncUserInfo(
    val userId: String,
    val username: String,
    val isHost: Boolean,
    val isConnected: Boolean = true
)

data class SyncRoomState(
    val roomCode: String,
    val hostId: String,
    val users: List<SyncUserInfo> = emptyList(),
    val currentTrack: SyncTrackInfo? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val lastUpdate: Long = 0L,
    val volume: Float = 1.0f,
    val queue: List<SyncTrackInfo> = emptyList(),
    val allowParticipantControl: Boolean = false
)

data class SyncActionPayload(
    val action: String,
    val trackId: String? = null,
    val position: Long? = null,
    val trackInfo: SyncTrackInfo? = null,
    val insertNext: Boolean? = null,
    val queue: List<SyncTrackInfo>? = null,
    val queueTitle: String? = null,
    val volume: Float? = null,
    val serverTime: Long? = null
)

data class SyncChatMessage(
    val userId: String,
    val username: String,
    val message: String,
    val timestamp: Long,
    val replyTo: SyncRepliedMessage? = null
)

data class SyncRepliedMessage(
    val username: String,
    val message: String
)

data class SyncJoinRequest(
    val userId: String,
    val username: String
)

data class SyncSuggestion(
    val suggestionId: String,
    val fromUserId: String,
    val fromUsername: String,
    val trackInfo: SyncTrackInfo
)

data class SyncLogEntry(
    val timestamp: String,
    val level: String, // "INFO", "WARN", "ERROR", "DEBUG"
    val message: String,
    val details: String? = null
)

sealed class SyncEvent {
    data class Connected(val userId: String) : SyncEvent()
    data object Disconnected : SyncEvent()
    data class ConnectionError(val error: String) : SyncEvent()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : SyncEvent()
    data class RoomCreated(val roomCode: String, val userId: String) : SyncEvent()
    data class JoinRequestReceived(val request: SyncJoinRequest) : SyncEvent()
    data class JoinApproved(val roomCode: String, val userId: String, val state: SyncRoomState) : SyncEvent()
    data class JoinRejected(val reason: String) : SyncEvent()
    data class UserJoined(val userId: String, val username: String) : SyncEvent()
    data class UserLeft(val userId: String, val username: String) : SyncEvent()
    data class HostChanged(val newHostId: String, val newHostName: String) : SyncEvent()
    data class Kicked(val reason: String) : SyncEvent()
    data class Reconnected(val roomCode: String, val userId: String, val state: SyncRoomState, val isHost: Boolean) : SyncEvent()
    data class PlaybackSync(val action: SyncActionPayload) : SyncEvent()
    data class BufferWait(val trackId: String, val waitingFor: List<String>) : SyncEvent()
    data class BufferComplete(val trackId: String) : SyncEvent()
    data class ChatMessageReceived(val message: SyncChatMessage) : SyncEvent()
    data class SuggestionReceived(val suggestion: SyncSuggestion) : SyncEvent()
    data class SuggestionApproved(val suggestionId: String, val trackInfo: SyncTrackInfo) : SyncEvent()
    data class SuggestionRejected(val suggestionId: String, val reason: String?) : SyncEvent()
    data class RoomSettingsChanged(val allowParticipantControl: Boolean) : SyncEvent()
}
