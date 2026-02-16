package com.basicorganizer.planner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basicorganizer.planner.adapter.TodoAdapter
import com.basicorganizer.planner.data.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TodoAdapter.OnTodoInteractionListener {

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var tvDatePrimary: TextView
    private lateinit var tvDateSecondary: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnDayView: MaterialButton
    private lateinit var btnWeekView: MaterialButton
    private lateinit var btnMonthView: MaterialButton
    private lateinit var dayView: View
    private lateinit var weekView: View
    private lateinit var monthView: View
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var database: PlannerDatabase

    private var currentDate: Calendar = Calendar.getInstance()
    private var currentViewMode: ViewMode = ViewMode.WEEK
    private var firstDayOfWeek: Int = Calendar.MONDAY

    enum class ViewMode { DAY, WEEK, MONTH }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = PlannerDatabase(this)
        initializeViews()
        setupToolbar()
        setupViewSwitcher()
        setupNavigation()
        loadData()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tvDatePrimary = findViewById(R.id.tv_date_primary)
        tvDateSecondary = findViewById(R.id.tv_date_secondary)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        btnDayView = findViewById(R.id.btn_day_view)
        btnWeekView = findViewById(R.id.btn_week_view)
        btnMonthView = findViewById(R.id.btn_month_view)
        dayView = findViewById(R.id.day_view)
        weekView = findViewById(R.id.week_view)
        monthView = findViewById(R.id.month_view)
        fabAdd = findViewById(R.id.fab_add)

        fabAdd.setOnClickListener { showAddOptionsDialog() }
    }

    private fun setupToolbar() {
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupViewSwitcher() {
        btnDayView.setOnClickListener { switchToView(ViewMode.DAY) }
        btnWeekView.setOnClickListener { switchToView(ViewMode.WEEK) }
        btnMonthView.setOnClickListener { switchToView(ViewMode.MONTH) }
        updateViewButtons()
    }

    private fun setupNavigation() {
        btnPrev.setOnClickListener { navigatePrevious() }
        btnNext.setOnClickListener { navigateNext() }
    }

    private fun navigatePrevious() {
        when (currentViewMode) {
            ViewMode.DAY -> currentDate.add(Calendar.DAY_OF_MONTH, -1)
            ViewMode.WEEK -> currentDate.add(Calendar.WEEK_OF_YEAR, -1)
            ViewMode.MONTH -> currentDate.add(Calendar.MONTH, -1)
        }
        loadData()
    }

    private fun navigateNext() {
        when (currentViewMode) {
            ViewMode.DAY -> currentDate.add(Calendar.DAY_OF_MONTH, 1)
            ViewMode.WEEK -> currentDate.add(Calendar.WEEK_OF_YEAR, 1)
            ViewMode.MONTH -> currentDate.add(Calendar.MONTH, 1)
        }
        loadData()
    }

    private fun switchToView(mode: ViewMode) {
        currentViewMode = mode
        updateViewButtons()
        loadData()
    }

    private fun updateViewButtons() {
        val selectedColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val defaultColor = ContextCompat.getColor(this, R.color.text_secondary)

        btnDayView.setTextColor(if (currentViewMode == ViewMode.DAY) selectedColor else defaultColor)
        btnWeekView.setTextColor(if (currentViewMode == ViewMode.WEEK) selectedColor else defaultColor)
        btnMonthView.setTextColor(if (currentViewMode == ViewMode.MONTH) selectedColor else defaultColor)

        btnDayView.strokeColor = android.content.res.ColorStateList.valueOf(
            if (currentViewMode == ViewMode.DAY) selectedColor else defaultColor
        )
        btnWeekView.strokeColor = android.content.res.ColorStateList.valueOf(
            if (currentViewMode == ViewMode.WEEK) selectedColor else defaultColor
        )
        btnMonthView.strokeColor = android.content.res.ColorStateList.valueOf(
            if (currentViewMode == ViewMode.MONTH) selectedColor else defaultColor
        )

        dayView.visibility = if (currentViewMode == ViewMode.DAY) View.VISIBLE else View.GONE
        weekView.visibility = if (currentViewMode == ViewMode.WEEK) View.VISIBLE else View.GONE
        monthView.visibility = if (currentViewMode == ViewMode.MONTH) View.VISIBLE else View.GONE
    }

    private fun loadData() {
        updateDateDisplay()
        when (currentViewMode) {
            ViewMode.DAY -> loadDayView()
            ViewMode.WEEK -> loadWeekView()
            ViewMode.MONTH -> loadMonthView()
        }
    }

    private fun updateDateDisplay() {
        when (currentViewMode) {
            ViewMode.DAY -> {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                val dateFormat = SimpleDateFormat("d 'of' MMMM", Locale.getDefault())
                tvDatePrimary.text = dayFormat.format(currentDate.time)
                tvDateSecondary.text = dateFormat.format(currentDate.time)
            }
            ViewMode.WEEK -> {
                val weekNum = currentDate.get(Calendar.WEEK_OF_YEAR)
                val weekStart = getWeekStartDate(currentDate)
                val weekEnd = getWeekEndDate(currentDate)
                val dateFormat = SimpleDateFormat("d", Locale.getDefault())
                val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
                
                tvDatePrimary.text = getString(R.string.week_number, weekNum)
                tvDateSecondary.text = "${dateFormat.format(weekStart.time)} - ${dateFormat.format(weekEnd.time)} ${monthFormat.format(weekEnd.time)}"
            }
            ViewMode.MONTH -> {
                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                tvDatePrimary.text = monthFormat.format(currentDate.time)
                tvDateSecondary.text = ""
            }
        }
    }

    private fun getWeekStartDate(date: Calendar): Calendar {
        val cal = date.clone() as Calendar
        cal.firstDayOfWeek = firstDayOfWeek
        cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        return cal
    }

    private fun getWeekEndDate(date: Calendar): Calendar {
        val cal = getWeekStartDate(date)
        cal.add(Calendar.DAY_OF_MONTH, 6)
        return cal
    }

    private fun getDateString(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    private fun loadDayView() {
        val hoursContainer = dayView.findViewById<LinearLayout>(R.id.hours_container)
        val eventsOverlay = dayView.findViewById<FrameLayout>(R.id.events_overlay)
        val rvTodos = dayView.findViewById<RecyclerView>(R.id.rv_day_todos)
        val tvNoTodos = dayView.findViewById<TextView>(R.id.tv_no_todos)

        hoursContainer.removeAllViews()
        eventsOverlay.removeAllViews()

        // Create hour lines (6 AM to 11 PM)
        for (hour in 6..23) {
            val hourView = LayoutInflater.from(this).inflate(R.layout.item_hour_line, hoursContainer, false)
            val tvHour = hourView.findViewById<TextView>(R.id.tv_hour)
            tvHour.text = String.format("%02d:00", hour)
            hoursContainer.addView(hourView)
        }

        // Load events
        val dateStr = getDateString(currentDate)
        val events = database.getEventsForDate(dateStr)
        
        for (event in events) {
            addEventToOverlay(eventsOverlay, event, events)
        }

        // Scroll to 7 AM
        dayView.findViewById<ScrollView>(R.id.scroll_hours).post {
            dayView.findViewById<ScrollView>(R.id.scroll_hours).scrollTo(0, 60 * 1) // 1 hour from 6 AM
        }

        // Load todos
        val todos = database.getTodosForDate(dateStr)
        rvTodos.layoutManager = LinearLayoutManager(this)
        rvTodos.adapter = TodoAdapter(this, todos, this)
        
        tvNoTodos.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
        rvTodos.visibility = if (todos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun addEventToOverlay(overlay: FrameLayout, event: Event, allEvents: List<Event>) {
        val startParts = event.startTime.split(":")
        val endParts = event.endTime.split(":")
        
        val startHour = startParts[0].toIntOrNull() ?: 0
        val startMin = startParts.getOrNull(1)?.toIntOrNull() ?: 0
        val endHour = endParts[0].toIntOrNull() ?: startHour + 1
        val endMin = endParts.getOrNull(1)?.toIntOrNull() ?: 0

        val hourHeight = 60 // dp per hour
        val topMargin = ((startHour - 6) * hourHeight + (startMin * hourHeight / 60))
        val height = ((endHour - startHour) * hourHeight + ((endMin - startMin) * hourHeight / 60))

        // Check for overlapping events
        val overlapping = allEvents.filter { other ->
            other.id != event.id && eventsOverlap(event, other)
        }
        val overlapCount = overlapping.size + 1
        val overlapIndex = overlapping.count { it.id < event.id }

        val eventView = TextView(this)
        eventView.text = "${event.startTime} ${event.title}"
        eventView.setBackgroundResource(R.drawable.event_background)
        eventView.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary))
        eventView.setPadding(8, 4, 8, 4)
        eventView.textSize = 12f

        val params = FrameLayout.LayoutParams(
            if (overlapCount > 1) FrameLayout.LayoutParams.MATCH_PARENT / overlapCount else FrameLayout.LayoutParams.MATCH_PARENT,
            (height * resources.displayMetrics.density).toInt()
        )
        params.topMargin = (topMargin * resources.displayMetrics.density).toInt()
        if (overlapCount > 1) {
            params.marginStart = (overlapIndex * (resources.displayMetrics.widthPixels - 50) / overlapCount)
        }
        
        eventView.layoutParams = params
        eventView.setOnClickListener { showEventDialog(event) }
        overlay.addView(eventView)
    }

    private fun eventsOverlap(e1: Event, e2: Event): Boolean {
        val s1 = timeToMinutes(e1.startTime)
        val e1End = timeToMinutes(e1.endTime)
        val s2 = timeToMinutes(e2.startTime)
        val e2End = timeToMinutes(e2.endTime)
        return s1 < e2End && s2 < e1End
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    private fun loadWeekView() {
        val weekStart = getWeekStartDate(currentDate)
        val dayCards = listOf(
            weekView.findViewById<View>(R.id.day_monday),
            weekView.findViewById<View>(R.id.day_tuesday),
            weekView.findViewById<View>(R.id.day_wednesday),
            weekView.findViewById<View>(R.id.day_thursday),
            weekView.findViewById<View>(R.id.day_friday),
            weekView.findViewById<View>(R.id.day_saturday),
            weekView.findViewById<View>(R.id.day_sunday)
        )

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dateFormat = SimpleDateFormat("d", Locale.getDefault())

        for (i in 0..6) {
            val dayCal = weekStart.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i)
            val dateStr = getDateString(dayCal)

            val card = dayCards[i]
            card.findViewById<TextView>(R.id.tv_day_name).text = dayNames[i]
            card.findViewById<TextView>(R.id.tv_day_number).text = dateFormat.format(dayCal.time)

            // Show todo indicator
            val hasTodos = database.hasTodosForDate(dateStr)
            card.findViewById<View>(R.id.indicator_todos).visibility = if (hasTodos) View.VISIBLE else View.GONE

            // Load events
            val eventsContainer = card.findViewById<LinearLayout>(R.id.events_container)
            eventsContainer.removeAllViews()
            
            val events = database.getEventsForDate(dateStr)
            for (event in events.take(3)) {
                val eventView = LayoutInflater.from(this).inflate(R.layout.item_event_small, eventsContainer, false)
                eventView.findViewById<TextView>(R.id.tv_event).text = "${event.startTime} ${event.title}"
                eventView.setOnClickListener { 
                    currentDate.time = dayCal.time
                    showEventDialog(event)
                }
                eventsContainer.addView(eventView)
            }
            if (events.size > 3) {
                val moreView = TextView(this)
                moreView.text = "+${events.size - 3} more"
                moreView.textSize = 10f
                moreView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                eventsContainer.addView(moreView)
            }

            card.setOnClickListener {
                currentDate.time = dayCal.time
                switchToView(ViewMode.DAY)
            }
        }

        // Load week todos
        val weekStartStr = getDateString(weekStart)
        val weekTodos = database.getTodosForWeek(weekStartStr)
        val rvWeekTodos = weekView.findViewById<RecyclerView>(R.id.rv_week_todos)
        val tvNoWeekTodos = weekView.findViewById<TextView>(R.id.tv_no_week_todos)
        
        rvWeekTodos.layoutManager = LinearLayoutManager(this)
        rvWeekTodos.adapter = TodoAdapter(this, weekTodos, this)
        
        tvNoWeekTodos.visibility = if (weekTodos.isEmpty()) View.VISIBLE else View.GONE
        rvWeekTodos.visibility = if (weekTodos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadMonthView() {
        val monthGrid = monthView.findViewById<GridLayout>(R.id.month_grid)
        val weekdayHeaders = monthView.findViewById<LinearLayout>(R.id.weekday_headers)
        
        monthGrid.removeAllViews()
        weekdayHeaders.removeAllViews()

        // Add weekday headers
        val weekdays = if (firstDayOfWeek == Calendar.MONDAY) {
            listOf("M", "T", "W", "T", "F", "S", "S")
        } else {
            listOf("S", "M", "T", "W", "T", "F", "S")
        }
        
        for (day in weekdays) {
            val tv = TextView(this)
            tv.text = day
            tv.textSize = 12f
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            tv.gravity = android.view.Gravity.CENTER
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            tv.layoutParams = params
            weekdayHeaders.addView(tv)
        }

        val calendar = currentDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.firstDayOfWeek = firstDayOfWeek

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        var firstDayOffset = calendar.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek
        if (firstDayOffset < 0) firstDayOffset += 7

        // Add empty cells
        for (i in 0 until firstDayOffset) {
            val emptyView = View(this)
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = (56 * resources.displayMetrics.density).toInt()
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            emptyView.layoutParams = params
            monthGrid.addView(emptyView)
        }

        // Add day cells
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dateStr = getDateString(calendar)

            val dayView = LayoutInflater.from(this).inflate(R.layout.item_month_day, monthGrid, false)
            dayView.findViewById<TextView>(R.id.tv_day).text = day.toString()

            // Check for events
            val hasEvents = database.hasEventsForDate(dateStr)
            dayView.findViewById<View>(R.id.indicator_events).visibility = if (hasEvents) View.VISIBLE else View.GONE

            // Check for todos
            val todoCount = database.countTodosForDate(dateStr)
            val tvTodoCount = dayView.findViewById<TextView>(R.id.tv_todo_count)
            if (todoCount > 0) {
                tvTodoCount.text = todoCount.toString()
                tvTodoCount.visibility = View.VISIBLE
            } else {
                tvTodoCount.visibility = View.GONE
            }

            // Highlight if has events
            if (hasEvents) {
                dayView.setBackgroundColor(ContextCompat.getColor(this, R.color.day_has_events))
            }

            val dayCopy = calendar.clone() as Calendar
            dayView.setOnClickListener {
                currentDate.time = dayCopy.time
                switchToView(ViewMode.DAY)
            }

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            dayView.layoutParams = params
            monthGrid.addView(dayView)
        }
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf(getString(R.string.add_event), getString(R.string.add_todo))
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEventDialog(null)
                    1 -> showTodoDialog(null)
                }
            }
            .show()
    }

    private fun showEventDialog(event: Event?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etNotes = dialogView.findViewById<EditText>(R.id.et_notes)
        val tvStartTime = dialogView.findViewById<TextView>(R.id.tv_start_time)
        val tvEndTime = dialogView.findViewById<TextView>(R.id.tv_end_time)
        val spinnerRecurrence = dialogView.findViewById<Spinner>(R.id.spinner_recurrence)
        val recurrenceEndContainer = dialogView.findViewById<View>(R.id.recurrence_end_container)
        val rgRecurrenceEnd = dialogView.findViewById<RadioGroup>(R.id.rg_recurrence_end)
        val tvEndDate = dialogView.findViewById<TextView>(R.id.tv_end_date)

        var startTime = event?.startTime ?: "09:00"
        var endTime = event?.endTime ?: "10:00"
        var endDate = event?.recurrenceEndDate

        etTitle.setText(event?.title ?: "")
        etNotes.setText(event?.notes ?: "")
        tvStartTime.text = startTime
        tvEndTime.text = endTime

        val recurrenceOptions = listOf(
            getString(R.string.recurrence_none),
            getString(R.string.recurrence_daily),
            getString(R.string.recurrence_weekly),
            getString(R.string.recurrence_monthly),
            getString(R.string.recurrence_yearly)
        )
        spinnerRecurrence.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, recurrenceOptions)
        spinnerRecurrence.setSelection(event?.recurrenceType?.ordinal ?: 0)

        spinnerRecurrence.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                recurrenceEndContainer.visibility = if (position > 0) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        tvStartTime.setOnClickListener {
            showTimePicker(startTime) { time ->
                startTime = time
                tvStartTime.text = time
            }
        }

        tvEndTime.setOnClickListener {
            showTimePicker(endTime) { time ->
                endTime = time
                tvEndTime.text = time
            }
        }

        rgRecurrenceEnd.setOnCheckedChangeListener { _, checkedId ->
            tvEndDate.visibility = if (checkedId == R.id.rb_until) View.VISIBLE else View.GONE
        }

        tvEndDate.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                tvEndDate.text = date
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (event == null) R.string.add_event else R.string.edit_event)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    val recurrenceType = RecurrenceType.values()[spinnerRecurrence.selectedItemPosition]
                    val recurrenceEnd = if (recurrenceType != RecurrenceType.NONE && rgRecurrenceEnd.checkedRadioButtonId == R.id.rb_until) endDate else null

                    if (event == null) {
                        val newEvent = Event(
                            title = title,
                            notes = etNotes.text.toString(),
                            date = getDateString(currentDate),
                            startTime = startTime,
                            endTime = endTime,
                            recurrenceType = recurrenceType,
                            recurrenceEndDate = recurrenceEnd,
                            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        )
                        database.addEvent(newEvent)
                    } else {
                        event.title = title
                        event.notes = etNotes.text.toString()
                        event.startTime = startTime
                        event.endTime = endTime
                        event.recurrenceType = recurrenceType
                        event.recurrenceEndDate = recurrenceEnd
                        database.updateEvent(event)
                    }
                    loadData()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                if (event != null) {
                    setNeutralButton(R.string.delete) { _, _ ->
                        database.deleteEvent(event.id)
                        loadData()
                    }
                }
            }
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    private fun showTodoDialog(todo: TodoItem?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_todo, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_todo_title)
        val cbMoveToNext = dialogView.findViewById<CheckBox>(R.id.cb_move_to_next)

        etTitle.setText(todo?.title ?: "")
        cbMoveToNext.isChecked = todo?.moveToNext ?: true

        // Update checkbox text based on current view
        if (currentViewMode == ViewMode.WEEK) {
            cbMoveToNext.text = getString(R.string.todo_move_next_week)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (todo == null) R.string.add_todo else R.string.edit_todo)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    if (todo == null) {
                        val scope = if (currentViewMode == ViewMode.WEEK) TodoScope.WEEK else TodoScope.DAY
                        val newTodo = TodoItem(
                            title = title,
                            date = getDateString(currentDate),
                            weekStartDate = if (scope == TodoScope.WEEK) getDateString(getWeekStartDate(currentDate)) else null,
                            scope = scope,
                            moveToNext = cbMoveToNext.isChecked,
                            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        )
                        database.addTodo(newTodo)
                    } else {
                        todo.title = title
                        todo.moveToNext = cbMoveToNext.isChecked
                        database.updateTodo(todo)
                    }
                    loadData()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                if (todo != null) {
                    setNeutralButton(R.string.delete) { _, _ ->
                        database.deleteTodo(todo.id)
                        loadData()
                    }
                }
            }
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnShowListener {
            etTitle.requestFocus()
            etTitle.postDelayed({
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etTitle, InputMethodManager.SHOW_FORCED)
            }, 100)
        }
        dialog.show()
    }

    private fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val parts = currentTime.split(":")
        val hour = parts[0].toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(this, { _, h, m ->
            onTimeSelected(String.format("%02d:%02d", h, m))
        }, hour, minute, true).show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(this, { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day)
            onDateSelected(getDateString(cal))
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onTodoCheckedChanged(todo: TodoItem, isChecked: Boolean) {
        todo.isCompleted = isChecked
        todo.completedDate = if (isChecked) getDateString(currentDate) else null
        database.updateTodo(todo)
        loadData()
    }

    override fun onTodoLongClick(todo: TodoItem) {
        showTodoDialog(todo)
    }
}
