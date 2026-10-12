package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WizDevice
import com.example.data.model.WizGroup
import com.example.data.model.WizPreset
import com.example.data.model.WizRoom
import com.example.ui.components.DeviceCard
import com.example.ui.components.GroupCard
import com.example.ui.components.RoomCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun DashboardScreen(
    devices: List<WizDevice>,
    rooms: List<WizRoom>,
    groups: List<WizGroup> = emptyList(),
    presets: List<WizPreset> = emptyList(),
    slidingBrightness: Map<String, Int>,
    onBroadcastAll: (Boolean) -> Unit,
    onRefreshDiscovery: () -> Unit,
    onToggleDevice: (WizDevice) -> Unit,
    onDeviceBrightnessChange: (WizDevice, Int) -> Unit,
    onDeviceClick: (WizDevice) -> Unit,
    onToggleRoom: (WizRoom, Boolean) -> Unit,
    onRoomClick: (WizRoom) -> Unit,
    onToggleGroup: (WizGroup) -> Unit = {},
    onGroupBrightnessChange: (WizGroup, Int) -> Unit = { _, _ -> },
    onEditGroup: (WizGroup) -> Unit = {},
    onApplyPreset: (WizPreset) -> Unit = {},
    onCreatePresetClick: () -> Unit = {},
    onDeletePreset: (WizPreset) -> Unit = {},
    onNavigateToSettings: () -> Unit,
    permissions: com.example.data.model.TokenPermissions = com.example.data.model.TokenPermissions.ALL_GRANTED,
    modifier: Modifier = Modifier
) {
    val totalCount = devices.size
    val activeCount = devices.count { it.isOnline && it.isTurnedOn }
    val onlineCount = devices.count { it.isOnline }
    val offlineCount = devices.count { !it.isOnline }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Welcome Header & Quick Action Buttons
        item {
            Column {
                Text(
                    text = "Mein Zuhause",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (totalCount > 0) "$activeCount von $totalCount Geräten eingeschaltet" else "Keine Geräte gefunden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary Stat Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Gesamt",
                    value = totalCount.toString(),
                    icon = Icons.Outlined.Devices,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Aktiv",
                    value = activeCount.toString(),
                    icon = Icons.Filled.Lightbulb,
                    tint = AmberWarm,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Erreichbar",
                    value = onlineCount.toString(),
                    icon = Icons.Outlined.CheckCircle,
                    tint = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                if (offlineCount > 0) {
                    StatCard(
                        title = "Offline",
                        value = offlineCount.toString(),
                        icon = Icons.Outlined.ErrorOutline,
                        tint = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Home Master Broadcast Controls
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gesamte Beleuchtung steuern",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onBroadcastAll(true) },
                            enabled = permissions.canSystemBroadcast,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("broadcast_on_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarm)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alles Ein")
                        }

                        FilledTonalButton(
                            onClick = { onBroadcastAll(false) },
                            enabled = permissions.canSystemBroadcast,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("broadcast_off_button")
                        ) {
                            Text("Alles Aus")
                        }

                        OutlinedButton(
                            onClick = onRefreshDiscovery,
                            enabled = permissions.canDevicesDiscover,
                            modifier = Modifier.testTag("dashboard_discovery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suche")
                        }
                    }
                }
            }
        }

        // Quick Presets Section (only if token has presets.read permission)
        if (permissions.canPresetsRead) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick-Presets (${presets.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        if (permissions.canPresetsManage) {
                            OutlinedButton(
                                onClick = onCreatePresetClick,
                                modifier = Modifier.testTag("create_preset_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Neues Preset")
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(10.dp))

                if (presets.isEmpty()) {
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
                                    text = "Keine Presets gespeichert",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Erstelle Szenen & Presets, um Gruppen oder Geräte mit einem Klick zu steuern.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = onCreatePresetClick,
                                modifier = Modifier.testTag("empty_add_preset_button")
                            ) {
                                Text("Erstellen")
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(presets, key = { it.id }) { preset ->
                            val iconVector = when (preset.icon) {
                                "night" -> Icons.Default.Bedtime
                                "movie" -> Icons.Default.Movie
                                "work" -> Icons.Default.Work
                                "star" -> Icons.Default.Star
                                else -> Icons.Default.Lightbulb
                            }

                            Card(
                                onClick = { if (permissions.canPresetsExecute) onApplyPreset(preset) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (preset.state) AmberWarm.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .width(180.dp)
                                    .testTag("preset_card_${preset.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (preset.state) AmberWarm else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = null,
                                                tint = if (preset.state) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        if (permissions.canPresetsManage) {
                                            IconButton(
                                                onClick = { onDeletePreset(preset) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Löschen",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )

                                    val targetLabel = if (preset.isGroupTarget) {
                                        "Gruppe • ${preset.targetName.ifBlank { "ID " + preset.targetId }}"
                                    } else {
                                        "Gerät • ${preset.targetName.ifBlank { preset.targetId }}"
                                    }

                                    Text(
                                        text = targetLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = if (preset.state) "Ein • ${preset.dimming}%" else "Aus",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (preset.state) AmberWarm else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Rooms Horizontal Carousel
        if (rooms.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Räume (${rooms.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(rooms, key = { it.id }) { room ->
                            Box(modifier = Modifier.width(220.dp)) {
                                RoomCard(
                                    room = room,
                                    onToggleRoom = { onToggleRoom(room, it) },
                                    onClick = { onRoomClick(room) },
                                    canControl = permissions.canDevicesControl
                                )
                            }
                        }
                    }
                }
            }
        }

        // Groups Section (only if token has groups.read permission)
        if (permissions.canGroupsRead && groups.isNotEmpty()) {
            item {
                Text(
                    text = "Gruppen (${groups.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(groups, key = { it.id }) { grp ->
                val room = rooms.firstOrNull { it.id == grp.roomId }
                GroupCard(
                    group = grp,
                    roomName = room?.name,
                    onToggle = { onToggleGroup(grp) },
                    onBrightnessChange = { onGroupBrightnessChange(grp, it) },
                    onEditClick = { onEditGroup(grp) },
                    canControl = permissions.canDevicesControl,
                    canManage = permissions.canGroupsUpdate
                )
            }
        }

        // Active / Spotlight Devices
        if (devices.isNotEmpty()) {
            item {
                Text(
                    text = "Geräte",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(devices, key = { it.mac.ifBlank { it.ip } }) { dev ->
                DeviceCard(
                    device = dev,
                    slidingBrightness = slidingBrightness[dev.mac.ifBlank { dev.ip }],
                    onToggle = { onToggleDevice(dev) },
                    onBrightnessChange = { onDeviceBrightnessChange(dev, it) },
                    onClick = { onDeviceClick(dev) },
                    canControl = permissions.canDevicesControl
                )
            }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Noch keine WiZ-Geräte verbunden",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Prüfe die Serveradresse in den Einstellungen oder starte die automatische Geräteerkennung.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateToSettings) {
                            Text("Einstellungen öffnen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }
}
