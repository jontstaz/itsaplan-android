package com.example.ui.components

import java.util.Calendar
import java.util.Locale

/**
 * Strict parsers for the free-text fields the app sends to the server.
 *
 * The rule these encode: an input that cannot be understood is reported to the user, not
 * silently turned into null or a different value. `"abc"` in an estimate field must not
 * quietly become "no estimate".
 */

/**
 * @return the date, or null when [input] is not a real `YYYY-MM-DD` date.
 *
 * Uses Calendar rather than java.time so the app keeps its API 24 floor without
 * core-library desugaring. A length check alone would accept 2024-02-31; this rejects
 * dates the calendar rolls over.
 */
fun parseIsoDateOrNull(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.length != 10) return null
    if (trimmed[4] != '-' || trimmed[7] != '-') return null
    val year = trimmed.substring(0, 4).toIntOrNull() ?: return null
    val month = trimmed.substring(5, 7).toIntOrNull() ?: return null
    val day = trimmed.substring(8, 10).toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31 || year < 1970) return null

    val calendar = Calendar.getInstance(Locale.US).apply {
        isLenient = false
        clear()
        set(year, month - 1, day)
    }
    return try {
        // A non-lenient Calendar throws on 31 February instead of rolling to March.
        calendar.time
        String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** @return an error message when [input] is non-blank but not a real date. */
fun isoDateError(input: String): String? =
    if (input.isBlank() || parseIsoDateOrNull(input) != null) null
    else "Use a real date in YYYY-MM-DD form."

/** @return the number, or null when [input] is blank; [error] is set when unparseable. */
fun parseDecimalOrNull(input: String, error: (String) -> Unit): Double? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val value = trimmed.replace(',', '.').toDoubleOrNull()
    if (value == null || value < 0 || value.isNaN() || value.isInfinite()) {
        error("Enter a number.")
        return null
    }
    return value
}

/** @return the minutes, or null when [input] is blank; [error] is set when unparseable. */
fun parseMinutesOrNull(input: String, error: (String) -> Unit): Int? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val value = trimmed.toIntOrNull()
    if (value == null || value < 0) {
        error("Enter whole minutes.")
        return null
    }
    return value
}

/** @return the points value, validating a fractional estimate. */
fun parsePointsOrNull(input: String, error: (String) -> Unit): Double? =
    parseDecimalOrNull(input, error)

/** True when [input] is blank or a valid duration in minutes. */
fun isValidMinutes(input: String): Boolean =
    input.isBlank() || (input.trim().toIntOrNull()?.let { it >= 0 } == true)
