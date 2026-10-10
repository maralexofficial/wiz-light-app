package com.example.data.model

data class WizRoom(
    val id: Long,
    val name: String,
    val devices: List<WizDevice> = emptyList()
) {
    val totalCount: Int
        get() = devices.size

    val activeCount: Int
        get() = devices.count { it.isOnline && it.isTurnedOn }

    val onlineCount: Int
        get() = devices.count { it.isOnline }

    val hasActiveLights: Boolean
        get() = activeCount > 0

    val averageBrightness: Int
        get() {
            val lights = devices.filter { it.isOnline && it.isLight && it.isTurnedOn }
            if (lights.isEmpty()) return 0
            return (lights.sumOf { it.brightnessPercent } / lights.size).coerceIn(0, 100)
        }
}
