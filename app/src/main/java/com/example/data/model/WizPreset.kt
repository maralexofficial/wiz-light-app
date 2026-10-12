package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WizPresetDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "icon") val icon: String? = "lightbulb",
    @Json(name = "targetType") val targetType: String = "group", // "group" or "device"
    @Json(name = "targetId") val targetId: String = "", // groupId or mac/ip
    @Json(name = "targetName") val targetName: String? = null,
    @Json(name = "state") val state: Boolean = true,
    @Json(name = "dimming") val dimming: Int? = 100, // 10..100
    @Json(name = "temperature") val temperature: Int? = null, // Kelvin 2000..9000
    @Json(name = "r") val r: Int? = null,
    @Json(name = "g") val g: Int? = null,
    @Json(name = "b") val b: Int? = null,
    @Json(name = "createdAt") val createdAt: Long? = null,
    @Json(name = "updatedAt") val updatedAt: Long? = null
)

data class WizPreset(
    val id: String,
    val name: String,
    val icon: String = "lightbulb",
    val targetType: String = "group", // "group" or "device"
    val targetId: String,
    val targetName: String = "",
    val state: Boolean = true,
    val dimming: Int = 100,
    val temperature: Int? = null,
    val r: Int? = null,
    val g: Int? = null,
    val b: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isGroupTarget: Boolean
        get() = targetType.equals("group", ignoreCase = true)

    val isDeviceTarget: Boolean
        get() = targetType.equals("device", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class CreatePresetRequest(
    @Json(name = "name") val name: String,
    @Json(name = "icon") val icon: String? = "lightbulb",
    @Json(name = "targetType") val targetType: String,
    @Json(name = "targetId") val targetId: String,
    @Json(name = "targetName") val targetName: String? = null,
    @Json(name = "state") val state: Boolean = true,
    @Json(name = "dimming") val dimming: Int? = 100,
    @Json(name = "temperature") val temperature: Int? = null,
    @Json(name = "r") val r: Int? = null,
    @Json(name = "g") val g: Int? = null,
    @Json(name = "b") val b: Int? = null
)

@JsonClass(generateAdapter = true)
data class UpdatePresetRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "targetType") val targetType: String? = null,
    @Json(name = "targetId") val targetId: String? = null,
    @Json(name = "targetName") val targetName: String? = null,
    @Json(name = "state") val state: Boolean? = null,
    @Json(name = "dimming") val dimming: Int? = null,
    @Json(name = "temperature") val temperature: Int? = null,
    @Json(name = "r") val r: Int? = null,
    @Json(name = "g") val g: Int? = null,
    @Json(name = "b") val b: Int? = null
)
