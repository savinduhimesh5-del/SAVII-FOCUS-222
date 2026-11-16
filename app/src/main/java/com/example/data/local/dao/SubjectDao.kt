package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.model.SubjectProgressRow
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE ASC")
    fun observeAllSubjects(): Flow<List<SubjectEntity>>

    @Query(
        """
        SELECT s.*,
               (SELECT COUNT(*) FROM tasks t WHERE t.subjectId = s.id AND t.isCompleted = 0) AS pendingTaskCount,
               (SELECT COUNT(*) FROM tasks t WHERE t.subjectId = s.id AND t.isCompleted = 1) AS completedTaskCount,
               COALESCE((SELECT SUM(fs.actualSeconds) FROM focus_sessions fs WHERE fs.subjectId = s.id), 0) AS totalFocusSeconds,
               COALESCE((SELECT SUM(fs.actualSeconds) FROM focus_sessions fs WHERE fs.subjectId = s.id AND fs.localDateEpochDay >= :weekStartEpochDay), 0) AS weekFocusSeconds
        FROM subjects s
        ORDER BY s.name COLLATE NOCASE ASC
        """
    )
    fun observeSubjectsWithProgress(weekStartEpochDay: Long): Flow<List<SubjectProgressRow>>

    @Query(
        """
        SELECT * FROM subjects
        WHERE normalizedName = :normalizedName
          AND category = :category
          AND aatLevel = :aatLevel
        LIMIT 1
        """
    )
    suspend fun findByNormalizedNameInScope(
        normalizedName: String,
        category: String,
        aatLevel: Int
    ): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubjectById(subjectId: Long)
}
