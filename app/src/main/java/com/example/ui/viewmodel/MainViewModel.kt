package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesStore
import com.example.domain.ThemeMode
import com.example.timer.FocusNotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val appPreferencesStore: AppPreferencesStore,
    private val notificationHelper: FocusNotificationHelper
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = appPreferencesStore.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferencesStore.setThemeMode(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { appPreferencesStore.setDynamicColors(enabled) }
    }

    fun setDefaultFocusMinutes(minutes: Int) {
        viewModelScope.launch { appPreferencesStore.setDefaultFocusMinutes(minutes) }
    }

    fun setDailyFocusGoalMinutes(minutes: Int) {
        viewModelScope.launch { appPreferencesStore.setDailyFocusGoalMinutes(minutes) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                notificationHelper.ensureChannel()
            }
            appPreferencesStore.setNotificationsEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesStore.setHapticsEnabled(enabled) }
    }

    fun hasNotificationPermission(): Boolean = notificationHelper.hasNotificationPermission()
}
