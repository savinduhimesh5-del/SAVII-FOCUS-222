package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.model.FocusSessionWithDetails
import com.example.data.local.model.TaskWithSubject
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppPreferencesStore
import com.example.data.preferences.PersistedTimerSnapshot
import com.example.data.preferences.TimerPreferencesStore
import com.example.data.repository.SaviiFocusRepository
import com.example.domain.FocusPresets
import com.example.domain.TimerStatus
import com.example.timer.FocusNotificationHelper
import com.example.timer.FocusTimerAlarmScheduler
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FocusTimerUiState(
    val status: TimerStatus = TimerStatus.IDLE,
    val presetMinutes: Int = FocusPresets.DEFAULT_MINUTES,
    val totalDurationMillis: Long = FocusPresets.DEFAULT_MINUTES * 60_000L,
    val remainingMillis: Long = FocusPresets.DEFAULT_MINUTES * 60_000L,
    val selectedSubjectId: Long? = null,
    val selectedTaskId: Long? = null,
    val subjects: List<SubjectEntity> = emptyList(),
    val pendingTasks: List<TaskWithSubject> = emptyList(),
    val recentSessions: List<FocusSessionWithDetails> = emptyList(),
    val lastCompletedMinutes: Int? = null
) {
    val progress: Float
        get() {
            if (totalDurationMillis <= 0L) return 0f
            val elapsed = (totalDurationMillis - remainingMillis).coerceIn(0L, totalDurationMillis)
            return elapsed.toFloat() / totalDurationMillis.toFloat()
        }
}

class FocusTimerViewModel(
    private val repository: SaviiFocusRepository,
    private val timerPreferencesStore: TimerPreferencesStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val alarmScheduler: FocusTimerAlarmScheduler,
    private val notificationHelper: FocusNotificationHelper,
    private val clockMillis: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val remainingMillisState = MutableStateFlow(FocusPresets.DEFAULT_MINUTES * 60_000L)
    private val lastCompletedMinutesState = MutableStateFlow<Int?>(null)
    private val completionMutex = Mutex()
    private var tickerJob: Job? = null
    private var latestSnapshot: PersistedTimerSnapshot = PersistedTimerSnapshot()
    private var latestPrefs: AppPreferences = AppPreferences()
    private var lastSyncedDefaultMinutes: Int? = null

    val uiState: StateFlow<FocusTimerUiState> = combine(
        timerPreferencesStore.snapshotFlow,
        remainingMillisState,
        repository.observeAllSubjects(),
        repository.observePendingTasksWithSubject(),
        repository.observeRecentFocusSessions(20)
    ) { snapshot, remaining, subjects, tasks, sessions ->
        FocusTimerUiState(
            status = snapshot.status,
            presetMinutes = snapshot.presetMinutes,
            totalDurationMillis = snapshot.totalDurationMillis,
            remainingMillis = when (snapshot.status) {
                TimerStatus.IDLE -> snapshot.presetMinutes * 60_000L
                TimerStatus.PAUSED -> snapshot.remainingWhenPausedMillis
                TimerStatus.COMPLETED -> 0L
                TimerStatus.RUNNING -> remaining
            },
            selectedSubjectId = snapshot.selectedSubjectId,
            selectedTaskId = snapshot.selectedTaskId,
            subjects = subjects,
            pendingTasks = tasks,
            recentSessions = sessions,
            lastCompletedMinutes = lastCompletedMinutesState.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusTimerUiState()
    )

    init {
        viewModelScope.launch {
            // Wait until the persisted timer snapshot has loaded from DataStore before
            // syncing defaultFocusMinutes, avoiding overwriting PAUSED/RUNNING sessions on startup.
            val initialSnapshot = timerPreferencesStore.snapshotFlow.first()
            latestSnapshot = initialSnapshot
            reconcileSnapshot(initialSnapshot)

            launch {
                timerPreferencesStore.snapshotFlow.collect { snapshot ->
                    latestSnapshot = snapshot
                    reconcileSnapshot(snapshot)
                }
            }

            appPreferencesStore.preferencesFlow.collect { prefs ->
                latestPrefs = prefs
                val previousDefault = lastSyncedDefaultMinutes
                lastSyncedDefaultMinutes = prefs.defaultFocusMinutes
                if (previousDefault != null && previousDefault != prefs.defaultFocusMinutes) {
                    val current = timerPreferencesStore.snapshotFlow.first()
                    if (current.status == TimerStatus.IDLE &&
                        current.sessionKey.isEmpty() &&
                        current.presetMinutes != prefs.defaultFocusMinutes
                    ) {
                        val updatedDuration = prefs.defaultFocusMinutes * 60_000L
                        timerPreferencesStore.saveSnapshot(
                            current.copy(
                                presetMinutes = prefs.defaultFocusMinutes,
                                totalDurationMillis = updatedDuration,
                                remainingWhenPausedMillis = updatedDuration
                            )
                        )
                    }
                }
            }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            val snapshot = timerPreferencesStore.snapshotFlow.first()
            latestSnapshot = snapshot
            reconcileSnapshot(snapshot)
        }
    }

    fun onAppBackgrounded() {
        stopTicker()
    }

    private fun reconcileSnapshot(snapshot: PersistedTimerSnapshot) {
        when (snapshot.status) {
            TimerStatus.IDLE -> {
                stopTicker()
                remainingMillisState.value = snapshot.presetMinutes * 60_000L
            }
            TimerStatus.PAUSED -> {
                stopTicker()
                remainingMillisState.value = snapshot.remainingWhenPausedMillis.coerceAtLeast(0L)
            }
            TimerStatus.COMPLETED -> {
                stopTicker()
                remainingMillisState.value = 0L
                lastCompletedMinutesState.value = snapshot.presetMinutes
            }
            TimerStatus.RUNNING -> {
                val remaining = (snapshot.endEpochMillis - clockMillis()).coerceAtLeast(0L)
                remainingMillisState.value = remaining
                if (remaining == 0L) {
                    stopTicker()
                    completeSessionOnce(snapshot)
                } else {
                    startTicker(snapshot)
                }
            }
        }
    }

    private fun startTicker(snapshot: PersistedTimerSnapshot) {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val activeSnapshot = latestSnapshot
                if (activeSnapshot.status != TimerStatus.RUNNING) break
                val remaining = (activeSnapshot.endEpochMillis - clockMillis()).coerceAtLeast(0L)
                remainingMillisState.value = remaining
                if (remaining <= 0L) {
                    completeSessionOnce(activeSnapshot)
                    break
                }
                delay(500L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun completeSessionOnce(snapshot: PersistedTimerSnapshot) {
        viewModelScope.launch {
            completionMutex.withLock {
                val fresh = timerPreferencesStore.snapshotFlow.first()
                if (fresh.sessionKey.isBlank()) return@withLock
                if (fresh.lastSavedSessionKey == fresh.sessionKey) {
                    if (fresh.status != TimerStatus.COMPLETED) {
                        timerPreferencesStore.markSessionSaved(fresh.sessionKey)
                    }
                    return@withLock
                }

                alarmScheduler.cancelCompletion()

                val completedAt = fresh.endEpochMillis.takeIf { it > 0L } ?: clockMillis()
                val actualSeconds = (fresh.totalDurationMillis / 1000L).toInt().coerceAtLeast(1)
                val inserted = repository.recordCompletedFocusSessionOnce(
                    sessionKey = fresh.sessionKey,
                    subjectId = fresh.selectedSubjectId,
                    taskId = fresh.selectedTaskId,
                    plannedMinutes = fresh.presetMinutes,
                    actualSeconds = actualSeconds,
                    startedAtMillis = fresh.startedAtEpochMillis.takeIf { it > 0L }
                        ?: (completedAt - fresh.totalDurationMillis),
                    completedAtMillis = completedAt
                )

                timerPreferencesStore.markSessionSaved(fresh.sessionKey)
                lastCompletedMinutesState.value = fresh.presetMinutes

                if (inserted && latestPrefs.notificationsEnabled) {
                    notificationHelper.showSessionCompleteNotification(fresh.presetMinutes)
                }
            }
        }
    }

    fun selectPreset(minutes: Int) {
        if (minutes !in FocusPresets.minutesOptions) return
        viewModelScope.launch {
            val current = timerPreferencesStore.snapshotFlow.first()
            if (current.status == TimerStatus.RUNNING || current.status == TimerStatus.PAUSED) {
                return@launch
            }
            val durationMs = minutes * 60_000L
            lastCompletedMinutesState.value = null
            timerPreferencesStore.saveSnapshot(
                current.copy(
                    status = TimerStatus.IDLE,
                    sessionKey = "",
                    presetMinutes = minutes,
                    totalDurationMillis = durationMs,
                    endEpochMillis = 0L,
                    remainingWhenPausedMillis = durationMs,
                    startedAtEpochMillis = 0L
                )
            )
        }
    }

    fun selectSubject(subjectId: Long?) {
        viewModelScope.launch {
            val current = timerPreferencesStore.snapshotFlow.first()
            val taskMatches = current.selectedTaskId?.let { taskId ->
                uiState.value.pendingTasks.firstOrNull { it.task.id == taskId }?.task?.subjectId == subjectId
            } ?: true
            timerPreferencesStore.saveSnapshot(
                current.copy(
                    selectedSubjectId = subjectId,
                    selectedTaskId = if (taskMatches || subjectId == null) current.selectedTaskId else null
                )
            )
        }
    }

    fun selectTask(task: TaskWithSubject?) {
        viewModelScope.launch {
            val current = timerPreferencesStore.snapshotFlow.first()
            timerPreferencesStore.saveSnapshot(
                current.copy(
                    selectedTaskId = task?.task?.id,
                    selectedSubjectId = task?.task?.subjectId ?: current.selectedSubjectId
                )
            )
        }
    }

    fun startTimer() {
        viewModelScope.launch {
            val current = timerPreferencesStore.snapshotFlow.first()
            val now = clockMillis()
            val durationMs = current.presetMinutes * 60_000L
            val sessionKey = UUID.randomUUID().toString()
            val endEpoch = now + durationMs
            lastCompletedMinutesState.value = null

            timerPreferencesStore.saveSnapshot(
                current.copy(
                    status = TimerStatus.RUNNING,
                    sessionKey = sessionKey,
                    totalDurationMillis = durationMs,
                    endEpochMillis = endEpoch,
                    remainingWhenPausedMillis = durationMs,
                    startedAtEpochMillis = now
                )
            )

            if (latestPrefs.notificationsEnabled) {
                alarmScheduler.scheduleCompletion(
                    triggerAtMillis = endEpoch,
                    sessionKey = sessionKey,
                    plannedMinutes = current.presetMinutes
                )
            }
        }
    }

    fun pauseTimer() {
        viewModelScope.launch {
            val current = timerPreferencesStore.snapshotFlow.first()
            if (current.status != TimerStatus.RUNNING) return@launch
            val remaining = (current.endEpochMillis - clockMillis()).coerceAtLeast(0L)
            if (remaining <= 0L) {
                completeSessionOnce(current)
                return@launch
            }
            alarmScheduler.cancelCompletion()
            timerPreferencesStore.saveSnapshot(
                current.copy(
                    status = TimerStatus.PAUSED,
                    endEpochMillis = 0L,
                    remainingWhenPausedMillis = remaining
                )
            )
        }
    }

    fun resumeTimer() {
        viewModelScope.launch {
            val current = timerPreferencesStore.snapshotFlow.first()
            if (current.status != TimerStatus.PAUSED) return@launch
            val remaining = current.remainingWhenPausedMillis.coerceAtLeast(1_000L)
            val now = clockMillis()
            val endEpoch = now + remaining

            timerPreferencesStore.saveSnapshot(
                current.copy(
                    status = TimerStatus.RUNNING,
                    endEpochMillis = endEpoch
                )
            )

            if (latestPrefs.notificationsEnabled) {
                alarmScheduler.scheduleCompletion(
                    triggerAtMillis = endEpoch,
                    sessionKey = current.sessionKey,
                    plannedMinutes = current.presetMinutes
                )
            }
        }
    }

    fun resetTimer() {
        viewModelScope.launch {
            stopTicker()
            alarmScheduler.cancelCompletion()
            lastCompletedMinutesState.value = null
            val current = timerPreferencesStore.snapshotFlow.first()
            val durationMs = current.presetMinutes * 60_000L
            timerPreferencesStore.saveSnapshot(
                current.copy(
                    status = TimerStatus.IDLE,
                    sessionKey = "",
                    totalDurationMillis = durationMs,
                    endEpochMillis = 0L,
                    remainingWhenPausedMillis = durationMs,
                    startedAtEpochMillis = 0L
                )
            )
        }
    }

    fun markLinkedTaskCompleted() {
        val taskId = uiState.value.selectedTaskId ?: return
        viewModelScope.launch {
            repository.setTaskCompleted(taskId, true)
        }
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }
}
