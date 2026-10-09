package com.refeast.app.util

import com.refeast.app.R

/** Picks the coloured background for a status label. */
object StatusColors {

    fun background(status: String): Int {
        return when (status) {
            "Reserved", "Fully Reserved" -> R.drawable.bg_chip_reserved
            "In-Transit" -> R.drawable.bg_chip_transit
            "Delivered", "Available", "Completed" -> R.drawable.bg_chip_delivered
            "Expired", "Withdrawn" -> R.drawable.bg_chip_neutral
            else -> R.drawable.bg_chip_cancelled
        }
    }
}
