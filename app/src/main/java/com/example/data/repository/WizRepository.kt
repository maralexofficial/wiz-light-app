package com.example.data.repository

import com.example.data.local.PreferencesManager
import com.example.data.model.ApiConfig
import com.example.data.model.WizBulbDto
import com.example.data.model.WizDevice
import com.example.data.model.WizDeviceState
import com.example.data.model.WizRoom
import com.example.data.remote.ApiClient
import com.example.data.remote.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WizRepository(
    private val preferencesManager: PreferencesManager,
    private val apiClient: ApiClient = ApiClient()
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val discoveryMutex = Mutex()

    private val _devices = MutableStateFlow<List<WizDevice>>(emptyList())
    val devices: StateFlow<List<WizDevice>> = _devices.asStateFlow()

    private val _rooms = MutableStateFlow<List<WizRoom>>(emptyList())
    val rooms: StateFlow<List<WizRoom>> = _rooms.asStateFlow()

    private val _apiConfig = MutableStateFlow(preferencesManager.getApiConfig())
    val apiConfig: StateFlow<ApiConfig> = _apiConfig.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _eventMessages = MutableSharedFlow<String>()
    val eventMessages: SharedFlow<String> = _eventMessages.asSharedFlow()

    init {
        apiClient.updateConfig(_apiConfig.value)
        loadCachedDevices()
    }

    private fun loadCachedDevices() {
        val cachedDtos = preferencesManager.getCachedBulbs()
        if (cachedDtos.isNotEmpty()) {
            val mapped = cachedDtos.map { dtoToDomain(it, isOnline = true) }
            _devices.value = mapped
            updateRoomsFromDevices(mapped)
        }
    }

    fun updateConfig(newConfig: ApiConfig) {
        _apiConfig.value = newConfig
        preferencesManager.saveApiConfig(newConfig)
        apiClient.updateConfig(newConfig)
    }

    suspend fun testConnection(): ApiResult<String> {
        val result = apiClient.testConnection()
        val now = System.currentTimeMillis()
        val updated = when (result) {
            is ApiResult.Success -> {
                _apiConfig.value.copy(
                    lastTestStatus = true,
                    lastTestMessage = result.data,
                    lastTestTimestamp = now
                )
            }
            is ApiResult.Error -> {
                _apiConfig.value.copy(
                    lastTestStatus = false,
                    lastTestMessage = result.message,
                    lastTestTimestamp = now
                )
            }
        }
        updateConfig(updated)
        return result
    }

    suspend fun discoverDevices(force: Boolean = false): ApiResult<Int> {
        if (!discoveryMutex.tryLock()) {
            return ApiResult.Error("Eine Geräteerkennung läuft bereits.")
        }
        _isDiscovering.value = true
        try {
            when (val result = apiClient.discoverDevices()) {
                is ApiResult.Success -> {
                    val dtos = result.data
                    val now = System.currentTimeMillis()
                    val currentMap = _devices.value.associateBy { it.mac.ifBlank { it.ip } }.toMutableMap()

                    // Update or insert discovered devices
                    val newlySeenKeys = mutableSetOf<String>()
                    val bulbDtosToCache = mutableListOf<WizBulbDto>()

                    for (dto in dtos) {
                        val key = (dto.mac?.trim()?.lowercase() ?: "").ifBlank { dto.ip.trim() }
                        if (key.isBlank()) continue
                        newlySeenKeys.add(key)
                        bulbDtosToCache.add(dto)

                        val device = dtoToDomain(dto, isOnline = true)
                        currentMap[key] = device
                    }

                    // Mark devices not found in this round as offline rather than deleting them
                    currentMap.forEach { (key, existingDev) ->
                        if (!newlySeenKeys.contains(key)) {
                            currentMap[key] = existingDev.copy(isOnline = false)
                        }
                    }

                    val updatedList = currentMap.values.toList().sortedWith(
                        compareBy<WizDevice> { it.roomName }.thenBy { it.deviceName }
                    )
                    _devices.value = updatedList
                    updateRoomsFromDevices(updatedList)

                    preferencesManager.saveCachedBulbs(bulbDtosToCache)
                    val updatedCfg = _apiConfig.value.copy(lastSyncTimestamp = now)
                    updateConfig(updatedCfg)

                    return ApiResult.Success(dtos.size)
                }
                is ApiResult.Error -> {
                    _eventMessages.emit(result.message)
                    return ApiResult.Error(result.message, result.code, result.httpStatus)
                }
            }
        } finally {
            _isDiscovering.value = false
            discoveryMutex.unlock()
        }
    }

    private fun updateRoomsFromDevices(deviceList: List<WizDevice>) {
        val grouped = deviceList.groupBy { dev ->
            val roomId = dev.roomId ?: -1L
            val roomName = if (dev.roomName.isNotBlank()) dev.roomName else if (roomId != -1L) "Raum $roomId" else "Nicht zugeordnet"
            roomId to roomName
        }

        val roomList = grouped.map { (key, devs) ->
            WizRoom(
                id = key.first,
                name = key.second,
                devices = devs.sortedBy { it.deviceName }
            )
        }.sortedBy { it.name }

        _rooms.value = roomList
    }

    private fun dtoToDomain(dto: WizBulbDto, isOnline: Boolean): WizDevice {
        val ip = dto.ip.trim()
        val mac = dto.mac?.trim()?.lowercase() ?: ip
        val roomId = dto.roomId
        val roomName = when {
            !dto.roomName.isNullOrBlank() -> dto.roomName.trim()
            roomId != null -> "Raum $roomId"
            else -> "Nicht zugeordnet"
        }
        val deviceName = dto.deviceName?.trim()?.ifBlank { null } ?: "WiZ Lampe ($ip)"
        val deviceType = dto.deviceType?.trim()?.ifBlank { null } ?: "light"

        return WizDevice(
            ip = ip,
            mac = mac,
            roomId = roomId,
            roomName = roomName,
            deviceName = deviceName,
            deviceType = deviceType,
            moduleName = dto.moduleName,
            firmwareVersion = dto.firmwareVersion,
            state = dto.state ?: WizDeviceState(),
            rssi = dto.rssi,
            isOnline = isOnline,
            lastSeenTimestamp = System.currentTimeMillis()
        )
    }

    suspend fun setDevicePower(device: WizDevice, turnOn: Boolean) {
        val originalState = device.state
        // Optimistic UI update
        updateLocalDeviceState(device.mac, device.state.copy(state = turnOn))

        val result = if (turnOn) apiClient.turnOn(device.ip) else apiClient.turnOff(device.ip)
        if (result is ApiResult.Error) {
            // Rollback on error
            updateLocalDeviceState(device.mac, originalState)
            _eventMessages.emit("Fehler beim Schalten von ${device.deviceName}: ${result.message}")
        }
    }

    suspend fun setDeviceBrightness(device: WizDevice, brightness: Int) {
        val clamped = brightness.coerceIn(0, 100)
        val originalState = device.state
        // Optimistic update
        updateLocalDeviceState(device.mac, device.state.copy(dimming = clamped, state = if (clamped > 0) true else device.state.state))

        val result = apiClient.setBrightness(device.ip, clamped)
        if (result is ApiResult.Error) {
            updateLocalDeviceState(device.mac, originalState)
            _eventMessages.emit("Fehler beim Dimmen von ${device.deviceName}: ${result.message}")
        }
    }

    suspend fun setDeviceColor(device: WizDevice, r: Int, g: Int, b: Int) {
        val originalState = device.state
        // Optimistic update
        updateLocalDeviceState(device.mac, device.state.copy(r = r, g = g, b = b, state = true))

        val result = apiClient.setColor(device.ip, r, g, b)
        if (result is ApiResult.Error) {
            updateLocalDeviceState(device.mac, originalState)
            _eventMessages.emit("Fehler beim Einstellen der Farbe für ${device.deviceName}: ${result.message}")
        }
    }

    suspend fun setRoomPower(room: WizRoom, turnOn: Boolean) {
        val targetDevices = room.devices.filter { it.isOnline }
        if (targetDevices.isEmpty()) return

        // Optimistic update
        val updatedDevices = _devices.value.map { dev ->
            if (dev.roomId == room.id && dev.isOnline) {
                dev.copy(state = dev.state.copy(state = turnOn))
            } else dev
        }
        _devices.value = updatedDevices
        updateRoomsFromDevices(updatedDevices)

        // Dispatch requests in parallel
        targetDevices.forEach { dev ->
            scope.launch {
                val res = if (turnOn) apiClient.turnOn(dev.ip) else apiClient.turnOff(dev.ip)
                if (res is ApiResult.Error) {
                    _eventMessages.emit("Fehler bei ${dev.deviceName}: ${res.message}")
                }
            }
        }
    }

    suspend fun setRoomBrightness(room: WizRoom, brightness: Int) {
        val clamped = brightness.coerceIn(0, 100)
        val lights = room.devices.filter { it.isOnline && it.isLight }
        if (lights.isEmpty()) return

        val updatedDevices = _devices.value.map { dev ->
            if (dev.roomId == room.id && dev.isOnline && dev.isLight) {
                dev.copy(state = dev.state.copy(dimming = clamped, state = if (clamped > 0) true else dev.state.state))
            } else dev
        }
        _devices.value = updatedDevices
        updateRoomsFromDevices(updatedDevices)

        lights.forEach { dev ->
            scope.launch {
                apiClient.setBrightness(dev.ip, clamped)
            }
        }
    }

    suspend fun broadcastAll(turnOn: Boolean) {
        // Optimistic update for all online devices
        val updatedDevices = _devices.value.map { dev ->
            if (dev.isOnline) dev.copy(state = dev.state.copy(state = turnOn)) else dev
        }
        _devices.value = updatedDevices
        updateRoomsFromDevices(updatedDevices)

        val result = if (turnOn) apiClient.broadcastOn() else apiClient.broadcastOff()
        if (result is ApiResult.Error) {
            _eventMessages.emit("Broadcast fehlgeschlagen: ${result.message}")
        }
    }

    private fun updateLocalDeviceState(macOrIp: String, newState: WizDeviceState) {
        val list = _devices.value.map { dev ->
            if (dev.mac == macOrIp || dev.ip == macOrIp) {
                dev.copy(state = newState)
            } else dev
        }
        _devices.value = list
        updateRoomsFromDevices(list)
    }

    fun clearLocalConfiguration() {
        preferencesManager.clearAll()
        _devices.value = emptyList()
        _rooms.value = emptyList()
        _apiConfig.value = ApiConfig()
        apiClient.updateConfig(_apiConfig.value)
    }
}
