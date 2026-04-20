package com.basicorganizer.planner.data

enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

data class Event(
    var id: Long = 0,
    var title: String = "",
    var notes: String = "",
    var date: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var recurrenceType: RecurrenceType = RecurrenceType.NONE,
    var recurrenceEndDate: String? = null,
    /** Bitmask of days when recurrenceType == CUSTOM.
     *  Bit 0 = Sunday, 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday. */
    var customDaysMask: Int = 0,
    /** Interval in weeks for CUSTOM recurrence. 1 = every week, 2 = every other week. */
    var intervalWeeks: Int = 1,
    var createdAt: String = ""
)
