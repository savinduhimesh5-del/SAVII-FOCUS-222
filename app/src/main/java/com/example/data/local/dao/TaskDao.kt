package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TaskEntity
import com.example.data.local.model.TaskWithSubject
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query(
        """
        SELECT t.*, s.name AS subjectName, s.colorHex AS subjectColorHex,
               s.category AS subjectCategory, s.aatLevel AS subjectAatLevel
        FROM tasks t
        LEFT JOIN subjects s ON t.subjectId = s.id
        ORDER BY t.isCompleted ASC,
                 CASE WHEN t.dueDateEpochDay IS NULL THEN 1 ELSE 0 END ASC,
                 t.dueDateEpochDay ASC,
                 t.priority DESC,
                 t.createdAtMillis DESC
        """
    )
    fun observeAllTasksWithSubject(): Flow<List<TaskWithSubject>>

    @Query(
        """
        SELECT t.*, s.name AS subjectName, s.colorHex AS subjectColorHex,
               s.category AS subjectCategory, s.aatLevel AS subjectAatLevel
        FROM tasks t
        LEFT JOIN subjects s ON t.subjectId = s.id
        WHERE t.isCompleted = 0
        ORDER BY CASE WHEN t.dueDateEpochDay IS NULL THEN 1 ELSE 0 END ASC,
                 t.dueDateEpochDay ASC,
                 t.priority DESC,
                 t.createdAtMillis DESC
        """
    )
    fun observePendingTasksWithSubject(): Flow<List<TaskWithSubject>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun observeTotalCompletedTaskCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE isCompleted = 1
          AND completedAtMillis IS NOT NULL
          AND completedAtMillis BETWEEN :startMillis AND :endMillis
        """
    )
    fun observeCompletedTaskCountInRange(startMillis: Long, endMillis: Long): Flow<Int>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun findById(taskId: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query(
        """
        UPDATE tasks
        SET isCompleted = :isCompleted,
            completedAtMillis = :completedAtMillis,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :taskId
        """
    )
    suspend fun updateCompletionStatus(
        taskId: Long,
        isCompleted: Boolean,
        completedAtMillis: Long?,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
}
