package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WizDevice
import com.example.data.model.WizGroup
import com.example.data.model.WizRoom
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    rooms: List<WizRoom>,
    allDevices: List<WizDevice>,
    preselectedRoomId: Long?,
    onDismiss: () -> Unit,
    onCreate: (name: String, roomId: Long, deviceType: String, deviceMacs: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRoomId by remember {
        mutableLongStateOf(preselectedRoomId ?: rooms.firstOrNull()?.id ?: 0L)
    }
    var deviceType by remember { mutableStateOf("light") } // "light" or "plug"
    val selectedMacs = remember { mutableStateListOf<String>() }
    var roomDropdownExpanded by remember { mutableStateOf(false) }

    val currentRoom = rooms.firstOrNull { it.id == selectedRoomId }
    val candidateDevices = (currentRoom?.devices ?: allDevices.filter { it.roomId == selectedRoomId })
        .filter { dev -> if (deviceType == "light") dev.isLight else dev.isPlug }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Neue Gruppe erstellen",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Gruppenname") },
                    placeholder = { Text("z.B. Deckenlampen, Wandspots") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_group_name_input"),
                    singleLine = true
                )

                // Room Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = roomDropdownExpanded,
                    onExpandedChange = { roomDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentRoom?.name ?: "Raum wählen",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Zugeordneter Raum") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = roomDropdownExpanded,
                        onDismissRequest = { roomDropdownExpanded = false }
                    ) {
                        rooms.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name) },
                                onClick = {
                                    selectedRoomId = r.id
                                    selectedMacs.clear()
                                    roomDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Device Type Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = deviceType == "light",
                        onClick = {
                            deviceType = "light"
                            selectedMacs.clear()
                        },
                        label = { Text("Lampen") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberWarm.copy(alpha = 0.2f),
                            selectedLabelColor = AmberWarm
                        )
                    )
                    FilterChip(
                        selected = deviceType == "plug",
                        onClick = {
                            deviceType = "plug"
                            selectedMacs.clear()
                        },
                        label = { Text("Steckdosen") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberWarm.copy(alpha = 0.2f),
                            selectedLabelColor = AmberWarm
                        )
                    )
                }

                Text(
                    text = "Geräte auswählen (${selectedMacs.size} gewählt):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (candidateDevices.isEmpty()) {
                    Text(
                        text = "Keine passenden Geräte in diesem Raum vorhanden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        LazyColumn(modifier = Modifier.height(160.dp)) {
                            items(candidateDevices) { dev ->
                                val isChecked = selectedMacs.contains(dev.mac)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedMacs.remove(dev.mac)
                                            else selectedMacs.add(dev.mac)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedMacs.add(dev.mac)
                                            else selectedMacs.remove(dev.mac)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = AmberWarm)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = dev.deviceName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            text = dev.ip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedRoomId, deviceType, selectedMacs.toList())
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("create_group_confirm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarm, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Gruppe erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun EditGroupDialog(
    group: WizGroup,
    roomDevices: List<WizDevice>,
    onDismiss: () -> Unit,
    onSave: (name: String, deviceMacs: List<String>) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(group.name) { mutableStateOf(group.name) }
    val selectedMacs = remember(group.deviceMacs) { mutableStateListOf(*group.deviceMacs.toTypedArray()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val candidateDevices = roomDevices.filter { dev ->
        if (group.isLight) dev.isLight else dev.isPlug
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Gruppe löschen?") },
            text = { Text("Möchtest du die Gruppe '${group.name}' wirklich löschen? Die einzelnen Geräte bleiben erhalten.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gruppe bearbeiten",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Löschen")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Gruppenname") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Geräte in der Gruppe (${selectedMacs.size} gewählt):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(candidateDevices) { dev ->
                            val isChecked = selectedMacs.contains(dev.mac)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedMacs.remove(dev.mac)
                                        else selectedMacs.add(dev.mac)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedMacs.add(dev.mac)
                                        else selectedMacs.remove(dev.mac)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = AmberWarm)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = dev.deviceName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    Text(
                                        text = dev.ip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedMacs.toList())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarm, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
