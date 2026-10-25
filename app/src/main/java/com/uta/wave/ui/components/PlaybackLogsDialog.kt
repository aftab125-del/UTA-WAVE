package com.uta.wave.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.player.AppLogger
import com.uta.wave.player.LogEntry
import com.uta.wave.player.WaveEqualizer
import com.uta.wave.player.WavePlayerState
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackLogsDialog(
    playerState: WavePlayerState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logs by AppLogger.logs.collectAsState()
    val eqState by WaveEqualizer.state.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == "ALL") logs
        else logs.filter { it.level.equals(selectedFilter, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DIAGNOSTICS & TELEMETRY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ElectricViolet
                        )
                        Text(
                            text = "Playback Event Logs",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            AppLogger.clearLogs()
                            Toast.makeText(context, "Playback logs cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = TextSecondary)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Player State Snapshot Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ENGINE TELEMETRY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextMuted
                        )
                        val status = when {
                            playerState.isPlaying -> "PLAYING"
                            playerState.isBuffering -> "BUFFERING"
                            playerState.isResolving -> "RESOLVING STREAM"
                            else -> "IDLE"
                        }
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (playerState.isPlaying) Color(0xFF4EBE89) else if (playerState.isBuffering) Color(0xFFFFB74D) else ElectricViolet
                        )
                    }

                    playerState.currentTrack?.let { track ->
                        Text(
                            text = "Track: ${track.title} - ${track.artist}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Equalizer: ${if (eqState.isEnabled) "ACTIVE (${eqState.presetName})" else "OFF"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (eqState.isEnabled) ElectricViolet else TextMuted
                        )
                        Text(
                            text = "Queue: ${playerState.queueIndex + 1}/${playerState.queue.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    if (playerState.error != null) {
                        Text(
                            text = "Last Error: ${playerState.error}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF5252),
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips + Copy Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL", "ERROR", "WARN", "INFO", "DEBUG").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ElectricViolet else SurfaceElevated)
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) ObsidianBackground else TextSecondary
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val text = AppLogger.getExportText()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("PlaybackLogs", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied ${logs.size} log entries to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = ObsidianBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy Logs",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = ObsidianBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Log Items List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No logs for filter: $selectedFilter",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(filteredLogs) { entry ->
                        PlaybackLogItem(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackLogItem(entry: LogEntry) {
    val levelColor = when (entry.level) {
        "ERROR" -> Color(0xFFFF5252)
        "WARN" -> Color(0xFFFFB74D)
        "DEBUG" -> Color(0xFF64B5F6)
        else -> Color(0xFF81C784)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "${entry.timestamp} ",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            ),
            color = TextMuted
        )
        Text(
            text = "[${entry.level}] ",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = levelColor
        )
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = TextPrimary
        )
    }
}
