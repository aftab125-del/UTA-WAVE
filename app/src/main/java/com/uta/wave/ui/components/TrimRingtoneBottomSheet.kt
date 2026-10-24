package com.uta.wave.ui.components

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.ringtone.RingtoneHelper
import com.uta.wave.data.ringtone.RingtoneResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 1:1 Visual & Functional Replica of the "Trim Ringtone" bottom sheet.
 * Features:
 * - Subtitle: Select the part of "{title}" to use as ringtone. (Max 30s recommended)
 * - Centered play/pause preview button for the selected slice.
 * - Dual-handle range slider with start/end timestamps and custom handles.
 * - "Selected duration: 0:30" display.
 * - Action buttons: Cancel + "Set as Ringtone" pill button in #F2AEC7 blush pink.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimRingtoneBottomSheet(
    track: TrackItem,
    durationSeconds: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ringtoneHelper = remember { RingtoneHelper(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val maxDuration = remember(durationSeconds, track.durationSeconds) {
        val d = if (durationSeconds > 0) durationSeconds else if (track.durationSeconds > 0) track.durationSeconds else 180
        d.coerceAtLeast(30).toFloat()
    }

    var startSec by remember { mutableFloatStateOf(0f) }
    var endSec by remember { mutableFloatStateOf(30f.coerceAtMost(maxDuration)) }

    // Preview audio player state
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Cleanup preview player on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                // ignore
            }
            mediaPlayer = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isProcessing) {
                mediaPlayer?.stop()
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = Color(0xFF221A24),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Title
            Text(
                text = "Trim Ringtone",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Select the part of \"${track.title}\" to use as ringtone.\n(Max 30s recommended)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = Color.White.copy(alpha = 0.70f)
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Centered Play/Pause Preview Button
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (isPreviewPlaying) {
                            mediaPlayer?.pause()
                            isPreviewPlaying = false
                        } else {
                            scope.launch {
                                try {
                                    if (mediaPlayer == null) {
                                        val streamUrl = withContext(Dispatchers.IO) {
                                            InnerTubeClient().resolveStreamUrl(track.videoId)
                                        }
                                        if (streamUrl.isNullOrBlank()) {
                                            Toast.makeText(context, "Could not load preview", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        mediaPlayer = MediaPlayer().apply {
                                            setDataSource(streamUrl)
                                            prepare()
                                        }
                                    }
                                    mediaPlayer?.seekTo((startSec * 1000).toInt())
                                    mediaPlayer?.start()
                                    isPreviewPlaying = true

                                    // Auto stop preview at endSec
                                    launch {
                                        while (isPreviewPlaying && mediaPlayer?.isPlaying == true) {
                                            delay(100)
                                            val currentPos = mediaPlayer?.currentPosition ?: 0
                                            if (currentPos >= (endSec * 1000).toInt()) {
                                                mediaPlayer?.pause()
                                                isPreviewPlaying = false
                                                break
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isPreviewPlaying = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Preview",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Range Timestamps (Start & End)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatSeconds(startSec.toInt()),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = formatSeconds(endSec.toInt()),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // Dual Handle Range Slider
            RangeSlider(
                value = startSec..endSec,
                onValueChange = { range ->
                    val newStart = range.start
                    var newEnd = range.endInclusive
                    // Maintain max 30s recommended or clamp
                    if (newEnd - newStart > 30f) {
                        if (newStart != startSec) {
                            newEnd = (newStart + 30f).coerceAtMost(maxDuration)
                        } else {
                            // end thumb moved
                            newEnd = (startSec + 30f).coerceAtMost(maxDuration)
                        }
                    }
                    if (newEnd - newStart < 5f) {
                        newEnd = (newStart + 5f).coerceAtMost(maxDuration)
                    }
                    startSec = newStart
                    endSec = newEnd

                    if (isPreviewPlaying) {
                        mediaPlayer?.seekTo((startSec * 1000).toInt())
                    }
                },
                valueRange = 0f..maxDuration,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFF2AEC7),
                    activeTrackColor = Color(0xFFF2AEC7),
                    inactiveTrackColor = Color(0xFF4A3845)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Selected Duration Text
            val selectedDuration = (endSec - startSec).toInt()
            Text(
                text = "Selected duration: ${formatSeconds(selectedDuration)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.70f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons Row: Cancel + Set as Ringtone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        mediaPlayer?.stop()
                        onDismiss()
                    },
                    enabled = !isProcessing
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        mediaPlayer?.stop()
                        isPreviewPlaying = false
                        isProcessing = true

                        scope.launch {
                            val startMs = (startSec * 1000).toLong()
                            val endMs = (endSec * 1000).toLong()

                            val result = ringtoneHelper.trimAndSetRingtone(
                                track = track,
                                startMs = startMs,
                                endMs = endMs
                            )

                            isProcessing = false

                            when (result) {
                                is RingtoneResult.Success -> {
                                    val message = if (result.isDefaultSet) {
                                        "Ringtone set: \"${track.title}\" (${selectedDuration}s)"
                                    } else {
                                        "Ringtone saved to device Ringtones"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                                is RingtoneResult.PermissionRequired -> {
                                    Toast.makeText(
                                        context,
                                        "Ringtone saved! Please grant permission to apply it automatically.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        try {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                    onDismiss()
                                }
                                is RingtoneResult.Error -> {
                                    Toast.makeText(
                                        context,
                                        "Error: ${result.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2AEC7),
                        contentColor = Color(0xFF3B1528)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color(0xFF3B1528),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trimming...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "Set as Ringtone",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
