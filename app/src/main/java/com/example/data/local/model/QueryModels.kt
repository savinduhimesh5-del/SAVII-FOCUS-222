package com.example.data.local.model

import androidx.room.Embedded
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.AatLevel
import com.example.domain.StudyCategory

private fun formatSubjectDisplayBadge(
    name: String?,
    categoryStorage: String?,
    aatLevelCode: Int?
): String? {
    if (name == null) return null
    val category = StudyCategory.fromStorage(categoryStorage)
    val badge = when (category) {
        StudyCategory.AL -> StudyCategory.AL.displayName
        StudyCategory.AAT -> "AAT ${AatLevel.fromCode(aatLevelCode ?: 0)?.shortBadge ?: "L1"}"
        StudyCategory.OTHER -> StudyCategory.OTHER.displayName
    }
    return "$name ($badge)"
}

data class TaskWithSubject(
    @Embedded val task: TaskEntity,
    val subjectName: String?,
    val subjectColorHex: Long?,
    val subjectCategory: String? = null,
    val subjectAatLevel: Int? = null
) {
    val subjectDisplayName: String?
        get() = formatSubjectDisplayBadge(subjectName, subjectCategory, subjectAatLevel)
}

data class FocusSessionWithDetails(
    @Embedded val session: FocusSessionEntity,
    val subjectName: String?,
    val subjectColorHex: Long?,
    val taskTitle: String?,
    val subjectCategory: String? = null,
    val subjectAatLevel: Int? = null
) {
    val subjectDisplayName: String?
        get() = formatSubjectDisplayBadge(subjectName, subjectCategory, subjectAatLevel)
}

data class SubjectProgressRow(
    @Embedded val subject: SubjectEntity,
    val pendingTaskCount: Int,
    val completedTaskCount: Int,
    val totalFocusSeconds: Int,
    val weekFocusSeconds: Int
)

data class DailyFocusAggregate(
    val localDateEpochDay: Long,
    val totalSeconds: Int,
    val sessionCount: Int
)

data class PeriodFocusSummary(
    val totalSeconds: Int,
    val sessionCount: Int
)
