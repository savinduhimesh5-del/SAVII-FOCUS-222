package com.example.domain

enum class TaskPriority(val level: Int, val label: String) {
    LOW(0, "Low"),
    MEDIUM(1, "Medium"),
    HIGH(2, "High");

    companion object {
        fun fromLevel(level: Int): TaskPriority =
            entries.firstOrNull { it.level == level } ?: MEDIUM
    }
}

enum class ThemeMode(val storageValue: String, val label: String) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    SYSTEM("system", "System");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: DARK
    }
}

enum class TimerStatus(val storageValue: String) {
    IDLE("idle"),
    RUNNING("running"),
    PAUSED("paused"),
    COMPLETED("completed");

    companion object {
        fun fromStorage(value: String?): TimerStatus =
            entries.firstOrNull { it.storageValue == value } ?: IDLE
    }
}

enum class PlannerFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed")
}

enum class StudyCategory(val storageValue: String, val displayName: String) {
    AL("AL", "A/L"),
    AAT("AAT", "AAT"),
    OTHER("OTHER", "OTHER");

    companion object {
        fun fromStorage(value: String?): StudyCategory =
            entries.firstOrNull { it.storageValue == value } ?: AL
    }
}

enum class AatLevel(val levelNumber: Int, val displayName: String, val shortBadge: String) {
    LEVEL_1(1, "Level 1", "L1"),
    LEVEL_2(2, "Level 2", "L2"),
    LEVEL_3(3, "Level 3", "L3");

    companion object {
        fun fromCode(code: Int): AatLevel? =
            entries.firstOrNull { it.levelNumber == code }
    }
}

enum class SubjectIconOption(val key: String, val label: String) {
    BOOK("book", "Book"),
    CALCULATE("calculate", "Math"),
    SCIENCE("science", "Science"),
    ACCOUNTING("accounting", "Finance"),
    COMPUTER("computer", "Computing"),
    LANGUAGE("language", "Language"),
    PSYCHOLOGY("psychology", "Theory"),
    ARCHITECTURE("architecture", "Design");

    companion object {
        fun fromKey(key: String?): SubjectIconOption =
            entries.firstOrNull { it.key == key } ?: BOOK
    }
}

object FocusPresets {
    val minutesOptions: List<Int> = listOf(15, 25, 45, 60)
    const val DEFAULT_MINUTES = 25
}
