package com.example.data.remote

import com.example.data.model.ApiConfig
import com.example.data.model.ApiResponse
import com.example.data.model.CreateGroupRequest
import com.example.data.model.CreatePresetRequest
import com.example.data.model.DiscoveryData
import com.example.data.model.GroupActionResult
import com.example.data.model.GroupBrightnessRequest
import com.example.data.model.GroupTemperatureRequest
import com.example.data.model.SyncData
import com.example.data.model.UpdateGroupRequest
import com.example.data.model.UpdatePresetRequest
import com.example.data.model.WizBulbDto
import com.example.data.model.WizDeviceState
import com.example.data.model.WizGroupDto
import com.example.data.model.WizGroupStateDto
import com.example.data.model.WizPresetDto
import com.example.data.model.WizRoomDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T, val message: String? = null) : ApiResult<T>()
    data class Error(val message: String, val code: String? = null, val httpStatus: Int? = null) : ApiResult<Nothing>()
}

class ApiClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private var currentConfig: ApiConfig = ApiConfig()
    private var apiService: WizApiService? = null

    init {
        updateConfig(currentConfig)
    }

    @Synchronized
    fun updateConfig(config: ApiConfig) {
        this.currentConfig = config
        val normalizedUrl = normalizeBaseUrl(config.baseUrl)

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                if (config.apiToken.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer ${config.apiToken.trim()}")
                }

                chain.proceed(requestBuilder.build())
            })

        // Add logging for debug without logging secrets
        val logging = HttpLoggingInterceptor { _ -> }
        logging.level = HttpLoggingInterceptor.Level.BASIC
        clientBuilder.addInterceptor(logging)

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(clientBuilder.build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        this.apiService = retrofit.create(WizApiService::class.java)
    }

    fun normalizeBaseUrl(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            trimmed = "http://192.168.1.100:3000"
        }
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed = "http://$trimmed"
        }
        trimmed = trimmed.trimEnd('/')
        return "$trimmed/"
    }

    private fun getService(): WizApiService {
        return apiService ?: synchronized(this) {
            apiService ?: run {
                updateConfig(currentConfig)
                apiService!!
            }
        }
    }

    suspend fun discoverDevices(): ApiResult<List<WizBulbDto>> {
        return try {
            val response = getService().discover()
            handleDiscoveryResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun testConnection(): ApiResult<String> {
        return try {
            val response = getService().checkHealth()
            if (response.isSuccessful) {
                val bodyStr = response.body()?.string() ?: ""
                val statusText = parseHealthStatus(bodyStr)
                ApiResult.Success("Verbindung erfolgreich. Server meldet: $statusText")
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            try {
                val rootRes = getService().getRootInfo()
                if (rootRes.isSuccessful) {
                    ApiResult.Success("Verbindung zur REST-API erfolgreich (Root-Status ${rootRes.code()}).")
                } else {
                    handleHttpError(rootRes.code(), rootRes.errorBody()?.string())
                }
            } catch (inner: Exception) {
                handleException(inner)
            }
        }
    }

    suspend fun testConnectionWith(baseUrl: String, token: String): ApiResult<String> {
        val normalized = normalizeBaseUrl(baseUrl)
        val tempClientBuilder = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                if (token.isNotBlank()) {
                    req.header("Authorization", "Bearer ${token.trim()}")
                }
                chain.proceed(req.build())
            }

        return try {
            val tempRetrofit = Retrofit.Builder()
                .baseUrl(normalized)
                .client(tempClientBuilder.build())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            val tempService = tempRetrofit.create(WizApiService::class.java)

            // 1. Try health check
            val healthRes = tempService.checkHealth()
            if (!healthRes.isSuccessful) {
                return handleHttpError(healthRes.code(), healthRes.errorBody()?.string())
            }

            // 2. Validate token against authenticated reading endpoint (getBulbs or getStates)
            val authCheckRes = tempService.getBulbs()
            if (authCheckRes.isSuccessful) {
                ApiResult.Success("Verbindung und Authentifizierung erfolgreich!")
            } else if (authCheckRes.code() == 401) {
                ApiResult.Error(
                    message = "Authentifizierung fehlgeschlagen: Das angegebene API-Token ist ungültig oder abgelaufen (HTTP 401).",
                    httpStatus = 401
                )
            } else if (authCheckRes.code() == 403) {
                // 403 means token is valid, but devices.read is not granted for this specific endpoint
                ApiResult.Success("Verbindung erfolgreich. Hinweis: Token besitzt eingeschränkte Berechtigungen (HTTP 403 für Lampenliste).")
            } else {
                val bodyStr = healthRes.body()?.string() ?: ""
                val statusText = parseHealthStatus(bodyStr)
                ApiResult.Success("Verbindung erfolgreich ($statusText).")
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun turnOn(ip: String): ApiResult<Unit> {
        return try {
            val response = getService().turnOn(com.example.data.model.IpRequest(ip))
            handleGenericApiResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun turnOff(ip: String): ApiResult<Unit> {
        return try {
            val response = getService().turnOff(com.example.data.model.IpRequest(ip))
            handleGenericApiResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun toggle(ip: String): ApiResult<Unit> {
        return try {
            val response = getService().toggle(com.example.data.model.IpRequest(ip))
            handleGenericApiResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun setBrightness(ip: String, brightness: Int): ApiResult<Unit> {
        return try {
            val clamped = brightness.coerceIn(0, 100)
            val response = getService().setBrightness(com.example.data.model.BrightnessRequest(ip, clamped))
            handleGenericApiResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun setColor(ip: String, r: Int, g: Int, b: Int): ApiResult<Unit> {
        return try {
            val cr = r.coerceIn(0, 255)
            val cg = g.coerceIn(0, 255)
            val cb = b.coerceIn(0, 255)
            val response = getService().setColor(com.example.data.model.ColorRequest(ip, cr, cg, cb))
            handleGenericApiResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun broadcastOn(): ApiResult<Unit> {
        return try {
            val response = getService().broadcastOn()
            handleGenericApiResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun broadcastOff(): ApiResult<Unit> {
        return try {
            val response = getService().broadcastOff()
            handleGenericApiResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    // =========================================================================
    // Groups & Rooms API
    // =========================================================================

    suspend fun fetchSync(): ApiResult<SyncData> {
        return try {
            val response = getService().getSync()
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string() ?: ""
                val syncData = parseSyncData(jsonStr)
                ApiResult.Success(syncData)
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun fetchRooms(): ApiResult<List<WizRoomDto>> {
        return try {
            val response = getService().getRooms()
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string() ?: ""
                val rooms = parseRooms(jsonStr)
                ApiResult.Success(rooms)
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun fetchGroups(): ApiResult<List<WizGroupDto>> {
        return try {
            val response = getService().getGroups()
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string() ?: ""
                val groups = parseGroups(jsonStr)
                ApiResult.Success(groups)
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun fetchRoomGroups(roomId: Long): ApiResult<List<WizGroupDto>> {
        return try {
            val response = getService().getRoomGroups(roomId)
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string() ?: ""
                val groups = parseGroups(jsonStr)
                ApiResult.Success(groups)
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun createGroup(request: CreateGroupRequest): ApiResult<WizGroupDto> {
        return try {
            val response = getService().createGroup(request)
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string() ?: ""
                val group = parseSingleGroup(jsonStr)
                if (group != null) {
                    ApiResult.Success(group)
                } else {
                    ApiResult.Success(
                        WizGroupDto(
                            id = "grp_${System.currentTimeMillis()}",
                            name = request.name,
                            roomId = request.roomId,
                            deviceType = request.deviceType,
                            deviceMacs = request.deviceMacs
                        )
                    )
                }
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun updateGroup(id: String, request: UpdateGroupRequest): ApiResult<WizGroupDto> {
        return try {
            val response = getService().updateGroup(id, request)
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string() ?: ""
                val group = parseSingleGroup(jsonStr)
                if (group != null) {
                    ApiResult.Success(group)
                } else {
                    ApiResult.Success(
                        WizGroupDto(
                            id = id,
                            name = request.name ?: "",
                            deviceMacs = request.deviceMacs
                        )
                    )
                }
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun deleteGroup(id: String): ApiResult<Unit> {
        return try {
            val response = getService().deleteGroup(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun groupOn(id: String): ApiResult<GroupActionResult> {
        return try {
            val response = getService().groupOn(id)
            handleGroupActionResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun groupOff(id: String): ApiResult<GroupActionResult> {
        return try {
            val response = getService().groupOff(id)
            handleGroupActionResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun groupBrightness(id: String, brightness: Int): ApiResult<GroupActionResult> {
        return try {
            // Clamped 10..100 per specification
            val clamped = brightness.coerceIn(10, 100)
            val response = getService().groupBrightness(id, GroupBrightnessRequest(clamped))
            handleGroupActionResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun groupTemperature(id: String, temperatureKelvin: Int): ApiResult<GroupActionResult> {
        return try {
            val clamped = temperatureKelvin.coerceIn(2000, 9000)
            val response = getService().groupTemperature(id, GroupTemperatureRequest(clamped))
            handleGroupActionResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    // =========================================================================
    // Presets API
    // =========================================================================

    suspend fun fetchPresets(): ApiResult<List<WizPresetDto>> {
        return try {
            val response = getService().getPresets()
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string() ?: ""
                val presets = parsePresets(jsonStr)
                ApiResult.Success(presets)
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun createPreset(request: CreatePresetRequest): ApiResult<WizPresetDto> {
        return try {
            val response = getService().createPreset(request)
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string() ?: ""
                val created = parseSinglePreset(jsonStr)
                if (created != null) {
                    ApiResult.Success(created)
                } else {
                    ApiResult.Success(
                        WizPresetDto(
                            id = "preset_${System.currentTimeMillis()}",
                            name = request.name,
                            icon = request.icon,
                            targetType = request.targetType,
                            targetId = request.targetId,
                            targetName = request.targetName,
                            state = request.state,
                            dimming = request.dimming,
                            temperature = request.temperature,
                            r = request.r,
                            g = request.g,
                            b = request.b
                        )
                    )
                }
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun updatePreset(id: String, request: UpdatePresetRequest): ApiResult<WizPresetDto> {
        return try {
            val response = getService().updatePreset(id, request)
            if (response.isSuccessful) {
                val jsonStr = response.body()?.string() ?: ""
                val updated = parseSinglePreset(jsonStr)
                if (updated != null) {
                    ApiResult.Success(updated)
                } else {
                    ApiResult.Success(
                        WizPresetDto(
                            id = id,
                            name = request.name ?: "",
                            icon = request.icon,
                            targetType = request.targetType ?: "group",
                            targetId = request.targetId ?: "",
                            targetName = request.targetName,
                            state = request.state ?: true,
                            dimming = request.dimming,
                            temperature = request.temperature,
                            r = request.r,
                            g = request.g,
                            b = request.b
                        )
                    )
                }
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun deletePreset(id: String): ApiResult<Unit> {
        return try {
            val response = getService().deletePreset(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun applyPreset(id: String): ApiResult<GroupActionResult> {
        return try {
            val response = getService().applyPreset(id)
            handleGroupActionResponse(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun handleGroupActionResponse(response: Response<ResponseBody>): ApiResult<GroupActionResult> {
        val code = response.code()
        val bodyStr = if (response.isSuccessful || code == 207) {
            response.body()?.string() ?: response.errorBody()?.string().orEmpty()
        } else {
            response.errorBody()?.string().orEmpty()
        }

        if (code == 200 || code == 204) {
            return ApiResult.Success(
                GroupActionResult(
                    isFullSuccess = true,
                    isPartialSuccess = false,
                    successfulCount = 0,
                    failedCount = 0,
                    message = "Gruppe erfolgreich geschaltet."
                )
            )
        }

        if (code == 207) {
            // Multi-status partial failure handling
            val parsedResult = parseMultiStatus(bodyStr)
            return ApiResult.Success(parsedResult)
        }

        return handleHttpError(code, bodyStr)
    }

    private fun parseMultiStatus(jsonStr: String): GroupActionResult {
        var total = 0
        var successCount = 0
        var failCount = 0

        try {
            val root = JSONObject(jsonStr)
            val dataObj = if (root.has("data")) root.optJSONObject("data") else root
            if (dataObj != null) {
                if (dataObj.has("successful") && dataObj.has("failed")) {
                    successCount = dataObj.optInt("successful", 0)
                    failCount = dataObj.optInt("failed", 0)
                    total = dataObj.optInt("total", successCount + failCount)
                } else if (dataObj.has("results")) {
                    val arr = dataObj.optJSONArray("results")
                    if (arr != null) {
                        total = arr.length()
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i)
                            if (item != null && item.optBoolean("success", false)) {
                                successCount++
                            } else {
                                failCount++
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (total == 0) {
            total = successCount + failCount
        }

        val msg = if (failCount > 0) {
            "Teilweiser Erfolg (HTTP 207): $successCount erfolgreich, $failCount fehlgeschlagen."
        } else {
            "Gruppe geschaltet ($successCount Geräte erfolgreich)."
        }

        return GroupActionResult(
            isFullSuccess = failCount == 0,
            isPartialSuccess = failCount > 0,
            successfulCount = successCount,
            failedCount = failCount,
            message = msg
        )
    }

    private fun handleDiscoveryResponse(response: Response<ApiResponse<DiscoveryData>>): ApiResult<List<WizBulbDto>> {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                if (body.success && body.data != null) {
                    val bulbs = body.data.bulbs ?: emptyList()
                    return ApiResult.Success(bulbs)
                }
                if (!body.success && body.error != null) {
                    return ApiResult.Error(
                        message = body.error.message ?: "Geräteerkennung durch API abgelehnt.",
                        code = body.error.code
                    )
                }
            }
            return ApiResult.Success(emptyList())
        }
        return handleHttpError(response.code(), response.errorBody()?.string())
    }

    private fun handleGenericApiResponse(response: Response<ApiResponse<Any>>): ApiResult<Unit> {
        if (response.isSuccessful) {
            val body = response.body()
            if (body == null || body.success) {
                return ApiResult.Success(Unit)
            }
            val errMsg = body.error?.message ?: "Aktion fehlgeschlagen."
            return ApiResult.Error(errMsg, code = body.error?.code)
        }
        return handleHttpError(response.code(), response.errorBody()?.string())
    }

    private fun handleHttpError(code: Int, errorBody: String?): ApiResult.Error {
        var parsedCode: String? = null
        var parsedApiMessage: String? = null

        try {
            if (!errorBody.isNullOrBlank()) {
                val obj = JSONObject(errorBody)
                if (obj.has("error")) {
                    val errObj = obj.optJSONObject("error")
                    if (errObj != null) {
                        parsedApiMessage = errObj.optString("message").takeIf { it.isNotBlank() }
                        parsedCode = errObj.optString("code").takeIf { it.isNotBlank() }
                    } else {
                        parsedApiMessage = obj.optString("error").takeIf { it.isNotBlank() }
                    }
                } else if (obj.has("message")) {
                    parsedApiMessage = obj.optString("message").takeIf { it.isNotBlank() }
                }
                if (parsedCode == null && obj.has("code")) {
                    parsedCode = obj.optString("code").takeIf { it.isNotBlank() }
                }
            }
        } catch (_: Exception) {
            // JSON parsing fallback
        }

        val message = when (code) {
            400 -> parsedApiMessage ?: "Ungültige Anfrage: Bitte Eingaben und Gruppenzuordnungen prüfen (HTTP 400)."
            401 -> {
                val detail = parsedApiMessage?.let { " ($it)" } ?: ""
                "Authentifizierung fehlgeschlagen: Ungültiges oder abgelaufenes API-Token$detail (HTTP 401)."
            }
            403 -> {
                val detail = parsedApiMessage?.let { " ($it)" } ?: ""
                "Zugriff verweigert: Fehlende Berechtigung für diese Aktion$detail (HTTP 403)."
            }
            404 -> parsedApiMessage ?: "API-Endpunkt oder Ressource nicht gefunden: Bitte URL und ID prüfen (HTTP 404)."
            502 -> "Geräte-Timeout: Das WiZ-Gerät hat nicht rechtzeitig geantwortet (HTTP 502 Bad Gateway)."
            504 -> "Gateway-Timeout: Zeitüberschreitung bei der Kommunikation mit dem WiZ-Gerät (HTTP 504)."
            in 500..599 -> parsedApiMessage ?: "Serverfehler: Die REST-API meldet einen internen Fehler (HTTP $code)."
            else -> parsedApiMessage ?: "HTTP-Anfrage fehlgeschlagen mit Status $code."
        }
        return ApiResult.Error(message = message, code = parsedCode, httpStatus = code)
    }

    private fun handleException(e: Exception): ApiResult.Error {
        val germanMsg = when (e) {
            is ConnectException -> "Keine Verbindung zum Server möglich. Ist der REST-API Server gestartet und die IP korrekt?"
            is SocketTimeoutException -> "Zeitüberschreitung der Netzwerkanfrage (Timeout). Der Server antwortet nicht rechtzeitig."
            is UnknownHostException -> "Serveradresse nicht auflösbar. Bitte überprüfe Hostname oder IP-Adresse in den Einstellungen."
            is IOException -> "Netzwerkfehler bei der Kommunikation mit der REST-API: ${e.localizedMessage ?: "Verbindung unterbrochen"}."
            else -> "Unerwarteter Fehler: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
        return ApiResult.Error(germanMsg)
    }

    private fun parseHealthStatus(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            val data = obj.optJSONObject("data")
            if (data != null && data.has("status")) {
                data.optString("status")
            } else if (obj.has("status")) {
                obj.optString("status")
            } else {
                "Online"
            }
        } catch (_: Exception) {
            "Online (HTTP 200 OK)"
        }
    }

    private fun parseSyncData(jsonStr: String): SyncData {
        return try {
            val root = JSONObject(jsonStr)
            val dataObj = if (root.has("data")) root.optJSONObject("data") ?: root else root

            val revision = if (dataObj.has("revision")) dataObj.optLong("revision") else null
            val updatedAt = if (dataObj.has("updatedAt")) dataObj.optString("updatedAt") else null

            val roomList = mutableListOf<WizRoomDto>()
            val roomsArr = dataObj.optJSONArray("rooms")
            if (roomsArr != null) {
                for (i in 0 until roomsArr.length()) {
                    val rObj = roomsArr.optJSONObject(i) ?: continue
                    val id = rObj.optLong("id", rObj.optLong("roomId", 0L))
                    val name = rObj.optString("name", rObj.optString("roomName", "Raum $id"))
                    roomList.add(WizRoomDto(id, name))
                }
            }

            val groupList = mutableListOf<WizGroupDto>()
            val groupsArr = dataObj.optJSONArray("groups")
            if (groupsArr != null) {
                for (i in 0 until groupsArr.length()) {
                    val gObj = groupsArr.optJSONObject(i) ?: continue
                    val group = jsonToGroupDto(gObj)
                    if (group != null) groupList.add(group)
                }
            }

            SyncData(
                revision = revision,
                updatedAt = updatedAt,
                rooms = roomList,
                groups = groupList
            )
        } catch (_: Exception) {
            SyncData()
        }
    }

    private fun parseRooms(jsonStr: String): List<WizRoomDto> {
        val list = mutableListOf<WizRoomDto>()
        try {
            val root = JSONObject(jsonStr)
            val arr = (if (root.has("data")) {
                val d = root.opt("data")
                if (d is JSONArray) d else if (d is JSONObject && d.has("rooms")) d.optJSONArray("rooms") else null
            } else if (root.has("rooms")) {
                root.optJSONArray("rooms")
            } else null) ?: return list

            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optLong("id", obj.optLong("roomId", 0L))
                val name = obj.optString("name", obj.optString("roomName", "Raum $id"))
                list.add(WizRoomDto(id, name))
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseGroups(jsonStr: String): List<WizGroupDto> {
        val list = mutableListOf<WizGroupDto>()
        try {
            val root = JSONObject(jsonStr)
            val arr = (if (root.has("data")) {
                val d = root.opt("data")
                if (d is JSONArray) d else if (d is JSONObject && d.has("groups")) d.optJSONArray("groups") else null
            } else if (root.has("groups")) {
                root.optJSONArray("groups")
            } else null) ?: return list

            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val group = jsonToGroupDto(obj)
                if (group != null) list.add(group)
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseSingleGroup(jsonStr: String): WizGroupDto? {
        return try {
            val root = JSONObject(jsonStr)
            val obj = if (root.has("data")) root.optJSONObject("data") ?: root else root
            jsonToGroupDto(obj)
        } catch (_: Exception) {
            null
        }
    }

    private fun jsonToGroupDto(obj: JSONObject): WizGroupDto? {
        val id = obj.optString("id", obj.optString("_id", ""))
        if (id.isBlank()) return null
        val name = obj.optString("name", "Gruppe")
        val roomId = obj.optLong("roomId", 0L)
        val deviceType = obj.optString("deviceType", "light")

        val members = mutableListOf<String>()
        val membersArr = obj.optJSONArray("deviceMacs") ?: obj.optJSONArray("members")
        if (membersArr != null) {
            for (j in 0 until membersArr.length()) {
                val m = membersArr.optString(j)
                if (!m.isNullOrBlank()) members.add(m)
            }
        }

        val stateObj = obj.optJSONObject("state")
        val stateDto = if (stateObj != null) {
            WizGroupStateDto(
                state = stateObj.optBoolean("state", false),
                dimming = if (stateObj.has("dimming")) stateObj.optInt("dimming") else 100,
                temp = if (stateObj.has("temp")) stateObj.optInt("temp") else null,
                temperature = if (stateObj.has("temperature")) stateObj.optInt("temperature") else null
            )
        } else null

        return WizGroupDto(
            id = id,
            name = name,
            roomId = roomId,
            deviceType = deviceType,
            deviceMacs = members,
            state = stateDto
        )
    }

    private fun parsePresets(jsonStr: String): List<WizPresetDto> {
        val list = mutableListOf<WizPresetDto>()
        try {
            val trimmed = jsonStr.trim()
            val root = if (trimmed.startsWith("{")) JSONObject(trimmed) else null
            val arr = (if (root != null) {
                if (root.has("data")) {
                    val d = root.opt("data")
                    if (d is JSONArray) d else if (d is JSONObject && d.has("presets")) d.optJSONArray("presets") else null
                } else if (root.has("presets")) {
                    root.optJSONArray("presets")
                } else null
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else null) ?: return list

            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val dto = jsonToPresetDto(item)
                if (dto != null) list.add(dto)
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseSinglePreset(jsonStr: String): WizPresetDto? {
        return try {
            val root = JSONObject(jsonStr)
            val obj = if (root.has("data")) root.optJSONObject("data") ?: root else root
            jsonToPresetDto(obj)
        } catch (_: Exception) {
            null
        }
    }

    private fun jsonToPresetDto(obj: JSONObject): WizPresetDto? {
        val id = obj.optString("id", obj.optString("_id", ""))
        if (id.isBlank()) return null
        val name = obj.optString("name", "Preset")
        val icon = if (obj.has("icon")) obj.optString("icon", "lightbulb") else "lightbulb"
        val targetType = obj.optString("targetType", "group")
        val targetId = obj.optString("targetId", "")
        val targetName = if (obj.has("targetName")) obj.optString("targetName", null) else null
        val state = obj.optBoolean("state", true)
        val dimming = if (obj.has("dimming")) obj.optInt("dimming", 100) else 100
        val temp = if (obj.has("temperature")) obj.optInt("temperature") else null
        val r = if (obj.has("r")) obj.optInt("r") else null
        val g = if (obj.has("g")) obj.optInt("g") else null
        val b = if (obj.has("b")) obj.optInt("b") else null

        return WizPresetDto(
            id = id,
            name = name,
            icon = icon,
            targetType = targetType,
            targetId = targetId,
            targetName = targetName,
            state = state,
            dimming = dimming,
            temperature = temp,
            r = r,
            g = g,
            b = b
        )
    }
}
