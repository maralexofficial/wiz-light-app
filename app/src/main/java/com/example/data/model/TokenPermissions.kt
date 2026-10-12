package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Repräsentiert die Berechtigungen des aktuellen Tokens im Backend.
 * Standardmäßig sind bei fehlender expliziter Einschränkung alle Aktionen aktiv (Full Access).
 * Sobald das Backend 403 Forbidden meldet oder ein bestimmtes Token konfiguriert ist,
 * wird die Matrix präzise angepasst.
 */
@JsonClass(generateAdapter = true)
data class TokenPermissions(
    @Json(name = "systemRead") val canSystemRead: Boolean = true,
    @Json(name = "systemBroadcast") val canSystemBroadcast: Boolean = true,
    @Json(name = "devicesDiscover") val canDevicesDiscover: Boolean = true,
    @Json(name = "devicesRead") val canDevicesRead: Boolean = true,
    @Json(name = "devicesControl") val canDevicesControl: Boolean = true,
    @Json(name = "roomsRead") val canRoomsRead: Boolean = true,
    @Json(name = "groupsRead") val canGroupsRead: Boolean = true,
    @Json(name = "groupsCreate") val canGroupsCreate: Boolean = true,
    @Json(name = "groupsUpdate") val canGroupsUpdate: Boolean = true,
    @Json(name = "groupsDelete") val canGroupsDelete: Boolean = true,
    @Json(name = "presetsRead") val canPresetsRead: Boolean = true,
    @Json(name = "presetsExecute") val canPresetsExecute: Boolean = true,
    @Json(name = "presetsManage") val canPresetsManage: Boolean = true
) {
    val isReadOnly: Boolean
        get() = !canDevicesControl && !canSystemBroadcast && !canGroupsCreate &&
                !canGroupsUpdate && !canGroupsDelete && !canPresetsManage && !canPresetsExecute

    companion object {
        val ALL_GRANTED = TokenPermissions()

        val READ_ONLY = TokenPermissions(
            canSystemRead = true,
            canSystemBroadcast = false,
            canDevicesDiscover = true,
            canDevicesRead = true,
            canDevicesControl = false,
            canRoomsRead = true,
            canGroupsRead = true,
            canGroupsCreate = false,
            canGroupsUpdate = false,
            canGroupsDelete = false,
            canPresetsRead = true,
            canPresetsExecute = false,
            canPresetsManage = false
        )
    }
}
