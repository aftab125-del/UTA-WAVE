package com.uta.wave.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.player.EqualizerPreset
import com.uta.wave.player.WaveEqualizer
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.ObsidianBackground
import com.uta.wave.ui.theme.SoftViolet
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eqState by WaveEqualizer.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ACOUSTIC EQUALIZER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ElectricViolet
                        )
                        Text(
                            text = if (eqState.isEnabled) "DSP Effect Active" else "Equalizer Disabled",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = eqState.isEnabled,
                        onCheckedChange = { WaveEqualizer.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBackground,
                            checkedTrackColor = ElectricViolet,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceElevated
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency Response Curve Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ObsidianBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                EqualizerCurveCanvas(
                    bandGains = eqState.bandGains,
                    isEnabled = eqState.isEnabled,
                    accentColor = ElectricViolet,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Horizontal Scroll Row
            Text(
                text = "DSP PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WaveEqualizer.PRESETS.forEach { preset ->
                    val isSelected = eqState.presetName == preset.name
                    val chipBg = if (isSelected) ElectricViolet else SurfaceElevated
                    val chipText = if (isSelected) ObsidianBackground else TextSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(chipBg)
                            .border(1.dp, if (isSelected) ElectricViolet else CardBorder, RoundedCornerShape(20.dp))
                            .clickable {
                                WaveEqualizer.setPreset(preset.name)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = chipText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Mode Selector: Simple (3-Band) vs Advanced (10-Band)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val mode = eqState.mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (mode == 0) ElectricViolet else Color.Transparent)
                        .clickable { WaveEqualizer.setMode(0) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Simple (Bass / Mid / Treble)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (mode == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = if (mode == 0) ObsidianBackground else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (mode == 1) ElectricViolet else Color.Transparent)
                        .clickable { WaveEqualizer.setMode(1) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Advanced (10-Band Parametric)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (mode == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = if (mode == 1) ObsidianBackground else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sliders Section
            if (eqState.mode == 0) {
                // SIMPLE 3-BAND CONTROLS
                SimpleEqControls(bandGains = eqState.bandGains, isEnabled = eqState.isEnabled)
            } else {
                // ADVANCED 10-BAND PARAMETRIC CONTROLS
                Advanced10BandControls(bandGains = eqState.bandGains, isEnabled = eqState.isEnabled)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bass Boost & Virtualizer Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bass Boost",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${(eqState.bassBoost / 10)}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = ElectricViolet
                        )
                    }
                    Slider(
                        value = eqState.bassBoost.toFloat(),
                        onValueChange = { WaveEqualizer.setBassBoost(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = eqState.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricViolet,
                            activeTrackColor = ElectricViolet,
                            inactiveTrackColor = ObsidianBackground
                        )
                    )

                    // Virtualizer (Spatial Audio)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Virtualizer (Spatializer)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${(eqState.virtualizer / 10)}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = ElectricViolet
                        )
                    }
                    Slider(
                        value = eqState.virtualizer.toFloat(),
                        onValueChange = { WaveEqualizer.setVirtualizer(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = eqState.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricViolet,
                            activeTrackColor = ElectricViolet,
                            inactiveTrackColor = ObsidianBackground
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset Button
            Button(
                onClick = { WaveEqualizer.setPreset("FLAT") },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reset to Flat",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SimpleEqControls(
    bandGains: List<Float>,
    isEnabled: Boolean
) {
    val bassGain = (bandGains.getOrElse(0) { 0f } + bandGains.getOrElse(1) { 0f } + bandGains.getOrElse(2) { 0f }) / 3f
    val midGain = (bandGains.getOrElse(3) { 0f } + bandGains.getOrElse(4) { 0f } + bandGains.getOrElse(5) { 0f } + bandGains.getOrElse(6) { 0f }) / 4f
    val trebleGain = (bandGains.getOrElse(7) { 0f } + bandGains.getOrElse(8) { 0f } + bandGains.getOrElse(9) { 0f }) / 3f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SimpleBandSlider(
            label = "Bass (Low end)",
            subLabel = "31Hz - 125Hz",
            gainDb = bassGain,
            isEnabled = isEnabled,
            onValueChange = { newBass ->
                WaveEqualizer.setSimpleBands(newBass, midGain, trebleGain)
            }
        )

        SimpleBandSlider(
            label = "Mid (Vocals & Instruments)",
            subLabel = "250Hz - 2kHz",
            gainDb = midGain,
            isEnabled = isEnabled,
            onValueChange = { newMid ->
                WaveEqualizer.setSimpleBands(bassGain, newMid, trebleGain)
            }
        )

        SimpleBandSlider(
            label = "Treble (High end & Detail)",
            subLabel = "4kHz - 16kHz",
            gainDb = trebleGain,
            isEnabled = isEnabled,
            onValueChange = { newTreble ->
                WaveEqualizer.setSimpleBands(bassGain, midGain, newTreble)
            }
        )
    }
}

@Composable
private fun SimpleBandSlider(
    label: String,
    subLabel: String,
    gainDb: Float,
    isEnabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                Text(text = subLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextMuted)
            }
            Text(
                text = "${if (gainDb > 0f) "+" else ""}${String.format("%.1f", gainDb)} dB",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = ElectricViolet
            )
        }
        Slider(
            value = gainDb,
            onValueChange = onValueChange,
            valueRange = -12f..12f,
            enabled = isEnabled,
            colors = SliderDefaults.colors(
                thumbColor = ElectricViolet,
                activeTrackColor = ElectricViolet,
                inactiveTrackColor = ObsidianBackground
            )
        )
    }
}

@Composable
private fun Advanced10BandControls(
    bandGains: List<Float>,
    isEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        bandGains.forEachIndexed { index, gain ->
            val freq = WaveEqualizer.FREQUENCIES.getOrElse(index) { "${index}Band" }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = freq,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                    color = TextSecondary,
                    modifier = Modifier.width(46.dp)
                )

                Slider(
                    value = gain,
                    onValueChange = { WaveEqualizer.setBandGain(index, it) },
                    valueRange = -12f..12f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricViolet,
                        activeTrackColor = ElectricViolet,
                        inactiveTrackColor = ObsidianBackground
                    ),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${if (gain > 0f) "+" else ""}${String.format("%.1f", gain)} dB",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = ElectricViolet,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(48.dp)
                )
            }
        }
    }
}

@Composable
private fun EqualizerCurveCanvas(
    bandGains: List<Float>,
    isEnabled: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Draw 0dB centerline
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.dp.toPx()
        )

        // Draw +12dB and -12dB reference lines
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, h * 0.1f),
            end = Offset(w, h * 0.1f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, h * 0.9f),
            end = Offset(w, h * 0.9f),
            strokeWidth = 1.dp.toPx()
        )

        val points = mutableListOf<Offset>()
        val count = bandGains.size.coerceAtLeast(1)

        for (i in 0 until count) {
            val x = (i.toFloat() / (count - 1).coerceAtLeast(1)) * w
            val gain = if (isEnabled) bandGains[i].coerceIn(-12f, 12f) else 0f
            // gain: -12dB -> +12dB maps to 0.9h -> 0.1h
            val norm = (gain + 12f) / 24f // 0f .. 1f
            val y = h * (0.9f - (norm * 0.8f))
            points.add(Offset(x, y))
        }

        if (points.size >= 2) {
            val path = Path()
            path.moveTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlX = (p0.x + p1.x) / 2f
                path.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
            }

            // Fill gradient under curve
            val fillPath = Path()
            fillPath.addPath(path)
            fillPath.lineTo(w, h)
            fillPath.lineTo(0f, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        (if (isEnabled) accentColor else Color.Gray).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = if (isEnabled) accentColor else Color.Gray,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw point dots
            for (p in points) {
                drawCircle(
                    color = if (isEnabled) accentColor else Color.Gray,
                    radius = 3.dp.toPx(),
                    center = p
                )
            }
        }
    }
}
