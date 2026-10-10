package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ApiConfig
import com.example.data.model.WizBulbDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wiz_smart_home_prefs", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val bulbListType = Types.newParameterizedType(List::class.java, WizBulbDto::class.java)
    private val bulbListAdapter = moshi.adapter<List<WizBulbDto>>(bulbListType)

    fun getApiConfig(): ApiConfig {
        return ApiConfig(
            baseUrl = prefs.getString(KEY_BASE_URL, "http://192.168.1.100:3000") ?: "http://192.168.1.100:3000",
            apiToken = prefs.getString(KEY_API_TOKEN, "") ?: "",
            autoRefresh = prefs.getBoolean(KEY_AUTO_REFRESH, false),
            refreshIntervalSeconds = prefs.getInt(KEY_REFRESH_INTERVAL, 30),
            themeMode = prefs.getString(KEY_THEME_MODE, "system") ?: "system",
            lastTestStatus = if (prefs.contains(KEY_LAST_TEST_STATUS)) prefs.getBoolean(KEY_LAST_TEST_STATUS, false) else null,
            lastTestMessage = prefs.getString(KEY_LAST_TEST_MSG, null),
            lastTestTimestamp = if (prefs.contains(KEY_LAST_TEST_TIME)) prefs.getLong(KEY_LAST_TEST_TIME, 0L) else null,
            lastSyncTimestamp = if (prefs.contains(KEY_LAST_SYNC_TIME)) prefs.getLong(KEY_LAST_SYNC_TIME, 0L) else null
        )
    }

    fun saveApiConfig(config: ApiConfig) {
        prefs.edit().apply {
            putString(KEY_BASE_URL, config.baseUrl.trim())
            putString(KEY_API_TOKEN, config.apiToken.trim())
            putBoolean(KEY_AUTO_REFRESH, config.autoRefresh)
            putInt(KEY_REFRESH_INTERVAL, config.refreshIntervalSeconds)
            putString(KEY_THEME_MODE, config.themeMode)
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
    }
}
