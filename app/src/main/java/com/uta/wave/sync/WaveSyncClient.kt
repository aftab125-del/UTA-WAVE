package com.uta.wave.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.uta.wave.R
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.sync.model.SyncActionPayload
import com.uta.wave.sync.model.SyncConnectionState
import com.uta.wave.sync.model.SyncEvent
import com.uta.wave.sync.model.SyncJoinRequest
import com.uta.wave.sync.model.SyncLogEntry
import com.uta.wave.sync.model.SyncMessageTypes
import com.uta.wave.sync.model.SyncPlaybackActions
import com.uta.wave.sync.model.SyncRepliedMessage
import com.uta.wave.sync.model.SyncRoomRole
import com.uta.wave.sync.model.SyncRoomState
import com.uta.wave.sync.model.SyncSuggestion
import com.uta.wave.sync.model.SyncTrackInfo
import com.uta.wave.sync.model.SyncUserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class PendingAction {
    data class CreateRoom(val username: String) : PendingAction()
    data class JoinRoom(val roomCode: String, val username: String) : PendingAction()
}

class WaveSyncClient private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WaveSyncClient"
        private const val MAX_RECONNECT_ATTEMPTS = 15
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 120000L
        private const val PING_INTERVAL_MS = 25000L
        private const val MAX_LOG_ENTRIES = 500
        private const val SESSION_GRACE_PERIOD_MS = 10 * 60 * 1000L

        const val NOTIFICATION_CHANNEL_ID = "uta_wave_listen_together"
        const val ACTION_APPROVE_JOIN = "com.uta.wave.sync.ACTION_APPROVE_JOIN"
        const val ACTION_REJECT_JOIN = "com.uta.wave.sync.ACTION_REJECT_JOIN"
        const val ACTION_APPROVE_SUGGESTION = "com.uta.wave.sync.ACTION_APPROVE_SUGGESTION"
        const val ACTION_REJECT_SUGGESTION = "com.uta.wave.sync.ACTION_REJECT_SUGGESTION"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_SUGGESTION_ID = "extra_suggestion_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        @Volatile
        private var INSTANCE: WaveSyncClient? = null

        fun getInstance(context: Context): WaveSyncClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WaveSyncClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val userPrefs by lazy { UserPreferences.getInstance(context) }

    // --- StateFlows ---
    private val _connectionState = MutableStateFlow(SyncConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    private val _roomState = MutableStateFlow<SyncRoomState?>(null)
    val roomState: StateFlow<SyncRoomState?> = _roomState.asStateFlow()

    private val _role = MutableStateFlow(SyncRoomRole.NONE)
    val role: StateFlow<SyncRoomRole> = _role.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _pendingJoinRequests = MutableStateFlow<List<SyncJoinRequest>>(emptyList())
    val pendingJoinRequests: StateFlow<List<SyncJoinRequest>> = _pendingJoinRequests.asStateFlow()

    private val _bufferingUsers = MutableStateFlow<List<String>>(emptyList())
    val bufferingUsers: StateFlow<List<String>> = _bufferingUsers.asStateFlow()

    private val _pendingSuggestions = MutableStateFlow<List<SyncSuggestion>>(emptyList())
    val pendingSuggestions: StateFlow<List<SyncSuggestion>> = _pendingSuggestions.asStateFlow()

    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    private val _logs = MutableStateFlow<List<SyncLogEntry>>(emptyList())
    val logs: StateFlow<List<SyncLogEntry>> = _logs.asStateFlow()

    private val _events = MutableSharedFlow<SyncEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    private val _rtt = MutableStateFlow(0L)
    val rtt: StateFlow<Long> = _rtt.asStateFlow()

    // --- Internal State ---
    @Volatile private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var pingSentTime: Long = 0L
    private var reconnectAttempts = 0

    @Volatile private var sessionToken: String? = null
    @Volatile private var storedUsername: String? = null
    @Volatile private var storedRoomCode: String? = null
    private var wasHost: Boolean = false
    private var sessionStartTime: Long = 0L

    @Volatile private var pendingAction: PendingAction? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val joinRequestNotifications = ConcurrentHashMap<String, Int>()
    private val suggestionNotifications = ConcurrentHashMap<String, Int>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    init {
        ensureNotificationChannel()
        loadPersistedSession()
        loadBlockedUsers()
    }

    private fun ensureNotificationChannel() {
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm != null && nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.listen_together_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.listen_together_notification_channel_desc)
                }
                nm.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            log("WARN", "Failed to create notification channel", e.message)
        }
    }

    private fun loadPersistedSession() {
        try {
            val settings = userPrefs.settingsFlow.value
            val token = settings.listenTogetherSessionToken
            val roomCode = settings.listenTogetherRoomCode
            val timestamp = settings.listenTogetherSessionTimestamp

            if (token.isNotEmpty() && roomCode.isNotEmpty() &&
                (System.currentTimeMillis() - timestamp < SESSION_GRACE_PERIOD_MS)
            ) {
                sessionToken = token
                storedRoomCode = roomCode
                sessionStartTime = timestamp
                log("INFO", "Loaded persisted session", "Room: $roomCode")
            } else if (token.isNotEmpty()) {
                log("WARN", "Session expired, clearing", "Age: ${System.currentTimeMillis() - timestamp}ms")
                clearPersistedSession()
            }
        } catch (e: Exception) {
            log("ERROR", "Failed to load persisted session", e.message)
        }
    }

    private fun loadBlockedUsers() {
        try {
            val blockedRaw = userPrefs.settingsFlow.value.listenTogetherBlockedUsers
            if (blockedRaw.isNotEmpty()) {
                val arr = JSONArray(blockedRaw)
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                _blockedUsers.value = set
            }
        } catch (e: Exception) {
            log("ERROR", "Failed to load blocked users", e.message)
        }
    }

    private fun saveBlockedUsers() {
        try {
            val arr = JSONArray()
            _blockedUsers.value.forEach { arr.put(it) }
            userPrefs.updateListenTogetherBlockedUsers(arr.toString())
        } catch (e: Exception) {
            log("ERROR", "Failed to save blocked users", e.message)
        }
    }

    private fun savePersistedSession() {
        val token = sessionToken ?: return
        val roomCode = storedRoomCode ?: return
        userPrefs.saveListenTogetherSession(token, roomCode)
    }

    private fun clearPersistedSession() {
        userPrefs.clearListenTogetherSession()
        sessionToken = null
        storedRoomCode = null
    }

    private fun log(level: String, message: String, details: String? = null) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val entry = SyncLogEntry(timestamp, level, message, details)
        _logs.value = (_logs.value + entry).takeLast(MAX_LOG_ENTRIES)

        when (level) {
            "ERROR" -> Log.e(TAG, "$message ${details ?: ""}")
            "WARN" -> Log.w(TAG, "$message ${details ?: ""}")
            "DEBUG" -> Log.d(TAG, "$message ${details ?: ""}")
            else -> Log.i(TAG, "$message ${details ?: ""}")
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun getServerUrl(): String {
        val configured = userPrefs.settingsFlow.value.listenTogetherServerUrl
        return configured.ifEmpty { WaveSyncServers.defaultServerUrl }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = INITIAL_RECONNECT_DELAY_MS * (1 shl (minOf(attempt - 1, 4)))
        val cappedDelay = minOf(exponentialDelay, MAX_RECONNECT_DELAY_MS)
        val jitter = (cappedDelay * 0.2 * Math.random()).toLong()
        return cappedDelay + jitter
    }

    @Synchronized
    fun connect(serverUrl: String? = null) {
        if (_connectionState.value == SyncConnectionState.CONNECTED ||
            _connectionState.value == SyncConnectionState.CONNECTING
        ) {
            log("WARN", "Already connected or connecting")
            return
        }

        _connectionState.value = SyncConnectionState.CONNECTING
        val targetUrl = serverUrl ?: getServerUrl()
        log("INFO", "Connecting to sync relay", targetUrl)

        val request = Request.Builder().url(targetUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                log("INFO", "WebSocket connected successfully")
                this@WaveSyncClient.webSocket = webSocket
                _connectionState.value = SyncConnectionState.CONNECTED
                reconnectAttempts = 0
                startPingJob()

                if (sessionToken != null && storedRoomCode != null) {
                    log("INFO", "Attempting session resume with token", "Room: $storedRoomCode")
                    sendRawMessage(WaveSyncCodec.encodeReconnect(sessionToken!!))
                } else {
                    executePendingAction()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleIncomingMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                log("INFO", "Server requested connection close", "code=$code reason=$reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                log("INFO", "WebSocket closed", "code=$code reason=$reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                log("ERROR", "WebSocket connection failure", t.message)
                handleConnectionFailure(t)
            }
        })
    }

    private fun executePendingAction() {
        val action = pendingAction ?: return
        pendingAction = null
        when (action) {
            is PendingAction.CreateRoom -> {
                log("INFO", "Executing queued room creation", action.username)
                sendRawMessage(WaveSyncCodec.encodeCreateRoom(action.username))
            }
            is PendingAction.JoinRoom -> {
                log("INFO", "Executing queued room join", "${action.roomCode} as ${action.username}")
                sendRawMessage(WaveSyncCodec.encodeJoinRoom(action.roomCode, action.username))
            }
        }
    }

    fun disconnect() {
        log("INFO", "Disconnecting from sync session")
        releaseWakeLock()
        pingJob?.cancel()
        pingJob = null
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        webSocket = null
        _connectionState.value = SyncConnectionState.DISCONNECTED

        sessionToken = null
        storedRoomCode = null
        storedUsername = null
        pendingAction = null
        _roomState.value = null
        _role.value = SyncRoomRole.NONE
        _userId.value = null
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()

        clearPersistedSession()
        reconnectAttempts = 0

        scope.launch { _events.emit(SyncEvent.Disconnected) }
    }

    private fun startPingJob() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (true) {
                delay(PING_INTERVAL_MS)
                pingSentTime = System.currentTimeMillis()
                sendRawMessage(WaveSyncCodec.encodePing())
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = context.getSystemService<PowerManager>()
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "utawave:SyncWakeLock")
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(10 * 60 * 1000L)
            log("DEBUG", "WakeLock acquired for background sync")
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            log("DEBUG", "WakeLock released")
        }
    }

    private fun handleDisconnect() {
        pingJob?.cancel()
        pingJob = null
        _connectionState.value = SyncConnectionState.DISCONNECTED
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()

        if (sessionToken != null && _roomState.value != null) {
            log("INFO", "Connection lost, scheduling automatic reconnect")
            handleConnectionFailure(Exception("Connection lost"))
        } else {
            scope.launch { _events.emit(SyncEvent.Disconnected) }
        }
    }

    private fun handleConnectionFailure(t: Throwable) {
        pingJob?.cancel()
        pingJob = null

        val shouldReconnect = sessionToken != null || _roomState.value != null || pendingAction != null

        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && shouldReconnect) {
            reconnectAttempts++
            _connectionState.value = SyncConnectionState.RECONNECTING
            val delayMs = calculateBackoffDelay(reconnectAttempts)
            log("INFO", "Attempting reconnect $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS in ${delayMs / 1000}s", t.message)

            scope.launch {
                _events.emit(SyncEvent.Reconnecting(reconnectAttempts, MAX_RECONNECT_ATTEMPTS))
                delay(delayMs)
                if (_connectionState.value == SyncConnectionState.RECONNECTING ||
                    _connectionState.value == SyncConnectionState.DISCONNECTED
                ) {
                    connect()
                }
            }
        } else {
            _connectionState.value = SyncConnectionState.ERROR
            if (sessionToken != null) {
                log("ERROR", "Reconnection exhausted, session kept for manual retry")
                scope.launch {
                    _events.emit(SyncEvent.ConnectionError("Connection failed after $MAX_RECONNECT_ATTEMPTS attempts."))
                }
            } else {
                clearPersistedSession()
                _roomState.value = null
                _role.value = SyncRoomRole.NONE
                scope.launch {
                    _events.emit(SyncEvent.ConnectionError(t.message ?: "Sync connection failed"))
                }
            }
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val (msgType, payloadJson) = WaveSyncCodec.decodeMessage(text)
            if (msgType.isEmpty()) return

            when (msgType) {
                SyncMessageTypes.ROOM_CREATED -> {
                    if (payloadJson == null) return
                    val code = payloadJson.optString("room_code")
                    val uId = payloadJson.optString("user_id")
                    val token = payloadJson.optString("session_token")

                    _userId.value = uId
                    _role.value = SyncRoomRole.HOST
                    sessionToken = token
                    storedRoomCode = code
                    wasHost = true
                    sessionStartTime = System.currentTimeMillis()

                    _roomState.value = SyncRoomState(
                        roomCode = code,
                        hostId = uId,
                        users = listOf(SyncUserInfo(uId, storedUsername ?: "Host", true)),
                        isPlaying = false,
                        position = 0L,
                        lastUpdate = System.currentTimeMillis(),
                        volume = 1f
                    )

                    savePersistedSession()
                    acquireWakeLock()
                    log("INFO", "Room created successfully", "Code: $code")

                    scope.launch { _events.emit(SyncEvent.RoomCreated(code, uId)) }
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.room_created, code), Toast.LENGTH_LONG).show()
                    }
                }

                SyncMessageTypes.JOIN_REQUEST -> {
                    if (payloadJson == null) return
                    val req = WaveSyncCodec.parseJoinRequest(payloadJson)
                    if (isUserBlocked(req.username)) {
                        log("INFO", "Ignored join request from blocked user", req.username)
                        rejectJoin(req.userId, "Blocked")
                        return
                    }

                    _pendingJoinRequests.value += req
                    log("INFO", "Received join request from", req.username)

                    val autoApprove = userPrefs.settingsFlow.value.listenTogetherAutoApproval
                    if (_role.value == SyncRoomRole.HOST) {
                        if (autoApprove) {
                            log("INFO", "Auto-approving join request for", req.username)
                            approveJoin(req.userId)
                        } else {
                            showJoinNotification(req)
                        }
                    }
                    scope.launch { _events.emit(SyncEvent.JoinRequestReceived(req)) }
                }

                SyncMessageTypes.JOIN_APPROVED -> {
                    if (payloadJson == null) return
                    val code = payloadJson.optString("room_code")
                    val uId = payloadJson.optString("user_id")
                    val token = payloadJson.optString("session_token")
                    val stateJson = payloadJson.optJSONObject("state")
                    val parsedState = if (stateJson != null) WaveSyncCodec.parseRoomState(stateJson) else SyncRoomState(code, "")

                    _userId.value = uId
                    _role.value = SyncRoomRole.GUEST
                    sessionToken = token
                    storedRoomCode = code
                    wasHost = false
                    sessionStartTime = System.currentTimeMillis()
                    _roomState.value = parsedState

                    savePersistedSession()
                    acquireWakeLock()
                    log("INFO", "Join approved for room", code)

                    scope.launch { _events.emit(SyncEvent.JoinApproved(code, uId, parsedState)) }
                }

                SyncMessageTypes.JOIN_REJECTED -> {
                    val reason = payloadJson?.optString("reason", "Host rejected join request") ?: "Rejected"
                    log("WARN", "Join request rejected", reason)
                    scope.launch { _events.emit(SyncEvent.JoinRejected(reason)) }
                }

                SyncMessageTypes.USER_JOINED -> {
                    if (payloadJson == null) return
                    val uId = payloadJson.optString("user_id")
                    val uName = payloadJson.optString("username")

                    val current = _roomState.value
                    if (current != null) {
                        val updatedUsers = current.users + SyncUserInfo(uId, uName, false)
                        _roomState.value = current.copy(users = updatedUsers)
                    }
                    _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != uId }
                    joinRequestNotifications.remove(uId)?.let { NotificationManagerCompat.from(context).cancel(it) }

                    log("INFO", "User joined room", uName)
                    scope.launch { _events.emit(SyncEvent.UserJoined(uId, uName)) }
                }

                SyncMessageTypes.USER_LEFT -> {
                    if (payloadJson == null) return
                    val uId = payloadJson.optString("user_id")
                    val uName = payloadJson.optString("username")

                    val current = _roomState.value
                    if (current != null) {
                        _roomState.value = current.copy(users = current.users.filter { it.userId != uId })
                    }
                    log("INFO", "User left room", uName)
                    scope.launch { _events.emit(SyncEvent.UserLeft(uId, uName)) }
                }

                SyncMessageTypes.HOST_CHANGED -> {
                    if (payloadJson == null) return
                    val newHostId = payloadJson.optString("new_host_id")
                    val newHostName = payloadJson.optString("new_host_name")

                    val current = _roomState.value
                    if (current != null) {
                        val mapped = current.users.map { it.copy(isHost = it.userId == newHostId) }
                        _roomState.value = current.copy(hostId = newHostId, users = mapped)
                    }
                    _role.value = if (newHostId == _userId.value) SyncRoomRole.HOST else SyncRoomRole.GUEST

                    log("INFO", "Host changed to", newHostName)
                    scope.launch { _events.emit(SyncEvent.HostChanged(newHostId, newHostName)) }
                }

                SyncMessageTypes.KICKED -> {
                    val reason = payloadJson?.optString("reason", "Removed by host") ?: "Kicked"
                    log("WARN", "Kicked from room", reason)
                    releaseWakeLock()
                    sessionToken = null
                    _roomState.value = null
                    _role.value = SyncRoomRole.NONE
                    clearPersistedSession()
                    scope.launch { _events.emit(SyncEvent.Kicked(reason)) }
                }

                SyncMessageTypes.SYNC_PLAYBACK -> {
                    if (payloadJson == null) return
                    val actionPayload = WaveSyncCodec.parseActionPayload(payloadJson)
                    log("DEBUG", "Received playback sync action", actionPayload.action)

                    val cur = _roomState.value
                    if (cur != null) {
                        when (actionPayload.action) {
                            SyncPlaybackActions.PLAY -> {
                                _roomState.value = cur.copy(
                                    isPlaying = true,
                                    position = actionPayload.position ?: cur.position
                                )
                            }
                            SyncPlaybackActions.PAUSE -> {
                                _roomState.value = cur.copy(
                                    isPlaying = false,
                                    position = actionPayload.position ?: cur.position
                                )
                            }
                            SyncPlaybackActions.SEEK -> {
                                _roomState.value = cur.copy(
                                    position = actionPayload.position ?: cur.position
                                )
                            }
                            SyncPlaybackActions.CHANGE_TRACK -> {
                                _roomState.value = cur.copy(
                                    currentTrack = actionPayload.trackInfo,
                                    isPlaying = false,
                                    position = 0L
                                )
                            }
                            SyncPlaybackActions.QUEUE_ADD -> {
                                val track = actionPayload.trackInfo
                                if (track != null) {
                                    val q = cur.queue
                                    _roomState.value = cur.copy(
                                        queue = if (actionPayload.insertNext == true) listOf(track) + q else q + track
                                    )
                                }
                            }
                            SyncPlaybackActions.QUEUE_REMOVE -> {
                                val tId = actionPayload.trackId
                                if (!tId.isNullOrEmpty()) {
                                    _roomState.value = cur.copy(queue = cur.queue.filter { it.id != tId })
                                }
                            }
                            SyncPlaybackActions.QUEUE_CLEAR -> {
                                _roomState.value = cur.copy(queue = emptyList())
                            }
                            SyncPlaybackActions.SET_VOLUME -> {
                                actionPayload.volume?.let { v ->
                                    _roomState.value = cur.copy(volume = v.coerceIn(0f, 1f))
                                }
                            }
                        }
                    }

                    scope.launch { _events.emit(SyncEvent.PlaybackSync(actionPayload)) }
                }

                SyncMessageTypes.BUFFER_WAIT -> {
                    if (payloadJson == null) return
                    val tId = payloadJson.optString("track_id")
                    val arr = payloadJson.optJSONArray("waiting_for")
                    val list = mutableListOf<String>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) list.add(arr.getString(i))
                    }
                    _bufferingUsers.value = list
                    log("DEBUG", "Buffer wait active", "Users waiting: ${list.size}")
                    scope.launch { _events.emit(SyncEvent.BufferWait(tId, list)) }
                }

                SyncMessageTypes.BUFFER_COMPLETE -> {
                    val tId = payloadJson?.optString("track_id", "") ?: ""
                    _bufferingUsers.value = emptyList()
                    log("INFO", "All clients buffered; releasing playback", tId)
                    scope.launch { _events.emit(SyncEvent.BufferComplete(tId)) }
                }

                SyncMessageTypes.SUGGESTION_RECEIVED -> {
                    if (payloadJson == null) return
                    val suggestion = WaveSyncCodec.parseSuggestion(payloadJson) ?: return
                    if (_role.value == SyncRoomRole.HOST) {
                        if (isUserBlocked(suggestion.fromUsername)) return
                        _pendingSuggestions.value += suggestion
                        log("INFO", "Suggestion received", "${suggestion.fromUsername}: ${suggestion.trackInfo.title}")
                        showSuggestionNotification(suggestion)
                    }
                    scope.launch { _events.emit(SyncEvent.SuggestionReceived(suggestion)) }
                }

                SyncMessageTypes.SUGGESTION_APPROVED -> {
                    if (payloadJson == null) return
                    val sId = payloadJson.optString("suggestion_id")
                    val track = WaveSyncCodec.parseTrackInfo(payloadJson.optJSONObject("track_info"))
                    suggestionNotifications.remove(sId)?.let { NotificationManagerCompat.from(context).cancel(it) }
                    _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != sId }
                    if (track != null) {
                        scope.launch { _events.emit(SyncEvent.SuggestionApproved(sId, track)) }
                    }
                }

                SyncMessageTypes.SUGGESTION_REJECTED -> {
                    val sId = payloadJson?.optString("suggestion_id", "") ?: ""
                    val reason = if (payloadJson != null && payloadJson.has("reason") && !payloadJson.isNull("reason")) {
                        payloadJson.optString("reason")
                    } else null
                    suggestionNotifications.remove(sId)?.let { NotificationManagerCompat.from(context).cancel(it) }
                    _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != sId }
                    scope.launch { _events.emit(SyncEvent.SuggestionRejected(sId, reason)) }
                }

                SyncMessageTypes.CHAT -> {
                    if (payloadJson == null) return
                    val chatMsg = WaveSyncCodec.parseChatMessage(payloadJson)
                    log("INFO", "Chat message received from", chatMsg.username)
                    scope.launch { _events.emit(SyncEvent.ChatMessageReceived(chatMsg)) }
                }

                SyncMessageTypes.ROOM_SETTINGS_CHANGED -> {
                    val allow = payloadJson?.optBoolean("allow_participant_control", false) ?: false
                    _roomState.value = _roomState.value?.copy(allowParticipantControl = allow)
                    log("INFO", "Room settings updated", "allowParticipantControl=$allow")
                    scope.launch { _events.emit(SyncEvent.RoomSettingsChanged(allow)) }
                }

                SyncMessageTypes.RECONNECTED -> {
                    if (payloadJson == null) return
                    val code = payloadJson.optString("room_code")
                    val uId = payloadJson.optString("user_id")
                    val isHost = payloadJson.optBoolean("is_host", false)
                    val stJson = payloadJson.optJSONObject("state")
                    val state = if (stJson != null) WaveSyncCodec.parseRoomState(stJson) else SyncRoomState(code, "")

                    _userId.value = uId
                    _role.value = if (isHost) SyncRoomRole.HOST else SyncRoomRole.GUEST
                    _roomState.value = state
                    wasHost = isHost
                    sessionStartTime = System.currentTimeMillis()
                    savePersistedSession()
                    reconnectAttempts = 0
                    acquireWakeLock()

                    log("INFO", "Successfully reconnected to room", "$code (host=$isHost)")
                    scope.launch { _events.emit(SyncEvent.Reconnected(code, uId, state, isHost)) }
                }

                SyncMessageTypes.USER_RECONNECTED -> {
                    val uId = payloadJson?.optString("user_id") ?: return
                    val cur = _roomState.value
                    if (cur != null) {
                        _roomState.value = cur.copy(
                            users = cur.users.map { if (it.userId == uId) it.copy(isConnected = true) else it }
                        )
                    }
                }

                SyncMessageTypes.USER_DISCONNECTED -> {
                    val uId = payloadJson?.optString("user_id") ?: return
                    val cur = _roomState.value
                    if (cur != null) {
                        _roomState.value = cur.copy(
                            users = cur.users.map { if (it.userId == uId) it.copy(isConnected = false) else it }
                        )
                    }
                }

                SyncMessageTypes.PONG -> {
                    if (pingSentTime > 0L) {
                        val latency = System.currentTimeMillis() - pingSentTime
                        _rtt.value = latency
                        pingSentTime = 0L
                        log("DEBUG", "PONG received", "RTT: ${latency}ms")
                    }
                }

                SyncMessageTypes.ERROR -> {
                    val code = payloadJson?.optString("code", "") ?: ""
                    val msg = payloadJson?.optString("message", "Error") ?: "Error"
                    log("ERROR", "Relay error", "$code: $msg")

                    if (code == "session_not_found") {
                        if (storedRoomCode != null && storedUsername != null && !wasHost) {
                            log("WARN", "Session not found, attempting clean rejoin")
                            scope.launch {
                                delay(500)
                                joinRoom(storedRoomCode!!, storedUsername!!)
                            }
                        } else {
                            clearPersistedSession()
                        }
                    }
                    scope.launch { _events.emit(SyncEvent.ConnectionError("$code: $msg")) }
                }
            }
        } catch (e: Exception) {
            log("ERROR", "Error parsing incoming frame", e.message)
        }
    }

    private fun showJoinNotification(req: SyncJoinRequest) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        joinRequestNotifications[req.userId] = notifId

        val approveIntent = Intent(context, WaveSyncActionReceiver::class.java).apply {
            action = ACTION_APPROVE_JOIN
            putExtra(EXTRA_USER_ID, req.userId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val rejectIntent = Intent(context, WaveSyncActionReceiver::class.java).apply {
            action = ACTION_REJECT_JOIN
            putExtra(EXTRA_USER_ID, req.userId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }

        val approvePI = PendingIntent.getBroadcast(
            context, req.userId.hashCode(), approveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rejectPI = PendingIntent.getBroadcast(
            context, req.userId.hashCode().inv(), rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(context.getString(R.string.listen_together))
            .setContentText(context.getString(R.string.listen_together_join_request_notification, req.username))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.approve), approvePI)
            .addAction(0, context.getString(R.string.reject), rejectPI)

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    private fun showSuggestionNotification(s: SyncSuggestion) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        suggestionNotifications[s.suggestionId] = notifId

        val approveIntent = Intent(context, WaveSyncActionReceiver::class.java).apply {
            action = ACTION_APPROVE_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, s.suggestionId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val rejectIntent = Intent(context, WaveSyncActionReceiver::class.java).apply {
            action = ACTION_REJECT_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, s.suggestionId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }

        val approvePI = PendingIntent.getBroadcast(
            context, s.suggestionId.hashCode(), approveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rejectPI = PendingIntent.getBroadcast(
            context, s.suggestionId.hashCode().inv(), rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(context.getString(R.string.listen_together))
            .setContentText(context.getString(R.string.listen_together_suggestion_received, s.fromUsername, s.trackInfo.title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.approve), approvePI)
            .addAction(0, context.getString(R.string.reject), rejectPI)

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    private fun sendRawMessage(msg: String) {
        try {
            val ok = webSocket?.send(msg) ?: false
            if (!ok) log("WARN", "WebSocket send failed (socket offline)")
        } catch (e: Exception) {
            log("ERROR", "Exception sending message", e.message)
        }
    }

    // --- Exposed Protocol Actions ---

    fun createRoom(username: String) {
        clearPersistedSession()
        storedUsername = username

        if (_connectionState.value == SyncConnectionState.CONNECTED) {
            sendRawMessage(WaveSyncCodec.encodeCreateRoom(username))
        } else {
            pendingAction = PendingAction.CreateRoom(username)
            connect()
        }
    }

    fun joinRoom(roomCode: String, username: String) {
        clearPersistedSession()
        storedUsername = username

        if (_connectionState.value == SyncConnectionState.CONNECTED) {
            sendRawMessage(WaveSyncCodec.encodeJoinRoom(roomCode, username))
        } else {
            pendingAction = PendingAction.JoinRoom(roomCode, username)
            connect()
        }
    }

    fun leaveRoom() {
        sendRawMessage(WaveSyncCodec.encodeLeaveRoom())
        disconnect()
    }

    fun approveJoin(userId: String) {
        if (_role.value != SyncRoomRole.HOST) return
        sendRawMessage(WaveSyncCodec.encodeApproveJoin(userId))
        joinRequestNotifications.remove(userId)?.let { NotificationManagerCompat.from(context).cancel(it) }
    }

    fun rejectJoin(userId: String, reason: String? = null) {
        if (_role.value != SyncRoomRole.HOST) return
        sendRawMessage(WaveSyncCodec.encodeRejectJoin(userId, reason))
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
        joinRequestNotifications.remove(userId)?.let { NotificationManagerCompat.from(context).cancel(it) }
    }

    fun kickUser(userId: String, reason: String? = null) {
        if (_role.value != SyncRoomRole.HOST) return
        sendRawMessage(WaveSyncCodec.encodeKickUser(userId, reason))
    }

    fun transferHost(newHostId: String) {
        if (_role.value != SyncRoomRole.HOST) return
        sendRawMessage(WaveSyncCodec.encodeTransferHost(newHostId))
    }

    fun updateRoomSettings(allowParticipantControl: Boolean) {
        if (_role.value != SyncRoomRole.HOST) return
        sendRawMessage(WaveSyncCodec.encodeUpdateRoomSettings(allowParticipantControl))
        _roomState.value = _roomState.value?.copy(allowParticipantControl = allowParticipantControl)
    }

    fun sendPlaybackAction(
        action: String,
        trackId: String? = null,
        position: Long? = null,
        trackInfo: SyncTrackInfo? = null,
        insertNext: Boolean? = null,
        queue: List<SyncTrackInfo>? = null,
        queueTitle: String? = null,
        volume: Float? = null
    ) {
        val canControl = _role.value == SyncRoomRole.HOST ||
                (_role.value == SyncRoomRole.GUEST && _roomState.value?.allowParticipantControl == true)
        if (!canControl) {
            log("WARN", "Playback control denied (guest control locked)")
            return
        }

        val payload = SyncActionPayload(
            action = action,
            trackId = trackId,
            position = position,
            trackInfo = trackInfo,
            insertNext = insertNext,
            queue = queue,
            queueTitle = queueTitle,
            volume = volume,
            serverTime = System.currentTimeMillis()
        )
        sendRawMessage(WaveSyncCodec.encodePlaybackAction(payload))
    }

    fun sendBufferReady(trackId: String) {
        sendRawMessage(WaveSyncCodec.encodeBufferReady(trackId))
    }

    fun sendChatMessage(message: String, replyTo: SyncRepliedMessage? = null) {
        if (_roomState.value == null) return
        sendRawMessage(WaveSyncCodec.encodeChat(message, replyTo))
    }

    fun suggestTrack(trackInfo: SyncTrackInfo) {
        if (_roomState.value == null || _role.value == SyncRoomRole.HOST) return
        sendRawMessage(WaveSyncCodec.encodeSuggestTrack(trackInfo))
    }

    fun approveSuggestion(suggestionId: String) {
        if (_role.value != SyncRoomRole.HOST) return
        sendRawMessage(WaveSyncCodec.encodeApproveSuggestion(suggestionId))
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
        suggestionNotifications.remove(suggestionId)?.let { NotificationManagerCompat.from(context).cancel(it) }
    }

    fun rejectSuggestion(suggestionId: String, reason: String? = null) {
        if (_role.value != SyncRoomRole.HOST) return
        sendRawMessage(WaveSyncCodec.encodeRejectSuggestion(suggestionId, reason))
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
        suggestionNotifications.remove(suggestionId)?.let { NotificationManagerCompat.from(context).cancel(it) }
    }

    fun requestSync() {
        if (_roomState.value == null) return
        sendRawMessage(WaveSyncCodec.encodeRequestSync())
    }

    fun blockUser(username: String) {
        val updated = _blockedUsers.value.toMutableSet()
        updated.add(username)
        _blockedUsers.value = updated
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.username != username }
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.fromUsername != username }
        saveBlockedUsers()
        log("INFO", "Blocked user", username)
    }

    fun unblockUser(username: String) {
        val updated = _blockedUsers.value.toMutableSet()
        updated.remove(username)
        _blockedUsers.value = updated
        saveBlockedUsers()
        log("INFO", "Unblocked user", username)
    }

    fun isUserBlocked(username: String): Boolean = username in _blockedUsers.value

    fun forceReconnect() {
        reconnectAttempts = 0
        try {
            webSocket?.close(1000, "Force reconnect")
        } catch (_: Exception) {}
        webSocket = null
        _connectionState.value = SyncConnectionState.DISCONNECTED
        scope.launch {
            delay(500)
            connect()
        }
    }

    val isInRoom: Boolean
        get() = _roomState.value != null

    val isHost: Boolean
        get() = _role.value == SyncRoomRole.HOST
}
