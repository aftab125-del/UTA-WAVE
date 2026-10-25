package com.uta.wave.ui.settings.icon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropOriginal
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.preferences.UserPreferences
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences.getInstance(context) }
    val settings by prefs.settingsFlow.collectAsState()

    var customImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedShape by remember { mutableStateOf(settings.customIconShape) }
    var appLabel by remember { mutableStateOf(settings.customIconLabel) }
    var selectedPresetId by remember { mutableStateOf(settings.selectedIconPreset) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    BackHandler {
        onBack()
    }

    // Update live preview bitmap whenever image, shape, or preset changes
    LaunchedEffect(customImageUri, selectedShape, selectedPresetId) {
        if (customImageUri != null) {
            previewBitmap = AppIconHelper.processGalleryImageToIcon(
                context = context,
                imageUri = customImageUri!!,
                shape = selectedShape,
                size = 512
            )
        } else if (!settings.customIconPath.isNullOrBlank() && File(settings.customIconPath!!).exists() && selectedPresetId == "CUSTOM") {
            try {
                previewBitmap = BitmapFactory.decodeFile(settings.customIconPath)
            } catch (e: Exception) {
                // fallback
            }
        } else {
            val preset = AppIconHelper.PRESETS.find { it.id == selectedPresetId }
                ?: AppIconHelper.PRESETS.first()
            previewBitmap = AppIconHelper.generatePresetIcon(preset, selectedShape, 512)
        }
    }

    // Photo gallery picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customImageUri = uri
            selectedPresetId = "CUSTOM"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Icon & Branding",
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
            // 1. HERO PREVIEW: Phone Home Screen Mockup
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color(0xFFF2AEC7),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "HOME SCREEN LAUNCHER PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFFF2AEC7)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // The Custom Icon with drop shadow
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFF2AEC7).copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = "Custom Icon Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF351A2E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropOriginal,
                                contentDescription = null,
                                tint = Color(0xFFF2AEC7),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The App Label underneath
                Text(
                    text = appLabel.ifBlank { "UTA-wave" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // -------------------------------------------------------------
            // 2. CUSTOM GALLERY IMAGE SECTION
            // -------------------------------------------------------------
            Column {
                SectionTitle("CUSTOM IMAGE FROM GALLERY")
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1522))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Select any personal photo, anime character, or custom logo from your device to use as your app icon.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = Color.White.copy(alpha = 0.70f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF2AEC7),
                                contentColor = Color(0xFF3B1528)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (customImageUri != null) "Change Photo" else "Choose Photo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }

                        if (customImageUri != null || selectedPresetId == "CUSTOM") {
                            OutlinedButton(
                                onClick = {
                                    customImageUri = null
                                    selectedPresetId = "DEFAULT"
                                    prefs.updateCustomAppIcon(null, appLabel, selectedShape)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF43F5E)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset")
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. APP DISPLAY NAME FIELD
            // -------------------------------------------------------------
            Column {
                SectionTitle("HOME SCREEN APP NAME")
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = appLabel,
                    onValueChange = { appLabel = it.take(25) },
                    placeholder = { Text("UTA-wave", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF2AEC7),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color(0xFF1E1522),
                        unfocusedContainerColor = Color(0xFF1E1522)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // -------------------------------------------------------------
            // 4. ICON SHAPE SELECTOR
            // -------------------------------------------------------------
            Column {
                SectionTitle("ICON SHAPE")
                Spacer(modifier = Modifier.height(8.dp))

                val shapes = listOf(
                    Pair("SQUIRCLE", "Squircle"),
                    Pair("CIRCLE", "Circle"),
                    Pair("ROUNDED", "Rounded")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    shapes.forEach { (shapeKey, shapeLabel) ->
                        val isSelected = selectedShape.equals(shapeKey, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFF2E1C2C) else Color(0xFF1E1522))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFF2AEC7) else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedShape = shapeKey }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = when (shapeKey) {
                                        "CIRCLE" -> Icons.Default.RadioButtonUnchecked
                                        "ROUNDED" -> Icons.Default.RoundedCorner
                                        else -> Icons.Default.CropOriginal
                                    },
                                    contentDescription = shapeLabel,
                                    tint = if (isSelected) Color(0xFFF2AEC7) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = shapeLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = if (isSelected) Color(0xFFF2AEC7) else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. CURATED PRESETS PACK
            // -------------------------------------------------------------
            Column {
                SectionTitle("PRESET ICON PACKS")
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1522))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                ) {
                    AppIconHelper.PRESETS.forEachIndexed { idx, preset ->
                        val isSelected = selectedPresetId == preset.id && customImageUri == null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    customImageUri = null
                                    selectedPresetId = preset.id
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini gradient preview circle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(preset.startColor), Color(preset.endColor))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.accentColor))
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFF2AEC7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (idx < AppIconHelper.PRESETS.size - 1) {
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

            // -------------------------------------------------------------
            // 6. IN-APP BRANDING TOGGLE
            // -------------------------------------------------------------
            Column {
                SectionTitle("IN-APP LOGO BRANDING")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1522))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display icon inside app header",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Replaces the default top bar logo with your customized icon",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }

                    Switch(
                        checked = settings.useCustomIconInApp,
                        onCheckedChange = { prefs.updateUseCustomIconInApp(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFF2AEC7),
                            uncheckedThumbColor = Color(0xFF8E7D8A),
                            uncheckedTrackColor = Color(0xFF2C2230)
                        )
                    )
                }
            }

            // -------------------------------------------------------------
            // 7. HERO ACTION: ADD TO HOME SCREEN
            // -------------------------------------------------------------
            Button(
                onClick = {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        // 1. Save icon to app internal storage for persistence
                        val savedFile = AppIconHelper.saveIconFile(context, bmp)
                        if (savedFile != null) {
                            prefs.updateCustomAppIcon(savedFile.absolutePath, appLabel, selectedShape)
                            prefs.updateSelectedIconPreset(selectedPresetId)
                        }

                        // 2. Request pin to home screen
                        val pinned = AppIconHelper.pinToHomeScreen(context, bmp, appLabel)
                        if (pinned) {
                            Toast.makeText(
                                context,
                                "Custom icon pinned to Home Screen! Check your launcher.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Custom icon saved for in-app branding! (Launcher pin not supported on this device)",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(context, "No icon preview generated", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF2AEC7),
                    contentColor = Color(0xFF351225)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Add Custom Icon to Home Screen",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
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
