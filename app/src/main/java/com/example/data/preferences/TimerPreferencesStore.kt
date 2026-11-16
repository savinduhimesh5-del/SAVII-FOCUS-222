package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.FocusPresets
import com.example.domain.TimerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "savii_timer_state"
)

data class PersistedTimerSnapshot(
    val status: TimerStatus = TimerStatus.IDLE,
    val sessionKey: String = "",
    val presetMinutes: Int = FocusPresets.DEFAULT_MINUTES,
    val totalDurationMillis: Long = FocusPresets.DEFAULT_MINUTES * 60_000L,
    val endEpochMillis: Long = 0L,
    val remainingWhenPausedMillis: Long = FocusPresets.DEFAULT_MINUTES * 60_000L,
    val startedAtEpochMillis: Long = 0L,
    val selectedSubjectId: Long? = null,
    val selectedTaskId: Long? = null,
    val lastSavedSessionKey: String = ""
)

class TimerPreferencesStore(private val context: Context) {

    private object Keys {
        val STATUS = stringPreferencesKey("timer_status")
        val SESSION_KEY = stringPreferencesKey("session_key")
        val PRESET_MINUTES = intPreferencesKey("preset_minutes")
        val TOTAL_DURATION_MS = longPreferencesKey("total_duration_ms")
        val END_EPOCH_MS = longPreferencesKey("end_epoch_ms")
        val REMAINING_PAUSED_MS = longPreferencesKey("remaining_paused_ms")
        val STARTED_AT_MS = longPreferencesKey("started_at_ms")
        val SELECTED_SUBJECT_ID = longPreferencesKey("selected_subject_id")
        val SELECTED_TASK_ID = longPreferencesKey("selected_task_id")
        val LAST_SAVED_SESSION_KEY = stringPreferencesKey("last_saved_session_key")
    }

    val snapshotFlow: Flow<PersistedTimerSnapshot> = context.timerDataStore.data.map { prefs ->
        val preset = (prefs[Keys.PRESET_MINUTES] ?: FocusPresets.DEFAULT_MINUTES).let {
            if (it in FocusPresets.minutesOptions) it else FocusPresets.DEFAULT_MINUTES
        }
        val defaultDuration = preset * 60_000L
        val subjectRaw = prefs[Keys.SELECTED_SUBJECT_ID] ?: -1L
        val taskRaw = prefs[Keys.SELECTED_TASK_ID] ?: -1L
        PersistedTimerSnapshot(
            status = TimerStatus.fromStorage(prefs[Keys.STATUS]),
            sessionKey = prefs[Keys.SESSION_KEY] ?: "",
            presetMinutes = preset,
            totalDurationMillis = prefs[Keys.TOTAL_DURATION_MS] ?: defaultDuration,
            endEpochMillis = prefs[Keys.END_EPOCH_MS] ?: 0L,
            remainingWhenPausedMillis = prefs[Keys.REMAINING_PAUSED_MS] ?: defaultDuration,
            startedAtEpochMillis = prefs[Keys.STARTED_AT_MS] ?: 0L,
            selectedSubjectId = subjectRaw.takeIf { it > 0L },
            selectedTaskId = taskRaw.takeIf { it > 0L },
            lastSavedSessionKey = prefs[Keys.LAST_SAVED_SESSION_KEY] ?: ""
        )
    }

    suspend fun saveSnapshot(snapshot: PersistedTimerSnapshot) {
        context.timerDataStore.edit { prefs ->
            prefs[Keys.STATUS] = snapshot.status.storageValue
            prefs[Keys.SESSION_KEY] = snapshot.sessionKey
            prefs[Keys.PRESET_MINUTES] = snapshot.presetMinutes
            prefs[Keys.TOTAL_DURATION_MS] = snapshot.totalDurationMillis
            prefs[Keys.END_EPOCH_MS] = snapshot.endEpochMillis
            prefs[Keys.REMAINING_PAUSED_MS] = snapshot.remainingWhenPausedMillis
            prefs[Keys.STARTED_AT_MS] = snapshot.startedAtEpochMillis
            prefs[Keys.SELECTED_SUBJECT_ID] = snapshot.selectedSubjectId ?: -1L
            prefs[Keys.SELECTED_TASK_ID] = snapshot.selectedTaskId ?: -1L
            prefs[Keys.LAST_SAVED_SESSION_KEY] = snapshot.lastSavedSessionKey
        }
    }

    suspend fun markSessionSaved(sessionKey: String) {
        context.timerDataStore.edit { prefs ->
            prefs[Keys.STATUS] = TimerStatus.COMPLETED.storageValue
            prefs[Keys.END_EPOCH_MS] = 0L
            prefs[Keys.REMAINING_PAUSED_MS] = 0L
            prefs[Keys.LAST_SAVED_SESSION_KEY] = sessionKey
        }
    }
}
