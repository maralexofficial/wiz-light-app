package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WizRoom
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.AmberSurfaceDark
import com.example.ui.theme.AmberWarm

@Composable
fun RoomCard(
    room: WizRoom,
    onToggleRoom: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActive = room.hasActiveLights
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val cardBg by animateColorAsState(
        targetValue = when {
            hasActive -> if (isDark) AmberSurfaceDark else AmberSurface
            else -> MaterialTheme.colorScheme.surface
        },
        label = "roomCardBg"
    )

    val borderStroke = when {
        hasActive -> BorderStroke(1.5.dp, AmberWarm.copy(alpha = 0.8f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("room_card_${room.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasActive) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasActive) AmberWarm.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MeetingRoom,
                        contentDescription = "Raum",
                        tint = if (hasActive) AmberGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Switch(
                    checked = hasActive,
                    onCheckedChange = { onToggleRoom(it) },
                    enabled = room.onlineCount > 0,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AmberWarm,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("room_toggle_${room.id}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = room.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    room.totalCount == 0 -> "Keine Geräte"
                    room.activeCount > 0 -> "${room.totalCount} Geräte • ${room.activeCount} aktiv"
                    else -> "${room.totalCount} Geräte • alle aus"
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (hasActive) AmberWarm else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (hasActive) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 13.sp
                )
            )
        }
    }
}
