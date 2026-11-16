package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SaviiFocusDatabase
import com.example.data.local.entity.TaskEntity
import com.example.data.preferences.AppPreferencesStore
import com.example.data.preferences.PersistedTimerSnapshot
import com.example.data.preferences.TimerPreferencesStore
import com.example.data.repository.SaviiFocusRepository
import com.example.data.repository.SubjectMutationResult
import com.example.domain.AatLevel
import com.example.domain.DateTimeUtils
import com.example.domain.StudyCategory
import com.example.domain.SubjectIconOption
import com.example.domain.TaskPriority
import com.example.domain.TimerStatus
import com.example.timer.FocusNotificationHelper
import com.example.timer.FocusTimerAlarmScheduler
import com.example.ui.viewmodel.FocusTimerUiState
import com.example.ui.viewmodel.FocusTimerViewModel
import com.example.ui.viewmodel.InsightsUiState
import com.example.ui.viewmodel.InsightsViewModel
import com.example.ui.viewmodel.PlannerUiState
import com.example.ui.viewmodel.PlannerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SaviiRepositoryRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: SaviiFocusDatabase
    private lateinit var repository: SaviiFocusRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SaviiFocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SaviiFocusRepository(
            subjectDao = database.subjectDao(),
            taskDao = database.taskDao(),
            focusSessionDao = database.focusSessionDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `duplicate subject prevention ignores case and extra whitespace within same scope`() = runTest {
        val first = repository.addSubject(
            rawName = "  Linear   Algebra ",
            colorHex = 0xFF9D6BFF,
            targetMinutesPerWeek = 180,
            category = StudyCategory.AL
        )
        assertTrue(first is SubjectMutationResult.Success)

        val duplicate = repository.addSubject(
            rawName = "linear algebra",
            colorHex = 0xFF5EEAD4,
            targetMinutesPerWeek = 120,
            category = StudyCategory.AL
        )
        assertEquals(SubjectMutationResult.DuplicateName, duplicate)

        val subjects = repository.observeAllSubjects().first()
        assertEquals(1, subjects.size)
        assertEquals("Linear Algebra", subjects.first().name)
        assertEquals(StudyCategory.AL, subjects.first().studyCategory)
    }

    @Test
    fun `study categories and AAT levels allow same name across levels while blocking same-level duplicates`() = runTest {
        val alResult = repository.addSubject(
            rawName = "Accounting",
            colorHex = 0xFF9D6BFF,
            targetMinutesPerWeek = 180,
            category = StudyCategory.AL,
            iconKey = SubjectIconOption.ACCOUNTING.key
        )
        val aat1Result = repository.addSubject(
            rawName = "Accounting",
            colorHex = 0xFF5EEAD4,
            targetMinutesPerWeek = 180,
            category = StudyCategory.AAT,
            aatLevel = AatLevel.LEVEL_1,
            iconKey = SubjectIconOption.ACCOUNTING.key
        )
        val aat2Result = repository.addSubject(
            rawName = "Accounting",
            colorHex = 0xFF60A5FA,
            targetMinutesPerWeek = 240,
            category = StudyCategory.AAT,
            aatLevel = AatLevel.LEVEL_2,
            iconKey = SubjectIconOption.CALCULATE.key
        )
        val aat3Result = repository.addSubject(
            rawName = "Accounting",
            colorHex = 0xFFF472B6,
            targetMinutesPerWeek = 300,
            category = StudyCategory.AAT,
            aatLevel = AatLevel.LEVEL_3
        )
        val otherResult = repository.addSubject(
            rawName = "Accounting",
            colorHex = 0xFFFBBF24,
            targetMinutesPerWeek = 120,
            category = StudyCategory.OTHER
        )

        assertTrue(alResult is SubjectMutationResult.Success)
        assertTrue(aat1Result is SubjectMutationResult.Success)
        assertTrue(aat2Result is SubjectMutationResult.Success)
        assertTrue(aat3Result is SubjectMutationResult.Success)
        assertTrue(otherResult is SubjectMutationResult.Success)

        val duplicateSameLevel = repository.addSubject(
            rawName = "  accounting ",
            colorHex = 0xFF9D6BFF,
            targetMinutesPerWeek = 120,
            category = StudyCategory.AAT,
            aatLevel = AatLevel.LEVEL_1
        )
        assertEquals(SubjectMutationResult.DuplicateName, duplicateSameLevel)

        val duplicateOther = repository.addSubject(
            rawName = "ACCOUNTING",
            colorHex = 0xFF9D6BFF,
            targetMinutesPerWeek = 120,
            category = StudyCategory.OTHER
        )
        assertEquals(SubjectMutationResult.DuplicateName, duplicateOther)

        val missingLevel = repository.addSubject(
            rawName = "Taxation",
            colorHex = 0xFF9D6BFF,
            targetMinutesPerWeek = 120,
            category = StudyCategory.AAT,
            aatLevel = null
        )
        assertEquals(SubjectMutationResult.MissingAatLevel, missingLevel)

        val allSubjects = repository.observeAllSubjects().first()
        assertEquals(5, allSubjects.size)
    }

    @Test
    fun `migration 1 to 2 preserves existing v1 subjects tasks and focus sessions`() = runTest {
        val dbName = "migration_v1_to_v2_test.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()

        val v1Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v1Db.execSQL(
            "CREATE TABLE IF NOT EXISTS `subjects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `colorHex` INTEGER NOT NULL, `targetMinutesPerWeek` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL)"
        )
        v1Db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_subjects_normalizedName` ON `subjects` (`normalizedName`)"
        )
        v1Db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `subjectId` INTEGER, `priority` INTEGER NOT NULL, `dueDateEpochDay` INTEGER, `estimatedMinutes` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `completedAtMillis` INTEGER, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_subjectId` ON `tasks` (`subjectId`)")
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDateEpochDay` ON `tasks` (`dueDateEpochDay`)")
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
        v1Db.execSQL(
            "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionKey` TEXT NOT NULL, `subjectId` INTEGER, `taskId` INTEGER, `plannedMinutes` INTEGER NOT NULL, `actualSeconds` INTEGER NOT NULL, `startedAtMillis` INTEGER NOT NULL, `completedAtMillis` INTEGER NOT NULL, `localDateEpochDay` INTEGER NOT NULL, FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        v1Db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_focus_sessions_sessionKey` ON `focus_sessions` (`sessionKey`)")
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_subjectId` ON `focus_sessions` (`subjectId`)")
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_taskId` ON `focus_sessions` (`taskId`)")
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_localDateEpochDay` ON `focus_sessions` (`localDateEpochDay`)")
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_completedAtMillis` ON `focus_sessions` (`completedAtMillis`)")

        v1Db.execSQL(
            "INSERT INTO `subjects` (`id`, `name`, `normalizedName`, `colorHex`, `targetMinutesPerWeek`, `createdAtMillis`) VALUES (7, 'Combined Maths', 'combined maths', 4288506879, 240, 1700000000000)"
        )
        v1Db.execSQL(
            "INSERT INTO `subjects` (`id`, `name`, `normalizedName`, `colorHex`, `targetMinutesPerWeek`, `createdAtMillis`) VALUES (8, 'Physics', 'physics', 4284507642, 180, 1700000001000)"
        )
        v1Db.execSQL(
            "INSERT INTO `tasks` (`id`, `title`, `notes`, `subjectId`, `priority`, `dueDateEpochDay`, `estimatedMinutes`, `isCompleted`, `completedAtMillis`, `createdAtMillis`, `updatedAtMillis`) VALUES (11, 'Integration Practice', 'Past papers', 7, 2, 20000, 45, 0, NULL, 1700000000000, 1700000000000)"
        )
        v1Db.execSQL(
            "INSERT INTO `tasks` (`id`, `title`, `notes`, `subjectId`, `priority`, `dueDateEpochDay`, `estimatedMinutes`, `isCompleted`, `completedAtMillis`, `createdAtMillis`, `updatedAtMillis`) VALUES (12, 'Mechanics Revision', 'Completed sheet', 8, 1, 20001, 30, 1, 1700002000000, 1700000000000, 1700002000000)"
        )
        v1Db.execSQL(
            "INSERT INTO `tasks` (`id`, `title`, `notes`, `subjectId`, `priority`, `dueDateEpochDay`, `estimatedMinutes`, `isCompleted`, `completedAtMillis`, `createdAtMillis`, `updatedAtMillis`) VALUES (13, 'General Study Plan', 'No subject attached', NULL, 0, NULL, 25, 0, NULL, 1700000000000, 1700000000000)"
        )
        v1Db.execSQL(
            "INSERT INTO `focus_sessions` (`id`, `sessionKey`, `subjectId`, `taskId`, `plannedMinutes`, `actualSeconds`, `startedAtMillis`, `completedAtMillis`, `localDateEpochDay`) VALUES (3, 'legacy-session-key', 7, 11, 25, 1500, 1700000000000, 1700001500000, 20000)"
        )
        v1Db.execSQL(
            "INSERT INTO `focus_sessions` (`id`, `sessionKey`, `subjectId`, `taskId`, `plannedMinutes`, `actualSeconds`, `startedAtMillis`, `completedAtMillis`, `localDateEpochDay`) VALUES (4, 'legacy-unlinked-session', NULL, NULL, 15, 900, 1700003000000, 1700003900000, 20001)"
        )
        v1Db.version = 1
        v1Db.close()

        val migratedDb = Room.databaseBuilder(context, SaviiFocusDatabase::class.java, dbName)
            .addMigrations(SaviiFocusDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        try {
            // Verify SQLite foreign key integrity after v1 -> v2 schema migration
            migratedDb.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { cursor ->
                assertEquals(0, cursor.count)
            }

            val migratedRepo = SaviiFocusRepository(
                subjectDao = migratedDb.subjectDao(),
                taskDao = migratedDb.taskDao(),
                focusSessionDao = migratedDb.focusSessionDao()
            )

            val subjects = migratedRepo.observeAllSubjects().first()
            assertEquals(2, subjects.size)
            val maths = subjects.first { it.id == 7L }
            assertEquals("Combined Maths", maths.name)
            assertEquals(StudyCategory.AL, maths.studyCategory)
            assertEquals(0, maths.aatLevel)
            assertEquals(SubjectIconOption.BOOK.key, maths.iconKey)
            val physics = subjects.first { it.id == 8L }
            assertEquals("Physics", physics.name)
            assertEquals(StudyCategory.AL, physics.studyCategory)

            val tasks = migratedRepo.observeAllTasksWithSubject().first()
            assertEquals(3, tasks.size)
            val task11 = tasks.first { it.task.id == 11L }
            assertEquals(7L, task11.task.subjectId)
            assertEquals("Combined Maths", task11.subjectName)
            assertFalse(task11.task.isCompleted)

            val task12 = tasks.first { it.task.id == 12L }
            assertEquals(8L, task12.task.subjectId)
            assertTrue(task12.task.isCompleted)
            assertEquals(1700002000000L, task12.task.completedAtMillis)

            val task13 = tasks.first { it.task.id == 13L }
            assertEquals(null, task13.task.subjectId)
            assertEquals("General Study Plan", task13.task.title)

            val sessions = migratedRepo.observeRecentFocusSessions(10).first()
            assertEquals(2, sessions.size)
            val linkedSession = sessions.first { it.session.id == 3L }
            assertEquals(7L, linkedSession.session.subjectId)
            assertEquals(11L, linkedSession.session.taskId)
            val unlinkedSession = sessions.first { it.session.id == 4L }
            assertEquals(null, unlinkedSession.session.subjectId)
            assertEquals(null, unlinkedSession.session.taskId)

            val progressRows = migratedRepo.observeSubjectsWithProgress(weekStartEpochDay = 19999L).first()
            val mathsProgress = progressRows.first { it.subject.id == 7L }
            assertEquals(1, mathsProgress.pendingTaskCount)
            assertEquals(0, mathsProgress.completedTaskCount)
            assertEquals(1500, mathsProgress.totalFocusSeconds)
            val physicsProgress = progressRows.first { it.subject.id == 8L }
            assertEquals(0, physicsProgress.pendingTaskCount)
            assertEquals(1, physicsProgress.completedTaskCount)

            // Same name in A/L should still be blocked, while AAT Level 1 allows it
            assertEquals(
                SubjectMutationResult.DuplicateName,
                migratedRepo.addSubject("combined maths", 0xFF9D6BFF, 180, StudyCategory.AL)
            )
            assertTrue(
                migratedRepo.addSubject(
                    rawName = "Combined Maths",
                    colorHex = 0xFF5EEAD4,
                    targetMinutesPerWeek = 180,
                    category = StudyCategory.AAT,
                    aatLevel = AatLevel.LEVEL_1
                ) is SubjectMutationResult.Success
            )

            // Recategorizing a migrated v1 subject keeps its ID and preserves task + session links
            val updateResult = migratedRepo.updateSubject(
                subjectId = 7L,
                rawName = "Combined Maths",
                colorHex = maths.colorHex,
                targetMinutesPerWeek = maths.targetMinutesPerWeek,
                category = StudyCategory.OTHER,
                aatLevel = null,
                iconKey = SubjectIconOption.CALCULATE.key
            )
            assertTrue(updateResult is SubjectMutationResult.Success)
            val updatedTask11 = migratedRepo.observeAllTasksWithSubject().first().first { it.task.id == 11L }
            assertEquals("Combined Maths (OTHER)", updatedTask11.subjectDisplayName)
            val updatedSession3 = migratedRepo.observeRecentFocusSessions(10).first().first { it.session.id == 3L }
            assertEquals("Combined Maths (OTHER)", updatedSession3.subjectDisplayName)
        } finally {
            migratedDb.close()
            dbFile.delete()
        }
    }

    @Test
    fun `focus timer preserves persisted paused 45m session across startup when default is 15m`() = runBlocking {
        val timerPrefs = TimerPreferencesStore(context)
        val appPrefs = AppPreferencesStore(context)

        try {
            appPrefs.setDefaultFocusMinutes(15)
            timerPrefs.saveSnapshot(
                PersistedTimerSnapshot(
                    status = TimerStatus.PAUSED,
                    sessionKey = "paused-session-45m",
                    presetMinutes = 45,
                    totalDurationMillis = 45 * 60_000L,
                    endEpochMillis = 0L,
                    remainingWhenPausedMillis = 30 * 60_000L,
                    startedAtEpochMillis = 1_700_000_000_000L
                )
            )

            val viewModel = FocusTimerViewModel(
                repository = repository,
                timerPreferencesStore = timerPrefs,
                appPreferencesStore = appPrefs,
                alarmScheduler = FocusTimerAlarmScheduler(context),
                notificationHelper = FocusNotificationHelper(context)
            )

            var latestState: FocusTimerUiState? = null
            val collectorJob = CoroutineScope(Dispatchers.Default).launch {
                viewModel.uiState.collect { latestState = it }
            }

            try {
                val deadline = System.currentTimeMillis() + 5_000L
                while (latestState?.status != TimerStatus.PAUSED && System.currentTimeMillis() < deadline) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    Thread.sleep(20)
                }

                // Pump one extra cycle so any pending appPreferencesStore.preferencesFlow emission finishes
                repeat(5) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    Thread.sleep(20)
                }

                assertEquals(TimerStatus.PAUSED, latestState?.status)
                assertEquals(45, latestState?.presetMinutes)
                assertEquals(30 * 60_000L, latestState?.remainingMillis)

                val persistedAfterStartup = timerPrefs.snapshotFlow.first()
                assertEquals(TimerStatus.PAUSED, persistedAfterStartup.status)
                assertEquals(45, persistedAfterStartup.presetMinutes)
                assertEquals(30 * 60_000L, persistedAfterStartup.remainingWhenPausedMillis)
            } finally {
                collectorJob.cancel()
                Shadows.shadowOf(Looper.getMainLooper()).idle()
            }
        } finally {
            timerPrefs.saveSnapshot(PersistedTimerSnapshot())
            appPrefs.setDefaultFocusMinutes(25)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `focus session insertion is idempotent for same sessionKey`() = runTest {
        val now = System.currentTimeMillis()
        val todayEpoch = DateTimeUtils.epochDayFromMillis(now)

        val firstInsert = repository.recordCompletedFocusSessionOnce(
            sessionKey = "session-uuid-123",
            subjectId = null,
            taskId = null,
            plannedMinutes = 25,
            actualSeconds = 1500,
            startedAtMillis = now - 1_500_000L,
            completedAtMillis = now
        )
        val duplicateInsert = repository.recordCompletedFocusSessionOnce(
            sessionKey = "session-uuid-123",
            subjectId = null,
            taskId = null,
            plannedMinutes = 25,
            actualSeconds = 1500,
            startedAtMillis = now - 1_500_000L,
            completedAtMillis = now
        )

        assertTrue(firstInsert)
        assertFalse(duplicateInsert)

        val summary = repository.observeFocusSummaryForDay(todayEpoch).first()
        assertEquals(1, summary.sessionCount)
        assertEquals(1500, summary.totalSeconds)
    }

    @Test
    fun `task CRUD and completion updates reflect in repository flows`() = runTest {
        val subjectResult = repository.addSubject("Physics", 0xFF60A5FA, 240, StudyCategory.AL)
        val subjectId = (subjectResult as SubjectMutationResult.Success).subjectId

        val taskId = repository.saveTask(
            taskId = null,
            title = "Review Kinematics",
            notes = "Chapter 2",
            subjectId = subjectId,
            priority = TaskPriority.HIGH,
            dueDateEpochDay = DateTimeUtils.todayEpochDay(),
            estimatedMinutes = 45
        )!!

        var tasks = repository.observeAllTasksWithSubject().first()
        assertEquals(1, tasks.size)
        assertEquals("Physics", tasks.first().subjectName)
        assertFalse(tasks.first().task.isCompleted)

        repository.setTaskCompleted(taskId, true)
        tasks = repository.observeAllTasksWithSubject().first()
        assertTrue(tasks.first().task.isCompleted)
        assertEquals(1, repository.observeTotalCompletedTaskCount().first())
    }

    @Test
    fun `deleted subject or task foreign keys fall back cleanly on undo and session logging`() = runTest {
        val subjectId = (repository.addSubject(
            rawName = "Economics",
            colorHex = 0xFF9D6BFF,
            targetMinutesPerWeek = 180,
            category = StudyCategory.AL
        ) as SubjectMutationResult.Success).subjectId

        val taskId = repository.saveTask(
            taskId = null,
            title = "Demand Curves",
            notes = "",
            subjectId = subjectId,
            priority = TaskPriority.MEDIUM,
            dueDateEpochDay = DateTimeUtils.todayEpochDay(),
            estimatedMinutes = 25
        )!!

        val deletedTaskSnapshot = TaskEntity(
            id = taskId,
            title = "Demand Curves",
            notes = "",
            subjectId = subjectId,
            priority = TaskPriority.MEDIUM.level,
            dueDateEpochDay = DateTimeUtils.todayEpochDay(),
            estimatedMinutes = 25
        )

        repository.deleteTask(taskId)
        repository.deleteSubject(subjectId)

        // Session referencing deleted subject/task should persist without SQLiteConstraintException
        val now = System.currentTimeMillis()
        val inserted = repository.recordCompletedFocusSessionOnce(
            sessionKey = "fk-safe-session",
            subjectId = subjectId,
            taskId = taskId,
            plannedMinutes = 25,
            actualSeconds = 1500,
            startedAtMillis = now - 1_500_000L,
            completedAtMillis = now
        )
        assertTrue(inserted)
        val sessions = repository.observeRecentFocusSessions(10).first()
        assertEquals(1, sessions.size)
        assertEquals(null, sessions.first().session.subjectId)
        assertEquals(null, sessions.first().session.taskId)
        assertEquals(1500, sessions.first().session.actualSeconds)

        // Undo delete of task referencing deleted subject should restore task with null subjectId
        repository.restoreTask(deletedTaskSnapshot)
        val restoredTasks = repository.observeAllTasksWithSubject().first()
        assertEquals(1, restoredTasks.size)
        assertEquals("Demand Curves", restoredTasks.first().task.title)
        assertEquals(null, restoredTasks.first().task.subjectId)
    }

    @Test
    fun `updating existing subject category preserves linked tasks and sessions`() = runTest {
        val subjectId = (repository.addSubject(
            rawName = "Financial Accounting",
            colorHex = 0xFF5EEAD4,
            targetMinutesPerWeek = 180,
            category = StudyCategory.AL
        ) as SubjectMutationResult.Success).subjectId

        val taskId = repository.saveTask(
            taskId = null,
            title = "Balance Sheet",
            notes = "",
            subjectId = subjectId,
            priority = TaskPriority.HIGH,
            dueDateEpochDay = DateTimeUtils.todayEpochDay(),
            estimatedMinutes = 45
        )!!

        val now = System.currentTimeMillis()
        repository.recordCompletedFocusSessionOnce(
            sessionKey = "recat-session",
            subjectId = subjectId,
            taskId = taskId,
            plannedMinutes = 45,
            actualSeconds = 2700,
            startedAtMillis = now - 2_700_000L,
            completedAtMillis = now
        )

        val updateResult = repository.updateSubject(
            subjectId = subjectId,
            rawName = "Financial Accounting",
            colorHex = 0xFF5EEAD4,
            targetMinutesPerWeek = 240,
            category = StudyCategory.AAT,
            aatLevel = AatLevel.LEVEL_2,
            iconKey = SubjectIconOption.ACCOUNTING.key
        )
        assertTrue(updateResult is SubjectMutationResult.Success)

        val tasks = repository.observeAllTasksWithSubject().first()
        assertEquals(1, tasks.size)
        assertEquals("Financial Accounting (AAT L2)", tasks.first().subjectDisplayName)

        val sessions = repository.observeRecentFocusSessions(10).first()
        assertEquals(1, sessions.size)
        assertEquals("Financial Accounting (AAT L2)", sessions.first().subjectDisplayName)
    }

    @Test
    fun `unrelated preference change does not reset idle focus timer preset`() = runBlocking {
        val timerPrefs = TimerPreferencesStore(context)
        val appPrefs = AppPreferencesStore(context)

        try {
            appPrefs.setDefaultFocusMinutes(25)
            timerPrefs.saveSnapshot(PersistedTimerSnapshot())

            val viewModel = FocusTimerViewModel(
                repository = repository,
                timerPreferencesStore = timerPrefs,
                appPreferencesStore = appPrefs,
                alarmScheduler = FocusTimerAlarmScheduler(context),
                notificationHelper = FocusNotificationHelper(context)
            )

            var latestState: FocusTimerUiState? = null
            val collectorJob = CoroutineScope(Dispatchers.Default).launch {
                viewModel.uiState.collect { latestState = it }
            }

            try {
                val initDeadline = System.currentTimeMillis() + 5_000L
                while (latestState == null && System.currentTimeMillis() < initDeadline) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    Thread.sleep(20)
                }

                viewModel.selectPreset(45)
                val presetDeadline = System.currentTimeMillis() + 5_000L
                while (latestState?.presetMinutes != 45 && System.currentTimeMillis() < presetDeadline) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    Thread.sleep(20)
                }
                assertEquals(45, latestState?.presetMinutes)

                // Toggle unrelated preference -> should keep 45m preset
                appPrefs.setHapticsEnabled(false)
                repeat(6) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    Thread.sleep(20)
                }
                assertEquals(45, latestState?.presetMinutes)

                // Explicitly changing defaultFocusMinutes -> should update IDLE preset to 60m
                appPrefs.setDefaultFocusMinutes(60)
                val defaultDeadline = System.currentTimeMillis() + 5_000L
                while (latestState?.presetMinutes != 60 && System.currentTimeMillis() < defaultDeadline) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    Thread.sleep(20)
                }
                assertEquals(60, latestState?.presetMinutes)
            } finally {
                collectorJob.cancel()
                Shadows.shadowOf(Looper.getMainLooper()).idle()
            }
        } finally {
            timerPrefs.saveSnapshot(PersistedTimerSnapshot())
            appPrefs.setDefaultFocusMinutes(25)
            appPrefs.setHapticsEnabled(true)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `planner search matches category badge and insights computes category stats`() = runBlocking {
        val aatId = (repository.addSubject(
            rawName = "Costing",
            colorHex = 0xFF5EEAD4,
            targetMinutesPerWeek = 180,
            category = StudyCategory.AAT,
            aatLevel = AatLevel.LEVEL_1
        ) as SubjectMutationResult.Success).subjectId
        val alId = (repository.addSubject(
            rawName = "Chemistry",
            colorHex = 0xFF9D6BFF,
            targetMinutesPerWeek = 180,
            category = StudyCategory.AL
        ) as SubjectMutationResult.Success).subjectId

        repository.saveTask(
            taskId = null,
            title = "Marginal Costing Sheet",
            notes = "",
            subjectId = aatId,
            priority = TaskPriority.HIGH,
            dueDateEpochDay = DateTimeUtils.todayEpochDay(),
            estimatedMinutes = 30
        )
        repository.saveTask(
            taskId = null,
            title = "Organic Revision",
            notes = "",
            subjectId = alId,
            priority = TaskPriority.MEDIUM,
            dueDateEpochDay = DateTimeUtils.todayEpochDay(),
            estimatedMinutes = 25
        )

        val now = System.currentTimeMillis()
        repository.recordCompletedFocusSessionOnce(
            sessionKey = "cat-stats-aat",
            subjectId = aatId,
            taskId = null,
            plannedMinutes = 25,
            actualSeconds = 1500,
            startedAtMillis = now - 1_500_000L,
            completedAtMillis = now
        )

        val plannerVm = PlannerViewModel(repository)
        val insightsVm = InsightsViewModel(repository)

        var plannerState: PlannerUiState? = null
        var insightsState: InsightsUiState? = null
        val job1 = CoroutineScope(Dispatchers.Default).launch {
            plannerVm.uiState.collect { plannerState = it }
        }
        val job2 = CoroutineScope(Dispatchers.Default).launch {
            insightsVm.uiState.collect { insightsState = it }
        }

        try {
            plannerVm.setSearchQuery("AAT L1")
            val deadline = System.currentTimeMillis() + 5_000L
            while (
                (plannerState?.isLoaded != true ||
                    plannerState?.tasks?.size != 1 ||
                    insightsState?.isLoaded != true ||
                    insightsState?.subjectProgress?.size != 2) &&
                System.currentTimeMillis() < deadline
            ) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }

            assertEquals(1, plannerState?.tasks?.size)
            assertEquals("Marginal Costing Sheet", plannerState?.tasks?.first()?.task?.title)

            val aatRow = insightsState?.subjectProgress?.first { it.subject.studyCategory == StudyCategory.AAT }
            assertEquals(1500, aatRow?.totalFocusSeconds)
            assertEquals(1, aatRow?.pendingTaskCount)
        } finally {
            job1.cancel()
            job2.cancel()
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }
}
