package com.refeast.app.util

import android.util.Patterns

/** Input checks shared by every form. Each returns an error message, or null when the value is fine. */
object Validators {

    fun required(value: String, field: String): String? =
        if (value.isBlank()) "$field is required" else null

    fun name(value: String): String? = when {
        value.isBlank() -> "Name is required"
        value.trim().length < 3 -> "Name must be at least 3 characters"
        else -> null
    }

    fun email(value: String): String? = when {
        value.isBlank() -> "Email is required"
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "Enter a valid email address"
        else -> null
    }

    fun password(value: String): String? = when {
        value.isEmpty() -> "Password is required"
        value.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }

    /** Malaysian phone numbers, e.g. 012-3456789, 0123456789, 03-55101234 or +6012 345 6789. */
    fun phone(value: String): String? {
        val digits = value.replace(" ", "").replace("-", "")
        return when {
            value.isBlank() -> "Phone number is required"
            !Regex("^(\\+?6?0)[1-9][0-9]{7,9}$").matches(digits) -> "Enter a valid Malaysian phone number"
            else -> null
        }
    }

    fun title(value: String): String? = when {
        value.isBlank() -> "Food title is required"
        value.trim().length < 3 -> "Title must be at least 3 characters"
        value.trim().length > 60 -> "Title must be 60 characters or less"
        else -> null
    }

    fun quantity(value: String, max: Int = 1000): String? {
        val number = value.trim().toIntOrNull()
        return when {
            value.isBlank() -> "Quantity is required"
            number == null -> "Enter a whole number"
            number < 1 -> "Quantity must be at least 1"
            number > max -> "Quantity cannot be more than $max"
            else -> null
        }
    }

    /** Pickup must be in the future and no later than the donor's deadline. */
    fun pickupTimeError(pickupTime: Long?, deadline: Long, now: Long = System.currentTimeMillis()): String? = when {
        pickupTime == null -> "Choose a pickup time"
        pickupTime <= now -> "Pickup time must be in the future"
        pickupTime > deadline -> "Pickup must be before the deadline (${TimeFormat.friendly(deadline)})"
        else -> null
    }
}
