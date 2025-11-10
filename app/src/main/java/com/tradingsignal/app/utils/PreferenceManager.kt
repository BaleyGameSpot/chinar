package com.tradingsignal.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.tradingsignal.app.data.model.UserConfig

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "trading_signal_prefs"

        // Keys
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_USER_CONFIG = "user_config"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_AUTO_SCAN_ENABLED = "auto_scan_enabled"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
        private const val KEY_SCAN_COUNT = "scan_count"
        private const val KEY_SUBSCRIPTION_TIER = "subscription_tier"
        private const val KEY_THEME_MODE = "theme_mode"
    }

    // First launch
    var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    // User configuration
    var userConfig: UserConfig?
        get() {
            val json = sharedPreferences.getString(KEY_USER_CONFIG, null)
            return json?.let { gson.fromJson(it, UserConfig::class.java) }
        }
        set(value) {
            val json = value?.let { gson.toJson(it) }
            sharedPreferences.edit().putString(KEY_USER_CONFIG, json).apply()
        }

    // Notifications enabled
    var notificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    // Auto scan enabled
    var autoScanEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_SCAN_ENABLED, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_AUTO_SCAN_ENABLED, value).apply()

    // Last scan time
    var lastScanTime: Long
        get() = sharedPreferences.getLong(KEY_LAST_SCAN_TIME, 0)
        set(value) = sharedPreferences.edit().putLong(KEY_LAST_SCAN_TIME, value).apply()

    // Scan count
    var scanCount: Int
        get() = sharedPreferences.getInt(KEY_SCAN_COUNT, 0)
        set(value) = sharedPreferences.edit().putInt(KEY_SCAN_COUNT, value).apply()

    fun incrementScanCount() {
        scanCount += 1
    }

    // Subscription tier
    var subscriptionTier: String
        get() = sharedPreferences.getString(KEY_SUBSCRIPTION_TIER, "FREE") ?: "FREE"
        set(value) = sharedPreferences.edit().putString(KEY_SUBSCRIPTION_TIER, value).apply()

    // Theme mode
    var themeMode: String
        get() = sharedPreferences.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = sharedPreferences.edit().putString(KEY_THEME_MODE, value).apply()

    // Clear all preferences
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    // Get default user config
    fun getDefaultUserConfig(): UserConfig {
        return UserConfig()
    }
}