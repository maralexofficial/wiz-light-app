package com.example.data.remote

import com.example.data.model.ApiConfig
import com.example.data.model.ApiResponse
import com.example.data.model.DiscoveryData
import com.example.data.model.WizBulbDto
import com.example.data.model.WizDeviceState
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
        // remove trailing slashes
        trimmed = trimmed.trimEnd('/')
        // ensure single trailing slash for Retrofit baseUrl
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
            // If /api/health returned 404 or failed, try root endpoint "/"
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

    suspend fun fetchDeviceState(ip: String): ApiResult<WizDeviceState> {
        return try {
            val response = getService().getState(ip)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: ""
                val state = parseStateFromJson(json)
                if (state != null) {
                    ApiResult.Success(state)
                } else {
                    ApiResult.Error("Keine Gerätezustandsdaten empfangen.")
                }
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            handleException(e)
        }
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
        val parsedApiMessage = try {
            if (!errorBody.isNullOrBlank()) {
                val obj = JSONObject(errorBody)
                if (obj.has("error")) {
                    val err = obj.optJSONObject("error")
                    err?.optString("message") ?: obj.optString("error")
                } else if (obj.has("message")) {
                    obj.optString("message")
                } else null
            } else null
        } catch (_: Exception) {
            null
        }

        val message = when (code) {
            401 -> "Authentifizierung fehlgeschlagen: Ungültiges oder abgelaufenes API-Token (HTTP 401)."
            403 -> "Zugriff verweigert: Fehlende Berechtigung für diese Anfrage (HTTP 403)."
            404 -> "API-Endpunkt nicht gefunden: Bitte URL und Konfiguration prüfen (HTTP 404)."
            502 -> "Geräte-Timeout: Das WiZ-Gerät hat nicht rechtzeitig geantwortet (HTTP 502 Bad Gateway)."
            504 -> "Gateway-Timeout: Zeitüberschreitung bei der Kommunikation mit dem WiZ-Gerät (HTTP 504)."
            in 500..599 -> "Serverfehler: Die REST-API meldet einen internen Fehler (HTTP $code)."
            else -> parsedApiMessage ?: "HTTP-Anfrage fehlgeschlagen mit Status $code."
        }
        return ApiResult.Error(message = message, httpStatus = code)
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

    private fun parseStateFromJson(jsonStr: String): WizDeviceState? {
        return try {
            val obj = JSONObject(jsonStr)
            val stateObj: JSONObject = when {
                obj.has("data") -> {
                    val data = obj.opt("data")
                    if (data is JSONObject) {
                        data.optJSONObject("state") ?: data
                    } else obj
                }
                obj.has("state") -> obj.optJSONObject("state") ?: obj
                else -> obj
            }

            WizDeviceState(
                state = stateObj.optBoolean("state", false),
                sceneId = if (stateObj.has("sceneId")) stateObj.optInt("sceneId") else null,
                temp = if (stateObj.has("temp")) stateObj.optInt("temp") else null,
                dimming = if (stateObj.has("dimming")) stateObj.optInt("dimming") else 100,
                r = if (stateObj.has("r")) stateObj.optInt("r") else null,
                g = if (stateObj.has("g")) stateObj.optInt("g") else null,
                b = if (stateObj.has("b")) stateObj.optInt("b") else null
            )
        } catch (_: Exception) {
            null
        }
    }
}
