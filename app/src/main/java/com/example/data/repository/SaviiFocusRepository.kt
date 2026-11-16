package com.example.data.repository

import com.example.data.local.dao.FocusSessionDao
import com.example.data.local.dao.SubjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.model.DailyFocusAggregate
import com.example.data.local.model.FocusSessionWithDetails
import com.example.data.local.model.PeriodFocusSummary
import com.example.data.local.model.SubjectProgressRow
import com.example.data.local.model.TaskWithSubject
import com.example.domain.AatLevel
import com.example.domain.DateTimeUtils
import com.example.domain.StudyCategory
import com.example.domain.SubjectIconOption
import com.example.domain.SubjectNameNormalizer
import com.example.domain.TaskPriority
import kotlinx.coroutines.flow.Flow

sealed interface SubjectMutationResult {
    data class Success(val subjectId: Long) : SubjectMutationResult
    data object EmptyName : SubjectMutationResult
    data object DuplicateName : SubjectMutationResult
    data object MissingAatLevel : SubjectMutationResult
    data object NotFound : SubjectMutationResult
}

class SaviiFocusRepository(
    private val subjectDao: SubjectDao,
    private val taskDao: TaskDao,
    private val focusSessionDao: FocusSessionDao
) {

    // Subjects
    fun observeAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.observeAllSubjects()

    fun observeSubjectsWithProgress(weekStartEpochDay: Long): Flow<List<SubjectProgressRow>> =
        subjectDao.observeSubjectsWithProgress(weekStartEpochDay)

    suspend fun addSubject(
        rawName: String,
        colorHex: Long,
        targetMinutesPerWeek: Int,
        category: StudyCategory = StudyCategory.AL,
        aatLevel: AatLevel? = null,
        iconKey: String = SubjectIconOption.BOOK.key
    ): SubjectMutationResult {
        val cleanName = SubjectNameNormalizer.cleanDisplayName(rawName)
        if (cleanName.isEmpty()) return SubjectMutationResult.EmptyName
        if (category == StudyCategory.AAT && aatLevel == null) {
            return SubjectMutationResult.MissingAatLevel
        }
        val levelCode = if (category == StudyCategory.AAT) aatLevel!!.levelNumber else 0
        val normalized = SubjectNameNormalizer.normalizeKey(cleanName)
        if (subjectDao.findByNormalizedNameInScope(normalized, category.storageValue, levelCode) != null) {
            return SubjectMutationResult.DuplicateName
        }
        val id = subjectDao.insertSubject(
            SubjectEntity(
                name = cleanName,
                normalizedName = normalized,
                category = category.storageValue,
                aatLevel = levelCode,
                iconKey = SubjectIconOption.fromKey(iconKey).key,
                colorHex = colorHex,
                targetMinutesPerWeek = targetMinutesPerWeek.coerceIn(30, 3000)
            )
        )
        return SubjectMutationResult.Success(id)
    }

    suspend fun updateSubject(
        subjectId: Long,
        rawName: String,
        colorHex: Long,
        targetMinutesPerWeek: Int,
        category: StudyCategory? = null,
        aatLevel: AatLevel? = null,
        iconKey: String? = null
    ): SubjectMutationResult {
        val existing = subjectDao.findById(subjectId) ?: return SubjectMutationResult.NotFound
        val cleanName = SubjectNameNormalizer.cleanDisplayName(rawName)
        if (cleanName.isEmpty()) return SubjectMutationResult.EmptyName
        val targetCategory = category ?: existing.studyCategory
        val resolvedAatLevel = when {
            targetCategory != StudyCategory.AAT -> 0
            aatLevel != null -> aatLevel.levelNumber
            existing.aatLevel in 1..3 -> existing.aatLevel
            else -> return SubjectMutationResult.MissingAatLevel
        }
        val normalized = SubjectNameNormalizer.normalizeKey(cleanName)
        val conflict = subjectDao.findByNormalizedNameInScope(
            normalizedName = normalized,
            category = targetCategory.storageValue,
            aatLevel = resolvedAatLevel
        )
        if (conflict != null && conflict.id != subjectId) {
            return SubjectMutationResult.DuplicateName
        }
        subjectDao.updateSubject(
            existing.copy(
                name = cleanName,
                normalizedName = normalized,
                category = targetCategory.storageValue,
                aatLevel = resolvedAatLevel,
                iconKey = iconKey?.let { SubjectIconOption.fromKey(it).key } ?: existing.iconKey,
                colorHex = colorHex,
                targetMinutesPerWeek = targetMinutesPerWeek.coerceIn(30, 3000)
            )
        )
        return SubjectMutationResult.Success(subjectId)
    }

    suspend fun deleteSubject(subjectId: Long) {
        subjectDao.deleteSubjectById(subjectId)
    }

    // Tasks
    fun observeAllTasksWithSubject(): Flow<List<TaskWithSubject>> =
        taskDao.observeAllTasksWithSubject()

    fun observePendingTasksWithSubject(): Flow<List<TaskWithSubject>> =
        taskDao.observePendingTasksWithSubject()

    fun observeTotalCompletedTaskCount(): Flow<Int> =
        taskDao.observeTotalCompletedTaskCount()

    fun observeCompletedTaskCountInRange(startMillis: Long, endMillis: Long): Flow<Int> =
        taskDao.observeCompletedTaskCountInRange(startMillis, endMillis)

    suspend fun saveTask(
        taskId: Long? = null,
        title: String,
        notes: String,
        subjectId: Long?,
        priority: TaskPriority,
        dueDateEpochDay: Long?,
        estimatedMinutes: Int
    ): Long? {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return null
        val now = System.currentTimeMillis()
        val safeSubjectId = subjectId?.takeIf { subjectDao.findById(it) != null }
        val existing = taskId?.let { taskDao.findById(it) }
        return if (existing == null) {
            taskDao.insertTask(
                TaskEntity(
                    title = cleanTitle,
                    notes = notes.trim(),
                    subjectId = safeSubjectId,
                    priority = priority.level,
                    dueDateEpochDay = dueDateEpochDay,
                    estimatedMinutes = estimatedMinutes.coerceIn(5, 360),
                    isCompleted = false,
                    completedAtMillis = null,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        } else {
            taskDao.updateTask(
                existing.copy(
                    title = cleanTitle,
                    notes = notes.trim(),
                    subjectId = safeSubjectId,
                    priority = priority.level,
                    dueDateEpochDay = dueDateEpochDay,
                    estimatedMinutes = estimatedMinutes.coerceIn(5, 360),
                    updatedAtMillis = now
                )
            )
            existing.id
        }
    }

    suspend fun setTaskCompleted(taskId: Long, completed: Boolean) {
        val now = System.currentTimeMillis()
        taskDao.updateCompletionStatus(
            taskId = taskId,
            isCompleted = completed,
            completedAtMillis = if (completed) now else null,
            updatedAtMillis = now
        )
    }

    suspend fun restoreTask(task: TaskEntity) {
        val safeSubjectId = task.subjectId?.takeIf { subjectDao.findById(it) != null }
        taskDao.insertTask(task.copy(subjectId = safeSubjectId))
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    // Focus Sessions
    fun observeRecentFocusSessions(limit: Int = 30): Flow<List<FocusSessionWithDetails>> =
        focusSessionDao.observeRecentSessions(limit)

    fun observeFocusSummaryForDay(epochDay: Long): Flow<PeriodFocusSummary> =
        focusSessionDao.observeSummaryForDay(epochDay)

    fun observeFocusSummaryInDayRange(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<PeriodFocusSummary> =
        focusSessionDao.observeSummaryInDayRange(startEpochDay, endEpochDay)

    fun observeAllTimeFocusSummary(): Flow<PeriodFocusSummary> =
        focusSessionDao.observeAllTimeSummary()

    fun observeDailyFocusBreakdown(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<DailyFocusAggregate>> =
        focusSessionDao.observeDailyBreakdown(startEpochDay, endEpochDay)

    suspend fun recordCompletedFocusSessionOnce(
        sessionKey: String,
        subjectId: Long?,
        taskId: Long?,
        plannedMinutes: Int,
        actualSeconds: Int,
        startedAtMillis: Long,
        completedAtMillis: Long
    ): Boolean {
        if (sessionKey.isBlank() || actualSeconds <= 0) return false
        if (focusSessionDao.existsBySessionKey(sessionKey)) return false
        val localEpochDay = DateTimeUtils.epochDayFromMillis(completedAtMillis)
        val safeSubjectId = subjectId?.takeIf { subjectDao.findById(it) != null }
        val safeTaskId = taskId?.takeIf { taskDao.findById(it) != null }
        val rowId = focusSessionDao.insertSessionIgnoreDuplicate(
            FocusSessionEntity(
                sessionKey = sessionKey,
                subjectId = safeSubjectId,
                taskId = safeTaskId,
                plannedMinutes = plannedMinutes,
                actualSeconds = actualSeconds,
                startedAtMillis = startedAtMillis,
                completedAtMillis = completedAtMillis,
                localDateEpochDay = localEpochDay
            )
        )
        return rowId != -1L
    }
}
