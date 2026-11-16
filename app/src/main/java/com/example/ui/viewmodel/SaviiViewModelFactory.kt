package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.AppContainer

class SaviiViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                MainViewModel(container.appPreferencesStore, container.notificationHelper) as T

            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(container.repository, container.appPreferencesStore) as T

            modelClass.isAssignableFrom(LearningViewModel::class.java) ->
                LearningViewModel(container.repository) as T

            modelClass.isAssignableFrom(PlannerViewModel::class.java) ->
                PlannerViewModel(container.repository) as T

            modelClass.isAssignableFrom(FocusTimerViewModel::class.java) ->
                FocusTimerViewModel(
                    repository = container.repository,
                    timerPreferencesStore = container.timerPreferencesStore,
                    appPreferencesStore = container.appPreferencesStore,
                    alarmScheduler = container.alarmScheduler,
                    notificationHelper = container.notificationHelper
                ) as T

            modelClass.isAssignableFrom(InsightsViewModel::class.java) ->
                InsightsViewModel(container.repository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
