package com.uta.wave.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.player.PlaybackManager
import com.uta.wave.sync.model.SyncActionPayload
import com.uta.wave.sync.model.SyncChatMessage
import com.uta.wave.sync.model.SyncEvent
import com.uta.wave.sync.model.SyncPlaybackActions
import com.uta.wave.sync.model.SyncRoomRole
import com.uta.wave.sync.model.SyncTrackInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

// --- Conversion Extensions ---

fun TrackItem.toSyncTrackInfo(suggestedBy: String? = null): SyncTrackInfo {
    val cleanId = this.videoId.ifEmpty { this.id.removePrefix("youtube:") }
    return SyncTrackInfo(
        id = cleanId,
        title = this.title,
        artist = this.artist,
        album = this.album,
        duration = this.durationSeconds.toLong(),
        thumbnail = this.artworkUrl,
        suggestedBy = suggestedBy
    )
}

fun SyncTrackInfo.toTrackItem(): TrackItem {
    val cleanVideoId = this.id.removePrefix("youtube:")
    return TrackItem(
        id = "youtube:$cleanVideoId",
        videoId = cleanVideoId,
        title = this.title,
        artist = this.artist,
        album = this.album,
        artworkUrl = this.thumbnail ?: "https://i.ytimg.com/vi/$cleanVideoId/hqdefault.jpg",
        durationSeconds = this.duration.toInt()
    )
}

class WaveSyncManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WaveSyncManager"
        private const val POSITION_TOLERANCE_MS = 2000L
        private const val PLAYBACK_POSITION_TOLERANCE_MS = 3000L
        private const val HEARTBEAT_INTERVAL_MS = 5000L
        private const val DEBOUNCE_THRESHOLD_MS = 800L

        @Volatile
        private var INSTANCE: WaveSyncManager? = null

        fun getInstance(context: Context): WaveSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WaveSyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val client = WaveSyncClient.getInstance(context)
    val playbackManager by lazy { PlaybackManager.getInstance(context) }
    private val userPrefs by lazy { UserPreferences.getInstance(context) }

    // --- State Delegation from WaveSyncClient ---
    val connectionState = client.connectionState
    val roomState = client.roomState
    val role = client.role
    val userId = client.userId
    val pendingJoinRequests = client.pendingJoinRequests
    val bufferingUsers = client.bufferingUsers
    val pendingSuggestions = client.pendingSuggestions
    val blockedUsers = client.blockedUsers
    val logs = client.logs
    val rtt = client.rtt

    val isInRoom: Boolean get() = client.isInRoom
    val isHost: Boolean get() = client.isHost
    val allowParticipantControl: Boolean get() = roomState.value?.allowParticipantControl == true
    val canControlMusic: Boolean get() = !isInRoom || isHost || allowParticipantControl
    val isGuestPlaybackRestricted: Boolean get() = isInRoom && !canControlMusic

    // --- Chat State ---
    private val _chatMessages = MutableStateFlow<List<SyncChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<SyncChatMessage>> = _chatMessages.asStateFlow()

    // --- Lockstep & Synchronization Engine Variables ---
    @Volatile private var isSyncing = false
    private var lastSyncActionTime = 0L
    private var lastSyncedTrackId: String? = null
    private var bufferingTrackId: String? = null
    private var pendingSyncState: SyncActionPayload? = null
    private var bufferCompleteReceivedForTrack: String? = null
    private var heartbeatJob: Job? = null
    private var currentTrackGeneration = 0

    init {
        playbackManager.waveSyncManager = this
        observeClientEvents()
        observeRoleChanges()
    }

    private fun observeClientEvents() {
        scope.launch {
            client.events.collect { event ->
                try {
                    handleSyncEvent(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling sync event: $event", e)
                }
            }
        }
    }

    private fun observeRoleChanges() {
        scope.launch {
            role.collect { currentRole ->
                if (currentRole == SyncRoomRole.HOST) {
                    startHeartbeat()
                } else {
                    stopHeartbeat()
                }
            }
        }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (isHost && isInRoom && playbackManager.player.isPlaying) {
                    val pos = playbackManager.player.currentPosition
                    client.sendPlaybackAction(SyncPlaybackActions.PLAY, position = pos)
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun handleSyncEvent(event: SyncEvent) {
        when (event) {
            is SyncEvent.RoomCreated -> {
                Log.i(TAG, "Host created room: ${event.roomCode}")
                val cur = playbackManager.state.value.currentTrack
                if (cur != null) {
                    lastSyncedTrackId = cur.videoId
                    val q = playbackManager.state.value.queue
                    client.sendPlaybackAction(
                        action = SyncPlaybackActions.CHANGE_TRACK,
                        trackId = cur.videoId,
                        trackInfo = cur.toSyncTrackInfo(),
                        queue = q.map { it.toSyncTrackInfo() }
                    )
                    if (playbackManager.player.isPlaying) {
                        client.sendPlaybackAction(SyncPlaybackActions.PLAY, position = playbackManager.player.currentPosition)
                    }
                }
                startHeartbeat()
            }

            is SyncEvent.JoinApproved -> {
                Log.i(TAG, "Guest joined room: ${event.roomCode}")
                applyInitialRoomState(event.state)
            }

            is SyncEvent.Reconnected -> {
                Log.i(TAG, "Reconnected to room: ${event.roomCode} (isHost=${event.isHost})")
                if (event.isHost) {
                    val cur = playbackManager.state.value.currentTrack
                    if (cur != null) {
                        client.sendPlaybackAction(
                            action = SyncPlaybackActions.CHANGE_TRACK,
                            trackId = cur.videoId,
                            trackInfo = cur.toSyncTrackInfo(),
                            queue = playbackManager.state.value.queue.map { it.toSyncTrackInfo() }
                        )
                    }
                    startHeartbeat()
                } else {
                    applyInitialRoomState(event.state)
                }
            }

            is SyncEvent.PlaybackSync -> {
                handleRemotePlaybackSync(event.action)
            }

            is SyncEvent.BufferWait -> {
                Log.d(TAG, "Buffer wait: ${event.waitingFor.size} participants buffering ${event.trackId}")
            }

            is SyncEvent.BufferComplete -> {
                Log.i(TAG, "Buffer complete for track: ${event.trackId}")
                if (!isHost && bufferingTrackId == event.trackId) {
                    bufferCompleteReceivedForTrack = event.trackId
                    applyPendingSyncIfReady()
                }
            }

            is SyncEvent.ChatMessageReceived -> {
                _chatMessages.value = _chatMessages.value + event.message
            }

            is SyncEvent.Kicked, is SyncEvent.Disconnected -> {
                cleanupSession()
            }

            else -> {}
        }
    }

    private fun applyInitialRoomState(state: com.uta.wave.sync.model.SyncRoomState) {
        val track = state.currentTrack
        if (track != null) {
            val trackItem = track.toTrackItem()
            val queueItems = state.queue.map { it.toTrackItem() }.ifEmpty { listOf(trackItem) }
            val now = System.currentTimeMillis()
            val adjustedPos = if (state.isPlaying && state.lastUpdate > 0) {
                state.position + max(0L, now - state.lastUpdate)
            } else {
                state.position
            }

            bufferingTrackId = track.id
            pendingSyncState = SyncActionPayload(
                action = if (state.isPlaying) SyncPlaybackActions.PLAY else SyncPlaybackActions.PAUSE,
                trackId = track.id,
                position = adjustedPos,
                trackInfo = track
            )

            isSyncing = true
            playbackManager.applyRemoteTrackChange(
                track = trackItem,
                queue = queueItems,
                positionMs = adjustedPos,
                shouldPlay = false,
                onPrepared = {
                    client.sendBufferReady(track.id)
                    applyPendingSyncIfReady()
                }
            )
        }
    }

    private fun handleRemotePlaybackSync(action: SyncActionPayload) {
        if (isHost && action.action != SyncPlaybackActions.PLAY && action.action != SyncPlaybackActions.PAUSE) {
            // Host only accepts actions from guests if participant control is active
            if (!allowParticipantControl) return
        }

        isSyncing = true
        val now = System.currentTimeMillis()

        try {
            when (action.action) {
                SyncPlaybackActions.PLAY -> {
                    val basePos = action.position ?: 0L
                    val transitLatency = action.serverTime?.let { max(0L, now - it) } ?: 0L
                    val targetPos = basePos + transitLatency
                    val currentPos = playbackManager.player.currentPosition
                    val drift = abs(currentPos - targetPos)

                    if (bufferingTrackId != null) {
                        pendingSyncState = action.copy(position = targetPos)
                        applyPendingSyncIfReady()
                        return
                    }

                    if (playbackManager.player.isPlaying) {
                        if (drift > PLAYBACK_POSITION_TOLERANCE_MS) {
                            Log.d(TAG, "Guest PLAY: drift ${drift}ms > threshold, seeking to $targetPos")
                            playbackManager.applyRemoteSeek(targetPos)
                        }
                    } else {
                        if (drift > POSITION_TOLERANCE_MS) {
                            playbackManager.applyRemoteSeek(targetPos)
                        }
                        playbackManager.applyRemotePlay(targetPos)
                    }
                    lastSyncActionTime = now
                }

                SyncPlaybackActions.PAUSE -> {
                    val targetPos = action.position ?: playbackManager.player.currentPosition
                    val drift = abs(playbackManager.player.currentPosition - targetPos)

                    if (bufferingTrackId != null) {
                        pendingSyncState = action
                        applyPendingSyncIfReady()
                        return
                    }

                    if (playbackManager.player.isPlaying) {
                        playbackManager.applyRemotePause(targetPos)
                    }
                    if (drift > POSITION_TOLERANCE_MS) {
                        playbackManager.applyRemoteSeek(targetPos)
                    }
                    lastSyncActionTime = now
                }

                SyncPlaybackActions.SEEK -> {
                    val targetPos = action.position ?: return
                    val drift = abs(playbackManager.player.currentPosition - targetPos)
                    if (now - lastSyncActionTime < DEBOUNCE_THRESHOLD_MS) return

                    if (drift > POSITION_TOLERANCE_MS) {
                        playbackManager.applyRemoteSeek(targetPos)
                        lastSyncActionTime = now
                    }
                }

                SyncPlaybackActions.CHANGE_TRACK -> {
                    val trackInfo = action.trackInfo ?: return
                    val trackItem = trackInfo.toTrackItem()
                    val queueItems = action.queue?.map { it.toTrackItem() } ?: listOf(trackItem)

                    bufferingTrackId = trackInfo.id
                    bufferCompleteReceivedForTrack = null
                    pendingSyncState = SyncActionPayload(
                        action = SyncPlaybackActions.PLAY,
                        trackId = trackInfo.id,
                        position = action.position ?: 0L,
                        trackInfo = trackInfo
                    )

                    playbackManager.applyRemoteTrackChange(
                        track = trackItem,
                        queue = queueItems,
                        positionMs = action.position ?: 0L,
                        shouldPlay = false,
                        onPrepared = {
                            client.sendBufferReady(trackInfo.id)
                            applyPendingSyncIfReady()
                        }
                    )
                }

                SyncPlaybackActions.SKIP_NEXT -> {
                    playbackManager.next()
                }

                SyncPlaybackActions.SKIP_PREV -> {
                    playbackManager.previous()
                }

                SyncPlaybackActions.QUEUE_ADD -> {
                    action.trackInfo?.let {
                        playbackManager.applyRemoteQueueAdd(it.toTrackItem(), action.insertNext == true)
                    }
                }

                SyncPlaybackActions.QUEUE_REMOVE -> {
                    action.trackId?.let {
                        playbackManager.applyRemoteQueueRemove(it)
                    }
                }

                SyncPlaybackActions.QUEUE_CLEAR -> {
                    playbackManager.applyRemoteQueueClear()
                }

                SyncPlaybackActions.SET_VOLUME -> {
                    if (userPrefs.settingsFlow.value.listenTogetherSyncVolume && !isHost) {
                        action.volume?.let { playbackManager.applyRemoteVolume(it) }
                    }
                }
            }
        } finally {
            scope.launch {
                delay(300)
                isSyncing = false
            }
        }
    }

    private fun applyPendingSyncIfReady() {
        val pending = pendingSyncState ?: return
        val currentTrack = bufferingTrackId ?: return

        // If buffer complete was received or host bypassed
        if (bufferCompleteReceivedForTrack == currentTrack || isHost) {
            val targetPos = pending.position ?: 0L
            playbackManager.applyRemoteSeek(targetPos)
            if (pending.action == SyncPlaybackActions.PLAY) {
                playbackManager.applyRemotePlay(targetPos)
            } else {
                playbackManager.applyRemotePause(targetPos)
            }
            bufferingTrackId = null
            pendingSyncState = null
            bufferCompleteReceivedForTrack = null
        }
    }

    private fun cleanupSession() {
        stopHeartbeat()
        isSyncing = false
        bufferingTrackId = null
        pendingSyncState = null
        bufferCompleteReceivedForTrack = null
        _chatMessages.value = emptyList()
    }

    // --- Hooks from PlaybackManager ---

    fun onLocalPlay(positionMs: Long) {
        if (isSyncing || !isInRoom || !canControlMusic) return
        client.sendPlaybackAction(SyncPlaybackActions.PLAY, position = positionMs)
    }

    fun onLocalPause(positionMs: Long) {
        if (isSyncing || !isInRoom || !canControlMusic) return
        client.sendPlaybackAction(SyncPlaybackActions.PAUSE, position = positionMs)
    }

    fun onLocalSeek(positionMs: Long) {
        if (isSyncing || !isInRoom || !canControlMusic) return
        client.sendPlaybackAction(SyncPlaybackActions.SEEK, position = positionMs)
    }

    fun onLocalTrackChange(track: TrackItem, queue: List<TrackItem>) {
        if (isSyncing || !isInRoom || !canControlMusic) return
        if (track.videoId == lastSyncedTrackId) return
        lastSyncedTrackId = track.videoId

        client.sendPlaybackAction(
            action = SyncPlaybackActions.CHANGE_TRACK,
            trackId = track.videoId,
            trackInfo = track.toSyncTrackInfo(),
            queue = queue.map { it.toSyncTrackInfo() }
        )
    }

    fun onLocalSkipNext() {
        if (isSyncing || !isInRoom || !canControlMusic) return
        client.sendPlaybackAction(SyncPlaybackActions.SKIP_NEXT)
    }

    fun onLocalSkipPrev() {
        if (isSyncing || !isInRoom || !canControlMusic) return
        client.sendPlaybackAction(SyncPlaybackActions.SKIP_PREV)
    }

    fun onLocalQueueAdd(track: TrackItem, insertNext: Boolean) {
        if (isSyncing || !isInRoom || !canControlMusic) return
        client.sendPlaybackAction(
            action = SyncPlaybackActions.QUEUE_ADD,
            trackInfo = track.toSyncTrackInfo(),
            insertNext = insertNext
        )
    }

    fun onLocalQueueRemove(trackId: String) {
        if (isSyncing || !isInRoom || !canControlMusic) return
        client.sendPlaybackAction(
            action = SyncPlaybackActions.QUEUE_REMOVE,
            trackId = trackId
        )
    }

    fun onLocalQueueClear() {
        if (isSyncing || !isInRoom || !canControlMusic) return
        client.sendPlaybackAction(SyncPlaybackActions.QUEUE_CLEAR)
    }

    fun onLocalVolumeChanged(volume: Float) {
        if (isSyncing || !isInRoom || !isHost) return
        if (userPrefs.settingsFlow.value.listenTogetherSyncVolume) {
            client.sendPlaybackAction(SyncPlaybackActions.SET_VOLUME, volume = volume)
        }
    }

    fun onPlayerIsPlayingChanged(isPlaying: Boolean, positionMs: Long) {
        if (isSyncing || !isInRoom || !canControlMusic) return
        if (isPlaying) {
            onLocalPlay(positionMs)
        } else {
            onLocalPause(positionMs)
        }
    }

    fun notifyGuestRestricted() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Room controls are locked by the host", Toast.LENGTH_SHORT).show()
        }
    }
}
