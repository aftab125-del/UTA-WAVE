package com.uta.wave.ui.library

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.importer.PlaylistImporter
import com.uta.wave.ui.recognizer.BlushRose
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SpotifyImportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler {
        onNavigateBack()
    }

    var showLinkDialog by remember { mutableStateOf(false) }
    var spotifyLinkInput by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var importStatusText by remember { mutableStateOf("") }
    var currentTrackText by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar: Back Arrow + "Import from Spotify"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import from Spotify",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section Header: "SPOTIFY IMPORT"
            Text(
                text = "SPOTIFY IMPORT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                ),
                color = TextMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Grouped Container Card with ONLY ONE OPTION: "import by pasting playlist link"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
            ) {
                SpotifyRowItem(
                    icon = Icons.Default.Link,
                    title = "Import by pasting playlist link",
                    subtitle = "Paste a Spotify playlist link to import it into your library",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clipText.contains("spotify.com") || clipText.contains("spotify.link")) {
                            spotifyLinkInput = clipText.trim()
                        }
                        showLinkDialog = true
                    }
                )
            }

            if (isImporting) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BlushRose.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = BlushRose, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = importStatusText.ifBlank { "Connecting to Spotify..." },
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (currentTrackText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentTrackText,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Dialog: Paste Spotify Link Dialog
        if (showLinkDialog) {
            AlertDialog(
                onDismissRequest = { if (!isImporting) showLinkDialog = false },
                containerColor = Color(0xFF201824),
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(BlushRose.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = BlushRose, modifier = Modifier.size(24.dp))
                    }
                },
                title = {
                    Text(
                        text = "Import playlist from Spotify",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Paste any public Spotify playlist link below (or enter 22-character playlist ID):",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = spotifyLinkInput,
                            onValueChange = { spotifyLinkInput = it },
                            placeholder = { Text("https://open.spotify.com/playlist/...", color = TextMuted, fontSize = 12.sp) },
                            shape = RoundedCornerShape(22.dp),
                            singleLine = true,
                            enabled = !isImporting,
                            trailingIcon = {
                                IconButton(
                                    enabled = !isImporting,
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        if (clipText.isNotBlank()) {
                                            spotifyLinkInput = clipText.trim()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = BlushRose, modifier = Modifier.size(20.dp))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlushRose,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = BlushRose
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isImporting) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = BlushRose, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = importStatusText.ifBlank { "Connecting to Spotify..." },
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (currentTrackText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentTrackText,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isImporting && spotifyLinkInput.isNotBlank(),
                        onClick = {
                            val link = spotifyLinkInput.trim()
                            isImporting = true
                            importStatusText = "Connecting to Spotify..."
                            currentTrackText = ""
                            scope.launch {
                                val (title, count) = PlaylistImporter.importFromSpotify(
                                    context = context,
                                    spotifyUrlOrId = link,
                                    onProgress = { cur, tot, trackTitle ->
                                        importStatusText = "Importing track $cur of $tot..."
                                        currentTrackText = trackTitle
                                    }
                                )
                                isImporting = false
                                if (count > 0) {
                                    showLinkDialog = false
                                    spotifyLinkInput = ""
                                    currentTrackText = ""
                                    Toast.makeText(context, "Imported \"$title\" ($count tracks)!", Toast.LENGTH_LONG).show()
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(context, "Import failed: $title", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Import", color = BlushRose, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isImporting,
                        onClick = { showLinkDialog = false }
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            )
        }
    }
}

@Composable
private fun SpotifyRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = BlushRose,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }
    }
}
