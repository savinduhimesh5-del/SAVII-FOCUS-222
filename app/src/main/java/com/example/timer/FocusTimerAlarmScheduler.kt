package com.example.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class FocusTimerAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleCompletion(
        triggerAtMillis: Long,
        sessionKey: String,
        plannedMinutes: Int
    ) {
        val intent = createIntent(sessionKey, plannedMinutes)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelCompletion() {
        val intent = createIntent("", 0)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun createIntent(sessionKey: String, plannedMinutes: Int): Intent =
        Intent(context, FocusTimerAlarmReceiver::class.java).apply {
            putExtra(FocusTimerAlarmReceiver.EXTRA_SESSION_KEY, sessionKey)
            putExtra(FocusTimerAlarmReceiver.EXTRA_PLANNED_MINUTES, plannedMinutes)
        }

    companion object {
        private const val REQUEST_CODE = 5102
    }
}
