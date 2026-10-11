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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApiConfig
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    apiConfig: ApiConfig,
    isTestingConnection: Boolean,
    isDiscovering: Boolean,
    onTestConnection: () -> Unit,
    onSaveConfig: (ApiConfig) -> Unit,
    onRefreshDiscovery: () -> Unit,
    onSyncData: () -> Unit = {},
    onResetConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    var baseUrl by remember(apiConfig.baseUrl) { mutableStateOf(apiConfig.baseUrl) }
    var apiToken by remember(apiConfig.apiToken) { mutableStateOf(apiConfig.apiToken) }
    var tokenVisible by remember { mutableStateOf(false) }
    var autoRefresh by remember(apiConfig.autoRefresh) { mutableStateOf(apiConfig.autoRefresh) }
    var refreshInterval by remember(apiConfig.refreshIntervalSeconds) { mutableIntStateOf(apiConfig.refreshIntervalSeconds) }
    var themeMode by remember(apiConfig.themeMode) { mutableStateOf(apiConfig.themeMode) }
    var showResetDialog by remember { mutableStateOf(false) }

    val isHttpUnencrypted = baseUrl.startsWith("http://", ignoreCase = true)
    val isLocalIp = baseUrl.contains("192.168.") || baseUrl.contains("10.") || baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Konfiguration der REST-API und App-Präferenzen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Connection Status Banner
        item {
            val statusColor = when (apiConfig.lastTestStatus) {
                true -> SuccessGreen
                false -> ErrorRed
                null -> MaterialTheme.colorScheme.outline
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (apiConfig.lastTestStatus) {
                                true -> "API-Verbindung: Erfolgreich verbunden"
                                false -> "API-Verbindung: Nicht erreichbar"
                                null -> "API-Verbindung: Noch nicht getestet"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!apiConfig.lastTestMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = apiConfig.lastTestMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val testTime = apiConfig.lastTestTimestamp?.let {
                        SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY).format(Date(it))
                    }
                    if (testTime != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Letzter Test: $testTime",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val syncTime = apiConfig.lastSyncTimestamp?.let {
                        SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY).format(Date(it))
                    }
                    if (syncTime != null) {
                        Text(
                            text = "Letzter Datenabgleich: $syncTime",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // REST API Configuration Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "REST-API Server",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Adresse und Authentifizierung deines Node.js WiZ Backends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Base URL Input
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_base_url_input"),
                        label = { Text("API-Basis-URL") },
                        placeholder = { Text("http://192.168.1.100:3000") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    if (isHttpUnencrypted && !isLocalIp) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Unverschlüsseltes HTTP außerhalb des Heimnetzwerks (LAN) ist unsicher.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = WarningAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // API Token Input
                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { apiToken = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_api_token_input"),
                        label = { Text("API-Token (Optional)") },
                        placeholder = { Text("Bearer Token falls aktiviert") },
                        singleLine = true,
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    imageVector = if (tokenVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (tokenVisible) "Token verbergen" else "Token anzeigen"
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons: Test Connection & Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestConnection,
                            enabled = !isTestingConnection,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_test_connection_button")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Prüfe...")
                            } else {
                                Text("Verbindung testen")
                            }
                        }

                        Button(
                            onClick = {
                                val updated = apiConfig.copy(
                                    baseUrl = baseUrl.trim(),
                                    apiToken = apiToken.trim(),
                                    autoRefresh = autoRefresh,
                                    refreshIntervalSeconds = refreshInterval,
                                    themeMode = themeMode
                                )
                                onSaveConfig(updated)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_save_button")
                        ) {
                            Text("Speichern")
                        }
                    }
                }
            }
        }

        // Automatic Refresh Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatische Aktualisierung",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Periodisch Gerätezustände von der REST-API abfragen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = autoRefresh,
                            onCheckedChange = {
                                autoRefresh = it
                                onSaveConfig(apiConfig.copy(autoRefresh = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("settings_auto_refresh_switch")
                        )
                    }

                    if (autoRefresh) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Aktualisierungsintervall",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15 to "15s", 30 to "30s", 60 to "60s", 120 to "2 Min").forEach { (sec, label) ->
                                FilterChip(
                                    selected = refreshInterval == sec,
                                    onClick = {
                                        refreshInterval = sec
                                        onSaveConfig(apiConfig.copy(refreshIntervalSeconds = sec))
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // App Appearance Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Erscheinungsbild",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Wähle das Farbschema für die Benutzeroberfläche",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "System", "light" to "Hell", "dark" to "Dunkel").forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = {
                                    themeMode = mode
                                    onSaveConfig(apiConfig.copy(themeMode = mode))
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("theme_chip_$mode")
                            )
                        }
                    }
                }
            }
        }

        // Synchronization Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Synchronisation & Gruppen (/api/sync)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Gleichzeitiger Abgleich von Räumen, Gruppen und Revisionsdaten",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (apiConfig.lastRevision != null) {
                        Text(
                            text = "Aktuelle Revision: ${apiConfig.lastRevision}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    if (!apiConfig.serverUpdatedAt.isNullOrBlank()) {
                        Text(
                            text = "Server-Stand: ${apiConfig.serverUpdatedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!apiConfig.lastSyncError.isNullOrBlank()) {
                        Text(
                            text = "Letzter Sync-Hinweis: ${apiConfig.lastSyncError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSyncData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_sync_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jetzt synchronisieren")
                    }
                }
            }
        }

        // Discovery & Reset Maintenance Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Geräteverwaltung & Wartung",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRefreshDiscovery,
                        enabled = !isDiscovering,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_rediscover_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarm)
                    ) {
                        if (isDiscovering) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Suche läuft...")
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Geräte neu erkennen (/api/discover)")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_reset_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gespeicherte Daten zurücksetzen")
                    }
                }
            }
        }

        // Security & Privacy Note
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Sicherheit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hinweis zur Datensicherheit: API-Token und Serveradressen werden ausschließlich verschlüsselt im lokalen Speicher dieses Geräts aufbewahrt. Es werden keine Zugangsdaten an externe Server oder Analysedienste gesendet.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Konfiguration zurücksetzen?") },
            text = { Text("Möchtest du alle gespeicherten API-Verbindungsdaten, Token und zwischengespeicherten Geräte wirklich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetConfig()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
