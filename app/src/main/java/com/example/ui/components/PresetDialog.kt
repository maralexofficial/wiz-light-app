package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WizDevice
import com.example.data.model.WizGroup
import com.example.data.model.WizRoom
import com.example.ui.theme.AmberWarm

data class PresetIconOption(val id: String, val label: String, val icon: ImageVector)

val PRESET_ICON_OPTIONS = listOf(
    PresetIconOption("lightbulb", "Licht", Icons.Default.Lightbulb),
    PresetIconOption("night", "Nacht", Icons.Default.Bedtime),
    PresetIconOption("movie", "Film", Icons.Default.Movie),
    PresetIconOption("work", "Arbeit", Icons.Default.Work),
    PresetIconOption("star", "Favorit", Icons.Default.Star)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePresetDialog(
    groups: List<WizGroup>,
    devices: List<WizDevice>,
    rooms: List<WizRoom>,
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        icon: String,
        targetType: String,
        targetId: String,
        targetName: String?,
        state: Boolean,
        dimming: Int,
        temperature: Int?,
        r: Int?,
        g: Int?,
        b: Int?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("lightbulb") }
    var targetType by remember { mutableStateOf(if (groups.isNotEmpty()) "group" else "device") }
    var selectedTargetId by remember(targetType) {
        mutableStateOf(
            if (targetType == "group") groups.firstOrNull()?.id ?: ""
            else devices.firstOrNull()?.mac?.ifBlank { devices.firstOrNull()?.ip } ?: ""
        )
    }
    var targetExpanded by remember { mutableStateOf(false) }
    var stateOn by remember { mutableStateOf(true) }
    var dimming by remember { mutableIntStateOf(80) }
    var selectedColorIndex by remember { mutableStateOf<Int?>(0) } // Warmweiß default

    val isFormValid = name.isNotBlank() && selectedTargetId.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Neues Quick-Preset erstellen",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Preset Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name des Presets (z. B. 'Filmabend')") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )

                // Icon Selection
                Text(
                    text = "Symbol",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_ICON_OPTIONS.forEach { opt ->
                        FilterChip(
                            selected = selectedIcon == opt.id,
                            onClick = { selectedIcon = opt.id },
                            label = { Text(opt.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = opt.label,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberWarm.copy(alpha = 0.2f),
                                selectedLabelColor = AmberWarm
                            )
                        )
                    }
                }

                // Target Type: Group vs Device
                Text(
                    text = "Zieltyp",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetType == "group",
                        onClick = {
                            targetType = "group"
                            selectedTargetId = groups.firstOrNull()?.id ?: ""
                        },
                        label = { Text("Gruppe (${groups.size})") },
                        enabled = groups.isNotEmpty()
                    )
                    FilterChip(
                        selected = targetType == "device",
                        onClick = {
                            targetType = "device"
                            selectedTargetId = devices.firstOrNull()?.mac?.ifBlank { devices.firstOrNull()?.ip } ?: ""
                        },
                        label = { Text("Einzelgerät (${devices.size})") },
                        enabled = devices.isNotEmpty()
                    )
                }

                // Target Dropdown
                ExposedDropdownMenuBox(
                    expanded = targetExpanded,
                    onExpandedChange = { targetExpanded = it }
                ) {
                    val targetDisplay = if (targetType == "group") {
                        val grp = groups.firstOrNull { it.id == selectedTargetId }
                        val rName = rooms.firstOrNull { it.id == grp?.roomId }?.name
                        if (grp != null) "${grp.name} ($rName)" else "Gruppe wählen..."
                    } else {
                        val dev = devices.firstOrNull { it.mac.equals(selectedTargetId, ignoreCase = true) || it.ip == selectedTargetId }
                        if (dev != null) "${dev.deviceName} (${dev.roomName})" else "Gerät wählen..."
                    }

                    OutlinedTextField(
                        value = targetDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (targetType == "group") "Zielgruppe" else "Zielgerät") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = targetExpanded,
                        onDismissRequest = { targetExpanded = false }
                    ) {
                        if (targetType == "group") {
                            groups.forEach { grp ->
                                val rName = rooms.firstOrNull { it.id == grp.roomId }?.name ?: ""
                                DropdownMenuItem(
                                    text = { Text("${grp.name} ($rName)") },
                                    onClick = {
                                        selectedTargetId = grp.id
                                        targetExpanded = false
                                    }
                                )
                            }
                        } else {
                            devices.forEach { dev ->
                                DropdownMenuItem(
                                    text = { Text("${dev.deviceName} (${dev.roomName})") },
                                    onClick = {
                                        selectedTargetId = dev.mac.ifBlank { dev.ip }
                                        targetExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Desired State On / Off
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (stateOn) "Zustand: Einschalten" else "Zustand: Ausschalten",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Switch(
                        checked = stateOn,
                        onCheckedChange = { stateOn = it }
                    )
                }

                if (stateOn) {
                    // Brightness slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Helligkeit",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "$dimming%",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarm
                                )
                            )
                        }
                        Slider(
                            value = dimming.toFloat(),
                            onValueChange = { dimming = it.toInt() },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(thumbColor = AmberWarm, activeTrackColor = AmberWarm),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Color / Light mood chips
                    Text(
                        text = "Lichtstimmung / Farbe",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_COLORS.take(6).forEachIndexed { idx, colorOption ->
                            val isSelected = selectedColorIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorOption.displayColor)
                                    .clickable {
                                        selectedColorIndex = if (isSelected) null else idx
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = colorOption.name,
                                        tint = if (colorOption.r > 200 && colorOption.g > 200 && colorOption.b > 200) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetName = if (targetType == "group") {
                        groups.firstOrNull { it.id == selectedTargetId }?.name
                    } else {
                        devices.firstOrNull { it.mac.equals(selectedTargetId, ignoreCase = true) || it.ip == selectedTargetId }?.deviceName
                    }

                    val chosenColor = selectedColorIndex?.let { PRESET_COLORS.getOrNull(it) }

                    onCreate(
                        name.trim(),
                        selectedIcon,
                        targetType,
                        selectedTargetId,
                        targetName,
                        stateOn,
                        dimming,
                        null,
                        chosenColor?.r,
                        chosenColor?.g,
                        chosenColor?.b
                    )
                },
                enabled = isFormValid,
                modifier = Modifier.testTag("save_preset_confirm_button")
            ) {
                Text("Preset speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
