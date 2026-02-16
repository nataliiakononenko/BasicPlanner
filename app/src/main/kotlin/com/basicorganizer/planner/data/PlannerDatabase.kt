package com.basicorganizer.planner.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PlannerDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "PlannerDB"

        private const val TABLE_EVENTS = "events"
        private const val TABLE_TODOS = "todos"

        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_NOTES = "notes"
        private const val KEY_DATE = "date"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_END_TIME = "end_time"
        private const val KEY_RECURRENCE_TYPE = "recurrence_type"
        private const val KEY_RECURRENCE_END_DATE = "recurrence_end_date"
        private const val KEY_CREATED_AT = "created_at"

        private const val KEY_WEEK_START_DATE = "week_start_date"
        private const val KEY_SCOPE = "scope"
        private const val KEY_MOVE_TO_NEXT = "move_to_next"
        private const val KEY_IS_COMPLETED = "is_completed"
        private const val KEY_COMPLETED_DATE = "completed_date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createEventsTable = """
            CREATE TABLE $TABLE_EVENTS (
                $KEY_ID INTEGER PRIMARY KEY,
                $KEY_TITLE TEXT,
                $KEY_NOTES TEXT,
                $KEY_DATE TEXT,
                $KEY_START_TIME TEXT,
                $KEY_END_TIME TEXT,
                $KEY_RECURRENCE_TYPE TEXT,
                $KEY_RECURRENCE_END_DATE TEXT,
                $KEY_CREATED_AT TEXT
            )
        """.trimIndent()
        db.execSQL(createEventsTable)

        val createTodosTable = """
            CREATE TABLE $TABLE_TODOS (
                $KEY_ID INTEGER PRIMARY KEY,
                $KEY_TITLE TEXT,
                $KEY_DATE TEXT,
                $KEY_WEEK_START_DATE TEXT,
                $KEY_SCOPE TEXT,
                $KEY_MOVE_TO_NEXT INTEGER,
                $KEY_IS_COMPLETED INTEGER,
                $KEY_COMPLETED_DATE TEXT,
                $KEY_CREATED_AT TEXT
            )
        """.trimIndent()
        db.execSQL(createTodosTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle future migrations
    }

    // Event methods
    fun addEvent(event: Event): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, event.title)
            put(KEY_NOTES, event.notes)
            put(KEY_DATE, event.date)
            put(KEY_START_TIME, event.startTime)
            put(KEY_END_TIME, event.endTime)
            put(KEY_RECURRENCE_TYPE, event.recurrenceType.name)
            put(KEY_RECURRENCE_END_DATE, event.recurrenceEndDate)
            put(KEY_CREATED_AT, event.createdAt)
        }
        val id = db.insert(TABLE_EVENTS, null, values)
        db.close()
        return id
    }

    fun getEventsForDate(date: String): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase
        
        // Get non-recurring events for this date
        val cursor = db.query(
            TABLE_EVENTS, null,
            "$KEY_DATE = ? AND $KEY_RECURRENCE_TYPE = ?",
            arrayOf(date, RecurrenceType.NONE.name),
            null, null, "$KEY_START_TIME ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                events.add(cursorToEvent(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()

        // Get recurring events that apply to this date
        val recurringCursor = db.query(
            TABLE_EVENTS, null,
            "$KEY_RECURRENCE_TYPE != ?",
            arrayOf(RecurrenceType.NONE.name),
            null, null, null
        )

        if (recurringCursor.moveToFirst()) {
            do {
                val event = cursorToEvent(recurringCursor)
                if (isRecurringEventOnDate(event, date)) {
                    events.add(event.copy(date = date))
                }
            } while (recurringCursor.moveToNext())
        }
        recurringCursor.close()

        return events.sortedBy { it.startTime }
    }

    private fun isRecurringEventOnDate(event: Event, date: String): Boolean {
        val eventDate = parseDate(event.date)
        val checkDate = parseDate(date)
        
        if (checkDate.before(eventDate)) return false
        
        event.recurrenceEndDate?.let {
            val endDate = parseDate(it)
            if (checkDate.after(endDate)) return false
        }

        return when (event.recurrenceType) {
            RecurrenceType.DAILY -> true
            RecurrenceType.WEEKLY -> {
                val eventCal = java.util.Calendar.getInstance().apply { time = eventDate }
                val checkCal = java.util.Calendar.getInstance().apply { time = checkDate }
                eventCal.get(java.util.Calendar.DAY_OF_WEEK) == checkCal.get(java.util.Calendar.DAY_OF_WEEK)
            }
            RecurrenceType.MONTHLY -> {
                val eventCal = java.util.Calendar.getInstance().apply { time = eventDate }
                val checkCal = java.util.Calendar.getInstance().apply { time = checkDate }
                eventCal.get(java.util.Calendar.DAY_OF_MONTH) == checkCal.get(java.util.Calendar.DAY_OF_MONTH)
            }
            RecurrenceType.YEARLY -> {
                val eventCal = java.util.Calendar.getInstance().apply { time = eventDate }
                val checkCal = java.util.Calendar.getInstance().apply { time = checkDate }
                eventCal.get(java.util.Calendar.DAY_OF_MONTH) == checkCal.get(java.util.Calendar.DAY_OF_MONTH) &&
                eventCal.get(java.util.Calendar.MONTH) == checkCal.get(java.util.Calendar.MONTH)
            }
            RecurrenceType.NONE -> false
        }
    }

    private fun parseDate(dateStr: String): java.util.Date {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateStr)!!
    }

    private fun cursorToEvent(cursor: android.database.Cursor): Event {
        return Event(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTES)) ?: "",
            date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
            startTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_START_TIME)),
            endTime = cursor.getString(cursor.getColumnIndexOrThrow(KEY_END_TIME)),
            recurrenceType = RecurrenceType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_RECURRENCE_TYPE))),
            recurrenceEndDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RECURRENCE_END_DATE)),
            createdAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT))
        )
    }

    fun updateEvent(event: Event): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, event.title)
            put(KEY_NOTES, event.notes)
            put(KEY_DATE, event.date)
            put(KEY_START_TIME, event.startTime)
            put(KEY_END_TIME, event.endTime)
            put(KEY_RECURRENCE_TYPE, event.recurrenceType.name)
            put(KEY_RECURRENCE_END_DATE, event.recurrenceEndDate)
        }
        val result = db.update(TABLE_EVENTS, values, "$KEY_ID = ?", arrayOf(event.id.toString()))
        db.close()
        return result
    }

    fun deleteEvent(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_EVENTS, "$KEY_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun hasEventsForDate(date: String): Boolean {
        return getEventsForDate(date).isNotEmpty()
    }

    // Todo methods
    fun addTodo(todo: TodoItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, todo.title)
            put(KEY_DATE, todo.date)
            put(KEY_WEEK_START_DATE, todo.weekStartDate)
            put(KEY_SCOPE, todo.scope.name)
            put(KEY_MOVE_TO_NEXT, if (todo.moveToNext) 1 else 0)
            put(KEY_IS_COMPLETED, if (todo.isCompleted) 1 else 0)
            put(KEY_COMPLETED_DATE, todo.completedDate)
            put(KEY_CREATED_AT, todo.createdAt)
        }
        val id = db.insert(TABLE_TODOS, null, values)
        db.close()
        return id
    }

    fun getTodosForDate(date: String): List<TodoItem> {
        val todos = mutableListOf<TodoItem>()
        val db = readableDatabase

        // Get day-scoped todos for this date (either created for this date or moved here)
        val cursor = db.query(
            TABLE_TODOS, null,
            "$KEY_SCOPE = ? AND (($KEY_DATE = ? AND $KEY_IS_COMPLETED = 0) OR ($KEY_DATE <= ? AND $KEY_MOVE_TO_NEXT = 1 AND $KEY_IS_COMPLETED = 0) OR ($KEY_COMPLETED_DATE = ?))",
            arrayOf(TodoScope.DAY.name, date, date, date),
            null, null, "$KEY_IS_COMPLETED ASC, $KEY_CREATED_AT ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                todos.add(cursorToTodo(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()

        return todos
    }

    fun getTodosForWeek(weekStartDate: String): List<TodoItem> {
        val todos = mutableListOf<TodoItem>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_TODOS, null,
            "$KEY_SCOPE = ? AND (($KEY_WEEK_START_DATE = ? AND $KEY_IS_COMPLETED = 0) OR ($KEY_WEEK_START_DATE <= ? AND $KEY_MOVE_TO_NEXT = 1 AND $KEY_IS_COMPLETED = 0) OR ($KEY_WEEK_START_DATE = ? AND $KEY_IS_COMPLETED = 1))",
            arrayOf(TodoScope.WEEK.name, weekStartDate, weekStartDate, weekStartDate),
            null, null, "$KEY_IS_COMPLETED ASC, $KEY_CREATED_AT ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                todos.add(cursorToTodo(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()

        return todos
    }

    private fun cursorToTodo(cursor: android.database.Cursor): TodoItem {
        return TodoItem(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
            date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
            weekStartDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WEEK_START_DATE)),
            scope = TodoScope.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SCOPE))),
            moveToNext = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MOVE_TO_NEXT)) == 1,
            isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_COMPLETED)) == 1,
            completedDate = cursor.getString(cursor.getColumnIndexOrThrow(KEY_COMPLETED_DATE)),
            createdAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT))
        )
    }

    fun updateTodo(todo: TodoItem): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, todo.title)
            put(KEY_DATE, todo.date)
            put(KEY_WEEK_START_DATE, todo.weekStartDate)
            put(KEY_SCOPE, todo.scope.name)
            put(KEY_MOVE_TO_NEXT, if (todo.moveToNext) 1 else 0)
            put(KEY_IS_COMPLETED, if (todo.isCompleted) 1 else 0)
            put(KEY_COMPLETED_DATE, todo.completedDate)
        }
        val result = db.update(TABLE_TODOS, values, "$KEY_ID = ?", arrayOf(todo.id.toString()))
        db.close()
        return result
    }

    fun deleteTodo(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_TODOS, "$KEY_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun hasTodosForDate(date: String): Boolean {
        return getTodosForDate(date).any { !it.isCompleted }
    }

    fun countTodosForDate(date: String): Int {
        return getTodosForDate(date).count { !it.isCompleted }
    }
}
