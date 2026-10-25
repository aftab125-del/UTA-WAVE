package com.uta.wave.ui.components

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

enum class DeviceTypeCategory {
    BLUETOOTH,
    WIRED_HEADPHONES,
    BUILTIN_SPEAKER,
    USB_AUDIO,
    CAST,
    OTHER
}

data class DiscoveredAudioDevice(
    val id: Int,
    val name: String,
    val type: DeviceTypeCategory,
    val isCurrent: Boolean,
    val rawDeviceInfo: AudioDeviceInfo? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDeviceBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentVol by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol.toFloat())
    }

    var devices by remember { mutableStateOf<List<DiscoveredAudioDevice>>(emptyList()) }

    fun refreshDevices() {
        val list = mutableListOf<DiscoveredAudioDevice>()
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val activeCommunicationDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice
        } else null

        outputDevices.forEach { dev ->
            val (category, icon) = categorizeDevice(dev.type)
            val isCurrent = if (activeCommunicationDevice != null) {
                dev.id == activeCommunicationDevice.id
            } else {
                // Heuristic: Bluetooth if music is routed to BT, else speaker/headphone
                if (audioManager.isBluetoothA2dpOn && category == DeviceTypeCategory.BLUETOOTH) true
                else if (audioManager.isWiredHeadsetOn && category == DeviceTypeCategory.WIRED_HEADPHONES) true
                else category == DeviceTypeCategory.BUILTIN_SPEAKER
            }

            val name = when {
                !dev.productName.isNullOrBlank() -> dev.productName.toString()
                category == DeviceTypeCategory.BLUETOOTH -> "Bluetooth Device (${dev.id})"
                category == DeviceTypeCategory.WIRED_HEADPHONES -> "Wired Headphones"
                category == DeviceTypeCategory.BUILTIN_SPEAKER -> "Phone Speaker"
                category == DeviceTypeCategory.USB_AUDIO -> "USB Audio / DAC"
                else -> "Audio Output (${dev.id})"
            }

            list.add(
                DiscoveredAudioDevice(
                    id = dev.id,
                    name = name,
                    type = category,
                    isCurrent = isCurrent,
                    rawDeviceInfo = dev
                )
            )
        }

        // Deduplicate devices by name and type
        devices = list.distinctBy { "${it.name}_${it.type}" }
    }

    LaunchedEffect(Unit) {
        refreshDevices()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speaker,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AUDIO ROUTING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ElectricViolet
                        )
                        Text(
                            text = "Output Devices",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Volume Scrub Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Media Volume",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${(currentVol * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = ElectricViolet
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (currentVol > 0f) {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                                    currentVol = 0f
                                } else {
                                    val mid = (maxVol / 2).coerceAtLeast(1)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mid, 0)
                                    currentVol = mid.toFloat() / maxVol.toFloat()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (currentVol <= 0.001f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeDown,
                                contentDescription = "Mute Toggle",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Slider(
                            value = currentVol,
                            onValueChange = { frac ->
                                currentVol = frac
                                val step = (frac * maxVol).toInt().coerceIn(0, maxVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, step, 0)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricViolet,
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = ObsidianBackground
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Output Devices List
            Text(
                text = "AVAILABLE OUTPUTS (${devices.size})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hardware output devices detected.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    devices.forEachIndexed { index, device ->
                        AudioDeviceRow(
                            device = device,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.rawDeviceInfo != null) {
                                    val success = audioManager.setCommunicationDevice(device.rawDeviceInfo)
                                    if (success) {
                                        Toast.makeText(context, "Routed audio to: ${device.name}", Toast.LENGTH_SHORT).show()
                                        refreshDevices()
                                    } else {
                                        Toast.makeText(context, "Could not set ${device.name} as active output", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Active: ${device.name}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        if (index < devices.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(CardBorder)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Android System Output Switcher Button
            Button(
                onClick = {
                    try {
                        val panelIntent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                            putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(panelIntent)
                    } catch (e: Exception) {
                        try {
                            val btIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(btIntent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, "Cannot open device sound settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open System Media Switcher",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AudioDeviceRow(
    device: DiscoveredAudioDevice,
    onClick: () -> Unit
) {
    val icon = when (device.type) {
        DeviceTypeCategory.BLUETOOTH -> Icons.Default.Bluetooth
        DeviceTypeCategory.WIRED_HEADPHONES -> Icons.Default.Headphones
        DeviceTypeCategory.BUILTIN_SPEAKER -> Icons.Default.PhoneAndroid
        DeviceTypeCategory.USB_AUDIO -> Icons.Default.Usb
        DeviceTypeCategory.CAST -> Icons.Default.Cast
        else -> Icons.Default.Speaker
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (device.isCurrent) ElectricViolet.copy(alpha = 0.2f) else ObsidianBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (device.isCurrent) ElectricViolet else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (device.isCurrent) ElectricViolet else TextPrimary
            )
            Text(
                text = if (device.isCurrent) "Active Output" else "Connected",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = if (device.isCurrent) ElectricViolet.copy(alpha = 0.8f) else TextMuted
            )
        }

        if (device.isCurrent) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ElectricViolet),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active",
                    tint = ObsidianBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun categorizeDevice(type: Int): Pair<DeviceTypeCategory, ImageVector> {
    return when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> Pair(DeviceTypeCategory.BLUETOOTH, Icons.Default.Bluetooth)

        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> Pair(DeviceTypeCategory.WIRED_HEADPHONES, Icons.Default.Headphones)

        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> Pair(DeviceTypeCategory.BUILTIN_SPEAKER, Icons.Default.PhoneAndroid)

        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> Pair(DeviceTypeCategory.USB_AUDIO, Icons.Default.Usb)

        AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> Pair(DeviceTypeCategory.CAST, Icons.Default.Cast)

        else -> Pair(DeviceTypeCategory.OTHER, Icons.Default.Speaker)
    }
}
