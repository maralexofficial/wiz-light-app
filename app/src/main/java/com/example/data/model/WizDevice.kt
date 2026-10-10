package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WizDeviceState(
    @Json(name = "state") val state: Boolean = false,
    @Json(name = "sceneId") val sceneId: Int? = null,
    @Json(name = "temp") val temp: Int? = null,
    @Json(name = "dimming") val dimming: Int? = 100,
    @Json(name = "r") val r: Int? = null,
    @Json(name = "g") val g: Int? = null,
    @Json(name = "b") val b: Int? = null
)

@JsonClass(generateAdapter = true)
data class WizBulbDto(
    @Json(name = "ip") val ip: String = "",
    @Json(name = "mac") val mac: String? = null,
    @Json(name = "success") val success: Boolean? = true,
    @Json(name = "roomId") val roomId: Long? = null,
    @Json(name = "roomName") val roomName: String? = null,
    @Json(name = "deviceName") val deviceName: String? = null,
    @Json(name = "deviceType") val deviceType: String? = "light",
    @Json(name = "moduleName") val moduleName: String? = null,
    @Json(name = "firmwareVersion") val firmwareVersion: String? = null,
    @Json(name = "state") val state: WizDeviceState? = null,
    @Json(name = "rssi") val rssi: Int? = null
)

data class WizDevice(
    val ip: String,
    val mac: String,
    val roomId: Long?,
    val roomName: String,
    val deviceName: String,
    val deviceType: String,
    val moduleName: String?,
    val firmwareVersion: String?,
    val state: WizDeviceState,
    val rssi: Int?,
    val isOnline: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    val isLight: Boolean
        get() = deviceType.equals("light", ignoreCase = true) || deviceType.isBlank()

    val isPlug: Boolean
        get() = deviceType.equals("plug", ignoreCase = true) || deviceType.contains("socket", ignoreCase = true)

    val supportsRgb: Boolean
        get() = isLight && (state.r != null || state.g != null || state.b != null || moduleName?.contains("RGB", ignoreCase = true) == true)

    val supportsDimming: Boolean
        get() = isLight

    val brightnessPercent: Int
        get() = (state.dimming ?: 100).coerceIn(0, 100)

    val isTurnedOn: Boolean
        get() = state.state
}
