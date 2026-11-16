package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.model.DailyFocusAggregate
import com.example.data.local.model.PeriodFocusSummary
import com.example.data.local.model.SubjectProgressRow
import com.example.data.repository.SaviiFocusRepository
import com.example.domain.DateTimeUtils
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class DayBarPoint(
    val date: LocalDate,
    val dayLabel: String,
    val minutes: Int,
    val sessionCount: Int
)

data class InsightsUiState(
    val todayFocusSeconds: Int = 0,
    val todaySessionCount: Int = 0,
    val todayCompletedTasks: Int = 0,
    val weekFocusSeconds: Int = 0,
    val weekSessionCount: Int = 0,
    val weekCompletedTasks: Int = 0,
    val allTimeFocusSeconds: Int = 0,
    val allTimeSessionCount: Int = 0,
    val allTimeCompletedTasks: Int = 0,
    val last7DaysChart: List<DayBarPoint> = emptyList(),
    val subjectProgress: List<SubjectProgressRow> = emptyList(),
    val isLoaded: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val repository: SaviiFocusRepository
) : ViewModel() {

    private val currentEpochDay = MutableStateFlow(DateTimeUtils.todayEpochDay())

    val uiState: StateFlow<InsightsUiState> = currentEpochDay
        .flatMapLatest { epochDay ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.ofEpochDay(epochDay)
            val weekStartDate = today.minusDays(6)
            val weekStartEpoch = weekStartDate.toEpochDay()
            val todayStartMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val todayEndMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
            val weekStartMs = weekStartDate.atStartOfDay(zone).toInstant().toEpochMilli()

            val focusFlow = combine(
                repository.observeFocusSummaryForDay(epochDay),
                repository.observeFocusSummaryInDayRange(weekStartEpoch, epochDay),
                repository.observeAllTimeFocusSummary(),
                repository.observeDailyFocusBreakdown(weekStartEpoch, epochDay)
            ) { todaySummary, weekSummary, allSummary, breakdown ->
                FocusStatsBundle(todaySummary, weekSummary, allSummary, breakdown)
            }

            val tasksFlow = combine(
                repository.observeCompletedTaskCountInRange(todayStartMs, todayEndMs),
                repository.observeCompletedTaskCountInRange(weekStartMs, todayEndMs),
                repository.observeTotalCompletedTaskCount(),
                repository.observeSubjectsWithProgress(weekStartEpoch)
            ) { todayTasks, weekTasks, allTasks, subjects ->
                TaskAndSubjectBundle(todayTasks, weekTasks, allTasks, subjects)
            }

            combine(focusFlow, tasksFlow) { focus, tasks ->
                val byDay = focus.breakdown.associateBy { it.localDateEpochDay }
                val chartPoints = DateTimeUtils.last7EpochDays(today).map { date ->
                    val aggregate = byDay[date.toEpochDay()]
                    DayBarPoint(
                        date = date,
                        dayLabel = DateTimeUtils.formatShortWeekday(date),
                        minutes = ((aggregate?.totalSeconds ?: 0) + 30) / 60,
                        sessionCount = aggregate?.sessionCount ?: 0
                    )
                }

                InsightsUiState(
                    todayFocusSeconds = focus.today.totalSeconds,
                    todaySessionCount = focus.today.sessionCount,
                    todayCompletedTasks = tasks.todayCompleted,
                    weekFocusSeconds = focus.week.totalSeconds,
                    weekSessionCount = focus.week.sessionCount,
                    weekCompletedTasks = tasks.weekCompleted,
                    allTimeFocusSeconds = focus.allTime.totalSeconds,
                    allTimeSessionCount = focus.allTime.sessionCount,
                    allTimeCompletedTasks = tasks.allCompleted,
                    last7DaysChart = chartPoints,
                    subjectProgress = tasks.subjects,
                    isLoaded = true
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InsightsUiState()
        )

    fun refreshDate() {
        val latest = DateTimeUtils.todayEpochDay()
        if (currentEpochDay.value != latest) {
            currentEpochDay.value = latest
        }
    }

    private data class FocusStatsBundle(
        val today: PeriodFocusSummary,
        val week: PeriodFocusSummary,
        val allTime: PeriodFocusSummary,
        val breakdown: List<DailyFocusAggregate>
    )

    private data class TaskAndSubjectBundle(
        val todayCompleted: Int,
        val weekCompleted: Int,
        val allCompleted: Int,
        val subjects: List<SubjectProgressRow>
    )
}
