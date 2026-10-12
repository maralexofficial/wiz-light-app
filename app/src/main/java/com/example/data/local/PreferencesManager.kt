package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ApiConfig
import com.example.data.model.WizBulbDto
import com.example.data.model.WizGroupDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wiz_smart_home_prefs", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val bulbListType = Types.newParameterizedType(List::class.java, WizBulbDto::class.java)
    private val bulbListAdapter = moshi.adapter<List<WizBulbDto>>(bulbListType)
    private val groupListType = Types.newParameterizedType(List::class.java, WizGroupDto::class.java)
    private val groupListAdapter = moshi.adapter<List<WizGroupDto>>(groupListType)

    private val permissionsAdapter = moshi.adapter(com.example.data.model.TokenPermissions::class.java)

    fun getApiConfig(): ApiConfig {
        val permissionsJson = prefs.getString(KEY_PERMISSIONS, null)
        val tokenPermissions = if (permissionsJson != null) {
            try {
                permissionsAdapter.fromJson(permissionsJson) ?: com.example.data.model.TokenPermissions.ALL_GRANTED
            } catch (_: Exception) {
                com.example.data.model.TokenPermissions.ALL_GRANTED
            }
        } else {
            com.example.data.model.TokenPermissions.ALL_GRANTED
        }

        val isConfigured = prefs.getBoolean(KEY_IS_CONFIGURED, false)

        return ApiConfig(
            baseUrl = prefs.getString(KEY_BASE_URL, "http://192.168.1.100:3000") ?: "http://192.168.1.100:3000",
            apiToken = prefs.getString(KEY_API_TOKEN, "") ?: "",
            isConfigured = isConfigured,
            autoRefresh = prefs.getBoolean(KEY_AUTO_REFRESH, false),
            refreshIntervalSeconds = prefs.getInt(KEY_REFRESH_INTERVAL, 30),
            themeMode = prefs.getString(KEY_THEME_MODE, "system") ?: "system",
            lastTestStatus = if (prefs.contains(KEY_LAST_TEST_STATUS)) prefs.getBoolean(KEY_LAST_TEST_STATUS, false) else null,
            lastTestMessage = prefs.getString(KEY_LAST_TEST_MSG, null),
            lastTestTimestamp = if (prefs.contains(KEY_LAST_TEST_TIME)) prefs.getLong(KEY_LAST_TEST_TIME, 0L) else null,
            lastSyncTimestamp = if (prefs.contains(KEY_LAST_SYNC_TIME)) prefs.getLong(KEY_LAST_SYNC_TIME, 0L) else null,
            syncMode = prefs.getString(KEY_SYNC_MODE, "AUTO") ?: "AUTO",
            lastRevision = if (prefs.contains(KEY_LAST_REVISION)) prefs.getLong(KEY_LAST_REVISION, 0L) else null,
            serverUpdatedAt = prefs.getString(KEY_SERVER_UPDATED_AT, null),
            lastSyncError = prefs.getString(KEY_LAST_SYNC_ERROR, null),
            tokenPermissions = tokenPermissions
        )
    }

    fun saveApiConfig(config: ApiConfig) {
        prefs.edit().apply {
            putString(KEY_BASE_URL, config.baseUrl.trim())
            putString(KEY_API_TOKEN, config.apiToken.trim())
            putBoolean(KEY_IS_CONFIGURED, config.isConfigured)
            putBoolean(KEY_AUTO_REFRESH, config.autoRefresh)
            putInt(KEY_REFRESH_INTERVAL, config.refreshIntervalSeconds)
            putString(KEY_THEME_MODE, config.themeMode)
            putString(KEY_SYNC_MODE, config.syncMode)
            try {
                putString(KEY_PERMISSIONS, permissionsAdapter.toJson(config.tokenPermissions))
            } catch (_: Exception) {}
            if (config.lastTestStatus != null) {
                putBoolean(KEY_LAST_TEST_STATUS, config.lastTestStatus)
            }
            if (config.lastTestMessage != null) {
                putString(KEY_LAST_TEST_MSG, config.lastTestMessage)
            }
            if (config.lastTestTimestamp != null) {
                putLong(KEY_LAST_TEST_TIME, config.lastTestTimestamp)
            }
            if (config.lastSyncTimestamp != null) {
                putLong(KEY_LAST_SYNC_TIME, config.lastSyncTimestamp)
            }
            if (config.lastRevision != null) {
                putLong(KEY_LAST_REVISION, config.lastRevision)
            } else {
                remove(KEY_LAST_REVISION)
            }
            if (config.serverUpdatedAt != null) {
                putString(KEY_SERVER_UPDATED_AT, config.serverUpdatedAt)
            } else {
                remove(KEY_SERVER_UPDATED_AT)
            }
            if (config.lastSyncError != null) {
                putString(KEY_LAST_SYNC_ERROR, config.lastSyncError)
            } else {
                remove(KEY_LAST_SYNC_ERROR)
            }
            apply()
        }
    }

    fun saveCachedBulbs(bulbs: List<WizBulbDto>) {
        try {
            val json = bulbListAdapter.toJson(bulbs)
            prefs.edit().putString(KEY_CACHED_DEVICES, json).apply()
        } catch (_: Exception) {}
    }

    fun getCachedBulbs(): List<WizBulbDto> {
        val json = prefs.getString(KEY_CACHED_DEVICES, null) ?: return emptyList()
        return try {
            bulbListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private val presetListType = Types.newParameterizedType(List::class.java, com.example.data.model.WizPresetDto::class.java)
    private val presetListAdapter = moshi.adapter<List<com.example.data.model.WizPresetDto>>(presetListType)

    fun saveCachedGroups(groups: List<WizGroupDto>) {
        try {
            val json = groupListAdapter.toJson(groups)
            prefs.edit().putString(KEY_CACHED_GROUPS, json).apply()
        } catch (_: Exception) {}
    }

    fun getCachedGroups(): List<WizGroupDto> {
        val json = prefs.getString(KEY_CACHED_GROUPS, null) ?: return emptyList()
        return try {
            groupListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCachedPresets(presets: List<com.example.data.model.WizPresetDto>) {
        try {
            val json = presetListAdapter.toJson(presets)
            prefs.edit().putString(KEY_CACHED_PRESETS, json).apply()
        } catch (_: Exception) {}
    }

    fun getCachedPresets(): List<com.example.data.model.WizPresetDto> {
        val json = prefs.getString(KEY_CACHED_PRESETS, null) ?: return emptyList()
        return try {
            presetListAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getHiddenDeviceIds(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_DEVICES, emptySet()) ?: emptySet()
    }

    fun setDeviceHidden(deviceId: String, hidden: Boolean) {
        val current = getHiddenDeviceIds().toMutableSet()
        if (hidden) {
            current.add(deviceId)
        } else {
            current.remove(deviceId)
        }
        prefs.edit().putStringSet(KEY_HIDDEN_DEVICES, current).apply()
    }

    fun isDeviceHidden(deviceId: String): Boolean {
        return getHiddenDeviceIds().contains(deviceId)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_AUTO_REFRESH = "auto_refresh"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_TEST_STATUS = "last_test_status"
        private const val KEY_LAST_TEST_MSG = "last_test_msg"
        private const val KEY_LAST_TEST_TIME = "last_test_time"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_CACHED_DEVICES = "cached_devices"
        private const val KEY_SYNC_MODE = "sync_mode"
        private const val KEY_LAST_REVISION = "last_revision"
        private const val KEY_SERVER_UPDATED_AT = "server_updated_at"
        private const val KEY_LAST_SYNC_ERROR = "last_sync_error"
        private const val KEY_CACHED_GROUPS = "cached_groups"
        private const val KEY_CACHED_PRESETS = "cached_presets"
        private const val KEY_HIDDEN_DEVICES = "hidden_devices"
        private const val KEY_PERMISSIONS = "token_permissions"
        private const val KEY_IS_CONFIGURED = "is_configured"
    }
}
