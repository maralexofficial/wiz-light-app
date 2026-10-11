package com.example.data.repository

import com.example.data.local.PreferencesManager
import com.example.data.model.ApiConfig
import com.example.data.model.CreateGroupRequest
import com.example.data.model.GroupActionResult
import com.example.data.model.SyncData
import com.example.data.model.UpdateGroupRequest
import com.example.data.model.WizBulbDto
import com.example.data.model.WizDevice
import com.example.data.model.WizDeviceState
import com.example.data.model.WizGroup
import com.example.data.model.WizGroupDto
import com.example.data.model.WizGroupStateDto
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

    private val _groups = MutableStateFlow<List<WizGroup>>(emptyList())
    val groups: StateFlow<List<WizGroup>> = _groups.asStateFlow()

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
        val cachedGroupDtos = preferencesManager.getCachedGroups()
        if (cachedGroupDtos.isNotEmpty()) {
            _groups.value = cachedGroupDtos.map { groupDtoToDomain(it) }
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

    private fun groupDtoToDomain(dto: WizGroupDto): WizGroup {
        val macs = dto.deviceMacs ?: dto.members ?: emptyList()
        val state = dto.state
        return WizGroup(
            id = dto.id,
            name = dto.name.ifBlank { "Gruppe" },
            roomId = dto.roomId,
            deviceType = dto.deviceType.ifBlank { "light" },
            deviceMacs = macs,
            isTurnedOn = state?.state ?: false,
            brightnessPercent = state?.dimming ?: 100,
            temperatureKelvin = state?.temperature ?: state?.temp
        )
    }

    private fun groupDomainToDto(group: WizGroup): WizGroupDto {
        return WizGroupDto(
            id = group.id,
            name = group.name,
            roomId = group.roomId,
            deviceType = group.deviceType,
            deviceMacs = group.deviceMacs,
            state = WizGroupStateDto(
                state = group.isTurnedOn,
                dimming = group.brightnessPercent,
                temperature = group.temperatureKelvin
            )
        )
    }

    suspend fun fetchGroups(): ApiResult<List<WizGroup>> {
        when (val res = apiClient.fetchGroups()) {
            is ApiResult.Success -> {
                val mapped = res.data.map { groupDtoToDomain(it) }
                _groups.value = mapped
                preferencesManager.saveCachedGroups(res.data)
                return ApiResult.Success(mapped)
            }
            is ApiResult.Error -> {
                _eventMessages.emit("Fehler beim Abrufen der Gruppen: ${res.message}")
                return ApiResult.Error(res.message, res.code, res.httpStatus)
            }
        }
    }

    suspend fun createGroup(name: String, roomId: Long, deviceType: String, deviceMacs: List<String>): ApiResult<WizGroup> {
        val req = CreateGroupRequest(name = name, roomId = roomId, deviceType = deviceType, deviceMacs = deviceMacs)
        when (val res = apiClient.createGroup(req)) {
            is ApiResult.Success -> {
                val newGroup = groupDtoToDomain(res.data)
                val updated = (_groups.value.filter { it.id != newGroup.id } + newGroup).sortedBy { it.name }
                _groups.value = updated
                preferencesManager.saveCachedGroups(updated.map { groupDomainToDto(it) })
                _eventMessages.emit("Gruppe '${newGroup.name}' erfolgreich erstellt.")
                return ApiResult.Success(newGroup)
            }
            is ApiResult.Error -> {
                _eventMessages.emit("Fehler beim Erstellen der Gruppe: ${res.message}")
                return ApiResult.Error(res.message, res.code, res.httpStatus)
            }
        }
    }

    suspend fun updateGroup(id: String, name: String? = null, deviceMacs: List<String>? = null): ApiResult<WizGroup> {
        val req = UpdateGroupRequest(name = name, deviceMacs = deviceMacs)
        when (val res = apiClient.updateGroup(id, req)) {
            is ApiResult.Success -> {
                val updatedGroup = groupDtoToDomain(res.data)
                val updated = _groups.value.map { if (it.id == id) updatedGroup else it }.sortedBy { it.name }
                _groups.value = updated
                preferencesManager.saveCachedGroups(updated.map { groupDomainToDto(it) })
                _eventMessages.emit("Gruppe '${updatedGroup.name}' aktualisiert.")
                return ApiResult.Success(updatedGroup)
            }
            is ApiResult.Error -> {
                _eventMessages.emit("Fehler beim Aktualisieren der Gruppe: ${res.message}")
                return ApiResult.Error(res.message, res.code, res.httpStatus)
            }
        }
    }

    suspend fun deleteGroup(id: String): ApiResult<Unit> {
        when (val res = apiClient.deleteGroup(id)) {
            is ApiResult.Success -> {
                val updated = _groups.value.filter { it.id != id }
                _groups.value = updated
                preferencesManager.saveCachedGroups(updated.map { groupDomainToDto(it) })
                _eventMessages.emit("Gruppe gelöscht.")
                return ApiResult.Success(Unit)
            }
            is ApiResult.Error -> {
                _eventMessages.emit("Fehler beim Löschen der Gruppe: ${res.message}")
                return ApiResult.Error(res.message, res.code, res.httpStatus)
            }
        }
    }

    suspend fun setGroupPower(group: WizGroup, turnOn: Boolean) {
        val originalGroups = _groups.value
        // Optimistic update for group state
        _groups.value = _groups.value.map {
            if (it.id == group.id) it.copy(isTurnedOn = turnOn) else it
        }

        // Also optimistically update devices that belong to this group
        val macSet = group.deviceMacs.map { it.trim().lowercase() }.toSet()
        if (macSet.isNotEmpty()) {
            val updatedDevs = _devices.value.map { dev ->
                val devMac = dev.mac.trim().lowercase()
                if (macSet.contains(devMac)) dev.copy(state = dev.state.copy(state = turnOn)) else dev
            }
            _devices.value = updatedDevs
            updateRoomsFromDevices(updatedDevs)
        }

        val res = if (turnOn) apiClient.groupOn(group.id) else apiClient.groupOff(group.id)
        when (res) {
            is ApiResult.Success -> {
                if (res.data.isPartialSuccess) {
                    _eventMessages.emit(res.data.message)
                }
            }
            is ApiResult.Error -> {
                // Rollback
                _groups.value = originalGroups
                _eventMessages.emit("Fehler beim Schalten der Gruppe '${group.name}': ${res.message}")
            }
        }
    }

    suspend fun setGroupBrightness(group: WizGroup, brightness: Int) {
        val clamped = brightness.coerceIn(10, 100)
        val originalGroups = _groups.value
        _groups.value = _groups.value.map {
            if (it.id == group.id) it.copy(brightnessPercent = clamped, isTurnedOn = true) else it
        }

        val macSet = group.deviceMacs.map { it.trim().lowercase() }.toSet()
        if (macSet.isNotEmpty()) {
            val updatedDevs = _devices.value.map { dev ->
                val devMac = dev.mac.trim().lowercase()
                if (macSet.contains(devMac)) {
                    dev.copy(state = dev.state.copy(dimming = clamped, state = true))
                } else dev
            }
            _devices.value = updatedDevs
            updateRoomsFromDevices(updatedDevs)
        }

        val res = apiClient.groupBrightness(group.id, clamped)
        when (res) {
            is ApiResult.Success -> {
                if (res.data.isPartialSuccess) {
                    _eventMessages.emit(res.data.message)
                }
            }
            is ApiResult.Error -> {
                _groups.value = originalGroups
                _eventMessages.emit("Fehler beim Dimmen der Gruppe '${group.name}': ${res.message}")
            }
        }
    }

    suspend fun setGroupTemperature(group: WizGroup, tempKelvin: Int) {
        val clamped = tempKelvin.coerceIn(2000, 9000)
        _groups.value = _groups.value.map {
            if (it.id == group.id) it.copy(temperatureKelvin = clamped, isTurnedOn = true) else it
        }

        val res = apiClient.groupTemperature(group.id, clamped)
        if (res is ApiResult.Error) {
            _eventMessages.emit("Fehler bei Farbtemperatur der Gruppe: ${res.message}")
        }
    }

    suspend fun syncRoomsAndGroups(): ApiResult<SyncData> {
        when (val res = apiClient.fetchSync()) {
            is ApiResult.Success -> {
                val syncData = res.data
                val syncGroups = syncData.groups
                if (!syncGroups.isNullOrEmpty()) {
                    val domainGroups = syncGroups.map { groupDtoToDomain(it) }
                    _groups.value = domainGroups
                    preferencesManager.saveCachedGroups(syncGroups)
                }
                val now = System.currentTimeMillis()
                val updatedCfg = _apiConfig.value.copy(
                    lastSyncTimestamp = now,
                    lastRevision = syncData.revision,
                    serverUpdatedAt = syncData.updatedAt,
                    lastSyncError = null
                )
                updateConfig(updatedCfg)
                _eventMessages.emit("Synchronisation erfolgreich (Revision: ${syncData.revision}).")
                return ApiResult.Success(syncData)
            }
            is ApiResult.Error -> {
                // Fallback: try fetching groups directly
                fetchGroups()
                val updatedCfg = _apiConfig.value.copy(lastSyncError = res.message)
                updateConfig(updatedCfg)
                return ApiResult.Error(res.message, res.code, res.httpStatus)
            }
        }
    }

    fun clearLocalConfiguration() {
        preferencesManager.clearAll()
        _devices.value = emptyList()
        _rooms.value = emptyList()
        _groups.value = emptyList()
        _apiConfig.value = ApiConfig()
        apiClient.updateConfig(_apiConfig.value)
    }
}
