package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["sessionKey"], unique = true),
        Index(value = ["subjectId"]),
        Index(value = ["taskId"]),
        Index(value = ["localDateEpochDay"]),
        Index(value = ["completedAtMillis"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionKey: String,
    val subjectId: Long? = null,
    val taskId: Long? = null,
    val plannedMinutes: Int,
    val actualSeconds: Int,
    val startedAtMillis: Long,
    val completedAtMillis: Long,
    val localDateEpochDay: Long
)
