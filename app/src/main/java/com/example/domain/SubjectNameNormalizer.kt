package com.example.domain

object SubjectNameNormalizer {

    private val whitespaceRegex = Regex("\\s+")

    /**
     * Collapses repeated whitespace and trims leading/trailing spaces for display.
     */
    fun cleanDisplayName(raw: String): String =
        raw.trim().replace(whitespaceRegex, " ")

    /**
     * Produces a canonical lowercase key to prevent duplicates differing only by
     * casing or extra whitespace (e.g., "  Math  101 " == "math 101").
     */
    fun normalizeKey(raw: String): String =
        cleanDisplayName(raw).lowercase()
}
