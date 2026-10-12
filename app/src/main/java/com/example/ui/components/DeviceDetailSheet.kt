package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WizDevice
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.Slate400
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ColorPreset(val name: String, val r: Int, val g: Int, val b: Int, val displayColor: Color)

val PRESET_COLORS = listOf(
    ColorPreset("Warmweiß", 255, 214, 170, Color(0xFFFFD6AA)),
    ColorPreset("Tageslicht", 255, 244, 229, Color(0xFFFFF4E5)),
    ColorPreset("Bernstein", 255, 183, 3, Color(0xFFFFB703)),
    ColorPreset("Sonnenuntergang", 251, 133, 0, Color(0xFFFB8500)),
    ColorPreset("Rubinrot", 239, 68, 68, Color(0xFFEF4444)),
    ColorPreset("Koralle", 249, 115, 22, Color(0xFFF97316)),
    ColorPreset("Smaragd", 16, 185, 129, Color(0xFF10B981)),
    ColorPreset("Türkis", 6, 182, 212, Color(0xFF06B6D4)),
    ColorPreset("Ozeanblau", 59, 130, 246, Color(0xFF3B82F6)),
    ColorPreset("Purpur", 168, 85, 247, Color(0xFFA855F7)),
    ColorPreset("Magenta", 236, 72, 153, Color(0xFFEC4899))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceDetailSheet(
    device: WizDevice,
    slidingBrightness: Int?,
    isHidden: Boolean = false,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onColorChange: (Int, Int, Int) -> Unit,
    onToggleHidden: () -> Unit = {},
    canControl: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTechDetails by remember { mutableStateOf(false) }

    val isOn = device.isOnline && device.isTurnedOn
    val currentBrightness = slidingBrightness ?: device.brightnessPercent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("device_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = device.roomName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_detail_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large Circular Power Button
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOn) AmberWarm.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 2.dp,
                        color = if (isOn) AmberWarm else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable(enabled = device.isOnline && canControl, onClick = onToggle)
                    .testTag("detail_sheet_power_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = "Schalten",
                    tint = if (isOn) AmberWarm else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when {
                    !device.isOnline -> "Offline (nicht erreichbar)"
                    !canControl -> if (isOn) "Eingeschaltet (Nur Lesezugriff)" else "Ausgeschaltet (Nur Lesezugriff)"
                    isOn -> "Eingeschaltet"
                    else -> "Ausgeschaltet"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        !device.isOnline -> ErrorRed
                        isOn -> AmberWarm
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            )

            // Brightness Control
            if (device.isOnline && device.supportsDimming) {
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (!canControl) "Helligkeit (Nur Lesezugriff)" else "Helligkeit",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "$currentBrightness%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarm
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = currentBrightness.toFloat(),
                            onValueChange = { if (canControl) onBrightnessChange(it.toInt()) },
                            enabled = canControl,
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = AmberWarm,
                                activeTrackColor = AmberWarm,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // RGB Color Picker (Only if device supports RGB!)
            if (device.isOnline && device.supportsRgb) {
                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (!canControl) "Lichtfarbe (Nur Lesezugriff)" else "Lichtfarbe & Farbstimmung",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PRESET_COLORS.forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(preset.displayColor)
                                        .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                        .clickable(enabled = canControl) {
                                            onColorChange(preset.r, preset.g, preset.b)
                                        }
                                        .testTag("color_preset_${preset.name}")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Device Visibility Setting Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "In Geräteübersicht ausblenden",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bleibt in Räumen und Gruppen weiterhin sichtbar und steuerbar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isHidden,
                        onCheckedChange = { onToggleHidden() },
                        modifier = Modifier.testTag("device_visibility_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Collapsible Technical Info Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showTechDetails = !showTechDetails },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Geräteinformationen",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (showTechDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Details aufklappen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = showTechDetails) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow(label = "IP-Adresse", value = device.ip)
                        InfoRow(label = "MAC-Adresse", value = device.mac)
                        InfoRow(label = "Gerätetyp", value = device.deviceType)
                        device.moduleName?.let { InfoRow(label = "Modulname", value = it) }
                        device.firmwareVersion?.let { InfoRow(label = "Firmware", value = it) }
                        device.rssi?.let {
                            val signalQuality = when {
                                it >= -60 -> "Ausgezeichnet ($it dBm)"
                                it >= -75 -> "Gut ($it dBm)"
                                it >= -85 -> "Mäßig ($it dBm)"
                                else -> "Schwach ($it dBm)"
                            }
                            InfoRow(label = "WLAN-Signal (RSSI)", value = signalQuality)
                        }
                        val syncDate = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY).format(Date(device.lastSeenTimestamp))
                        InfoRow(label = "Letzte Aktualisierung", value = syncDate)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
