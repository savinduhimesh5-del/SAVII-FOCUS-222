package com.example.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    @Volatile
    private var cachedFormattersLocale: Locale? = null
    @Volatile
    private var shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    @Volatile
    private var weekdayShortFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    @Volatile
    private var fullHeaderFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    @Volatile
    private var timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    @Volatile
    private var longDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    private fun ensureCurrentLocaleFormatters() {
        val current = Locale.getDefault()
        if (cachedFormattersLocale != current) {
            synchronized(this) {
                val recheck = Locale.getDefault()
                if (cachedFormattersLocale != recheck) {
                    shortDateFormatter = DateTimeFormatter.ofPattern("MMM d", recheck)
                    weekdayShortFormatter = DateTimeFormatter.ofPattern("EEE", recheck)
                    fullHeaderFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", recheck)
                    timeFormatter = DateTimeFormatter.ofPattern("h:mm a", recheck)
                    longDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", recheck)
                    cachedFormattersLocale = recheck
                }
            }
        }
    }

    fun todayEpochDay(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zoneId).toEpochDay()

    fun epochDayFromMillis(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().toEpochDay()

    fun startOfTodayMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun endOfTodayMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L

    fun startOfWeekEpochDay(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zoneId).minusDays(6).toEpochDay()

    fun last7EpochDays(today: LocalDate = LocalDate.now()): List<LocalDate> =
        (6L downTo 0L).map { today.minusDays(it) }

    fun formatHeaderDate(date: LocalDate = LocalDate.now()): String {
        ensureCurrentLocaleFormatters()
        return date.format(fullHeaderFormatter)
    }

    fun formatShortWeekday(date: LocalDate): String {
        ensureCurrentLocaleFormatters()
        return date.format(weekdayShortFormatter)
    }

    fun formatDueDateLabel(epochDay: Long?, today: LocalDate = LocalDate.now()): String {
        if (epochDay == null) return "No due date"
        val date = LocalDate.ofEpochDay(epochDay)
        ensureCurrentLocaleFormatters()
        return when {
            date == today -> "Today"
            date == today.plusDays(1) -> "Tomorrow"
            date == today.minusDays(1) -> "Yesterday"
            date.year == today.year -> date.format(shortDateFormatter)
            else -> date.format(longDateFormatter)
        }
    }

    fun isOverdue(epochDay: Long?, isCompleted: Boolean, today: LocalDate = LocalDate.now()): Boolean {
        if (epochDay == null || isCompleted) return false
        return epochDay < today.toEpochDay()
    }

    fun formatTimerClock(remainingMillis: Long): String {
        val totalSeconds = ((remainingMillis + 999L) / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun formatDurationMinutes(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(0)
        val hours = safe / 60
        val mins = safe % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun formatDurationFromSeconds(totalSeconds: Int): String {
        val minutes = (totalSeconds.coerceAtLeast(0) + 30) / 60
        return if (totalSeconds in 1..59) "${totalSeconds}s" else formatDurationMinutes(minutes)
    }

    fun formatSessionTimestamp(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val instant = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val today = LocalDate.now(zoneId)
        ensureCurrentLocaleFormatters()
        val dateLabel = when (instant.toLocalDate()) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> instant.toLocalDate().format(shortDateFormatter)
        }
        return "$dateLabel • ${instant.format(timeFormatter)}"
    }

    fun greetingForHour(time: LocalTime = LocalTime.now()): String = when (time.hour) {
        in 4..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night focus"
    }
}
