package com.example

import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.model.FocusSessionWithDetails
import com.example.data.local.model.TaskWithSubject
import com.example.domain.AatLevel
import com.example.domain.DateTimeUtils
import com.example.domain.StudyCategory
import com.example.domain.SubjectNameNormalizer
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaviiDomainUnitTest {

    @Test
    fun `subject normalizer collapses whitespace and ignores case`() {
        assertEquals("Discrete Math", SubjectNameNormalizer.cleanDisplayName("  Discrete   Math  "))
        assertEquals(
            SubjectNameNormalizer.normalizeKey("discrete math"),
            SubjectNameNormalizer.normalizeKey("  DISCRETE   MATH ")
        )
    }

    @Test
    fun `timer clock formats ceiling seconds cleanly`() {
        assertEquals("25:00", DateTimeUtils.formatTimerClock(25 * 60_000L))
        assertEquals("00:01", DateTimeUtils.formatTimerClock(250L))
        assertEquals("00:00", DateTimeUtils.formatTimerClock(0L))
        assertEquals("15:00", DateTimeUtils.formatTimerClock(15 * 60_000L))
    }

    @Test
    fun `overdue check respects completion status and local date`() {
        val today = LocalDate.of(2026, 11, 15)
        val yesterdayEpoch = today.minusDays(1).toEpochDay()
        val todayEpoch = today.toEpochDay()

        assertTrue(DateTimeUtils.isOverdue(yesterdayEpoch, isCompleted = false, today = today))
        assertFalse(DateTimeUtils.isOverdue(yesterdayEpoch, isCompleted = true, today = today))
        assertFalse(DateTimeUtils.isOverdue(todayEpoch, isCompleted = false, today = today))
        assertFalse(DateTimeUtils.isOverdue(null, isCompleted = false, today = today))
    }

    @Test
    fun `last7EpochDays returns 7 chronological days ending today`() {
        val today = LocalDate.of(2026, 11, 15)
        val days = DateTimeUtils.last7EpochDays(today)
        assertEquals(7, days.size)
        assertEquals(today.minusDays(6), days.first())
        assertEquals(today, days.last())
    }

    @Test
    fun `query models format category and AAT level badges accurately`() {
        val task = TaskEntity(id = 1L, title = "Past Paper")
        val session = FocusSessionEntity(
            id = 1L,
            sessionKey = "key-1",
            subjectId = 1L,
            taskId = 1L,
            plannedMinutes = 25,
            actualSeconds = 1500,
            startedAtMillis = 1000L,
            completedAtMillis = 2000L,
            localDateEpochDay = 20000L
        )

        val alTask = TaskWithSubject(task, "Physics", 0xFF60A5FA, StudyCategory.AL.storageValue, 0)
        val aatTask = TaskWithSubject(task, "Accounting", 0xFF5EEAD4, StudyCategory.AAT.storageValue, AatLevel.LEVEL_2.levelNumber)
        val otherTask = TaskWithSubject(task, "English", 0xFFFBBF24, StudyCategory.OTHER.storageValue, 0)
        val unlinkedTask = TaskWithSubject(task, null, null, null, null)

        assertEquals("Physics (A/L)", alTask.subjectDisplayName)
        assertEquals("Accounting (AAT L2)", aatTask.subjectDisplayName)
        assertEquals("English (OTHER)", otherTask.subjectDisplayName)
        assertNull(unlinkedTask.subjectDisplayName)

        val aatSession = FocusSessionWithDetails(
            session = session,
            subjectName = "Taxation",
            subjectColorHex = 0xFFF472B6,
            taskTitle = "Chapter 4",
            subjectCategory = StudyCategory.AAT.storageValue,
            subjectAatLevel = AatLevel.LEVEL_3.levelNumber
        )
        assertEquals("Taxation (AAT L3)", aatSession.subjectDisplayName)
    }
}
