package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.model.ApiConfig
import com.example.data.model.WizDevice
import com.example.data.model.WizRoom
import com.example.data.remote.ApiResult
import com.example.data.repository.WizRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Übersicht"),
    ROOMS("Räume"),
    DEVICES("Geräte"),
    SETTINGS("Einstellungen")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    val repository = WizRepository(preferencesManager)

    val devices: StateFlow<List<WizDevice>> = repository.devices
    val rooms: StateFlow<List<WizRoom>> = repository.rooms
    val apiConfig: StateFlow<ApiConfig> = repository.apiConfig
    val isDiscovering: StateFlow<Boolean> = repository.isDiscovering

    private val _selectedTab = MutableStateFlow(AppTab.DASHBOARD)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _selectedRoomId = MutableStateFlow<Long?>(null)
    val selectedRoomId: StateFlow<Long?> = _selectedRoomId.asStateFlow()

    private val _selectedDevice = MutableStateFlow<WizDevice?>(null)
    val selectedDevice: StateFlow<WizDevice?> = _selectedDevice.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _deviceFilter = MutableStateFlow("ALL")
    val deviceFilter: StateFlow<String> = _deviceFilter.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    // Temporary brightness slider state per device to allow smooth UI dragging without lag
    private val _slidingBrightness = MutableStateFlow<Map<String, Int>>(emptyMap())
    val slidingBrightness: StateFlow<Map<String, Int>> = _slidingBrightness.asStateFlow()

    private var brightnessDebounceJobs = mutableMapOf<String, Job>()
    private var roomBrightnessDebounceJob: Job? = null
    private var autoRefreshJob: Job? = null

    // Filtered devices flow
    val filteredDevices: StateFlow<List<WizDevice>> = combine(
        devices,
        searchQuery,
        deviceFilter
    ) { allDevs, query, filter ->
        allDevs.filter { dev ->
            val matchesSearch = query.isBlank() ||
                dev.deviceName.contains(query, ignoreCase = true) ||
                dev.roomName.contains(query, ignoreCase = true) ||
                dev.ip.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "ACTIVE" -> dev.isOnline && dev.isTurnedOn
                "INACTIVE" -> dev.isOnline && !dev.isTurnedOn
                "OFFLINE" -> !dev.isOnline
                "LIGHTS" -> dev.isLight
                "PLUGS" -> dev.isPlug
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect repository events
        viewModelScope.launch {
            repository.eventMessages.collect { msg ->
                _feedbackMessage.value = msg
            }
        }

        // Setup auto-refresh watcher
        viewModelScope.launch {
            apiConfig.collect { config ->
                setupAutoRefresh(config)
            }
        }

        // Initial discovery if URL is configured and devices are empty
        viewModelScope.launch {
            delay(400)
            if (repository.devices.value.isEmpty() && repository.apiConfig.value.baseUrl.isNotBlank()) {
                refreshDevices(showToast = false)
            }
        }
    }

    private fun setupAutoRefresh(config: ApiConfig) {
        autoRefreshJob?.cancel()
        if (config.autoRefresh && config.baseUrl.isNotBlank()) {
            val interval = (config.refreshIntervalSeconds.coerceAtLeast(10)) * 1000L
            autoRefreshJob = viewModelScope.launch {
                while (true) {
                    delay(interval)
                    if (!repository.isDiscovering.value) {
                        repository.discoverDevices()
                    }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun selectRoom(roomId: Long?) {
        _selectedRoomId.value = roomId
    }

    fun selectDevice(device: WizDevice?) {
        _selectedDevice.value = device
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDeviceFilter(filter: String) {
        _deviceFilter.value = filter
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun toggleDevice(device: WizDevice) {
        viewModelScope.launch {
            repository.setDevicePower(device, !device.isTurnedOn)
            // If currently selected in detail sheet, update reference
            if (_selectedDevice.value?.mac == device.mac) {
                _selectedDevice.value = _selectedDevice.value?.copy(
                    state = _selectedDevice.value!!.state.copy(state = !device.isTurnedOn)
                )
            }
        }
    }

    fun onBrightnessSliderChange(device: WizDevice, newBrightness: Int) {
        val key = device.mac.ifBlank { device.ip }
        _slidingBrightness.value = _slidingBrightness.value + (key to newBrightness)

        brightnessDebounceJobs[key]?.cancel()
        brightnessDebounceJobs[key] = viewModelScope.launch {
            delay(280) // 280ms debounce for network request
            repository.setDeviceBrightness(device, newBrightness)
            _slidingBrightness.value = _slidingBrightness.value - key
        }
    }

    fun onBrightnessChange(device: WizDevice, newBrightness: Int) = onBrightnessSliderChange(device, newBrightness)

    fun setDeviceColor(device: WizDevice, r: Int, g: Int, b: Int) {
        viewModelScope.launch {
            repository.setDeviceColor(device, r, g, b)
            if (_selectedDevice.value?.mac == device.mac) {
                _selectedDevice.value = _selectedDevice.value?.copy(
                    state = _selectedDevice.value!!.state.copy(r = r, g = g, b = b, state = true)
                )
            }
        }
    }

    fun toggleRoom(room: WizRoom, turnOn: Boolean) {
        viewModelScope.launch {
            repository.setRoomPower(room, turnOn)
        }
    }

    fun setRoomBrightness(room: WizRoom, brightness: Int) {
        roomBrightnessDebounceJob?.cancel()
        roomBrightnessDebounceJob = viewModelScope.launch {
            delay(280)
            repository.setRoomBrightness(room, brightness)
        }
    }

    fun broadcastAll(turnOn: Boolean) {
        viewModelScope.launch {
            repository.broadcastAll(turnOn)
        }
    }

    fun refreshDevices(showToast: Boolean = true) {
        viewModelScope.launch {
            when (val res = repository.discoverDevices(force = true)) {
                is ApiResult.Success -> {
                    if (showToast) {
                        _feedbackMessage.value = "${res.data} WiZ-Gerät(e) erfolgreich erkannt."
                    }
                }
                is ApiResult.Error -> {
                    if (showToast) {
                        _feedbackMessage.value = "Erkennung fehlgeschlagen: ${res.message}"
                    }
                }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            try {
                val res = repository.testConnection()
                when (res) {
                    is ApiResult.Success -> {
                        _feedbackMessage.value = res.data
                    }
                    is ApiResult.Error -> {
                        _feedbackMessage.value = "Verbindungstest fehlgeschlagen: ${res.message}"
                    }
                }
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    fun saveConfig(newConfig: ApiConfig) {
        repository.updateConfig(newConfig)
        _feedbackMessage.value = "Einstellungen erfolgreich gespeichert."
    }

    fun clearAllData() {
        repository.clearLocalConfiguration()
        _feedbackMessage.value = "Konfiguration und Gerätedaten wurden zurückgesetzt."
    }
}
