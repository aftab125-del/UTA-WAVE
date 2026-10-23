package com.uta.wave.ui.library

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.importer.PlaylistImporter
import com.uta.wave.ui.recognizer.BlushRose
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
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

    var showLinkDialog by remember { mutableStateOf(false) }
    var spotifyLinkInput by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var importStatusText by remember { mutableStateOf("") }

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

            // Grouped Container Card (Matching Screenshot 4)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
            ) {
                // 1. Connected as User
                SpotifyRowItem(
                    icon = Icons.Default.Checklist,
                    title = "Connected as Namikaze_aftab",
                    subtitle = "Spotify connection active",
                    onClick = {
                        Toast.makeText(context, "Logged in as Namikaze_aftab", Toast.LENGTH_SHORT).show()
                    }
                )

                DividerLine()

                // 2. Select Spotify Playlists
                SpotifyRowItem(
                    icon = Icons.Default.PlaylistAdd,
                    title = "Select Spotify playlists",
                    subtitle = "Browse your public playlists",
                    onClick = {
                        showLinkDialog = true
                    }
                )

                DividerLine()

                // 3. Import playlist by link (Primary Action)
                SpotifyRowItem(
                    icon = Icons.Default.Link,
                    title = "Import playlist by link",
                    subtitle = "Paste a Spotify playlist link to import it, even if you don't own the playlist",
                    onClick = {
                        showLinkDialog = true
                    }
                )

                DividerLine()

                // 4. Import selected
                SpotifyRowItem(
                    icon = Icons.Default.AddLink,
                    title = "Import selected",
                    subtitle = "0 selected",
                    onClick = {
                        showLinkDialog = true
                    }
                )

                DividerLine()

                // 5. Refresh Spotify library
                SpotifyRowItem(
                    icon = Icons.Default.Refresh,
                    title = "Refresh Spotify library",
                    subtitle = "Connect Spotify, choose playlists, and mirror them to local UTA-wave playlists.",
                    onClick = {
                        Toast.makeText(context, "Spotify library synced", Toast.LENGTH_SHORT).show()
                    }
                )

                DividerLine()

                // 6. Log out
                SpotifyRowItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Log out",
                    subtitle = "Manage Log out settings",
                    onClick = {
                        Toast.makeText(context, "Spotify session logged out", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (isImporting) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = BlushRose, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = importStatusText, color = TextPrimary, fontSize = 13.sp)
                }
            }
        }

        // Paste Spotify Link Dialog
        if (showLinkDialog) {
            AlertDialog(
                onDismissRequest = { showLinkDialog = false },
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
                        text = "Import from Spotify",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Paste the public Spotify playlist link below:",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = spotifyLinkInput,
                            onValueChange = { spotifyLinkInput = it },
                            placeholder = { Text("https://open.spotify.com/playlist/...", color = TextMuted, fontSize = 12.sp) },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlushRose,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = BlushRose
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (spotifyLinkInput.isNotBlank()) {
                                val link = spotifyLinkInput.trim()
                                showLinkDialog = false
                                isImporting = true
                                importStatusText = "Connecting to Spotify..."
                                scope.launch {
                                    val (title, count) = PlaylistImporter.importFromSpotify(
                                        context = context,
                                        spotifyUrlOrId = link,
                                        onProgress = { cur, tot ->
                                            importStatusText = "Importing $cur of $tot tracks..."
                                        }
                                    )
                                    isImporting = false
                                    spotifyLinkInput = ""
                                    if (count > 0) {
                                        Toast.makeText(context, "Imported \"$title\" ($count tracks)!", Toast.LENGTH_LONG).show()
                                        onNavigateBack()
                                    } else {
                                        Toast.makeText(context, "Import failed: $title", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Text("OK", color = BlushRose, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLinkDialog = false }) {
                        Text("Cancel", color = TextSecondary)
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
            tint = TextSecondary,
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

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.05f))
    )
}
