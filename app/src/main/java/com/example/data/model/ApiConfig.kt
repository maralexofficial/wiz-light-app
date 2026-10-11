package com.example.data.model

data class ApiConfig(
    val baseUrl: String = "http://192.168.1.100:3000",
    val apiToken: String = "",
    val autoRefresh: Boolean = false,
    val refreshIntervalSeconds: Int = 30,
    val themeMode: String = "system",
    val lastTestStatus: Boolean? = null,
    val lastTestMessage: String? = null,
    val lastTestTimestamp: Long? = null,
    val lastSyncTimestamp: Long? = null,
    val syncMode: String = "AUTO", // "AUTO" or "MANUAL"
    val lastRevision: Long? = null,
    val serverUpdatedAt: String? = null,
    val lastSyncError: String? = null
)
