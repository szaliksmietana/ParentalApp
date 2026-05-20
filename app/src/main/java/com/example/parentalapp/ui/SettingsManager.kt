package com.example.parentalapp.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PollingInterval(val ms: Long, val label: String) {
    STANDARD(10_000L, "Standardowe (co 10 sek)"),
    BATTERY_SAVER(30_000L, "Oszczędzanie baterii (co 30 sek)"),
    ULTRA_SAVER(60_000L, "Maksymalne oszczędzanie (co 1 min)")
}

enum class AppTheme(val label: String) {
    SYSTEM("Zgodnie z systemem"),
    LIGHT("Jasny"),
    DARK("Ciemny")
}

enum class MapStyle(val label: String) {
    STANDARD("Standardowa"),
    TERRAIN("Terenowa")
}

data class AppSettings(
    val pollingInterval: PollingInterval = PollingInterval.STANDARD,
    val chatNotificationsEnabled: Boolean = true,
    val geofenceNotificationsEnabled: Boolean = true,
    val gpsToleranceMeters: Int = 0,
    val mapStyle: MapStyle = MapStyle.STANDARD,
    val appTheme: AppTheme = AppTheme.SYSTEM
)

object SettingsManager {
    private const val PREFS_NAME = "app_settings"

    var settings by mutableStateOf(AppSettings())
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        settings = AppSettings(
            pollingInterval = PollingInterval.entries.find {
                it.name == prefs.getString("polling_interval", null)
            } ?: PollingInterval.STANDARD,
            chatNotificationsEnabled = prefs.getBoolean("chat_notifications", true),
            geofenceNotificationsEnabled = prefs.getBoolean("geofence_notifications", true),
            gpsToleranceMeters = prefs.getInt("gps_tolerance", 0),
            mapStyle = MapStyle.entries.find {
                it.name == prefs.getString("map_style", null)
            } ?: MapStyle.STANDARD,
            appTheme = AppTheme.entries.find {
                it.name == prefs.getString("app_theme", null)
            } ?: AppTheme.SYSTEM
        )
    }

    fun save(context: Context, newSettings: AppSettings) {
        settings = newSettings
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString("polling_interval", newSettings.pollingInterval.name)
            putBoolean("chat_notifications", newSettings.chatNotificationsEnabled)
            putBoolean("geofence_notifications", newSettings.geofenceNotificationsEnabled)
            putInt("gps_tolerance", newSettings.gpsToleranceMeters)
            putString("map_style", newSettings.mapStyle.name)
            putString("app_theme", newSettings.appTheme.name)
            apply()
        }
    }
}