package com.example.parentalapp.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ChildAppTheme(val label: String) {
    SYSTEM("Zgodnie z systemem"),
    LIGHT("Jasny"),
    DARK("Ciemny")
}

data class ChildSettings(
    val messageNotificationsEnabled: Boolean = true,
    val appTheme: ChildAppTheme = ChildAppTheme.SYSTEM
)

object ChildSettingsManager {
    private const val PREFS_NAME = "child_app_settings"

    var settings by mutableStateOf(ChildSettings())
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        settings = ChildSettings(
            messageNotificationsEnabled = prefs.getBoolean("message_notifications", true),
            appTheme = ChildAppTheme.entries.find {
                it.name == prefs.getString("app_theme", null)
            } ?: ChildAppTheme.SYSTEM
        )
    }

    fun save(context: Context, newSettings: ChildSettings) {
        settings = newSettings
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean("message_notifications", newSettings.messageNotificationsEnabled)
            putString("app_theme", newSettings.appTheme.name)
            apply()
        }
    }
}