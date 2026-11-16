package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.SaviiFocusDatabase
import com.example.data.preferences.AppPreferencesStore
import com.example.data.preferences.TimerPreferencesStore
import com.example.data.repository.SaviiFocusRepository
import com.example.timer.FocusNotificationHelper
import com.example.timer.FocusTimerAlarmScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: SaviiFocusDatabase by lazy {
        SaviiFocusDatabase.getInstance(appContext)
    }

    val repository: SaviiFocusRepository by lazy {
        SaviiFocusRepository(
            subjectDao = database.subjectDao(),
            taskDao = database.taskDao(),
            focusSessionDao = database.focusSessionDao()
        )
    }

    val appPreferencesStore: AppPreferencesStore by lazy {
        AppPreferencesStore(appContext)
    }

    val timerPreferencesStore: TimerPreferencesStore by lazy {
        TimerPreferencesStore(appContext)
    }

    val notificationHelper: FocusNotificationHelper by lazy {
        FocusNotificationHelper(appContext)
    }

    val alarmScheduler: FocusTimerAlarmScheduler by lazy {
        FocusTimerAlarmScheduler(appContext)
    }
}

class SaviiFocusApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
