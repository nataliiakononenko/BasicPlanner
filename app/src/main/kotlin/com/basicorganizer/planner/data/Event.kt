package com.basicorganizer.planner.data

enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
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
    var createdAt: String = ""
)
