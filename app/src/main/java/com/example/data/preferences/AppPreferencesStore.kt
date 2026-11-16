package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.FocusPresets
import com.example.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "savii_app_preferences"
)

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColors: Boolean = false,
    val defaultFocusMinutes: Int = FocusPresets.DEFAULT_MINUTES,
    val dailyFocusGoalMinutes: Int = 90,
    val notificationsEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true
)

class AppPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val DEFAULT_FOCUS_MINUTES = intPreferencesKey("default_focus_minutes")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val preferencesFlow: Flow<AppPreferences> = context.appPreferencesDataStore.data.map { prefs ->
        val preset = prefs[Keys.DEFAULT_FOCUS_MINUTES] ?: FocusPresets.DEFAULT_MINUTES
        val validPreset = if (preset in FocusPresets.minutesOptions) preset else FocusPresets.DEFAULT_MINUTES
        AppPreferences(
            themeMode = ThemeMode.fromStorage(prefs[Keys.THEME_MODE]),
            dynamicColors = prefs[Keys.DYNAMIC_COLORS] ?: false,
            defaultFocusMinutes = validPreset,
            dailyFocusGoalMinutes = (prefs[Keys.DAILY_GOAL_MINUTES] ?: 90).coerceIn(15, 480),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true
        )
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.appPreferencesDataStore.edit { it[Keys.THEME_MODE] = themeMode.storageValue }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
    }

    suspend fun setDefaultFocusMinutes(minutes: Int) {
        if (minutes in FocusPresets.minutesOptions) {
            context.appPreferencesDataStore.edit { it[Keys.DEFAULT_FOCUS_MINUTES] = minutes }
        }
    }

    suspend fun setDailyFocusGoalMinutes(minutes: Int) {
        context.appPreferencesDataStore.edit {
            it[Keys.DAILY_GOAL_MINUTES] = minutes.coerceIn(15, 480)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }
}
