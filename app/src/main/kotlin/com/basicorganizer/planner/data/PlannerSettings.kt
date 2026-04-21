package com.basicorganizer.planner.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * App settings persisted via SharedPreferences.
 * - Date format used for user-facing date display (DB storage remains ISO yyyy-MM-dd).
 * - First day of the week (Monday default, Sunday optional).
 */
class PlannerSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var dateFormatPattern: String
        get() = prefs.getString(KEY_DATE_FORMAT, DEFAULT_DATE_FORMAT) ?: DEFAULT_DATE_FORMAT
        set(value) = prefs.edit().putString(KEY_DATE_FORMAT, value).apply()

    /** One of Calendar.MONDAY or Calendar.SUNDAY. */
    var firstDayOfWeek: Int
        get() = prefs.getInt(KEY_FIRST_DAY, Calendar.MONDAY)
        set(value) = prefs.edit().putInt(KEY_FIRST_DAY, value).apply()

    /** Behavior for completed tasks in the Tasks view. */
    var completedTaskBehavior: CompletedTaskBehavior
        get() = CompletedTaskBehavior.valueOf(
            prefs.getString(KEY_COMPLETED_BEHAVIOR, CompletedTaskBehavior.MOVE_TO_BOTTOM.name)
                ?: CompletedTaskBehavior.MOVE_TO_BOTTOM.name
        )
        set(value) = prefs.edit().putString(KEY_COMPLETED_BEHAVIOR, value.name).apply()

    /** Format a date for display using the current pattern. */
    fun formatDisplay(date: Date): String =
        SimpleDateFormat(dateFormatPattern, Locale.getDefault()).format(date)

    /** Convert ISO yyyy-MM-dd (DB format) to display format. */
    fun isoToDisplay(iso: String): String {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(iso) ?: return iso
        return formatDisplay(parsed)
    }

    companion object {
        private const val PREFS_NAME = "planner_settings"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_FIRST_DAY = "first_day_of_week"
        private const val KEY_COMPLETED_BEHAVIOR = "completed_task_behavior"

        const val DEFAULT_DATE_FORMAT = "dd.MM.yyyy"

        val DATE_FORMAT_OPTIONS = listOf(
            "dd.MM.yyyy" to "31.12.2025 (European)",
            "dd/MM/yyyy" to "31/12/2025 (European, slash)",
            "MM/dd/yyyy" to "12/31/2025 (US)",
            "yyyy-MM-dd" to "2025-12-31 (ISO)"
        )
    }
}

/** Controls what happens to completed tasks in the Tasks view. */
enum class CompletedTaskBehavior {
    MOVE_TO_BOTTOM,
    DO_NOTHING,
    REMOVE
}
