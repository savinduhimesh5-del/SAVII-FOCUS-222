package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.model.PeriodFocusSummary
import com.example.data.local.model.SubjectProgressRow
import com.example.data.local.model.TaskWithSubject
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesStore
import com.example.data.repository.SaviiFocusRepository
import com.example.domain.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val todayTasks: List<TaskWithSubject> = emptyList(),
    val todayCompletedCount: Int = 0,
    val todayPendingCount: Int = 0,
    val totalPendingTasksCount: Int = 0,
    val totalCompletedTasksCount: Int = 0,
    val todayFocusMinutes: Int = 0,
    val todaySessionCount: Int = 0,
    val dailyGoalMinutes: Int = 90,
    val subjectsOverview: List<SubjectProgressRow> = emptyList(),
    val headerDateText: String = DateTimeUtils.formatHeaderDate(),
    val greetingText: String = DateTimeUtils.greetingForHour(),
    val isLoaded: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: SaviiFocusRepository,
    appPreferencesStore: AppPreferencesStore
) : ViewModel() {

    private val currentEpochDay = MutableStateFlow(DateTimeUtils.todayEpochDay())
    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<DashboardUiState> = combine(currentEpochDay, refreshTick) { epochDay, _ ->
        epochDay
    }
        .flatMapLatest { epochDay ->
            val startMillis = DateTimeUtils.startOfTodayMillis()
            val endMillis = DateTimeUtils.endOfTodayMillis()
            val weekStart = DateTimeUtils.startOfWeekEpochDay()

            combine(
                repository.observeAllTasksWithSubject(),
                repository.observeFocusSummaryForDay(epochDay),
                repository.observeSubjectsWithProgress(weekStart),
                appPreferencesStore.preferencesFlow
            ) { allTasks, focusSummary, subjects, prefs ->
                buildDashboardState(
                    epochDay = epochDay,
                    startOfTodayMillis = startMillis,
                    endOfTodayMillis = endMillis,
                    allTasks = allTasks,
                    focusSummary = focusSummary,
                    subjects = subjects,
                    prefs = prefs
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    fun refreshDate() {
        val latest = DateTimeUtils.todayEpochDay()
        if (currentEpochDay.value != latest) {
            currentEpochDay.value = latest
        } else {
            refreshTick.value += 1
        }
    }

    fun toggleTaskCompleted(taskId: Long, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompleted(taskId, !currentlyCompleted)
        }
    }

    private fun buildDashboardState(
        epochDay: Long,
        startOfTodayMillis: Long,
        endOfTodayMillis: Long,
        allTasks: List<TaskWithSubject>,
        focusSummary: PeriodFocusSummary,
        subjects: List<SubjectProgressRow>,
        prefs: AppPreferences
    ): DashboardUiState {
        val todayRelevant = allTasks.filter { item ->
            val task = item.task
            val dueTodayOrOverdue = task.dueDateEpochDay != null && task.dueDateEpochDay <= epochDay
            val completedToday = task.isCompleted &&
                task.completedAtMillis != null &&
                task.completedAtMillis in startOfTodayMillis..endOfTodayMillis
            dueTodayOrOverdue || completedToday
        }

        val todayCompleted = todayRelevant.count { it.task.isCompleted }
        val todayPending = todayRelevant.count { !it.task.isCompleted }
        val totalPending = allTasks.count { !it.task.isCompleted }
        val totalCompleted = allTasks.count { it.task.isCompleted }

        return DashboardUiState(
            todayTasks = todayRelevant.take(6),
            todayCompletedCount = todayCompleted,
            todayPendingCount = todayPending,
            totalPendingTasksCount = totalPending,
            totalCompletedTasksCount = totalCompleted,
            todayFocusMinutes = (focusSummary.totalSeconds + 30) / 60,
            todaySessionCount = focusSummary.sessionCount,
            dailyGoalMinutes = prefs.dailyFocusGoalMinutes,
            subjectsOverview = subjects,
            headerDateText = DateTimeUtils.formatHeaderDate(),
            greetingText = DateTimeUtils.greetingForHour(),
            isLoaded = true
        )
    }
}
