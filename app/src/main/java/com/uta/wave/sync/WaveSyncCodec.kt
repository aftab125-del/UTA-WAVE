package com.uta.wave.sync

import android.util.Base64
import com.uta.wave.sync.model.SyncActionPayload
import com.uta.wave.sync.model.SyncChatMessage
import com.uta.wave.sync.model.SyncJoinRequest
import com.uta.wave.sync.model.SyncMessageTypes
import com.uta.wave.sync.model.SyncRepliedMessage
import com.uta.wave.sync.model.SyncRoomState
import com.uta.wave.sync.model.SyncSuggestion
import com.uta.wave.sync.model.SyncTrackInfo
import com.uta.wave.sync.model.SyncUserInfo
import org.json.JSONArray
import org.json.JSONObject

object WaveSyncCodec {

    // --- ENCODERS ---

    fun encodeMessage(type: String, payload: JSONObject? = null): String {
        val root = JSONObject()
        root.put("type", type)
        if (payload != null) {
            root.put("payload", payload)
        }
        return root.toString()
    }

    fun encodeCreateRoom(username: String): String {
        val payload = JSONObject().apply {
            put("username", username)
        }
        return encodeMessage(SyncMessageTypes.CREATE_ROOM, payload)
    }

    fun encodeJoinRoom(roomCode: String, username: String): String {
        val payload = JSONObject().apply {
            put("room_code", roomCode.uppercase())
            put("username", username)
        }
        return encodeMessage(SyncMessageTypes.JOIN_ROOM, payload)
    }

    fun encodeLeaveRoom(): String {
        return encodeMessage(SyncMessageTypes.LEAVE_ROOM, null)
    }

    fun encodeApproveJoin(userId: String): String {
        val payload = JSONObject().apply {
            put("user_id", userId)
        }
        return encodeMessage(SyncMessageTypes.APPROVE_JOIN, payload)
    }

    fun encodeRejectJoin(userId: String, reason: String? = null): String {
        val payload = JSONObject().apply {
            put("user_id", userId)
            if (reason != null) put("reason", reason)
        }
        return encodeMessage(SyncMessageTypes.REJECT_JOIN, payload)
    }

    fun encodePlaybackAction(actionPayload: SyncActionPayload): String {
        val payload = JSONObject().apply {
            put("action", actionPayload.action)
            actionPayload.trackId?.let { put("track_id", it) }
            actionPayload.position?.let { put("position", it) }
            actionPayload.trackInfo?.let { put("track_info", trackInfoToJson(it)) }
            actionPayload.insertNext?.let { put("insert_next", it) }
            actionPayload.queue?.let { list ->
                val arr = JSONArray()
                list.forEach { arr.put(trackInfoToJson(it)) }
                put("queue", arr)
            }
            actionPayload.queueTitle?.let { put("queue_title", it) }
            actionPayload.volume?.let { put("volume", it.toDouble()) }
            actionPayload.serverTime?.let { put("server_time", it) }
        }
        return encodeMessage(SyncMessageTypes.PLAYBACK_ACTION, payload)
    }

    fun encodeBufferReady(trackId: String): String {
        val payload = JSONObject().apply {
            put("track_id", trackId)
        }
        return encodeMessage(SyncMessageTypes.BUFFER_READY, payload)
    }

    fun encodeKickUser(userId: String, reason: String? = null): String {
        val payload = JSONObject().apply {
            put("user_id", userId)
            if (reason != null) put("reason", reason)
        }
        return encodeMessage(SyncMessageTypes.KICK_USER, payload)
    }

    fun encodeTransferHost(newHostId: String): String {
        val payload = JSONObject().apply {
            put("new_host_id", newHostId)
        }
        return encodeMessage(SyncMessageTypes.TRANSFER_HOST, payload)
    }

    fun encodePing(): String {
        return encodeMessage(SyncMessageTypes.PING, null)
    }

    fun encodeChat(message: String, replyTo: SyncRepliedMessage? = null): String {
        val finalMessage = if (replyTo != null) {
            val metadata = "${replyTo.username}|${replyTo.message}"
            val encoded = Base64.encodeToString(metadata.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            "\u200B[RPLY:$encoded]\u200B$message"
        } else {
            message
        }

        val payload = JSONObject().apply {
            put("message", finalMessage)
            if (replyTo != null) {
                val rObj = JSONObject().apply {
                    put("username", replyTo.username)
                    put("message", replyTo.message)
                }
                put("reply_to", rObj)
            }
        }
        return encodeMessage(SyncMessageTypes.CHAT, payload)
    }

    fun encodeRequestSync(): String {
        return encodeMessage(SyncMessageTypes.REQUEST_SYNC, null)
    }

    fun encodeReconnect(sessionToken: String): String {
        val payload = JSONObject().apply {
            put("session_token", sessionToken)
        }
        return encodeMessage(SyncMessageTypes.RECONNECT, payload)
    }

    fun encodeSuggestTrack(trackInfo: SyncTrackInfo): String {
        val payload = JSONObject().apply {
            put("track_info", trackInfoToJson(trackInfo))
        }
        return encodeMessage(SyncMessageTypes.SUGGEST_TRACK, payload)
    }

    fun encodeApproveSuggestion(suggestionId: String): String {
        val payload = JSONObject().apply {
            put("suggestion_id", suggestionId)
        }
        return encodeMessage(SyncMessageTypes.APPROVE_SUGGESTION, payload)
    }

    fun encodeRejectSuggestion(suggestionId: String, reason: String? = null): String {
        val payload = JSONObject().apply {
            put("suggestion_id", suggestionId)
            if (reason != null) put("reason", reason)
        }
        return encodeMessage(SyncMessageTypes.REJECT_SUGGESTION, payload)
    }

    fun encodeUpdateRoomSettings(allowParticipantControl: Boolean): String {
        val payload = JSONObject().apply {
            put("allow_participant_control", allowParticipantControl)
        }
        return encodeMessage(SyncMessageTypes.UPDATE_ROOM_SETTINGS, payload)
    }

    // --- DECODERS & PARSERS ---

    fun decodeMessage(raw: String): Pair<String, JSONObject?> {
        val root = JSONObject(raw)
        val type = root.optString("type", "")
        val payload = root.optJSONObject("payload")
        return Pair(type, payload)
    }

    fun trackInfoToJson(track: SyncTrackInfo): JSONObject {
        return JSONObject().apply {
            put("id", track.id)
            put("title", track.title)
            put("artist", track.artist)
            track.album?.let { put("album", it) }
            put("duration", track.duration)
            track.thumbnail?.let { put("thumbnail", it) }
            track.suggestedBy?.let { put("suggested_by", it) }
        }
    }

    fun parseTrackInfo(json: JSONObject?): SyncTrackInfo? {
        if (json == null) return null
        val id = json.optString("id", "")
        if (id.isEmpty()) return null
        return SyncTrackInfo(
            id = id,
            title = json.optString("title", "Unknown Title"),
            artist = json.optString("artist", "Unknown Artist"),
            album = if (json.has("album") && !json.isNull("album")) json.optString("album") else null,
            duration = json.optLong("duration", 0L),
            thumbnail = if (json.has("thumbnail") && !json.isNull("thumbnail")) json.optString("thumbnail") else null,
            suggestedBy = if (json.has("suggested_by") && !json.isNull("suggested_by")) json.optString("suggested_by") else null
        )
    }

    fun parseUserInfo(json: JSONObject): SyncUserInfo {
        return SyncUserInfo(
            userId = json.optString("user_id", ""),
            username = json.optString("username", "Guest"),
            isHost = json.optBoolean("is_host", false),
            isConnected = json.optBoolean("is_connected", true)
        )
    }

    fun parseRoomState(json: JSONObject): SyncRoomState {
        val usersArr = json.optJSONArray("users")
        val userList = mutableListOf<SyncUserInfo>()
        if (usersArr != null) {
            for (i in 0 until usersArr.length()) {
                usersArr.optJSONObject(i)?.let { userList.add(parseUserInfo(it)) }
            }
        }

        val queueArr = json.optJSONArray("queue")
        val queueList = mutableListOf<SyncTrackInfo>()
        if (queueArr != null) {
            for (i in 0 until queueArr.length()) {
                queueArr.optJSONObject(i)?.let { parseTrackInfo(it)?.let { t -> queueList.add(t) } }
            }
        }

        return SyncRoomState(
            roomCode = json.optString("room_code", ""),
            hostId = json.optString("host_id", ""),
            users = userList,
            currentTrack = parseTrackInfo(json.optJSONObject("current_track")),
            isPlaying = json.optBoolean("is_playing", false),
            position = json.optLong("position", 0L),
            lastUpdate = json.optLong("last_update", 0L),
            volume = json.optDouble("volume", 1.0).toFloat(),
            queue = queueList,
            allowParticipantControl = json.optBoolean("allow_participant_control", false)
        )
    }

    fun parseActionPayload(json: JSONObject): SyncActionPayload {
        val queueArr = json.optJSONArray("queue")
        val queueList = if (queueArr != null) {
            val list = mutableListOf<SyncTrackInfo>()
            for (i in 0 until queueArr.length()) {
                queueArr.optJSONObject(i)?.let { parseTrackInfo(it)?.let { t -> list.add(t) } }
            }
            list
        } else null

        return SyncActionPayload(
            action = json.optString("action", ""),
            trackId = if (json.has("track_id")) json.optString("track_id") else null,
            position = if (json.has("position")) json.optLong("position") else null,
            trackInfo = parseTrackInfo(json.optJSONObject("track_info")),
            insertNext = if (json.has("insert_next")) json.optBoolean("insert_next") else null,
            queue = queueList,
            queueTitle = if (json.has("queue_title")) json.optString("queue_title") else null,
            volume = if (json.has("volume")) json.optDouble("volume").toFloat() else null,
            serverTime = if (json.has("server_time")) json.optLong("server_time") else null
        )
    }

    fun parseChatMessage(json: JSONObject): SyncChatMessage {
        var message = json.optString("message", "")
        var replyTo: SyncRepliedMessage? = null

        val replyObj = json.optJSONObject("reply_to")
        if (replyObj != null) {
            replyTo = SyncRepliedMessage(
                username = replyObj.optString("username", ""),
                message = replyObj.optString("message", "")
            )
        } else if (message.startsWith("\u200B[RPLY:")) {
            try {
                val endIdx = message.indexOf("]\u200B")
                if (endIdx != -1) {
                    val encoded = message.substring(7, endIdx)
                    val decoded = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
                    val parts = decoded.split("|", limit = 2)
                    if (parts.size == 2) {
                        replyTo = SyncRepliedMessage(parts[0], parts[1])
                        message = message.substring(endIdx + 2)
                    }
                }
            } catch (_: Exception) {}
        }

        return SyncChatMessage(
            userId = json.optString("user_id", ""),
            username = json.optString("username", "Anonymous"),
            message = message,
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            replyTo = replyTo
        )
    }

    fun parseJoinRequest(json: JSONObject): SyncJoinRequest {
        return SyncJoinRequest(
            userId = json.optString("user_id", ""),
            username = json.optString("username", "Guest")
        )
    }

    fun parseSuggestion(json: JSONObject): SyncSuggestion? {
        val track = parseTrackInfo(json.optJSONObject("track_info")) ?: return null
        return SyncSuggestion(
            suggestionId = json.optString("suggestion_id", ""),
            fromUserId = json.optString("from_user_id", ""),
            fromUsername = json.optString("from_username", "Guest"),
            trackInfo = track
        )
    }
}
