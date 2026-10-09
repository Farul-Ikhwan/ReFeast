package com.refeast.app.util

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

/** Shows a date picker, then a time picker, and gives back the chosen time in milliseconds. */
object DateTimeHelper {

    fun pick(context: Context, startTime: Long, latestTime: Long?, onPicked: (Long) -> Unit) {
        val start = Calendar.getInstance()
        start.timeInMillis = startTime

        val dateDialog = DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                val chosen = Calendar.getInstance()
                chosen.set(year, month, day, hour, minute, 0)
                onPicked(chosen.timeInMillis)
            }, start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE), false).show()
        }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH))

        // Error prevention: past dates (and dates after the deadline) cannot be chosen.
        dateDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        if (latestTime != null && latestTime > System.currentTimeMillis()) {
            dateDialog.datePicker.maxDate = latestTime
        }
        dateDialog.show()
    }
}
