package com.uta.wave.ui.account

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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.account.AccountManager

/**
 * 100% 1:1 Pixel-Accurate Replica of Echo Music's Account Screen.
 * Contains:
 * 1. SWITCH ACCOUNTS: Circular YouTube Avatar, Name ("Namikaze aftab"), "Log out" pill button, "+ Add another account"
 * 2. LOGIN WITH TOKEN: Cube icon + "Tap to show token" with clipboard copy dialog
 * 3. PLAYER & CONTENT: "More content" toggle, "Auto sync with account" toggle
 * 4. INTEGRATIONS: "Discord" - Manage Discord Integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val accountManager = remember { AccountManager.getInstance(context) }
    val profile by accountManager.profileFlow.collectAsState()

    var showSignInDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }

    // Auto-refresh profile from YouTube when screen opens
    LaunchedEffect(profile.isLoggedIn) {
        if (profile.isLoggedIn) {
            accountManager.refreshProfileFromYouTube()
        }
    }

    if (showSignInDialog) {
        GoogleSignInDialog(
            onDismiss = { showSignInDialog = false },
            onSuccess = {
                showSignInDialog = false
                Toast.makeText(context, "Account connected successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = {
                Text(
                    text = "Session Auth Token",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = if (profile.isLoggedIn) {
                            "SAPISID: ${profile.sapisid ?: "Active"}\n\nCookie:\n${profile.cookie?.take(80)}..."
                        } else {
                            "No active session token. Sign in with YouTube Music to generate authentication tokens."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                if (profile.isLoggedIn && !profile.cookie.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(profile.cookie ?: ""))
                            Toast.makeText(context, "Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showTokenDialog = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color(0xFFF2AEC7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Token", color = Color(0xFFF2AEC7), fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { showTokenDialog = false }) {
                        Text("Close", color = Color(0xFFF2AEC7))
                    }
                }
            },
            containerColor = Color(0xFF221826)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Customization",
                            tint = Color(0xFFF2AEC7)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF140C16)
                )
            )
        },
        containerColor = Color(0xFF100912),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // -------------------------------------------------------------
            // SECTION 1: SWITCH ACCOUNTS
            // -------------------------------------------------------------
            Column {
                AccountSectionHeader(title = "SWITCH ACCOUNTS")

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1522))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                ) {
                    // Active Account Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        if (!profile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color(0xFFF2AEC7).copy(alpha = 0.6f), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF2AEC7).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFF2AEC7),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color(0xFFF8C8D8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (profile.isLoggedIn) {
                                    profile.handle ?: profile.email
                                } else {
                                    "Guest Mode (Offline)"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        }

                        // Log out / Sign in pill button
                        if (profile.isLoggedIn) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF2C2230))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                                    .clickable {
                                        accountManager.logout()
                                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Log out",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFF2AEC7))
                                    .clickable { showSignInDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Log in",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF351225)
                                )
                            }
                        }
                    }

                    // Divider
                    AccountCardDivider()

                    // "+ Add another account" Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSignInDialog = true }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add account",
                            tint = Color(0xFFF2AEC7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Add another account",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 2: LOGIN WITH TOKEN
            // -------------------------------------------------------------
            Column {
                AccountSectionHeader(title = "LOGIN WITH TOKEN")

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1522))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTokenDialog = true }
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Token,
                            contentDescription = null,
                            tint = Color(0xFFF2AEC7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Tap to show token",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION: SETTINGS & CUSTOMIZATION
            // -------------------------------------------------------------
            Column {
                AccountSectionHeader(title = "SETTINGS & CUSTOMIZATION")

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1522))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF2AEC7).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFFF2AEC7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Settings & Customization",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Appearance, Themes, AI Hub, Audio & Playback",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp
                                ),
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 3: PLAYER & CONTENT
            // -------------------------------------------------------------
            Column {
                AccountSectionHeader(title = "PLAYER & CONTENT")

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1522))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                ) {
                    // Row 1: More content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "More content",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Personalized discovery & mixes from account",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        }

                        Switch(
                            checked = profile.moreContentEnabled,
                            onCheckedChange = { accountManager.updateMoreContent(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF2AEC7),
                                uncheckedThumbColor = Color(0xFF8E7D8A),
                                uncheckedTrackColor = Color(0xFF2C2230)
                            )
                        )
                    }

                    AccountCardDivider()

                    // Row 2: Auto sync with account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto sync with account",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Keep listening history & library in sync",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        }

                        Switch(
                            checked = profile.autoSyncEnabled,
                            onCheckedChange = { accountManager.updateAutoSync(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF2AEC7),
                                uncheckedThumbColor = Color(0xFF8E7D8A),
                                uncheckedTrackColor = Color(0xFF2C2230)
                            )
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 4: INTEGRATIONS
            // -------------------------------------------------------------
            Column {
                AccountSectionHeader(title = "INTEGRATIONS")

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1522))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Discord Rich Presence is active", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = Color(0xFF5865F2),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Discord",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Manage Discord Integration",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AccountSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            fontSize = 11.5.sp
        ),
        color = Color(0xFFC0AAB8),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun AccountCardDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}
