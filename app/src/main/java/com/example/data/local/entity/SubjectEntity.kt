package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.AatLevel
import com.example.domain.StudyCategory
import com.example.domain.SubjectIconOption

@Entity(
    tableName = "subjects",
    indices = [
        Index(value = ["normalizedName", "category", "aatLevel"], unique = true),
        Index(value = ["category", "aatLevel"])
    ]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val normalizedName: String,
    val category: String = StudyCategory.AL.storageValue,
    val aatLevel: Int = 0,
    val iconKey: String = SubjectIconOption.BOOK.key,
    val colorHex: Long,
    val targetMinutesPerWeek: Int = 180,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val studyCategory: StudyCategory
        get() = StudyCategory.fromStorage(category)

    val aatLevelEnum: AatLevel?
        get() = if (studyCategory == StudyCategory.AAT) AatLevel.fromCode(aatLevel) else null

    val iconOption: SubjectIconOption
        get() = SubjectIconOption.fromKey(iconKey)

    val categoryBadgeLabel: String
        get() = when (studyCategory) {
            StudyCategory.AL -> StudyCategory.AL.displayName
            StudyCategory.AAT -> "AAT ${aatLevelEnum?.shortBadge ?: "L1"}"
            StudyCategory.OTHER -> StudyCategory.OTHER.displayName
        }

    val displayWithCategory: String
        get() = "$name ($categoryBadgeLabel)"
}
