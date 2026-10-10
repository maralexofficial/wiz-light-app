package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.data.model.WizDevice
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.AmberSurfaceDark
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.Slate400

@Composable
fun DeviceCard(
    device: WizDevice,
    slidingBrightness: Int?,
    onToggle: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOn = device.isOnline && device.isTurnedOn
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val cardBg by animateColorAsState(
        targetValue = when {
            !device.isOnline -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            isOn -> if (isDark) AmberSurfaceDark else AmberSurface
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(250),
        label = "cardBg"
    )

    val borderStroke = when {
        !device.isOnline -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        isOn -> BorderStroke(1.5.dp, AmberWarm.copy(alpha = 0.8f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }

    val currentBrightness = slidingBrightness ?: device.brightnessPercent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("device_card_${device.mac.ifBlank { device.ip }}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOn) 4.dp else 1.dp)
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
                // Device Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !device.isOnline -> MaterialTheme.colorScheme.surfaceVariant
                                isOn -> AmberWarm.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconColor = when {
                        !device.isOnline -> Slate400
                        isOn -> AmberGlow
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Icon(
                        imageVector = if (device.isPlug) Icons.Outlined.Power else Icons.Outlined.Lightbulb,
                        contentDescription = device.deviceName,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Switch or Offline Badge
                if (device.isOnline) {
                    Switch(
                        checked = isOn,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AmberWarm,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("device_toggle_${device.mac.ifBlank { device.ip }}")
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ErrorRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Offline",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ErrorRed
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Device Name and Room
            Text(
                text = device.deviceName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = device.roomName,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Dimmable Brightness Slider for Lights
            if (device.isOnline && device.supportsDimming) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Helligkeit",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = if (isOn) "$currentBrightness%" else "Aus",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isOn) AmberWarm else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Slider(
                    value = currentBrightness.toFloat(),
                    onValueChange = { onBrightnessChange(it.toInt()) },
                    valueRange = 0f..100f,
                    enabled = device.isOnline,
                    colors = SliderDefaults.colors(
                        thumbColor = AmberWarm,
                        activeTrackColor = AmberWarm,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_slider_${device.mac.ifBlank { device.ip }}")
                )
            }
        }
    }
}
