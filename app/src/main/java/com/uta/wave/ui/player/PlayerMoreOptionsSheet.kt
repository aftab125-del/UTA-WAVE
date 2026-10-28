package com.uta.wave.ui.player

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMoreOptionsSheet(
    track: TrackItem,
    isDownloaded: Boolean,
    isLyricsVisible: Boolean,
    onDismiss: () -> Unit,
    onViewArtist: (String) -> Unit,
    onViewAlbum: (String) -> Unit,
    onAddToLibrary: () -> Unit,
    onCast: () -> Unit,
    onToggleAmbientMode: () -> Unit,
    onToggleLyrics: () -> Unit,
    onClearQueue: () -> Unit,
    onToggleDownload: () -> Unit,
    onSetRingtone: () -> Unit,
    onListenTogether: () -> Unit,
    onShowDetails: () -> Unit,
    onRadio: (() -> Unit)? = null,
    onAddToPlaylist: () -> Unit,
    onOpenEqualizer: (() -> Unit)? = null,
    onOpenAudioDevices: (() -> Unit)? = null,
    onOpenDiagnostics: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF140F18),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Top Quick Pill Action Buttons Row: Add to Playlist & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Add to Playlist Pill
                QuickActionPill(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    text = "Add to playlist",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onAddToPlaylist()
                    }
                )

                // Share Pill
                QuickActionPill(
                    icon = Icons.Default.Share,
                    text = "Share",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            val shareUrl = if (track.videoId.startsWith("http")) track.videoId else "https://youtu.be/${track.videoId}"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Listening to \"${track.title}\" by ${track.artist} on UTA-wave 🎵\n$shareUrl"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share song"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Main Grouped Options Card (Matching Screenshot 1)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF201826))
                    .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            ) {
                // 1. View artist
                OptionRow(
                    icon = Icons.Default.Person,
                    title = "View artist",
                    subtitle = track.artist,
                    onClick = {
                        onDismiss()
                        onViewArtist(track.artist)
                    }
                )

                OptionDivider()

                // 2. View album
                OptionRow(
                    icon = Icons.Default.Album,
                    title = "View album",
                    subtitle = "${track.title} - Single",
                    onClick = {
                        onDismiss()
                        onViewAlbum(track.title)
                    }
                )

                OptionDivider()

                // 3. Add to library
                OptionRow(
                    icon = Icons.Default.LibraryAdd,
                    title = "Add to library",
                    subtitle = null,
                    onClick = {
                        onDismiss()
                        onAddToLibrary()
                    }
                )

                OptionDivider()

                // 4. Cast to...
                OptionRow(
                    icon = Icons.Default.Cast,
                    title = "Cast to...",
                    subtitle = null,
                    onClick = {
                        onDismiss()
                        onCast()
                    }
                )

                OptionDivider()

                // 5. Ambient Mode
                OptionRow(
                    icon = Icons.Default.Fullscreen,
                    title = "Ambient Mode",
                    subtitle = null,
                    onClick = {
                        onDismiss()
                        onToggleAmbientMode()
                    }
                )

                OptionDivider()

                // 6. Hide Lyrics / Show Lyrics
                OptionRow(
                    icon = Icons.Default.Lyrics,
                    title = if (isLyricsVisible) "Hide Lyrics" else "Show Lyrics",
                    subtitle = null,
                    onClick = {
                        onDismiss()
                        onToggleLyrics()
                    }
                )

                OptionDivider()

                // 7. Clear queue
                OptionRow(
                    icon = Icons.Default.ClearAll,
                    title = "Clear queue",
                    subtitle = null,
                    onClick = {
                        onDismiss()
                        onClearQueue()
                    }
                )

                OptionDivider()

                // 8. Remove download / Download
                OptionRow(
                    icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                    title = if (isDownloaded) "Remove download" else "Download song",
                    subtitle = null,
                    onClick = {
                        onDismiss()
                        onToggleDownload()
                    }
                )

                OptionDivider()

                // 9. Set as Ringtone
                OptionRow(
                    icon = Icons.Default.Notifications,
                    title = "Set as Ringtone",
                    subtitle = null,
                    onClick = {
                        onDismiss()
                        onSetRingtone()
                    }
                )

                OptionDivider()

                // 10. Listen Together
                OptionRow(
                    icon = Icons.Default.Group,
                    title = "Listen Together",
                    subtitle = null,
                    onClick = {
                        onDismiss()
                        onListenTogether()
                    }
                )

                OptionDivider()

                // 11. Details
                OptionRow(
                    icon = Icons.Default.Info,
                    title = "Details",
                    subtitle = "View the song's information",
                    onClick = {
                        onDismiss()
                        onShowDetails()
                    }
                )

                OptionDivider()

                // 12. Equalizer
                OptionRow(
                    icon = Icons.Default.GraphicEq,
                    title = "Equalizer",
                    subtitle = "Acoustic DSP presets & 10-band controls",
                    onClick = {
                        onDismiss()
                        onOpenEqualizer?.invoke()
                    }
                )

                OptionDivider()

                // 13. Audio Output Devices
                OptionRow(
                    icon = Icons.Default.Speaker,
                    title = "Audio Output Devices",
                    subtitle = "Route between Bluetooth, headphones & speaker",
                    onClick = {
                        onDismiss()
                        onOpenAudioDevices?.invoke()
                    }
                )

                OptionDivider()

                // 14. Playback Logs & Diagnostics
                OptionRow(
                    icon = Icons.Default.BugReport,
                    title = "Playback Logs & HUD",
                    subtitle = "Real-time engine telemetry & decoder status",
                    onClick = {
                        onDismiss()
                        onOpenDiagnostics?.invoke()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF261D2B))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = TextPrimary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun OptionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.04f))
    )
}
