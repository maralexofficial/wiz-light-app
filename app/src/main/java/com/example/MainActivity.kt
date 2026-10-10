package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.DeviceDetailSheet
import com.example.ui.components.RoomDetailView
import com.example.ui.components.WizTopBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.RoomsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val apiConfig by mainViewModel.apiConfig.collectAsStateWithLifecycle()

            MyApplicationTheme(themePreference = apiConfig.themeMode) {
                WizAppContent(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun WizAppContent(viewModel: MainViewModel) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val apiConfig by viewModel.apiConfig.collectAsStateWithLifecycle()
    val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedRoomId by viewModel.selectedRoomId.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val deviceFilter by viewModel.deviceFilter.collectAsStateWithLifecycle()
    val filteredDevices by viewModel.filteredDevices.collectAsStateWithLifecycle()
    val isTestingConnection by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val slidingBrightness by viewModel.slidingBrightness.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val selectedRoom = rooms.firstOrNull { it.id == selectedRoomId }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (selectedRoomId == null) {
                    WizTopBar(
                        apiConfig = apiConfig,
                        isDiscovering = isDiscovering,
                        onRefresh = { viewModel.refreshDevices(showToast = true) },
                        onOpenSettings = { viewModel.selectTab(AppTab.SETTINGS) }
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen && selectedRoomId == null) {
                    NavigationBar(modifier = Modifier.testTag("mobile_bottom_bar")) {
                        NavigationBarItem(
                            selected = selectedTab == AppTab.DASHBOARD,
                            onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.DASHBOARD) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Übersicht"
                                )
                            },
                            label = { Text("Übersicht") },
                            modifier = Modifier.testTag("tab_dashboard")
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.ROOMS,
                            onClick = { viewModel.selectTab(AppTab.ROOMS) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.ROOMS) Icons.Filled.MeetingRoom else Icons.Outlined.MeetingRoom,
                                    contentDescription = "Räume"
                                )
                            },
                            label = { Text("Räume") },
                            modifier = Modifier.testTag("tab_rooms")
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.DEVICES,
                            onClick = { viewModel.selectTab(AppTab.DEVICES) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.DEVICES) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                                    contentDescription = "Geräte"
                                )
                            },
                            label = { Text("Geräte") },
                            modifier = Modifier.testTag("tab_devices")
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.SETTINGS,
                            onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Einstellungen"
                                )
                            },
                            label = { Text("Einstellungen") },
                            modifier = Modifier.testTag("tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen && selectedRoomId == null) {
                    NavigationRail(modifier = Modifier.testTag("tablet_navigation_rail")) {
                        NavigationRailItem(
                            selected = selectedTab == AppTab.DASHBOARD,
                            onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.DASHBOARD) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Übersicht"
                                )
                            },
                            label = { Text("Übersicht") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == AppTab.ROOMS,
                            onClick = { viewModel.selectTab(AppTab.ROOMS) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.ROOMS) Icons.Filled.MeetingRoom else Icons.Outlined.MeetingRoom,
                                    contentDescription = "Räume"
                                )
                            },
                            label = { Text("Räume") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == AppTab.DEVICES,
                            onClick = { viewModel.selectTab(AppTab.DEVICES) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.DEVICES) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                                    contentDescription = "Geräte"
                                )
                            },
                            label = { Text("Geräte") }
                        )
                        NavigationRailItem(
                            selected = selectedTab == AppTab.SETTINGS,
                            onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Einstellungen"
                                )
                            },
                            label = { Text("Einstellungen") }
                        )
                    }
                }

                if (selectedRoom != null) {
                    RoomDetailView(
                        room = selectedRoom,
                        slidingBrightness = slidingBrightness,
                        onBack = { viewModel.selectRoom(null) },
                        onToggleDevice = { viewModel.toggleDevice(it) },
                        onDeviceBrightnessChange = { dev, b -> viewModel.onBrightnessChange(dev, b) },
                        onDeviceClick = { viewModel.selectDevice(it) },
                        onToggleRoom = { viewModel.toggleRoom(selectedRoom, it) },
                        onRoomBrightnessChange = { viewModel.setRoomBrightness(selectedRoom, it) }
                    )
                } else {
                    when (selectedTab) {
                        AppTab.DASHBOARD -> DashboardScreen(
                            devices = devices,
                            rooms = rooms,
                            slidingBrightness = slidingBrightness,
                            onBroadcastAll = { viewModel.broadcastAll(it) },
                            onRefreshDiscovery = { viewModel.refreshDevices(showToast = true) },
                            onToggleDevice = { viewModel.toggleDevice(it) },
                            onDeviceBrightnessChange = { dev, b -> viewModel.onBrightnessChange(dev, b) },
                            onDeviceClick = { viewModel.selectDevice(it) },
                            onToggleRoom = { r, on -> viewModel.toggleRoom(r, on) },
                            onRoomClick = { viewModel.selectRoom(it.id) },
                            onNavigateToSettings = { viewModel.selectTab(AppTab.SETTINGS) }
                        )

                        AppTab.ROOMS -> RoomsScreen(
                            rooms = rooms,
                            onToggleRoom = { r, on -> viewModel.toggleRoom(r, on) },
                            onRoomClick = { viewModel.selectRoom(it.id) },
                            onRefreshDiscovery = { viewModel.refreshDevices(showToast = true) }
                        )

                        AppTab.DEVICES -> DevicesScreen(
                            devices = filteredDevices,
                            searchQuery = searchQuery,
                            currentFilter = deviceFilter,
                            slidingBrightness = slidingBrightness,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onFilterChange = { viewModel.setDeviceFilter(it) },
                            onToggleDevice = { viewModel.toggleDevice(it) },
                            onDeviceBrightnessChange = { dev, b -> viewModel.onBrightnessChange(dev, b) },
                            onDeviceClick = { viewModel.selectDevice(it) }
                        )

                        AppTab.SETTINGS -> SettingsScreen(
                            apiConfig = apiConfig,
                            isTestingConnection = isTestingConnection,
                            isDiscovering = isDiscovering,
                            onTestConnection = { viewModel.testConnection() },
                            onSaveConfig = { viewModel.saveConfig(it) },
                            onRefreshDiscovery = { viewModel.refreshDevices(showToast = true) },
                            onResetConfig = { viewModel.clearAllData() }
                        )
                    }
                }
            }
        }

        // Device Detail Modal BottomSheet
        selectedDevice?.let { dev ->
            DeviceDetailSheet(
                device = dev,
                slidingBrightness = slidingBrightness[dev.mac.ifBlank { dev.ip }],
                onDismiss = { viewModel.selectDevice(null) },
                onToggle = { viewModel.toggleDevice(dev) },
                onBrightnessChange = { viewModel.onBrightnessChange(dev, it) },
                onColorChange = { r, g, b -> viewModel.setDeviceColor(dev, r, g, b) }
            )
        }
    }
}
