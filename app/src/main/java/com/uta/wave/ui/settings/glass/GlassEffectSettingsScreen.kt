package com.uta.wave.ui.settings.glass

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.preferences.AppSettings
import com.uta.wave.data.preferences.UserPreferences
import com.uta.wave.ui.settings.theme.HsvColorPicker
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassEffectSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences.getInstance(context) }
    val settings by userPrefs.settingsFlow.collectAsState()

    val currentAccent = remember(settings.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(settings.accentColorHex))
        } catch (e: Exception) {
            ElectricViolet
        }
    }

    BackHandler {
        onBack()
    }

    // Dialog state controllers
    var showVibrancyDialog by remember { mutableStateOf(false) }
    var showBlurRadiusDialog by remember { mutableStateOf(false) }
    var showLensHeightDialog by remember { mutableStateOf(false) }
    var showLensAmountDialog by remember { mutableStateOf(false) }
    var showSurfaceOpacityDialog by remember { mutableStateOf(false) }
    var showSurfaceTintDialog by remember { mutableStateOf(false) }
    var showTextColorDialog by remember { mutableStateOf(false) }
    var showMiniplayerBackgroundDialog by remember { mutableStateOf(false) }
    var showMiniplayerProgressDialog by remember { mutableStateOf(false) }
    var showMiniplayerGlowDialog by remember { mutableStateOf(false) }
    var showNavBarOpacityDialog by remember { mutableStateOf(false) }
    var showNavBarBlurDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Liquid Glass & Optics",
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
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = ObsidianBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Permanent Engine Banner
            GlassSettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(currentAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = currentAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Liquid Glass Engine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = currentAccent.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, currentAccent.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Permanent",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = currentAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hardware-accelerated physical ray refraction, AGSL shaders & dynamic optical blur are active across all floating components.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // 2. Mini Player Customization Card
            GlassSectionHeader("Mini Player Optics & Styling")
            GlassSettingsCard {
                val bgLabel = when (settings.miniplayerBackgroundStyle) {
                    "LIQUID_GLASS" -> "Liquid Glass Refraction"
                    "GLOW_ANIMATED" -> "Breathing Neon Mesh"
                    "ACRYLIC_BLUR" -> "Frosted Acrylic Blur"
                    "SOLID_ELEVATED" -> "Solid Elevated Glass"
                    else -> "Liquid Glass Refraction"
                }
                GlassValueClickItem(
                    title = "Background Material",
                    subtitle = "Optic style for the floating pill surface",
                    valueText = bgLabel,
                    icon = Icons.Default.Layers,
                    accentColor = currentAccent,
                    onClick = { showMiniplayerBackgroundDialog = true }
                )

                GlassDivider()

                val progLabel = when (settings.miniplayerProgressStyle) {
                    "WAVE" -> "Live Waveform"
                    "LINEAR" -> "Continuous Line"
                    "NEON_TRACK" -> "Glowing Neon"
                    "HIDDEN" -> "Hidden"
                    else -> "Live Waveform"
                }
                GlassValueClickItem(
                    title = "Progress Indicator",
                    subtitle = "Track position feedback along bottom border",
                    valueText = progLabel,
                    icon = Icons.Default.GraphicEq,
                    accentColor = currentAccent,
                    onClick = { showMiniplayerProgressDialog = true }
                )

                GlassDivider()

                GlassValueClickItem(
                    title = "Mesh & Glow Intensity",
                    subtitle = "Glow radiance around miniplayer borders",
                    valueText = "${(settings.miniplayerGlowIntensity * 100).toInt()}%",
                    icon = Icons.Default.WbIncandescent,
                    accentColor = currentAccent,
                    onClick = { showMiniplayerGlowDialog = true }
                )
            }

            // 3. Floating Navigation Bar Customization Card
            GlassSectionHeader("Floating Navigation Bar")
            GlassSettingsCard {
                GlassToggleItem(
                    title = "Compact Dynamic Capsule",
                    subtitle = if (settings.navBarPillCompact) "Compact dynamic capsule layout" else "Full-width stadium floating bar",
                    icon = Icons.Default.Navigation,
                    accentColor = currentAccent,
                    checked = settings.navBarPillCompact,
                    onCheckedChange = { userPrefs.updateNavBarPillCompact(it) }
                )

                GlassDivider()

                GlassToggleItem(
                    title = "Auto-Hide on Feed Scroll",
                    subtitle = "Glide bottom navigation away smoothly when scrolling feeds",
                    icon = Icons.Default.SwapVert,
                    accentColor = currentAccent,
                    checked = settings.navBarAutoHideOnScroll,
                    onCheckedChange = { userPrefs.updateNavBarAutoHideOnScroll(it) }
                )

                GlassDivider()

                GlassValueClickItem(
                    title = "Glass Surface Opacity",
                    subtitle = "Translucency level of floating bottom pill",
                    valueText = "${(settings.navBarGlassOpacity * 100).toInt()}%",
                    icon = Icons.Default.Contrast,
                    accentColor = currentAccent,
                    onClick = { showNavBarOpacityDialog = true }
                )

                GlassDivider()

                GlassValueClickItem(
                    title = "Frosted Blur Depth",
                    subtitle = "Diffusive blur for backdrop underneath navigation",
                    valueText = "${settings.navBarBlurRadius.toInt()} dp",
                    icon = Icons.Default.BlurOn,
                    accentColor = currentAccent,
                    onClick = { showNavBarBlurDialog = true }
                )
            }

            // 4. Optical Shaders & Lens Refraction
            GlassSectionHeader("Optical Shaders & Lens Refraction")
            GlassSettingsCard {
                GlassValueClickItem(
                            title = "Backdrop Vibrancy",
                            subtitle = "Saturation boost for artwork refracted beneath the glass",
                            valueText = String.format("%.2fx", settings.liquidGlassVibrancy),
                            icon = Icons.Default.Tune,
                            accentColor = currentAccent,
                            onClick = { showVibrancyDialog = true }
                        )

                        GlassDivider()

                        GlassValueClickItem(
                            title = "Backdrop Blur Radius",
                            subtitle = "Diffusive depth-of-field blur applied to sampling surface",
                            valueText = "${settings.liquidGlassBlurRadius.toInt()} dp",
                            icon = Icons.Default.BlurOn,
                            accentColor = currentAccent,
                            onClick = { showBlurRadiusDialog = true }
                        )

                        GlassDivider()

                        GlassValueClickItem(
                            title = "Lens Height",
                            subtitle = "Physical curvature bevel height along pill borders",
                            valueText = String.format("%.2f", settings.liquidGlassLensHeight),
                            icon = Icons.Default.Layers,
                            accentColor = currentAccent,
                            onClick = { showLensHeightDialog = true }
                        )

                        GlassDivider()

                        GlassValueClickItem(
                            title = "Lens Refraction Amount",
                            subtitle = "Signed distance field optical ray displacement strength",
                            valueText = String.format("%.2f", settings.liquidGlassLensAmount),
                            icon = Icons.Default.InvertColors,
                            accentColor = currentAccent,
                            onClick = { showLensAmountDialog = true }
                        )

                        GlassDivider()

                        GlassToggleItem(
                            title = "Chromatic Aberration",
                            subtitle = "7-band rainbow spectral fringing at capsule contours",
                            icon = Icons.Default.Palette,
                            accentColor = currentAccent,
                            checked = settings.liquidGlassChromaticAberration,
                            onCheckedChange = { userPrefs.updateLiquidGlassChromaticAberration(it) }
                        )

                        GlassDivider()

                        GlassToggleItem(
                            title = "Depth Effect",
                            subtitle = "3D parallax optical depth mapping behind edges",
                            icon = Icons.Default.Visibility,
                            accentColor = currentAccent,
                            checked = settings.liquidGlassDepthEffect,
                            onCheckedChange = { userPrefs.updateLiquidGlassDepthEffect(it) }
                        )
                    }

                    // Appearance & Color Tint
                    GlassSectionHeader("Surface Appearance & Tint")
                    GlassSettingsCard {
                        GlassColorItem(
                            title = "Surface Tint Color",
                            subtitle = if (settings.liquidGlassSurfaceTintColorInt == 0) "Theme Adaptive (Dark Obsidian / Clean White)" else "Custom Color Tint",
                            color = if (settings.liquidGlassSurfaceTintColorInt == 0) Color(0xFF1E1522) else Color(settings.liquidGlassSurfaceTintColorInt),
                            isAdaptive = settings.liquidGlassSurfaceTintColorInt == 0,
                            accentColor = currentAccent,
                            onClick = { showSurfaceTintDialog = true }
                        )

                        GlassDivider()

                        GlassValueClickItem(
                            title = "Surface Tint Opacity",
                            subtitle = "Base material opacity blending over refracted backdrop",
                            valueText = "${(settings.liquidGlassSurfaceOpacity * 100).toInt()}%",
                            icon = Icons.Default.Contrast,
                            accentColor = currentAccent,
                            onClick = { showSurfaceOpacityDialog = true }
                        )

                        GlassDivider()

                        GlassColorItem(
                            title = "Text & Icon Color",
                            subtitle = if (settings.liquidGlassTextColorInt == 0) "Dynamic High-Contrast (Auto Black/White)" else "Custom Override",
                            color = if (settings.liquidGlassTextColorInt == 0) Color.White else Color(settings.liquidGlassTextColorInt),
                            isAdaptive = settings.liquidGlassTextColorInt == 0,
                            accentColor = currentAccent,
                            onClick = { showTextColorDialog = true }
                        )
                    }

                    // Target Component Toggles
                    GlassSectionHeader("Active Glass Surfaces")
                    GlassSettingsCard {
                        GlassToggleItem(
                            title = "Floating Navigation Bar",
                            subtitle = "Liquid glass pill tabs & circular search FAB",
                            icon = Icons.Default.Navigation,
                            accentColor = currentAccent,
                            checked = settings.liquidGlassNavBarEnabled,
                            onCheckedChange = { userPrefs.updateLiquidGlassNavBarEnabled(it) }
                        )

                        GlassDivider()

                        GlassToggleItem(
                            title = "Floating Mini Player",
                            subtitle = "Docked capsule pill above bottom navigation",
                            icon = Icons.Default.Layers,
                            accentColor = currentAccent,
                            checked = settings.liquidGlassMiniPlayerEnabled,
                            onCheckedChange = { userPrefs.updateLiquidGlassMiniPlayerEnabled(it) }
                        )

                        GlassDivider()

                        GlassToggleItem(
                            title = "Fullscreen Player Sheet",
                            subtitle = "Deep-blurred atmospheric backdrop behind player sheet",
                            icon = Icons.Default.AutoAwesome,
                            accentColor = currentAccent,
                            checked = settings.liquidGlassPlayerEnabled,
                            onCheckedChange = { userPrefs.updateLiquidGlassPlayerEnabled(it) }
                        )
                    }

                    // Reset to Apple Defaults Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1F1728))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .clickable {
                                userPrefs.resetLiquidGlassToDefaults()
                                Toast.makeText(context, "Reset to Apple liquid glass recipe!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = currentAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Reset to Apple Recipe Defaults",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- Slider & Picker Dialogs ---

    // 1. Vibrancy Dialog
    if (showVibrancyDialog) {
        var tempValue by remember { mutableFloatStateOf(settings.liquidGlassVibrancy) }
        GlassSliderDialog(
            title = "Backdrop Vibrancy",
            currentValueText = String.format("%.2fx", tempValue),
            value = tempValue,
            valueRange = 0f..2f,
            onValueChange = { tempValue = it },
            onReset = { tempValue = 1.0f },
            onDismiss = { showVibrancyDialog = false },
            onConfirm = {
                userPrefs.updateLiquidGlassVibrancy(tempValue)
                showVibrancyDialog = false
            }
        )
    }

    // 2. Blur Radius Dialog
    if (showBlurRadiusDialog) {
        var tempValue by remember { mutableFloatStateOf(settings.liquidGlassBlurRadius) }
        GlassSliderDialog(
            title = "Blur Radius",
            currentValueText = "${tempValue.toInt()} dp",
            value = tempValue,
            valueRange = 0f..32f,
            onValueChange = { tempValue = it },
            onReset = { tempValue = 8.0f },
            onDismiss = { showBlurRadiusDialog = false },
            onConfirm = {
                userPrefs.updateLiquidGlassBlurRadius(tempValue)
                showBlurRadiusDialog = false
            }
        )
    }

    // 3. Lens Height Dialog
    if (showLensHeightDialog) {
        var tempValue by remember { mutableFloatStateOf(settings.liquidGlassLensHeight) }
        GlassSliderDialog(
            title = "Lens Height",
            currentValueText = String.format("%.2f", tempValue),
            value = tempValue,
            valueRange = 0f..1f,
            onValueChange = { tempValue = it },
            onReset = { tempValue = 0.5f },
            onDismiss = { showLensHeightDialog = false },
            onConfirm = {
                userPrefs.updateLiquidGlassLensHeight(tempValue)
                showLensHeightDialog = false
            }
        )
    }

    // 4. Lens Amount Dialog
    if (showLensAmountDialog) {
        var tempValue by remember { mutableFloatStateOf(settings.liquidGlassLensAmount) }
        GlassSliderDialog(
            title = "Lens Refraction Amount",
            currentValueText = String.format("%.2f", tempValue),
            value = tempValue,
            valueRange = 0f..1f,
            onValueChange = { tempValue = it },
            onReset = { tempValue = 0.5f },
            onDismiss = { showLensAmountDialog = false },
            onConfirm = {
                userPrefs.updateLiquidGlassLensAmount(tempValue)
                showLensAmountDialog = false
            }
        )
    }

    // 5. Surface Opacity Dialog
    if (showSurfaceOpacityDialog) {
        var tempValue by remember { mutableFloatStateOf(settings.liquidGlassSurfaceOpacity) }
        GlassSliderDialog(
            title = "Surface Tint Opacity",
            currentValueText = "${(tempValue * 100).toInt()}%",
            value = tempValue,
            valueRange = 0f..1f,
            onValueChange = { tempValue = it },
            onReset = { tempValue = 0.4f },
            onDismiss = { showSurfaceOpacityDialog = false },
            onConfirm = {
                userPrefs.updateLiquidGlassSurfaceOpacity(tempValue)
                showSurfaceOpacityDialog = false
            }
        )
    }

    // 6. Surface Tint Color Dialog
    if (showSurfaceTintDialog) {
        var selectedColor by remember {
            mutableStateOf(
                if (settings.liquidGlassSurfaceTintColorInt != 0)
                    Color(settings.liquidGlassSurfaceTintColorInt)
                else
                    Color(0xFF1E1522)
            )
        }
        AlertDialog(
            onDismissRequest = { showSurfaceTintDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Surface Tint Color",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Customize the overlay glass tone or restore theme-adaptive mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    HsvColorPicker(
                        initialColor = selectedColor,
                        onColorCommit = { selectedColor = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    userPrefs.updateLiquidGlassSurfaceTintColorInt(selectedColor.toArgb())
                    showSurfaceTintDialog = false
                }) {
                    Text("Apply", color = currentAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        userPrefs.updateLiquidGlassSurfaceTintColorInt(0) // 0 = Adaptive
                        showSurfaceTintDialog = false
                    }) {
                        Text("Reset Adaptive", color = TextMuted)
                    }
                    TextButton(onClick = { showSurfaceTintDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            }
        )
    }

    // 7. Text & Icon Color Dialog
    if (showTextColorDialog) {
        var selectedColor by remember {
            mutableStateOf(
                if (settings.liquidGlassTextColorInt != 0)
                    Color(settings.liquidGlassTextColorInt)
                else
                    Color.White
            )
        }
        AlertDialog(
            onDismissRequest = { showTextColorDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Text & Icon Color",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Set a custom color tint for icons and text over glass, or keep automatic contrast.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    HsvColorPicker(
                        initialColor = selectedColor,
                        onColorCommit = { selectedColor = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    userPrefs.updateLiquidGlassTextColorInt(selectedColor.toArgb())
                    showTextColorDialog = false
                }) {
                    Text("Apply", color = currentAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        userPrefs.updateLiquidGlassTextColorInt(0) // 0 = Auto contrast
                        showTextColorDialog = false
                    }) {
                        Text("Reset Auto", color = TextMuted)
                    }
                    TextButton(onClick = { showTextColorDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            }
        )
    }

    // 8. MiniPlayer Background Style Dialog
    if (showMiniplayerBackgroundDialog) {
        val bgOptions = listOf(
            Pair("LIQUID_GLASS", Pair("Liquid Glass Refraction", "AGSL optical lens refraction & real-time backdrop blur")),
            Pair("GLOW_ANIMATED", Pair("Breathing Neon Mesh", "Dynamic breathing album artwork ambient glow")),
            Pair("ACRYLIC_BLUR", Pair("Frosted Acrylic Blur", "High-diffusion smooth frosted acrylic depth")),
            Pair("SOLID_ELEVATED", Pair("Solid Elevated Glass", "Elevated dark card with subtle refractive rim"))
        )
        GlassSelectionDialog(
            title = "MiniPlayer Background Style",
            options = bgOptions,
            selected = settings.miniplayerBackgroundStyle,
            accentColor = currentAccent,
            onDismiss = { showMiniplayerBackgroundDialog = false },
            onSelect = {
                userPrefs.updateMiniplayerBackgroundStyle(it)
                showMiniplayerBackgroundDialog = false
            }
        )
    }

    // 9. MiniPlayer Progress Style Dialog
    if (showMiniplayerProgressDialog) {
        val progressOptions = listOf(
            Pair("WAVE", Pair("Live Waveform Track", "Real-time squiggly waveform reacting to playback")),
            Pair("LINEAR", Pair("Continuous Sleek Line", "Minimalist 2.5dp continuous progress line")),
            Pair("NEON_TRACK", Pair("Glowing Neon Bar", "Vibrant accent gradient with glowing head dot")),
            Pair("HIDDEN", Pair("Hidden Edge-to-Edge", "Clean, uninterrupted gesture surface"))
        )
        GlassSelectionDialog(
            title = "MiniPlayer Progress Bar",
            options = progressOptions,
            selected = settings.miniplayerProgressStyle,
            accentColor = currentAccent,
            onDismiss = { showMiniplayerProgressDialog = false },
            onSelect = {
                userPrefs.updateMiniplayerProgressStyle(it)
                showMiniplayerProgressDialog = false
            }
        )
    }

    // 10. MiniPlayer Glow Intensity Dialog
    if (showMiniplayerGlowDialog) {
        var tempValue by remember { mutableFloatStateOf(settings.miniplayerGlowIntensity) }
        GlassSliderDialog(
            title = "MiniPlayer Glow Intensity",
            currentValueText = "${(tempValue * 100).toInt()}%",
            value = tempValue,
            valueRange = 0f..1f,
            onValueChange = { tempValue = it },
            onReset = { tempValue = 0.8f },
            onDismiss = { showMiniplayerGlowDialog = false },
            onConfirm = {
                userPrefs.updateMiniplayerGlowIntensity(tempValue)
                showMiniplayerGlowDialog = false
            }
        )
    }

    // 11. Nav Bar Opacity Dialog
    if (showNavBarOpacityDialog) {
        var tempValue by remember { mutableFloatStateOf(settings.navBarGlassOpacity) }
        GlassSliderDialog(
            title = "Navigation Bar Glass Opacity",
            currentValueText = "${(tempValue * 100).toInt()}%",
            value = tempValue,
            valueRange = 0.4f..1.0f,
            onValueChange = { tempValue = it },
            onReset = { tempValue = 0.85f },
            onDismiss = { showNavBarOpacityDialog = false },
            onConfirm = {
                userPrefs.updateNavBarGlassOpacity(tempValue)
                showNavBarOpacityDialog = false
            }
        )
    }

    // 12. Nav Bar Blur Dialog
    if (showNavBarBlurDialog) {
        var tempValue by remember { mutableFloatStateOf(settings.navBarBlurRadius) }
        GlassSliderDialog(
            title = "Navigation Bar Blur Radius",
            currentValueText = "${tempValue.toInt()} dp",
            value = tempValue,
            valueRange = 8f..40f,
            onValueChange = { tempValue = it },
            onReset = { tempValue = 24.0f },
            onDismiss = { showNavBarBlurDialog = false },
            onConfirm = {
                userPrefs.updateNavBarBlurRadius(tempValue)
                showNavBarBlurDialog = false
            }
        )
    }
}

@Composable
private fun GlassSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun GlassSettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1522))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun GlassDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.05f))
    )
}

@Composable
private fun GlassToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun GlassValueClickItem(
    title: String,
    subtitle: String,
    valueText: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = accentColor
            )
        }
    }
}

@Composable
private fun GlassColorItem(
    title: String,
    subtitle: String,
    color: Color,
    isAdaptive: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isAdaptive) {
                    Icon(
                        imageVector = Icons.Default.Contrast,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isAdaptive) "Adaptive" else "Custom",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = accentColor
            )
        }
    }
}

@Composable
private fun GlassSliderDialog(
    title: String,
    currentValueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = currentValueText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricViolet
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = ElectricViolet,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save", color = ElectricViolet, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text("Reset", color = TextMuted)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMuted)
                }
            }
        }
    )
}

@Composable
private fun <T> GlassSelectionDialog(
    title: String,
    options: List<Pair<T, Pair<String, String>>>,
    selected: T,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, details) ->
                    val isCurrent = value == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (isCurrent) accentColor.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { onSelect(value) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = details.first,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isCurrent) accentColor else Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = details.second,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
