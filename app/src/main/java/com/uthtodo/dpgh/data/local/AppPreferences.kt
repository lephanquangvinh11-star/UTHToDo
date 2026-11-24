package com.uthtodo.dpgh.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val themeMode: String = "Sáng", // "Sáng" | "Tối" | "Tự động"
    val accentColorHex: String = "#2F5BEA",
    val enableReminders: Boolean = true,
    val enableSound: Boolean = true,
    val enableDailySummary: Boolean = false,
    val readNotificationIds: Set<Int> = emptySet()
) {
    val accentColor: Color
        get() = when (accentColorHex.uppercase()) {
            "#34C759" -> Color(0xFF34C759)
            "#FF9500" -> Color(0xFFFF9500)
            "#AF52DE" -> Color(0xFFAF52DE)
            "#FF2D55" -> Color(0xFFFF2D55)
            else -> Color(0xFF2F5BEA)
        }
}

object AppPreferences {
    private const val PREFS_NAME = "uth_todo_app_settings"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT_COLOR = "accent_color_hex"
    private const val KEY_ENABLE_REMINDERS = "enable_reminders"
    private const val KEY_ENABLE_SOUND = "enable_sound"
    private const val KEY_ENABLE_DAILY_SUMMARY = "enable_daily_summary"
    private const val KEY_READ_NOTIFICATIONS = "read_notification_ids"

    private lateinit var prefs: SharedPreferences

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val readIds = prefs.getStringSet(KEY_READ_NOTIFICATIONS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()

        _settings.value = AppSettings(
            themeMode = prefs.getString(KEY_THEME_MODE, "Sáng") ?: "Sáng",
            accentColorHex = prefs.getString(KEY_ACCENT_COLOR, "#2F5BEA") ?: "#2F5BEA",
            enableReminders = prefs.getBoolean(KEY_ENABLE_REMINDERS, true),
            enableSound = prefs.getBoolean(KEY_ENABLE_SOUND, true),
            enableDailySummary = prefs.getBoolean(KEY_ENABLE_DAILY_SUMMARY, false),
            readNotificationIds = readIds
        )
    }

    fun setThemeMode(mode: String) {
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        }
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setAccentColorHex(hex: String) {
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_ACCENT_COLOR, hex).apply()
        }
        _settings.value = _settings.value.copy(accentColorHex = hex)
    }

    fun setEnableReminders(enabled: Boolean) {
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_ENABLE_REMINDERS, enabled).apply()
        }
        _settings.value = _settings.value.copy(enableReminders = enabled)
    }

    fun setEnableSound(enabled: Boolean) {
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_ENABLE_SOUND, enabled).apply()
        }
        _settings.value = _settings.value.copy(enableSound = enabled)
    }

    fun setEnableDailySummary(enabled: Boolean) {
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_ENABLE_DAILY_SUMMARY, enabled).apply()
        }
        _settings.value = _settings.value.copy(enableDailySummary = enabled)
    }

    fun markNotificationsRead(ids: Collection<Int>) {
        val updated = _settings.value.readNotificationIds + ids
        if (::prefs.isInitialized) {
            prefs.edit().putStringSet(KEY_READ_NOTIFICATIONS, updated.map { it.toString() }.toSet()).apply()
        }
        _settings.value = _settings.value.copy(readNotificationIds = updated)
    }
}
