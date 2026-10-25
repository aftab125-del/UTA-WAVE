package com.uta.wave.ui.library

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
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
    val prefs = remember { context.getSharedPreferences("uta_wave_spotify", Context.MODE_PRIVATE) }

    var connectedUser by remember {
        mutableStateOf(prefs.getString("connected_username", "Namikaze_aftab") ?: "Namikaze_aftab")
    }
    var isConnected by remember {
        mutableStateOf(prefs.getBoolean("is_connected", true))
    }

    BackHandler {
        onNavigateBack()
    }

    var showAccountDialog by remember { mutableStateOf(false) }
    var editUsernameInput by remember { mutableStateOf(connectedUser) }

    var showLinkDialog by remember { mutableStateOf(false) }
    var spotifyLinkInput by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var importStatusText by remember { mutableStateOf("") }
    var currentTrackText by remember { mutableStateOf("") }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSelectPlaylistsDialog by remember { mutableStateOf(false) }

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
                    title = if (isConnected) "Connected as $connectedUser" else "Connect Spotify account",
                    subtitle = if (isConnected) "Spotify connection active" else "Tap to configure Spotify username",
                    isActive = isConnected,
                    onClick = {
                        editUsernameInput = connectedUser
                        showAccountDialog = true
                    }
                )

                DividerLine()

                // 2. Select Spotify Playlists
                SpotifyRowItem(
                    icon = Icons.Default.PlaylistAdd,
                    title = "Select Spotify playlists",
                    subtitle = "Browse your public playlists",
                    onClick = {
                        showSelectPlaylistsDialog = true
                    }
                )

                DividerLine()

                // 3. Import playlist by link (Primary Action)
                SpotifyRowItem(
                    icon = Icons.Default.Link,
                    title = "Import playlist by link",
                    subtitle = "Paste a Spotify playlist link to import it, even if you don't own the playlist",
                    onClick = {
                        // Check clipboard for Spotify link automatically
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clipText.contains("spotify.com") || clipText.contains("spotify.link")) {
                            spotifyLinkInput = clipText.trim()
                        }
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
                        showSelectPlaylistsDialog = true
                    }
                )

                DividerLine()

                // 5. Refresh Spotify library
                SpotifyRowItem(
                    icon = Icons.Default.Refresh,
                    title = "Refresh Spotify library",
                    subtitle = "Connect Spotify, choose playlists, and mirror them to local UTA-wave playlists.",
                    onClick = {
                        Toast.makeText(context, "Spotify library is up to date", Toast.LENGTH_SHORT).show()
                    }
                )

                DividerLine()

                // 6. Log out
                SpotifyRowItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Log out",
                    subtitle = "Manage Log out settings",
                    onClick = {
                        showLogoutDialog = true
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
                        text = importStatusText,
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
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Dialog 1: Paste Spotify Link Dialog
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
                        text = "Import from Spotify",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Paste any public Spotify playlist link below (or enter 22-char playlist ID):",
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
                            trailingIcon = {
                                IconButton(
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
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isImporting && spotifyLinkInput.isNotBlank(),
                        onClick = {
                            val link = spotifyLinkInput.trim()
                            showLinkDialog = false
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
                                spotifyLinkInput = ""
                                currentTrackText = ""
                                if (count > 0) {
                                    Toast.makeText(context, "Imported \"$title\" ($count tracks)!", Toast.LENGTH_LONG).show()
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(context, "Import failed: $title", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Import", color = BlushRose, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isImporting,
                        onClick = { showLinkDialog = false }
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog 2: Spotify Account Configuration
        if (showAccountDialog) {
            AlertDialog(
                onDismissRequest = { showAccountDialog = false },
                containerColor = Color(0xFF201824),
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1DB954).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(24.dp))
                    }
                },
                title = {
                    Text(
                        text = "Spotify Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Set your Spotify username or account handle to personalize your library:",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editUsernameInput,
                            onValueChange = { editUsernameInput = it },
                            placeholder = { Text("e.g. Namikaze_aftab", color = TextMuted, fontSize = 12.sp) },
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
                            val newName = editUsernameInput.trim().ifBlank { "User" }
                            connectedUser = newName
                            isConnected = true
                            prefs.edit()
                                .putString("connected_username", newName)
                                .putBoolean("is_connected", true)
                                .apply()
                            showAccountDialog = false
                            Toast.makeText(context, "Connected as $newName", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Save", color = BlushRose, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog 3: Select Spotify Playlists Dialog
        if (showSelectPlaylistsDialog) {
            AlertDialog(
                onDismissRequest = { showSelectPlaylistsDialog = false },
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
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = BlushRose, modifier = Modifier.size(24.dp))
                    }
                },
                title = {
                    Text(
                        text = "Select Spotify Playlists",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "To import any Spotify playlist directly into UTA-wave without login credentials, use 'Import playlist by link' below.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tip: Open Spotify, open your playlist, tap 'Share' -> 'Copy link', then tap 'Paste Link' below.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSelectPlaylistsDialog = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clipText.contains("spotify.com") || clipText.contains("spotify.link")) {
                                spotifyLinkInput = clipText.trim()
                            }
                            showLinkDialog = true
                        }
                    ) {
                        Text("Paste Link", color = BlushRose, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSelectPlaylistsDialog = false }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog 4: Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = Color(0xFF201824),
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                    }
                },
                title = {
                    Text(
                        text = "Log Out from Spotify?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to disconnect the Spotify account session from UTA-wave?",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isConnected = false
                            connectedUser = "Guest"
                            prefs.edit()
                                .putBoolean("is_connected", false)
                                .putString("connected_username", "Guest")
                                .apply()
                            showLogoutDialog = false
                            Toast.makeText(context, "Logged out from Spotify", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Log Out", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
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
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) Color(0xFF1DB954) else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1DB954))
                )
            }
        }
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
