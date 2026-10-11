package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiError(
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "timestamp") val timestamp: Long? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "error") val error: ApiError? = null
)

@JsonClass(generateAdapter = true)
data class DiscoveryData(
    @Json(name = "count") val count: Int? = null,
    @Json(name = "bulbs") val bulbs: List<WizBulbDto>? = null,
    @Json(name = "lastDiscoveryTime") val lastDiscoveryTime: Long? = null
)

@JsonClass(generateAdapter = true)
data class IpRequest(
    @Json(name = "ip") val ip: String
)

@JsonClass(generateAdapter = true)
data class BrightnessRequest(
    @Json(name = "ip") val ip: String,
    @Json(name = "brightness") val brightness: Int
)

@JsonClass(generateAdapter = true)
data class ColorRequest(
    @Json(name = "ip") val ip: String,
    @Json(name = "r") val r: Int,
    @Json(name = "g") val g: Int,
    @Json(name = "b") val b: Int
)

@JsonClass(generateAdapter = true)
data class WizRoomDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class SyncData(
    @Json(name = "revision") val revision: Long? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "rooms") val rooms: List<WizRoomDto>? = null,
    @Json(name = "groups") val groups: List<WizGroupDto>? = null
)
