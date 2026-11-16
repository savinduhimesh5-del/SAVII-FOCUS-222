package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.model.DailyFocusAggregate
import com.example.data.local.model.FocusSessionWithDetails
import com.example.data.local.model.PeriodFocusSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionIgnoreDuplicate(session: FocusSessionEntity): Long

    @Query(
        """
        SELECT fs.*, s.name AS subjectName, s.colorHex AS subjectColorHex, t.title AS taskTitle,
               s.category AS subjectCategory, s.aatLevel AS subjectAatLevel
        FROM focus_sessions fs
        LEFT JOIN subjects s ON fs.subjectId = s.id
        LEFT JOIN tasks t ON fs.taskId = t.id
        ORDER BY fs.completedAtMillis DESC
        LIMIT :limit
        """
    )
    fun observeRecentSessions(limit: Int = 30): Flow<List<FocusSessionWithDetails>>

    @Query(
        """
        SELECT COALESCE(SUM(actualSeconds), 0) AS totalSeconds,
               COUNT(*) AS sessionCount
        FROM focus_sessions
        WHERE localDateEpochDay = :epochDay
        """
    )
    fun observeSummaryForDay(epochDay: Long): Flow<PeriodFocusSummary>

    @Query(
        """
        SELECT COALESCE(SUM(actualSeconds), 0) AS totalSeconds,
               COUNT(*) AS sessionCount
        FROM focus_sessions
        WHERE localDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        """
    )
    fun observeSummaryInDayRange(startEpochDay: Long, endEpochDay: Long): Flow<PeriodFocusSummary>

    @Query(
        """
        SELECT COALESCE(SUM(actualSeconds), 0) AS totalSeconds,
               COUNT(*) AS sessionCount
        FROM focus_sessions
        """
    )
    fun observeAllTimeSummary(): Flow<PeriodFocusSummary>

    @Query(
        """
        SELECT localDateEpochDay,
               COALESCE(SUM(actualSeconds), 0) AS totalSeconds,
               COUNT(*) AS sessionCount
        FROM focus_sessions
        WHERE localDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY localDateEpochDay
        ORDER BY localDateEpochDay ASC
        """
    )
    fun observeDailyBreakdown(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyFocusAggregate>>

    @Query("SELECT EXISTS(SELECT 1 FROM focus_sessions WHERE sessionKey = :sessionKey)")
    suspend fun existsBySessionKey(sessionKey: String): Boolean
}
