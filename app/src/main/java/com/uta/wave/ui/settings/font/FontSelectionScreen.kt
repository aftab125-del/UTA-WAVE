package com.uta.wave.ui.settings.font

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.ui.theme.AppFont
import com.uta.wave.ui.theme.GoogleSansFontFamily
import com.uta.wave.ui.theme.OutfitFontFamily
import com.uta.wave.ui.theme.PlusJakartaSansFontFamily
import com.uta.wave.ui.theme.SansFlexFontFamily
import com.uta.wave.ui.theme.resolveFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSelectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences.getInstance(context) }
    val settings by prefs.settingsFlow.collectAsState()

    val currentFontKey = settings.selectedFont
    val customPath = settings.customFontPath

    val activeFontFamily = remember(currentFontKey, customPath) {
        resolveFontFamily(currentFontKey, customPath)
    }

    BackHandler {
        onBack()
    }

    // Custom Font File Picker Launcher
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val file = File(context.filesDir, "custom_font.ttf")
                        FileOutputStream(file).use { outputStream ->
                            inputStream?.copyTo(outputStream)
                        }
                        inputStream?.close()

                        prefs.updateCustomFontPath(file.absolutePath)
                        prefs.updateSelectedFont(AppFont.CUSTOM.value)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Custom font imported successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to load font: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Font",
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
            // 1. LIVE TYPOGRAPHY PREVIEW CARD
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF26192A), Color(0xFF160E18))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF2AEC7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = null,
                            tint = Color(0xFFF2AEC7),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "TYPOGRAPHY LIVE PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFFF2AEC7)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Track Title in Active Font
                Text(
                    text = "Blinding Lights",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = activeFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Artist & Album line in Active Font
                Text(
                    text = "The Weeknd • After Hours",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = activeFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    ),
                    color = Color(0xFFF2AEC7)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sample lyrics / Pangram in Active Font
                Text(
                    text = "The quick brown fox jumps over the lazy dog. 1234567890",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = activeFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color.White.copy(alpha = 0.70f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status chip
                val activeFontEnum = AppFont.fromValue(currentFontKey)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Active Font: ${activeFontEnum.displayName}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            // -------------------------------------------------------------
            // 2. FONT SELECTION LIST
            // -------------------------------------------------------------
            Column {
                Text(
                    text = "SELECT APPLICATION FONT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                        fontSize = 11.5.sp
                    ),
                    color = Color(0xFFC0AAB8),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1522))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                ) {
                    AppFont.entries.forEachIndexed { idx, font ->
                        val isSelected = currentFontKey.equals(font.value, ignoreCase = true)
                        val rowFontFamily = when (font) {
                            AppFont.SYSTEM -> FontFamily.Default
                            AppFont.GOOGLE_SANS -> GoogleSansFontFamily
                            AppFont.SANS_FLEX -> SansFlexFontFamily
                            AppFont.OUTFIT -> OutfitFontFamily
                            AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
                            AppFont.CUSTOM -> activeFontFamily
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (font == AppFont.CUSTOM) {
                                        if (!customPath.isNullOrBlank() && File(customPath).exists()) {
                                            prefs.updateSelectedFont(AppFont.CUSTOM.value)
                                        } else {
                                            fontPickerLauncher.launch("*/*")
                                        }
                                    } else {
                                        prefs.updateSelectedFont(font.value)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFFF2AEC7),
                                    unselectedColor = Color(0xFF6B5868)
                                )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = font.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = rowFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 16.sp
                                    ),
                                    color = if (isSelected) Color(0xFFF8C8D8) else Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = font.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = rowFontFamily,
                                        fontSize = 12.5.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                            }

                            if (font == AppFont.CUSTOM) {
                                Button(
                                    onClick = { fontPickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2C2230),
                                        contentColor = Color(0xFFF2AEC7)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (customPath != null) "Change" else "Import",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        if (idx < AppFont.entries.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.05f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
