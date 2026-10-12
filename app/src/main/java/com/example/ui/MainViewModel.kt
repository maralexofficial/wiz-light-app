package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.model.ApiConfig
import com.example.data.model.CreatePresetRequest
import com.example.data.model.UpdatePresetRequest
import com.example.data.model.WizDevice
import com.example.data.model.WizGroup
import com.example.data.model.WizPreset
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
    val groups: StateFlow<List<WizGroup>> = repository.groups
    val presets: StateFlow<List<WizPreset>> = repository.presets
    val apiConfig: StateFlow<ApiConfig> = repository.apiConfig
    val isDiscovering: StateFlow<Boolean> = repository.isDiscovering

    val isConfigured: StateFlow<Boolean> = combine(apiConfig) { config ->
        val cfg = config[0]
        cfg.isConfigured && cfg.baseUrl.isNotBlank() && cfg.apiToken.isNotBlank()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        apiConfig.value.isConfigured && apiConfig.value.baseUrl.isNotBlank() && apiConfig.value.apiToken.isNotBlank()
    )

    val permissions: StateFlow<com.example.data.model.TokenPermissions> =
        combine(apiConfig) { config -> config[0].tokenPermissions }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), apiConfig.value.tokenPermissions)

    private val _selectedTab = MutableStateFlow(AppTab.DASHBOARD)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _selectedRoomId = MutableStateFlow<Long?>(null)
    val selectedRoomId: StateFlow<Long?> = _selectedRoomId.asStateFlow()

    private val _selectedDevice = MutableStateFlow<WizDevice?>(null)
    val selectedDevice: StateFlow<WizDevice?> = _selectedDevice.asStateFlow()

    private val _showCreateGroupDialog = MutableStateFlow(false)
    val showCreateGroupDialog: StateFlow<Boolean> = _showCreateGroupDialog.asStateFlow()

    private val _createGroupPreselectedRoomId = MutableStateFlow<Long?>(null)
    val createGroupPreselectedRoomId: StateFlow<Long?> = _createGroupPreselectedRoomId.asStateFlow()

    private val _editingGroup = MutableStateFlow<WizGroup?>(null)
    val editingGroup: StateFlow<WizGroup?> = _editingGroup.asStateFlow()

    private val _showCreatePresetDialog = MutableStateFlow(false)
    val showCreatePresetDialog: StateFlow<Boolean> = _showCreatePresetDialog.asStateFlow()

    private val _hiddenDeviceIds = MutableStateFlow<Set<String>>(preferencesManager.getHiddenDeviceIds())
    val hiddenDeviceIds: StateFlow<Set<String>> = _hiddenDeviceIds.asStateFlow()

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

    // Filtered devices flow for the general device overview
    val filteredDevices: StateFlow<List<WizDevice>> = combine(
        devices,
        searchQuery,
        deviceFilter,
        hiddenDeviceIds
    ) { allDevs, query, filter, hiddenIds ->
        allDevs.filter { dev ->
            val devId = dev.mac.ifBlank { dev.ip }
            val isHidden = hiddenIds.contains(devId)

            // If user explicitly selects the "HIDDEN" filter, show hidden devices
            if (filter == "HIDDEN") {
                return@filter isHidden && (query.isBlank() ||
                    dev.deviceName.contains(query, ignoreCase = true) ||
                    dev.roomName.contains(query, ignoreCase = true) ||
                    dev.ip.contains(query, ignoreCase = true))
            }

            // In all normal views of general devices, hide devices marked as hidden
            if (isHidden) return@filter false

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

        // Initial discovery only if app is configured
        viewModelScope.launch {
            delay(400)
            val cfg = repository.apiConfig.value
            if (cfg.isConfigured && cfg.apiToken.isNotBlank() && cfg.baseUrl.isNotBlank() && repository.devices.value.isEmpty()) {
                refreshDevices(showToast = false)
            }
        }
    }

    private fun setupAutoRefresh(config: ApiConfig) {
        autoRefreshJob?.cancel()
        if (!config.isConfigured || config.apiToken.isBlank()) {
            return
        }
        // In manual mode, no automatic interval synchronization or polling must run
        if (config.syncMode.equals("MANUAL", ignoreCase = true)) {
            return
        }
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

    fun openCreateGroupDialog(roomId: Long? = null) {
        _createGroupPreselectedRoomId.value = roomId
        _showCreateGroupDialog.value = true
    }

    fun closeCreateGroupDialog() {
        _showCreateGroupDialog.value = false
        _createGroupPreselectedRoomId.value = null
    }

    fun openEditGroupDialog(group: WizGroup) {
        _editingGroup.value = group
    }

    fun closeEditGroupDialog() {
        _editingGroup.value = null
    }

    fun toggleGroup(group: WizGroup) {
        viewModelScope.launch {
            repository.setGroupPower(group, !group.isTurnedOn)
        }
    }

    fun onGroupBrightnessChange(group: WizGroup, brightness: Int) {
        viewModelScope.launch {
            repository.setGroupBrightness(group, brightness)
        }
    }

    fun setGroupTemperature(group: WizGroup, tempKelvin: Int) {
        viewModelScope.launch {
            repository.setGroupTemperature(group, tempKelvin)
        }
    }

    fun createGroup(name: String, roomId: Long, deviceType: String, deviceMacs: List<String>) {
        viewModelScope.launch {
            val res = repository.createGroup(name, roomId, deviceType, deviceMacs)
            if (res is ApiResult.Success) {
                closeCreateGroupDialog()
            }
        }
    }

    fun updateGroup(id: String, name: String?, deviceMacs: List<String>?) {
        viewModelScope.launch {
            val res = repository.updateGroup(id, name, deviceMacs)
            if (res is ApiResult.Success) {
                closeEditGroupDialog()
            }
        }
    }

    fun deleteGroup(group: WizGroup) {
        viewModelScope.launch {
            val res = repository.deleteGroup(group.id)
            if (res is ApiResult.Success) {
                closeEditGroupDialog()
            }
        }
    }

    fun toggleDeviceHidden(device: WizDevice) {
        val devId = device.mac.ifBlank { device.ip }
        val isHidden = _hiddenDeviceIds.value.contains(devId)
        val newHidden = !isHidden
        preferencesManager.setDeviceHidden(devId, newHidden)
        _hiddenDeviceIds.value = preferencesManager.getHiddenDeviceIds()
        _feedbackMessage.value = if (newHidden) {
            "Gerät '${device.deviceName}' in Geräteübersicht ausgeblendet."
        } else {
            "Gerät '${device.deviceName}' wieder in Geräteübersicht sichtbar."
        }
    }

    fun openCreatePresetDialog() {
        _showCreatePresetDialog.value = true
    }

    fun closeCreatePresetDialog() {
        _showCreatePresetDialog.value = false
    }

    fun applyPreset(preset: WizPreset) {
        viewModelScope.launch {
            repository.applyPreset(preset)
        }
    }

    fun createPreset(
        name: String,
        icon: String,
        targetType: String,
        targetId: String,
        targetName: String?,
        state: Boolean,
        dimming: Int,
        temperature: Int?,
        r: Int?,
        g: Int?,
        b: Int?
    ) {
        viewModelScope.launch {
            val req = CreatePresetRequest(
                name = name,
                icon = icon,
                targetType = targetType,
                targetId = targetId,
                targetName = targetName,
                state = state,
                dimming = dimming,
                temperature = temperature,
                r = r,
                g = g,
                b = b
            )
            val res = repository.createPreset(req)
            if (res is ApiResult.Success) {
                closeCreatePresetDialog()
            }
        }
    }

    fun deletePreset(preset: WizPreset) {
        viewModelScope.launch {
            repository.deletePreset(preset)
        }
    }

    fun syncData() {
        viewModelScope.launch {
            repository.syncRoomsAndGroups()
        }
    }

    fun clearAllData() {
        autoRefreshJob?.cancel()
        repository.clearLocalConfiguration()
        _feedbackMessage.value = "Konfiguration und Gerätedaten wurden zurückgesetzt."
    }

    suspend fun testConnectionWith(baseUrl: String, token: String): ApiResult<String> {
        return repository.testConnectionWith(baseUrl, token)
    }

    fun completeSetup(baseUrl: String, apiToken: String) {
        val current = repository.apiConfig.value
        val updated = current.copy(
            baseUrl = baseUrl.trim(),
            apiToken = apiToken.trim(),
            isConfigured = true,
            lastTestStatus = true,
            lastTestMessage = "Einrichtung erfolgreich abgeschlossen",
            lastTestTimestamp = System.currentTimeMillis()
        )
        repository.updateConfig(updated)
        // Trigger initial data load
        viewModelScope.launch {
            refreshDevices(showToast = false)
            repository.syncRoomsAndGroups()
        }
    }

    fun resetApp() {
        autoRefreshJob?.cancel()
        brightnessDebounceJobs.values.forEach { it.cancel() }
        brightnessDebounceJobs.clear()
        roomBrightnessDebounceJob?.cancel()
        _selectedRoomId.value = null
        _selectedDevice.value = null
        _selectedTab.value = AppTab.DASHBOARD
        repository.clearLocalConfiguration()
        _feedbackMessage.value = "App wurde zurückgesetzt. Bitte führe die Einrichtung erneut durch."
    }

    fun updateTokenPermissions(permissions: com.example.data.model.TokenPermissions) {
        repository.updateTokenPermissions(permissions)
    }
}
