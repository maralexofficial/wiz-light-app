package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WizDevice
import com.example.data.model.WizGroup
import com.example.data.model.WizRoom
import com.example.ui.theme.AmberWarm

@Composable
fun RoomDetailView(
    room: WizRoom,
    groups: List<WizGroup>,
    slidingBrightness: Map<String, Int>,
    onBack: () -> Unit,
    onToggleDevice: (WizDevice) -> Unit,
    onDeviceBrightnessChange: (WizDevice, Int) -> Unit,
    onDeviceClick: (WizDevice) -> Unit,
    onToggleRoom: (Boolean) -> Unit,
    onRoomBrightnessChange: (Int) -> Unit,
    onToggleGroup: (WizGroup) -> Unit,
    onGroupBrightnessChange: (WizGroup, Int) -> Unit,
    onEditGroup: (WizGroup) -> Unit,
    onCreateGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val roomGroups = groups.filter { it.roomId == room.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("room_detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurück",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${room.totalCount} Geräte • ${room.activeCount} aktiv",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Room Master Control Bar
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Raumsteuerung",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onToggleRoom(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("room_turn_all_on_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Alle Ein")
                            }

                            FilledTonalButton(
                                onClick = { onToggleRoom(false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("room_turn_all_off_button")
                            ) {
                                Text("Alle Aus")
                            }
                        }

                        if (room.hasActiveLights) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gemeinsame Helligkeit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${room.averageBrightness}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarm
                                    )
                                )
                            }

                            Slider(
                                value = room.averageBrightness.toFloat(),
                                onValueChange = { onRoomBrightnessChange(it.toInt()) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AmberWarm,
                                    activeTrackColor = AmberWarm
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Room Groups Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gruppen (${roomGroups.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedButton(
                        onClick = onCreateGroupClick,
                        modifier = Modifier.testTag("create_room_group_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gruppe")
                    }
                }
            }

            if (roomGroups.isNotEmpty()) {
                items(roomGroups, key = { it.id }) { group ->
                    GroupCard(
                        group = group,
                        roomName = room.name,
                        onToggle = { onToggleGroup(group) },
                        onBrightnessChange = { onGroupBrightnessChange(group, it) },
                        onEditClick = { onEditGroup(group) }
                    )
                }
            }

            // Devices Section Header
            item {
                Text(
                    text = "Einzelgeräte (${room.devices.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(room.devices, key = { it.mac.ifBlank { it.ip } }) { dev ->
                DeviceCard(
                    device = dev,
                    slidingBrightness = slidingBrightness[dev.mac.ifBlank { dev.ip }],
                    onToggle = { onToggleDevice(dev) },
                    onBrightnessChange = { onDeviceBrightnessChange(dev, it) },
                    onClick = { onDeviceClick(dev) }
                )
            }
        }
    }
}
