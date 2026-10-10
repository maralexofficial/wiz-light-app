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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import com.example.data.model.WizRoom
import com.example.ui.components.DeviceCard
import com.example.ui.components.RoomCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun DashboardScreen(
    devices: List<WizDevice>,
    rooms: List<WizRoom>,
    slidingBrightness: Map<String, Int>,
    onBroadcastAll: (Boolean) -> Unit,
    onRefreshDiscovery: () -> Unit,
    onToggleDevice: (WizDevice) -> Unit,
    onDeviceBrightnessChange: (WizDevice, Int) -> Unit,
    onDeviceClick: (WizDevice) -> Unit,
    onToggleRoom: (WizRoom, Boolean) -> Unit,
    onRoomClick: (WizRoom) -> Unit,
    onNavigateToSettings: () -> Unit,
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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("broadcast_off_button")
                        ) {
                            Text("Alles Aus")
                        }

                        OutlinedButton(
                            onClick = onRefreshDiscovery,
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
                                    onClick = { onRoomClick(room) }
                                )
                            }
                        }
                    }
                }
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
                    onClick = { onDeviceClick(dev) }
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
