package com.example.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.SaviiFocusApplication
import com.example.domain.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusTimerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_SESSION_KEY = "extra_session_key"
        const val EXTRA_PLANNED_MINUTES = "extra_planned_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as? SaviiFocusApplication
        if (app == null) {
            pendingResult.finish()
            return
        }
        val sessionKey = intent.getStringExtra(EXTRA_SESSION_KEY).orEmpty()
        val plannedMinutes = intent.getIntExtra(EXTRA_PLANNED_MINUTES, 25)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = app.container.appPreferencesStore.preferencesFlow.first()
                val snapshot = app.container.timerPreferencesStore.snapshotFlow.first()
                if (prefs.notificationsEnabled &&
                    snapshot.status == TimerStatus.RUNNING &&
                    snapshot.sessionKey == sessionKey &&
                    snapshot.lastSavedSessionKey != sessionKey
                ) {
                    app.container.notificationHelper.showSessionCompleteNotification(plannedMinutes)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
