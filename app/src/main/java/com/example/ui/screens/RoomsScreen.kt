package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WizGroup
import com.example.data.model.WizRoom
import com.example.ui.components.GroupCard
import com.example.ui.components.RoomCard
import com.example.ui.theme.AmberWarm

enum class RoomsViewTab {
    ROOMS,
    GROUPS
}

@Composable
fun RoomsScreen(
    rooms: List<WizRoom>,
    groups: List<WizGroup> = emptyList(),
    onToggleRoom: (WizRoom, Boolean) -> Unit,
    onRoomClick: (WizRoom) -> Unit,
    onToggleGroup: (WizGroup) -> Unit = {},
    onGroupBrightnessChange: (WizGroup, Int) -> Unit = { _, _ -> },
    onEditGroup: (WizGroup) -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onRefreshDiscovery: () -> Unit,
    permissions: com.example.data.model.TokenPermissions = com.example.data.model.TokenPermissions.ALL_GRANTED,
    modifier: Modifier = Modifier
) {
    var selectedView by remember { mutableStateOf(RoomsViewTab.ROOMS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (selectedView == RoomsViewTab.ROOMS) "Räume" else "Gruppen",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val totalDevs = rooms.sumOf { it.totalCount }
                val activeDevs = rooms.sumOf { it.activeCount }

                Text(
                    text = if (selectedView == RoomsViewTab.ROOMS) {
                        "${rooms.size} Räume • $totalDevs Geräte ($activeDevs aktiv)"
                    } else {
                        "${groups.size} Gruppen über alle Räume definiert"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedView == RoomsViewTab.GROUPS && permissions.canGroupsCreate) {
                OutlinedButton(
                    onClick = onCreateGroupClick,
                    modifier = Modifier.testTag("create_central_group_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Neue Gruppe")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle: Räume | Gruppen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedView == RoomsViewTab.ROOMS,
                onClick = { selectedView = RoomsViewTab.ROOMS },
                label = { Text("Räume (${rooms.size})") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.MeetingRoom, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberWarm.copy(alpha = 0.2f),
                    selectedLabelColor = AmberWarm
                ),
                modifier = Modifier.testTag("tab_switch_rooms")
            )

            if (permissions.canGroupsRead) {
                FilterChip(
                    selected = selectedView == RoomsViewTab.GROUPS,
                    onClick = { selectedView = RoomsViewTab.GROUPS },
                    label = { Text("Gruppen (${groups.size})") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.GroupWork, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberWarm.copy(alpha = 0.2f),
                        selectedLabelColor = AmberWarm
                    ),
                    modifier = Modifier.testTag("tab_switch_groups")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedView) {
            RoomsViewTab.ROOMS -> {
                if (rooms.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MeetingRoom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Keine Räume erkannt",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Räume werden automatisch anhand der Geräte-Metadaten erstellt.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRefreshDiscovery,
                                enabled = permissions.canDevicesDiscover
                            ) {
                                Text("Geräte jetzt suchen")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(rooms, key = { it.id }) { room ->
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

            RoomsViewTab.GROUPS -> {
                if (groups.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GroupWork,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Keine Gruppen eingerichtet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Gruppen ermöglichen die gleichzeitige Steuerung mehrerer Leuchtmittel in einem Raum.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (permissions.canGroupsCreate) {
                                Button(
                                    onClick = onCreateGroupClick,
                                    modifier = Modifier.testTag("empty_create_group_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gruppe erstellen")
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(groups, key = { it.id }) { group ->
                            val room = rooms.firstOrNull { it.id == group.roomId }
                            GroupCard(
                                group = group,
                                roomName = room?.name,
                                onToggle = { onToggleGroup(group) },
                                onBrightnessChange = { onGroupBrightnessChange(group, it) },
                                onEditClick = { onEditGroup(group) },
                                canControl = permissions.canDevicesControl,
                                canManage = permissions.canGroupsUpdate
                            )
                        }
                    }
                }
            }
        }
    }
}
