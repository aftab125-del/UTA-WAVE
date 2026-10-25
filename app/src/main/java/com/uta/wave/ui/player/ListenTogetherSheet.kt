package com.uta.wave.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.sync.WaveSyncManager
import com.uta.wave.sync.WaveSyncServers
import com.uta.wave.sync.model.SyncConnectionState
import com.uta.wave.sync.model.SyncJoinRequest
import com.uta.wave.sync.model.SyncRoomRole
import com.uta.wave.sync.model.SyncSuggestion
import com.uta.wave.sync.model.SyncUserInfo
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val syncManager = remember { WaveSyncManager.getInstance(context) }
    val userPrefs = remember { UserPreferences.getInstance(context) }
    val settings by userPrefs.settingsFlow.collectAsState()

    val connectionState by syncManager.connectionState.collectAsState()
    val roomState by syncManager.roomState.collectAsState()
    val role by syncManager.role.collectAsState()
    val userId by syncManager.userId.collectAsState()
    val pendingJoinRequests by syncManager.pendingJoinRequests.collectAsState()
    val pendingSuggestions by syncManager.pendingSuggestions.collectAsState()
    val rtt by syncManager.rtt.collectAsState()

    var showChatSheet by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF140F18),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Listen Together",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = when (connectionState) {
                                SyncConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                SyncConnectionState.CONNECTING, SyncConnectionState.RECONNECTING -> Color(0xFFFFB300)
                                else -> Color(0xFFF44336)
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when (connectionState) {
                                    SyncConnectionState.CONNECTED -> if (rtt > 0) "Connected (${rtt}ms)" else "Connected"
                                    SyncConnectionState.CONNECTING -> "Connecting..."
                                    SyncConnectionState.RECONNECTING -> "Reconnecting..."
                                    SyncConnectionState.ERROR -> "Connection Error"
                                    SyncConnectionState.DISCONNECTED -> "Offline Relay"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextMuted
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!syncManager.isInRoom) {
                // LOBBY STATE: Setup, Create, or Join
                LobbyView(
                    syncManager = syncManager,
                    userPrefs = userPrefs,
                    settings = settings,
                    connectionState = connectionState
                )
            } else {
                // ACTIVE ROOM STATE: Room controls, member list, suggestions, chat trigger
                ActiveRoomView(
                    syncManager = syncManager,
                    roomState = roomState!!,
                    role = role,
                    myUserId = userId,
                    pendingJoinRequests = pendingJoinRequests,
                    pendingSuggestions = pendingSuggestions,
                    onOpenChat = { showChatSheet = true }
                )
            }
        }
    }

    if (showChatSheet) {
        RoomChatSheet(onDismiss = { showChatSheet = false })
    }
}

@Composable
private fun LobbyView(
    syncManager: WaveSyncManager,
    userPrefs: UserPreferences,
    settings: com.uta.wave.data.preferences.AppSettings,
    connectionState: SyncConnectionState
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf(settings.listenTogetherUsername.ifEmpty { "Listener" }) }
    var roomCodeInput by remember { mutableStateOf("") }
    val servers by WaveSyncServers.serversFlow.collectAsState()
    var selectedServerUrl by remember { mutableStateOf(settings.listenTogetherServerUrl) }
    var serverMenuExpanded by remember { mutableStateOf(false) }

    // 1. Profile / Username Row
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Your Display Name",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                userPrefs.updateListenTogetherUsername(it)
            },
            placeholder = { Text("Enter a nickname...", color = TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedBorderColor = ElectricViolet,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. Relay Server Selector
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { serverMenuExpanded = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val activeServer = servers.firstOrNull { it.url == selectedServerUrl } ?: servers.firstOrNull()
        Column {
            Text(
                text = "Relay Server",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextMuted
            )
            Text(
                text = activeServer?.name ?: "Metrolist Server",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary
            )
        }
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            tint = ElectricViolet,
            modifier = Modifier.size(18.dp)
        )

        DropdownMenu(
            expanded = serverMenuExpanded,
            onDismissRequest = { serverMenuExpanded = false }
        ) {
            servers.forEach { server ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(server.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(server.location, color = TextMuted, fontSize = 11.sp)
                        }
                    },
                    onClick = {
                        selectedServerUrl = server.url
                        userPrefs.updateListenTogetherServerUrl(server.url)
                        syncManager.client.disconnect()
                        syncManager.client.connect(server.url)
                        serverMenuExpanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Action Cards: Host or Join
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Host Button
        Button(
            onClick = {
                val finalName = username.trim().ifEmpty { "Host" }
                syncManager.client.createRoom(finalName)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricViolet,
                contentColor = Color(0xFF140F18)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text("Host Room", fontWeight = FontWeight.Bold)
        }

        // Join Room Action
        OutlinedButton(
            onClick = {
                if (roomCodeInput.isNotBlank()) {
                    val finalName = username.trim().ifEmpty { "Guest" }
                    syncManager.client.joinRoom(roomCodeInput.trim().uppercase(), finalName)
                } else {
                    Toast.makeText(context, "Enter a valid 6-character room code", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text("Join Room", fontWeight = FontWeight.SemiBold)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Room Code Input Field
    OutlinedTextField(
        value = roomCodeInput,
        onValueChange = {
            if (it.length <= 8) roomCodeInput = it.uppercase()
        },
        placeholder = { Text("Enter Room Code (e.g. AB12CD)", color = TextMuted) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            focusedBorderColor = ElectricViolet,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Session Options
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        SettingToggleRow(
            title = "Auto-Approve Join Requests",
            subtitle = "Allow guests to join instantly without manual confirmation",
            checked = settings.listenTogetherAutoApproval,
            onCheckedChange = { userPrefs.updateListenTogetherAutoApproval(it) }
        )
        SettingToggleRow(
            title = "Sync Volume With Host",
            subtitle = "Keep audio output volume level aligned with the host",
            checked = settings.listenTogetherSyncVolume,
            onCheckedChange = { userPrefs.updateListenTogetherSyncVolume(it) }
        )
        SettingToggleRow(
            title = "Smart Resync",
            subtitle = "Automatically re-align audio after pauses or network drops",
            checked = settings.listenTogetherSmartResync,
            onCheckedChange = { userPrefs.updateListenTogetherSmartResync(it) }
        )
    }
}

@Composable
private fun ActiveRoomView(
    syncManager: WaveSyncManager,
    roomState: com.uta.wave.sync.model.SyncRoomState,
    role: SyncRoomRole,
    myUserId: String?,
    pendingJoinRequests: List<SyncJoinRequest>,
    pendingSuggestions: List<SyncSuggestion>,
    onOpenChat: () -> Unit
) {
    val context = LocalContext.current
    var copiedFeedback by remember { mutableStateOf(false) }

    // 1. Room Header Banner
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ROOM CODE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = TextMuted
                    )
                    Text(
                        text = roomState.roomCode,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = ElectricViolet
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy Code Button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Listen Together Code", roomState.roomCode)
                            clipboard?.setPrimaryClip(clip)
                            copiedFeedback = true
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(
                            imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = if (copiedFeedback) Color(0xFF4CAF50) else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Share Button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join my UTA-wave Listen Together room!\nRoom Code: ${roomState.roomCode}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Room Code"))
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Role Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (role == SyncRoomRole.HOST) ElectricViolet.copy(alpha = 0.2f) else SurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (role == SyncRoomRole.HOST) "HOST" else "GUEST",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (role == SyncRoomRole.HOST) ElectricViolet else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${roomState.users.size} participant${if (roomState.users.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Action Buttons (Chat & Leave)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onOpenChat,
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceElevated,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
        ) {
            Icon(imageVector = Icons.Default.Forum, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Room Chat", fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = { syncManager.client.leaveRoom() },
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Leave Room", fontWeight = FontWeight.SemiBold)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Host Permissions Switch (Host only)
    if (role == SyncRoomRole.HOST) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            SettingToggleRow(
                title = "Participant Playback Control",
                subtitle = "Allow guests to play, pause, seek, and skip songs",
                checked = roomState.allowParticipantControl,
                onCheckedChange = { syncManager.client.updateRoomSettings(it) }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
    }

    // 4. Pending Join Requests (Host only)
    if (role == SyncRoomRole.HOST && pendingJoinRequests.isNotEmpty()) {
        Text(
            text = "Pending Join Requests (${pendingJoinRequests.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pendingJoinRequests.forEach { req ->
                JoinRequestCard(
                    req = req,
                    onApprove = { syncManager.client.approveJoin(req.userId) },
                    onReject = { syncManager.client.rejectJoin(req.userId) }
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }

    // 5. Song Suggestions (Host only)
    if (role == SyncRoomRole.HOST && pendingSuggestions.isNotEmpty()) {
        Text(
            text = "Song Suggestions (${pendingSuggestions.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pendingSuggestions.forEach { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    onApprove = { syncManager.client.approveSuggestion(suggestion.suggestionId) },
                    onReject = { syncManager.client.rejectSuggestion(suggestion.suggestionId) }
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }

    // 6. Connected Participants List
    Text(
        text = "Participants (${roomState.users.size})",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 4.dp)
    ) {
        roomState.users.forEach { user ->
            ParticipantRow(
                user = user,
                isMe = user.userId == myUserId,
                amHost = role == SyncRoomRole.HOST,
                onKick = { syncManager.client.kickUser(user.userId) },
                onTransferHost = { syncManager.client.transferHost(user.userId) },
                onBlock = { syncManager.client.blockUser(user.username) }
            )
        }
    }
}

@Composable
private fun JoinRequestCard(
    req: SyncJoinRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = req.username.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(req.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Wants to join the session", color = TextMuted, fontSize = 11.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onApprove,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElectricViolet)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Approve", tint = Color(0xFF140F18), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Reject", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: SyncSuggestion,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.trackInfo.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${suggestion.trackInfo.artist} • Suggested by ${suggestion.fromUsername}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onApprove,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElectricViolet)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Approve", tint = Color(0xFF140F18), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Reject", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    user: SyncUserInfo,
    isMe: Boolean,
    amHost: Boolean,
    onKick: () -> Unit,
    onTransferHost: () -> Unit,
    onBlock: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = if (user.isHost) ElectricViolet else TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(You)",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (user.isHost) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Host",
                            tint = ElectricViolet,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Host",
                            color = ElectricViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (amHost && !isMe) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Make Host") },
                        onClick = {
                            onTransferHost()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Kick User", color = Color(0xFFEF5350)) },
                        onClick = {
                            onKick()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Block User", color = Color(0xFFEF5350)) },
                        onClick = {
                            onBlock()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF140F18),
                checkedTrackColor = ElectricViolet,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceElevated
            )
        )
    }
}
