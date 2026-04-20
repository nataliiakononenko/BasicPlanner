package com.basicorganizer.planner.data

enum class TodoScope {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

data class TodoItem(
    var id: Long = 0,
    var title: String = "",
    var date: String = "",
    var weekStartDate: String? = null,
    var scope: TodoScope = TodoScope.DAY,
    var moveToNext: Boolean = false,
    var isImportant: Boolean = false,
    var isCompleted: Boolean = false,
    var completedDate: String? = null,
    var createdAt: String = ""
)
