package com.refeast.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormat {

    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    /** "Today, 10:00 PM", "Tomorrow, 6:00 PM", "Yesterday, 8:45 PM" or "24 Sep, 8:45 PM". */
    fun friendly(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
        val today = LocalDate.now(zone)
        val day = when (dateTime.toLocalDate()) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> dateTime.format(dayMonth)
        }
        return "$day, ${dateTime.format(time)}"
    }

    /** "in 45 min", "in 2 h", or "passed". */
    fun timeLeft(millis: Long, now: Long = System.currentTimeMillis()): String {
        val minutes = (millis - now) / 60_000
        return when {
            minutes <= 0 -> "passed"
            minutes < 60 -> "in $minutes min"
            minutes < 48 * 60 -> "in ${minutes / 60} h"
            else -> "in ${minutes / (24 * 60)} days"
        }
    }
}
