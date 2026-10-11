package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WizGroupStateDto(
    @Json(name = "state") val state: Boolean = false,
    @Json(name = "dimming") val dimming: Int? = 100,
    @Json(name = "temp") val temp: Int? = null,
    @Json(name = "temperature") val temperature: Int? = null
)

@JsonClass(generateAdapter = true)
data class WizGroupDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "roomId") val roomId: Long = 0L,
    @Json(name = "deviceType") val deviceType: String = "light",
    @Json(name = "deviceMacs") val deviceMacs: List<String>? = null,
    @Json(name = "members") val members: List<String>? = null,
    @Json(name = "state") val state: WizGroupStateDto? = null
)

data class WizGroup(
    val id: String,
    val name: String,
    val roomId: Long,
    val deviceType: String = "light", // "light" or "plug"
    val deviceMacs: List<String> = emptyList(),
    val isTurnedOn: Boolean = false,
    val brightnessPercent: Int = 100,
    val temperatureKelvin: Int? = null
) {
    val isLight: Boolean
        get() = deviceType.equals("light", ignoreCase = true)

    val isPlug: Boolean
        get() = deviceType.equals("plug", ignoreCase = true)

    val memberCount: Int
        get() = deviceMacs.size
}

@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    @Json(name = "name") val name: String,
    @Json(name = "roomId") val roomId: Long,
    @Json(name = "deviceType") val deviceType: String,
    @Json(name = "deviceMacs") val deviceMacs: List<String>
)

@JsonClass(generateAdapter = true)
data class UpdateGroupRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "deviceMacs") val deviceMacs: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GroupBrightnessRequest(
    @Json(name = "brightness") val brightness: Int // 10..100
)

@JsonClass(generateAdapter = true)
data class GroupTemperatureRequest(
    @Json(name = "temperature") val temperature: Int // in Kelvin
)

data class GroupActionResult(
    val isFullSuccess: Boolean,
    val isPartialSuccess: Boolean,
    val successfulCount: Int,
    val failedCount: Int,
    val message: String
)
