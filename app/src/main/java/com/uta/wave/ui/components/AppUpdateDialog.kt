package com.uta.wave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.update.AppUpdateInfo
import com.uta.wave.ui.recognizer.BlushRose
import com.uta.wave.ui.recognizer.DarkBlushText
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDownloadNow: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isMandatory) onLater()
        },
        containerColor = Color(0xFF1E1522),
        shape = RoundedCornerShape(26.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(BlushRose.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowCircleDown,
                    contentDescription = null,
                    tint = BlushRose,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "UTA-wave v${updateInfo.versionName} is ready to install",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "WHAT'S NEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = BlushRose
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = updateInfo.changelog.ifBlank { "• Latest bug fixes and playback enhancements" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownloadNow,
                colors = ButtonDefaults.buttonColors(containerColor = BlushRose),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = "Download now",
                    color = DarkBlushText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            if (!updateInfo.isMandatory) {
                TextButton(
                    onClick = onLater,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "Later",
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    )
}
