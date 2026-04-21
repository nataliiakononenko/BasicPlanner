package com.basicorganizer.planner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basicorganizer.planner.adapter.TodoAdapter
import com.basicorganizer.planner.data.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TodoAdapter.OnTodoInteractionListener {

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tvDateLabel: TextView
    private lateinit var tvDateValue: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var dayView: View
    private lateinit var weekView: View
    private lateinit var monthView: View
    private lateinit var yearView: View
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var database: PlannerDatabase
    private lateinit var settings: PlannerSettings

    private var currentDate: Calendar = Calendar.getInstance()
    private var currentViewMode: ViewMode = ViewMode.WEEK
    private var firstDayOfWeek: Int = Calendar.MONDAY

    enum class ViewMode { DAY, WEEK, MONTH, YEAR }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = PlannerDatabase(this)
        settings = PlannerSettings(this)
        firstDayOfWeek = settings.firstDayOfWeek
        initializeViews()
        setupToolbar()
        setupViewSwitcher()
        setupNavigation()
        loadData()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        tvDateLabel = findViewById(R.id.tv_date_label)
        tvDateValue = findViewById(R.id.tv_date_value)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        dayView = findViewById(R.id.day_view)
        weekView = findViewById(R.id.week_view)
        monthView = findViewById(R.id.month_view)
        yearView = findViewById(R.id.year_view)
        fabAdd = findViewById(R.id.fab_add)

        fabAdd.setOnClickListener { 
            when (currentViewMode) {
                ViewMode.DAY -> showTodoDialog(null)
                ViewMode.WEEK -> showTodoDialog(null)
                ViewMode.MONTH -> showMonthTodoDialog()
                ViewMode.YEAR -> showYearTodoDialog()
            }
        }
    }

    private fun setupToolbar() {
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupViewSwitcher() {
        val navView = findViewById<View>(R.id.nav_view)
        navView.findViewById<View>(R.id.nav_day_view).setOnClickListener {
            switchToView(ViewMode.DAY)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        navView.findViewById<View>(R.id.nav_week_view).setOnClickListener {
            switchToView(ViewMode.WEEK)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        navView.findViewById<View>(R.id.nav_month_view).setOnClickListener {
            switchToView(ViewMode.MONTH)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        navView.findViewById<View>(R.id.nav_year_view).setOnClickListener {
            switchToView(ViewMode.YEAR)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        navView.findViewById<View>(R.id.nav_settings).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showSettingsDialog()
        }
        updateViewButtons()
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val rgDateFormat = dialogView.findViewById<RadioGroup>(R.id.rg_date_format)
        val rgFirstDay = dialogView.findViewById<RadioGroup>(R.id.rg_first_day)
        val rbMonday = dialogView.findViewById<RadioButton>(R.id.rb_monday)
        val rbSunday = dialogView.findViewById<RadioButton>(R.id.rb_sunday)

        // Populate date format options dynamically
        val currentPattern = settings.dateFormatPattern
        val formatRadioIds = mutableListOf<Pair<Int, String>>()
        for ((pattern, label) in PlannerSettings.DATE_FORMAT_OPTIONS) {
            val rb = RadioButton(this)
            rb.text = label
            rb.id = View.generateViewId()
            rgDateFormat.addView(rb)
            formatRadioIds.add(rb.id to pattern)
            if (pattern == currentPattern) rb.isChecked = true
        }

        // First day of week
        if (settings.firstDayOfWeek == Calendar.SUNDAY) rbSunday.isChecked = true else rbMonday.isChecked = true

        AlertDialog.Builder(this)
            .setTitle(R.string.settings)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val checkedId = rgDateFormat.checkedRadioButtonId
                val newPattern = formatRadioIds.firstOrNull { it.first == checkedId }?.second
                if (newPattern != null) settings.dateFormatPattern = newPattern
                val newFirstDay = if (rgFirstDay.checkedRadioButtonId == R.id.rb_sunday) Calendar.SUNDAY else Calendar.MONDAY
                settings.firstDayOfWeek = newFirstDay
                firstDayOfWeek = newFirstDay
                loadData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupNavigation() {
        btnPrev.setOnClickListener { navigatePrevious() }
        btnNext.setOnClickListener { navigateNext() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Hide zoom out button when in year view (already at top level)
        menu.findItem(R.id.action_zoom_out)?.isVisible = currentViewMode != ViewMode.YEAR
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_zoom_out -> {
                // Navigate: day → week → month → year
                when (currentViewMode) {
                    ViewMode.DAY -> switchToView(ViewMode.WEEK)
                    ViewMode.WEEK -> switchToView(ViewMode.MONTH)
                    ViewMode.MONTH -> switchToView(ViewMode.YEAR)
                    ViewMode.YEAR -> {} // Already at top level, do nothing
                }
                true
            }
            R.id.action_today -> {
                goToToday()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun goToToday() {
        currentDate = Calendar.getInstance()
        loadData()
    }

    private fun navigatePrevious() {
        when (currentViewMode) {
            ViewMode.DAY -> currentDate.add(Calendar.DAY_OF_MONTH, -1)
            ViewMode.WEEK -> currentDate.add(Calendar.WEEK_OF_YEAR, -1)
            ViewMode.MONTH -> currentDate.add(Calendar.MONTH, -1)
            ViewMode.YEAR -> currentDate.add(Calendar.YEAR, -1)
        }
        loadData()
    }

    private fun navigateNext() {
        when (currentViewMode) {
            ViewMode.DAY -> currentDate.add(Calendar.DAY_OF_MONTH, 1)
            ViewMode.WEEK -> currentDate.add(Calendar.WEEK_OF_YEAR, 1)
            ViewMode.MONTH -> currentDate.add(Calendar.MONTH, 1)
            ViewMode.YEAR -> currentDate.add(Calendar.YEAR, 1)
        }
        loadData()
    }

    private fun switchToView(mode: ViewMode) {
        currentViewMode = mode
        updateViewButtons()
        invalidateOptionsMenu() // Refresh toolbar menu to update zoom out button visibility
        loadData()
    }

    private fun updateViewButtons() {
        dayView.visibility = if (currentViewMode == ViewMode.DAY) View.VISIBLE else View.GONE
        weekView.visibility = if (currentViewMode == ViewMode.WEEK) View.VISIBLE else View.GONE
        monthView.visibility = if (currentViewMode == ViewMode.MONTH) View.VISIBLE else View.GONE
        yearView.visibility = if (currentViewMode == ViewMode.YEAR) View.VISIBLE else View.GONE
    }

    private fun loadData() {
        updateDateLabel()
        when (currentViewMode) {
            ViewMode.DAY -> loadDayView()
            ViewMode.WEEK -> loadWeekView()
            ViewMode.MONTH -> loadMonthView()
            ViewMode.YEAR -> loadYearView()
        }
    }

    private fun updateDateLabel() {
        when (currentViewMode) {
            ViewMode.DAY -> {
                tvDateLabel.text = SimpleDateFormat("EEEE", Locale.getDefault()).format(currentDate.time)
                tvDateValue.text = SimpleDateFormat("d MMM", Locale.getDefault()).format(currentDate.time)
            }
            ViewMode.WEEK -> {
                val weekStart = getWeekStartDate(currentDate)
                val weekEnd = weekStart.clone() as Calendar
                weekEnd.add(Calendar.DAY_OF_MONTH, 6)
                val weekNumber = weekStart.get(Calendar.WEEK_OF_YEAR)
                tvDateLabel.text = "Week $weekNumber"
                tvDateValue.text = SimpleDateFormat("d MMM", Locale.getDefault()).format(weekStart.time) + " - " +
                        SimpleDateFormat("d MMM", Locale.getDefault()).format(weekEnd.time)
            }
            ViewMode.MONTH -> {
                tvDateLabel.text = SimpleDateFormat("MMMM", Locale.getDefault()).format(currentDate.time)
                tvDateValue.text = currentDate.get(Calendar.YEAR).toString()
            }
            ViewMode.YEAR -> {
                tvDateLabel.text = currentDate.get(Calendar.YEAR).toString()
                tvDateValue.text = ""
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

        // Create hour lines (00:00 to 23:00)
        for (hour in 0..23) {
            val hourView = LayoutInflater.from(this).inflate(R.layout.item_hour_line, hoursContainer, false)
            val tvHour = hourView.findViewById<TextView>(R.id.tv_hour)
            tvHour.text = String.format("%02d:00", hour)
            
            // Add long-click to create event at this hour
            hourView.setOnLongClickListener {
                val startTime = String.format("%02d:00", hour)
                val endTime = String.format("%02d:00", if (hour < 23) hour + 1 else 23)
                showEventDialog(null, startTime, endTime)
                true
            }
            
            hoursContainer.addView(hourView)
        }

        // Load events
        val dateStr = getDateString(currentDate)
        val events = database.getEventsForDate(dateStr)
        
        layoutEventsOnOverlay(eventsOverlay, hoursContainer, events)

        // Add current time indicator if viewing today
        val today = Calendar.getInstance()
        val isToday = currentDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                      currentDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        
        if (isToday) {
            val currentHour = today.get(Calendar.HOUR_OF_DAY)
            val currentMinute = today.get(Calendar.MINUTE)
            
            // Show current time indicator for any hour
            eventsOverlay.post {
                val d = resources.displayMetrics.density
                
                // Measure actual hour line positions
                val hourBlock = hoursContainer.getChildAt(currentHour)
                val nextHourBlock = if (currentHour < 23) hoursContainer.getChildAt(currentHour + 1) else null
                if (hourBlock != null) {
                    val greyLine = (hourBlock as android.view.ViewGroup).getChildAt(1)
                    val loc = IntArray(2)
                    greyLine.getLocationInWindow(loc)
                    val overlayLoc = IntArray(2)
                    eventsOverlay.getLocationInWindow(overlayLoc)
                    val baseY = loc[1] - overlayLoc[1]
                    
                    val topPx = if (nextHourBlock != null) {
                        val nextLine = (nextHourBlock as android.view.ViewGroup).getChildAt(1)
                        val nextLoc = IntArray(2)
                        nextLine.getLocationInWindow(nextLoc)
                        val nextY = nextLoc[1] - overlayLoc[1]
                        val hourHeightPx = nextY - baseY
                        baseY + (currentMinute * hourHeightPx / 60)
                    } else {
                        baseY
                    }
                    
                    val currentTimeLine = View(this)
                    currentTimeLine.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    
                    val params = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        (2 * d).toInt() // 2dp height
                    )
                    params.topMargin = topPx
                    currentTimeLine.layoutParams = params
                    eventsOverlay.addView(currentTimeLine)
                }
            }
        }

        // Scroll to 8 AM (8 hours from midnight at 40dp per hour)
        dayView.findViewById<ScrollView>(R.id.scroll_hours).post {
            val scrollPosition = (8 * 40 * resources.displayMetrics.density).toInt()
            dayView.findViewById<ScrollView>(R.id.scroll_hours).scrollTo(0, scrollPosition)
        }

        // Load todos
        val todos = database.getTodosForDate(dateStr)
        rvTodos.layoutManager = LinearLayoutManager(this)
        rvTodos.adapter = TodoAdapter(this, todos, this)
        
        tvNoTodos.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
        rvTodos.visibility = if (todos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun layoutEventsOnOverlay(overlay: FrameLayout, hoursContainer: LinearLayout, events: List<Event>) {
        if (events.isEmpty()) return

        val density = resources.displayMetrics.density

        // Sort events by start time, then by duration (longer first)
        val sorted = events.sortedWith(compareBy<Event> { timeToMinutes(it.startTime) }
            .thenByDescending { timeToMinutes(it.endTime) - timeToMinutes(it.startTime) })

        // Assign columns using greedy algorithm (like Outlook)
        // Each event gets a column index, and we track the max columns in each overlap group
        data class EventLayout(
            val event: Event,
            var column: Int = 0,
            var totalColumns: Int = 1
        )

        val layouts = sorted.map { EventLayout(it) }

        // Assign columns: for each event, find the first available column
        // that doesn't conflict with already-placed overlapping events
        for (i in layouts.indices) {
            val current = layouts[i]
            val occupiedColumns = mutableSetOf<Int>()

            // Check all previously placed events that overlap with this one
            for (j in 0 until i) {
                val other = layouts[j]
                if (eventsOverlap(current.event, other.event)) {
                    occupiedColumns.add(other.column)
                }
            }

            // Assign first available column
            var col = 0
            while (col in occupiedColumns) col++
            current.column = col
        }

        // Now determine total columns for each overlap group
        // Build overlap groups: events that are transitively connected through overlaps
        val visited = BooleanArray(layouts.size)
        val groups = mutableListOf<MutableList<Int>>()

        for (i in layouts.indices) {
            if (visited[i]) continue
            val group = mutableListOf<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(i)
            visited[i] = true
            while (queue.isNotEmpty()) {
                val idx = queue.removeFirst()
                group.add(idx)
                for (j in layouts.indices) {
                    if (!visited[j] && eventsOverlap(layouts[idx].event, layouts[j].event)) {
                        visited[j] = true
                        queue.add(j)
                    }
                }
            }
            groups.add(group)
        }

        // For each group, set totalColumns to the max column + 1 in that group
        for (group in groups) {
            val maxCol = group.maxOf { layouts[it].column }
            val totalCols = maxCol + 1
            for (idx in group) {
                layouts[idx].totalColumns = totalCols
            }
        }

        // Create views after layout so we can measure actual positions
        overlay.post {
            val overlayWidth = overlay.width

            // Measure the actual pixel position of each hour's grey line
            // by getting the Y of the grey line View relative to the overlay's parent
            val overlayTop = overlay.top
            val hourLinePositions = IntArray(24)
            for (h in 0..23) {
                val hourBlock = hoursContainer.getChildAt(h)
                if (hourBlock != null) {
                    // The grey line is the second child (index 1) inside the hour block LinearLayout
                    val greyLine = (hourBlock as android.view.ViewGroup).getChildAt(1)
                    // Get absolute position of grey line on screen
                    val loc = IntArray(2)
                    greyLine.getLocationInWindow(loc)
                    val overlayLoc = IntArray(2)
                    overlay.getLocationInWindow(overlayLoc)
                    hourLinePositions[h] = loc[1] - overlayLoc[1]
                }
            }

            // Helper to get pixel Y for a given time
            fun timeToPixelY(hour: Int, min: Int): Int {
                if (hour >= 23) return hourLinePositions[23]
                val baseY = hourLinePositions[hour]
                val nextY = hourLinePositions[minOf(hour + 1, 23)]
                val hourHeightPx = nextY - baseY
                return baseY + (min * hourHeightPx / 60)
            }

            for (layout in layouts) {
                val event = layout.event
                val startParts = event.startTime.split(":")
                val endParts = event.endTime.split(":")
                val startHour = startParts[0].toIntOrNull() ?: 0
                val startMin = startParts.getOrNull(1)?.toIntOrNull() ?: 0
                val endHour = endParts[0].toIntOrNull() ?: startHour + 1
                val endMin = endParts.getOrNull(1)?.toIntOrNull() ?: 0

                val topPx = timeToPixelY(startHour, startMin)
                val bottomPx = timeToPixelY(endHour, endMin)
                val heightPx = bottomPx - topPx

                val columnWidth = overlayWidth / layout.totalColumns

                val eventView = TextView(this)
                eventView.text = event.title
                eventView.setBackgroundResource(R.drawable.event_background)
                eventView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                eventView.setPadding(
                    (8 * density).toInt(), (2 * density).toInt(),
                    (4 * density).toInt(), (2 * density).toInt()
                )
                eventView.textSize = 12f

                val params = FrameLayout.LayoutParams(
                    columnWidth,
                    heightPx
                )
                params.topMargin = topPx
                params.marginStart = layout.column * columnWidth

                eventView.layoutParams = params
                eventView.setOnClickListener { showEventDialog(event) }
                overlay.addView(eventView)
            }
        }
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

    private fun layoutWeekEventsOnOverlay(overlay: FrameLayout, events: List<Event>, dayCal: Calendar) {
        if (events.isEmpty()) return

        val density = resources.displayMetrics.density
        val startOfDay = 8 * 60  // 8:00 in minutes
        val endOfDay = 20 * 60   // 20:00 in minutes
        val totalMinutes = endOfDay - startOfDay // 720 minutes

        // Sort events by start time, then by duration (longer first)
        val sorted = events.sortedWith(compareBy<Event> { timeToMinutes(it.startTime) }
            .thenByDescending { timeToMinutes(it.endTime) - timeToMinutes(it.startTime) })

        // Assign columns using greedy algorithm (same as day view)
        data class WeekEventLayout(
            val event: Event,
            var column: Int = 0,
            var totalColumns: Int = 1
        )

        val layouts = sorted.map { WeekEventLayout(it) }

        for (i in layouts.indices) {
            val current = layouts[i]
            val occupiedColumns = mutableSetOf<Int>()
            for (j in 0 until i) {
                val other = layouts[j]
                if (eventsOverlap(current.event, other.event)) {
                    occupiedColumns.add(other.column)
                }
            }
            var col = 0
            while (col in occupiedColumns) col++
            current.column = col
        }

        // Build overlap groups
        val visited = BooleanArray(layouts.size)
        val groups = mutableListOf<MutableList<Int>>()
        for (i in layouts.indices) {
            if (visited[i]) continue
            val group = mutableListOf<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(i)
            visited[i] = true
            while (queue.isNotEmpty()) {
                val idx = queue.removeFirst()
                group.add(idx)
                for (j in layouts.indices) {
                    if (!visited[j] && eventsOverlap(layouts[idx].event, layouts[j].event)) {
                        visited[j] = true
                        queue.add(j)
                    }
                }
            }
            groups.add(group)
        }

        for (group in groups) {
            val maxCol = group.maxOf { layouts[it].column }
            val totalCols = maxCol + 1
            for (idx in group) {
                layouts[idx].totalColumns = totalCols
            }
        }

        // Create views after layout to get actual overlay dimensions
        overlay.post {
            val overlayHeight = overlay.height
            val overlayWidth = overlay.width

            for (layout in layouts) {
                val event = layout.event
                val eventStartMin = timeToMinutes(event.startTime).coerceIn(startOfDay, endOfDay)
                val eventEndMin = timeToMinutes(event.endTime).coerceIn(startOfDay, endOfDay)

                val topFraction = (eventStartMin - startOfDay).toFloat() / totalMinutes
                val bottomFraction = (eventEndMin - startOfDay).toFloat() / totalMinutes
                val topPx = (topFraction * overlayHeight).toInt()
                val heightPx = ((bottomFraction - topFraction) * overlayHeight).toInt()

                val columnWidth = overlayWidth / layout.totalColumns

                val eventView = TextView(this)
                eventView.text = event.title
                eventView.setBackgroundResource(R.drawable.event_background)
                eventView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                eventView.setPadding(
                    (4 * density).toInt(), (2 * density).toInt(),
                    (2 * density).toInt(), (2 * density).toInt()
                )
                eventView.textSize = 9f
                eventView.gravity = android.view.Gravity.TOP
                eventView.maxLines = 3
                eventView.ellipsize = android.text.TextUtils.TruncateAt.END

                val params = FrameLayout.LayoutParams(
                    columnWidth,
                    heightPx.coerceAtLeast((8 * density).toInt())
                )
                params.topMargin = topPx
                params.marginStart = layout.column * columnWidth

                eventView.layoutParams = params
                eventView.setOnClickListener {
                    currentDate.time = dayCal.time
                    showEventDialog(event)
                }
                overlay.addView(eventView)
            }
        }
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
        val today = Calendar.getInstance()

        for (i in 0..6) {
            val dayCal = weekStart.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i)
            val dateStr = getDateString(dayCal)

            val card = dayCards[i]
            val tvDayName = card.findViewById<TextView>(R.id.tv_day_name)
            val tvDayNumber = card.findViewById<TextView>(R.id.tv_day_number)
            tvDayName.text = dayNames[i]
            tvDayNumber.text = dateFormat.format(dayCal.time)

            // Highlight Sunday in primary color (no bold).
            val isSunday = dayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            val dayColor = ContextCompat.getColor(this, if (isSunday) R.color.colorPrimary else R.color.text_primary)
            tvDayName.setTextColor(dayColor)
            tvDayNumber.setTextColor(dayColor)

            // Add border to current day
            val isToday = dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                          dayCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            if (isToday) {
                card.setBackgroundResource(R.drawable.border_current_day)
            } else {
                card.setBackgroundResource(0) // Clear background
            }

            // Show todo indicator
            val hasTodos = database.hasTodosForDate(dateStr)
            card.findViewById<View>(R.id.indicator_todos).visibility = if (hasTodos) View.VISIBLE else View.GONE

            // Load events proportionally into the events_overlay
            val events = database.getEventsForDate(dateStr)
            val isWeekend = i >= 5 // Saturday (5) and Sunday (6)
            
            val eventsOverlay = card.findViewById<FrameLayout>(R.id.events_overlay)
            eventsOverlay.removeAllViews()
            
            // Add events with proportional positioning
            layoutWeekEventsOnOverlay(eventsOverlay, events, dayCal)
            
            // Add long-click listeners to time chunks
            if (isWeekend) {
                card.findViewById<View>(R.id.chunk_8_14)?.setOnLongClickListener {
                    currentDate.time = dayCal.time
                    showEventDialog(null, "08:00", "14:00")
                    true
                }
                card.findViewById<View>(R.id.chunk_14_20)?.setOnLongClickListener {
                    currentDate.time = dayCal.time
                    showEventDialog(null, "14:00", "20:00")
                    true
                }
            } else {
                card.findViewById<View>(R.id.chunk_8_12)?.setOnLongClickListener {
                    currentDate.time = dayCal.time
                    showEventDialog(null, "08:00", "12:00")
                    true
                }
                card.findViewById<View>(R.id.chunk_12_16)?.setOnLongClickListener {
                    currentDate.time = dayCal.time
                    showEventDialog(null, "12:00", "16:00")
                    true
                }
                card.findViewById<View>(R.id.chunk_16_20)?.setOnLongClickListener {
                    currentDate.time = dayCal.time
                    showEventDialog(null, "16:00", "20:00")
                    true
                }
            }
            
            // Add current time indicator if this is today
            val timeIndicatorOverlay = card.findViewById<FrameLayout>(R.id.time_indicator_overlay)
            timeIndicatorOverlay.removeAllViews()
            
            if (isToday) {
                val currentHour = today.get(Calendar.HOUR_OF_DAY)
                val currentMinute = today.get(Calendar.MINUTE)
                
                // Show time indicator (8 AM to 8 PM range for time chunks)
                if (currentHour in 8..19) {
                    val totalMinutesFromStart = (currentHour - 8) * 60 + currentMinute
                    val totalDisplayMinutes = (20 - 8) * 60 // 8 AM to 8 PM
                    
                    card.post {
                        // Measure actual positions of hour 8 and hour 20 label rows
                        val hour8Row = card.findViewById<View>(R.id.hour_line_8)
                        val hour20Row = card.findViewById<View>(R.id.hour_line_20)
                        if (hour8Row == null || hour20Row == null) return@post
                        
                        // The hour label line is centered vertically within each row
                        val hour8Y = hour8Row.top + hour8Row.height / 2
                        val hour20Y = hour20Row.top + hour20Row.height / 2
                        val usableSpan = hour20Y - hour8Y
                        
                        val topPosition = hour8Y + (totalMinutesFromStart.toFloat() / totalDisplayMinutes.toFloat() * usableSpan).toInt()
                        
                        val currentTimeLine = View(this)
                        currentTimeLine.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        
                        val params = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            (2 * resources.displayMetrics.density).toInt() // 2dp height
                        )
                        params.topMargin = topPosition - (resources.displayMetrics.density).toInt() // center line on position
                        currentTimeLine.layoutParams = params
                        
                        timeIndicatorOverlay.addView(currentTimeLine)
                    }
                }
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
        val monthContainer = monthView.findViewById<LinearLayout>(R.id.month_container)
        val weekdayHeaders = monthView.findViewById<LinearLayout>(R.id.weekday_headers)
        val rvMonthTodos = monthView.findViewById<RecyclerView>(R.id.rv_month_todos)
        val tvNoMonthTodos = monthView.findViewById<TextView>(R.id.tv_no_month_todos)
        
        monthContainer.removeAllViews()
        weekdayHeaders.removeAllViews()

        // Add weekday headers
        val weekdays = if (firstDayOfWeek == Calendar.MONDAY) {
            listOf("M", "T", "W", "T", "F", "S", "S")
        } else {
            listOf("S", "M", "T", "W", "T", "F", "S")
        }
        
        for (i in weekdays.indices) {
            val tv = TextView(this)
            tv.text = weekdays[i]
            tv.textSize = 12f
            // Make Sunday header primary color
            val isSunday = if (firstDayOfWeek == Calendar.MONDAY) i == 6 else i == 0
            tv.setTextColor(ContextCompat.getColor(this, if (isSunday) R.color.colorPrimary else R.color.text_secondary))
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

        val today = Calendar.getInstance()
        
        // Build calendar week by week
        var currentDay = 1 - firstDayOffset
        while (currentDay <= daysInMonth) {
            // Create week row
            val weekRow = LinearLayout(this)
            weekRow.orientation = LinearLayout.HORIZONTAL
            weekRow.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // Add week number
            calendar.set(Calendar.DAY_OF_MONTH, maxOf(1, currentDay))
            val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
            val weekNumView = TextView(this)
            weekNumView.text = weekNumber.toString()
            weekNumView.textSize = 10f
            weekNumView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            weekNumView.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.CENTER_HORIZONTAL
            val weekNumParams = LinearLayout.LayoutParams(
                (24 * resources.displayMetrics.density).toInt(),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            weekNumView.layoutParams = weekNumParams
            
            // Click week number to navigate to that week
            val weekCal = calendar.clone() as Calendar
            weekNumView.setOnClickListener {
                currentDate.time = weekCal.time
                switchToView(ViewMode.WEEK)
            }
            weekRow.addView(weekNumView)
            
            // Add 7 day cells for this week
            for (dayOfWeek in 0..6) {
                if (currentDay < 1 || currentDay > daysInMonth) {
                    // Empty cell
                    val emptyView = View(this)
                    val params = LinearLayout.LayoutParams(0, (56 * resources.displayMetrics.density).toInt(), 1f)
                    emptyView.layoutParams = params
                    weekRow.addView(emptyView)
                } else {
                    // Day cell
                    calendar.set(Calendar.DAY_OF_MONTH, currentDay)
                    val dateStr = getDateString(calendar)

                    val dayView = LayoutInflater.from(this).inflate(R.layout.item_month_day, weekRow, false)
                    val tvDay = dayView.findViewById<TextView>(R.id.tv_day)
                    tvDay.text = currentDay.toString()

                    // Highlight Sundays in primary color (no bold).
                    val isSunday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    tvDay.setTextColor(
                        ContextCompat.getColor(this, if (isSunday) R.color.colorPrimary else R.color.text_primary)
                    )

                    // Check for todos and show indicator
                    val hasTodos = database.hasTodosForDate(dateStr)
                    dayView.findViewById<View>(R.id.indicator_todos).visibility = if (hasTodos) View.VISIBLE else View.INVISIBLE

                    // Add border to current day
                    val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                  calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                    if (isToday) {
                        dayView.setBackgroundResource(R.drawable.border_current_day)
                    }

                    val dayCopy = calendar.clone() as Calendar
                    dayView.setOnClickListener {
                        currentDate.time = dayCopy.time
                        switchToView(ViewMode.DAY)
                    }

                    val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    dayView.layoutParams = params
                    weekRow.addView(dayView)
                }
                currentDay++
            }
            
            monthContainer.addView(weekRow)
        }
        
        // Load monthly goals
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentDate.time)
        val monthTodos = database.getTodosForMonth(monthYear)
        rvMonthTodos.layoutManager = LinearLayoutManager(this)
        rvMonthTodos.adapter = TodoAdapter(this, monthTodos, this)
        
        tvNoMonthTodos.visibility = if (monthTodos.isEmpty()) View.VISIBLE else View.GONE
        rvMonthTodos.visibility = if (monthTodos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadYearView() {
        val yearGrid = yearView.findViewById<GridLayout>(R.id.year_grid)
        val rvYearTodos = yearView.findViewById<RecyclerView>(R.id.rv_year_todos)
        val tvNoYearTodos = yearView.findViewById<TextView>(R.id.tv_no_year_todos)
        
        yearGrid.removeAllViews()

        val year = currentDate.get(Calendar.YEAR)
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val today = Calendar.getInstance()

        for (month in 0..11) {
            val monthCal = Calendar.getInstance()
            monthCal.set(Calendar.YEAR, year)
            monthCal.set(Calendar.MONTH, month)
            monthCal.set(Calendar.DAY_OF_MONTH, 1)

            val monthView = LayoutInflater.from(this).inflate(R.layout.item_year_month, yearGrid, false)
            val tvMonthName = monthView.findViewById<TextView>(R.id.tv_month_name)
            tvMonthName.text = monthNames[month]
            
            // Click month name to navigate to that month
            val monthCopy = monthCal.clone() as Calendar
            tvMonthName.setOnClickListener {
                currentDate.time = monthCopy.time
                switchToView(ViewMode.MONTH)
            }

            // Add day names header
            val dayNamesHeader = monthView.findViewById<LinearLayout>(R.id.day_names_header)
            dayNamesHeader.removeAllViews()
            
            // Add empty space for week number column
            val emptySpace = View(this)
            val emptyParams = LinearLayout.LayoutParams(
                (12 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt()
            )
            emptySpace.layoutParams = emptyParams
            dayNamesHeader.addView(emptySpace)
            
            // Add day name labels
            val dayNames = if (firstDayOfWeek == Calendar.MONDAY) {
                listOf("M", "T", "W", "T", "F", "S", "S")
            } else {
                listOf("S", "M", "T", "W", "T", "F", "S")
            }
            
            for (i in dayNames.indices) {
                val dayNameView = TextView(this)
                dayNameView.text = dayNames[i]
                dayNameView.textSize = 6f
                dayNameView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                dayNameView.gravity = android.view.Gravity.CENTER
                val params = LinearLayout.LayoutParams(
                    (12 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt()
                )
                dayNameView.layoutParams = params
                
                // Make Sunday primary color
                val isSunday = if (firstDayOfWeek == Calendar.MONDAY) i == 6 else i == 0
                if (isSunday) {
                    dayNameView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                }
                
                dayNamesHeader.addView(dayNameView)
            }

            val miniContainer = monthView.findViewById<LinearLayout>(R.id.mini_month_container)
            miniContainer.removeAllViews()

            monthCal.firstDayOfWeek = firstDayOfWeek
            val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            var firstDayOffset = monthCal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek
            if (firstDayOffset < 0) firstDayOffset += 7

            // Build mini calendar week by week with week numbers
            var currentDay = 1 - firstDayOffset
            while (currentDay <= daysInMonth) {
                val weekRow = LinearLayout(this)
                weekRow.orientation = LinearLayout.HORIZONTAL
                weekRow.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                
                // Add week number
                monthCal.set(Calendar.DAY_OF_MONTH, maxOf(1, currentDay))
                val weekNumber = monthCal.get(Calendar.WEEK_OF_YEAR)
                val weekNumView = TextView(this)
                weekNumView.text = weekNumber.toString()
                weekNumView.textSize = 6f
                weekNumView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                weekNumView.gravity = android.view.Gravity.CENTER
                weekNumView.setPadding(2, 0, 2, 0)
                val weekNumParams = LinearLayout.LayoutParams(
                    (12 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt()
                )
                weekNumView.layoutParams = weekNumParams
                
                // Click week number to navigate to that month
                val weekCal = monthCal.clone() as Calendar
                weekNumView.setOnClickListener {
                    currentDate.time = weekCal.time
                    switchToView(ViewMode.MONTH)
                }
                weekRow.addView(weekNumView)
                
                // Add 7 day cells
                for (dayOfWeek in 0..6) {
                    if (currentDay < 1 || currentDay > daysInMonth) {
                        // Empty cell
                        val emptyView = View(this)
                        val params = LinearLayout.LayoutParams(
                            (12 * resources.displayMetrics.density).toInt(),
                            (12 * resources.displayMetrics.density).toInt()
                        )
                        emptyView.layoutParams = params
                        weekRow.addView(emptyView)
                    } else {
                        // Day cell
                        monthCal.set(Calendar.DAY_OF_MONTH, currentDay)
                        
                        val dayView = TextView(this)
                        dayView.text = currentDay.toString()
                        dayView.textSize = 7f
                        
                        // Make Sunday day numbers primary color
                        val isSunday = monthCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                        if (isSunday) {
                            dayView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        } else {
                            dayView.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                        }
                        
                        dayView.gravity = android.view.Gravity.CENTER
                        
                        // Highlight current day
                        val isToday = monthCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                      monthCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                        if (isToday) {
                            dayView.setBackgroundResource(R.drawable.border_current_day)
                        }
                        
                        // Click day to navigate to that month
                        val dayCopy = monthCal.clone() as Calendar
                        dayView.setOnClickListener {
                            currentDate.time = dayCopy.time
                            switchToView(ViewMode.MONTH)
                        }
                        
                        val params = LinearLayout.LayoutParams(
                            (12 * resources.displayMetrics.density).toInt(),
                            (12 * resources.displayMetrics.density).toInt()
                        )
                        dayView.layoutParams = params
                        weekRow.addView(dayView)
                    }
                    currentDay++
                }
                
                miniContainer.addView(weekRow)
            }

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(4, 4, 4, 4)
            monthView.layoutParams = params
            yearGrid.addView(monthView)
        }
        
        // Load yearly goals/to-dos
        val yearStr = year.toString()
        val yearTodos = database.getTodosForYear(yearStr)
        rvYearTodos.layoutManager = LinearLayoutManager(this)
        rvYearTodos.adapter = TodoAdapter(this, yearTodos, this)
        
        tvNoYearTodos.visibility = if (yearTodos.isEmpty()) View.VISIBLE else View.GONE
        rvYearTodos.visibility = if (yearTodos.isEmpty()) View.GONE else View.VISIBLE
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

    private fun showEventDialog(event: Event?, prefilledStartTime: String? = null, prefilledEndTime: String? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etNotes = dialogView.findViewById<EditText>(R.id.et_notes)
        val tvStartTime = dialogView.findViewById<TextView>(R.id.tv_start_time)
        val tvEndTime = dialogView.findViewById<TextView>(R.id.tv_end_time)
        val spinnerRecurrence = dialogView.findViewById<Spinner>(R.id.spinner_recurrence)
        val customContainer = dialogView.findViewById<View>(R.id.custom_recurrence_container)
        val recurrenceEndContainer = dialogView.findViewById<View>(R.id.recurrence_end_container)
        val rgRecurrenceEnd = dialogView.findViewById<RadioGroup>(R.id.rg_recurrence_end)
        val tvEndDate = dialogView.findViewById<TextView>(R.id.tv_end_date)

        // Day checkboxes ordered Mon..Sun for UI; mapped to bit indices Sun=0..Sat=6.
        val dayCheckboxes = listOf(
            dialogView.findViewById<CheckBox>(R.id.cb_day_mon),
            dialogView.findViewById<CheckBox>(R.id.cb_day_tue),
            dialogView.findViewById<CheckBox>(R.id.cb_day_wed),
            dialogView.findViewById<CheckBox>(R.id.cb_day_thu),
            dialogView.findViewById<CheckBox>(R.id.cb_day_fri),
            dialogView.findViewById<CheckBox>(R.id.cb_day_sat),
            dialogView.findViewById<CheckBox>(R.id.cb_day_sun)
        )
        // Bit indices: Sun=0, Mon=1, Tue=2, Wed=3, Thu=4, Fri=5, Sat=6
        val uiDayBits = intArrayOf(1, 2, 3, 4, 5, 6, 0) // positions: Mon..Sun
        val btnPresetWeekdays = dialogView.findViewById<View>(R.id.btn_preset_weekdays)
        val btnPresetWeekends = dialogView.findViewById<View>(R.id.btn_preset_weekends)
        val btnPresetClear = dialogView.findViewById<View>(R.id.btn_preset_clear)
        val cbBiweekly = dialogView.findViewById<CheckBox>(R.id.cb_biweekly)

        var startTime = event?.startTime ?: prefilledStartTime ?: "09:00"
        var endTime = event?.endTime ?: prefilledEndTime ?: "10:00"
        // Default the recurrence end date to one week from today when none is set yet.
        var endDate = event?.recurrenceEndDate ?: run {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }
            getDateString(cal)
        }

        etTitle.setText(event?.title ?: "")
        etNotes.setText(event?.notes ?: "")
        tvStartTime.text = startTime
        tvEndTime.text = endTime
        // Show end date pre-filled in the user's preferred format (DB stays ISO yyyy-MM-dd).
        tvEndDate.text = settings.isoToDisplay(endDate)

        // Initialize custom panel state from event
        if (event?.recurrenceType == RecurrenceType.CUSTOM) {
            for (i in dayCheckboxes.indices) {
                dayCheckboxes[i].isChecked = (event.customDaysMask and (1 shl uiDayBits[i])) != 0
            }
            cbBiweekly.isChecked = event.intervalWeeks == 2
        }

        val recurrenceOptions = listOf(
            getString(R.string.recurrence_none),
            getString(R.string.recurrence_daily),
            getString(R.string.recurrence_weekly),
            getString(R.string.recurrence_monthly),
            getString(R.string.recurrence_yearly),
            "Custom"
        )
        // Map spinner positions to RecurrenceType enum ordinals:
        // 0=NONE, 1=DAILY, 2=WEEKLY, 3=MONTHLY, 4=YEARLY, 5=CUSTOM
        spinnerRecurrence.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, recurrenceOptions)
        spinnerRecurrence.setSelection(event?.recurrenceType?.ordinal ?: 0)

        spinnerRecurrence.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                recurrenceEndContainer.visibility = if (position > 0) View.VISIBLE else View.GONE
                customContainer.visibility = if (position == RecurrenceType.CUSTOM.ordinal) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnPresetWeekdays.setOnClickListener {
            // Mon..Fri (positions 0..4)
            for (i in dayCheckboxes.indices) dayCheckboxes[i].isChecked = i in 0..4
        }
        btnPresetWeekends.setOnClickListener {
            // Sat (5), Sun (6)
            for (i in dayCheckboxes.indices) dayCheckboxes[i].isChecked = i in 5..6
        }
        btnPresetClear.setOnClickListener {
            for (cb in dayCheckboxes) cb.isChecked = false
            cbBiweekly.isChecked = false
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
            showDatePicker(endDate) { date ->
                endDate = date
                tvEndDate.text = settings.isoToDisplay(date)
            }
        }

        val tilTitle = dialogView.findViewById<TextInputLayout>(R.id.til_title)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (event == null) R.string.add_event else R.string.edit_event)
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
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
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = etTitle.text.toString().trim()
                if (title.isEmpty()) {
                    tilTitle.error = "Title is required"
                    etTitle.requestFocus()
                    return@setOnClickListener
                }
                tilTitle.error = null

                var recurrenceType = RecurrenceType.values()[spinnerRecurrence.selectedItemPosition]
                val recurrenceEnd = if (recurrenceType != RecurrenceType.NONE && rgRecurrenceEnd.checkedRadioButtonId == R.id.rb_until) endDate else null

                // Build custom days mask from UI
                var customMask = 0
                for (i in dayCheckboxes.indices) {
                    if (dayCheckboxes[i].isChecked) customMask = customMask or (1 shl uiDayBits[i])
                }
                val intervalWeeks = if (cbBiweekly.isChecked) 2 else 1

                // If user chose Custom but selected zero days, fall back to NONE to avoid empty recurrence
                if (recurrenceType == RecurrenceType.CUSTOM && customMask == 0) {
                    recurrenceType = RecurrenceType.NONE
                }

                if (event == null) {
                    val newEvent = Event(
                        title = title,
                        notes = etNotes.text.toString(),
                        date = getDateString(currentDate),
                        startTime = startTime,
                        endTime = endTime,
                        recurrenceType = recurrenceType,
                        recurrenceEndDate = recurrenceEnd,
                        customDaysMask = if (recurrenceType == RecurrenceType.CUSTOM) customMask else 0,
                        intervalWeeks = if (recurrenceType == RecurrenceType.CUSTOM) intervalWeeks else 1,
                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    val eventId = database.addEvent(newEvent)
                    android.util.Log.d("MainActivity", "Event saved with ID: $eventId, time: $startTime-$endTime")
                    android.widget.Toast.makeText(this, "Event saved", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    event.title = title
                    event.notes = etNotes.text.toString()
                    event.startTime = startTime
                    event.endTime = endTime
                    event.recurrenceType = recurrenceType
                    event.recurrenceEndDate = recurrenceEnd
                    event.customDaysMask = if (recurrenceType == RecurrenceType.CUSTOM) customMask else 0
                    event.intervalWeeks = if (recurrenceType == RecurrenceType.CUSTOM) intervalWeeks else 1
                    database.updateEvent(event)
                    android.util.Log.d("MainActivity", "Event updated: ${event.id}, time: $startTime-$endTime")
                    android.widget.Toast.makeText(this, "Event updated", android.widget.Toast.LENGTH_SHORT).show()
                }
                loadData()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showTodoDialog(todo: TodoItem?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_todo, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_todo_title)
        val tilTitle = dialogView.findViewById<TextInputLayout>(R.id.til_todo_title)
        val cbMoveToNext = dialogView.findViewById<CheckBox>(R.id.cb_move_to_next)
        val cbImportant = dialogView.findViewById<CheckBox>(R.id.cb_important)

        etTitle.setText(todo?.title ?: "")
        cbMoveToNext.isChecked = todo?.moveToNext ?: false
        cbImportant.isChecked = todo?.isImportant ?: false

        // Update checkbox text based on current view
        if (currentViewMode == ViewMode.WEEK) {
            cbMoveToNext.text = getString(R.string.todo_move_next_week)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (todo == null) R.string.add_todo else R.string.edit_todo)
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
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
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = etTitle.text.toString().trim()
                if (title.isEmpty()) {
                    tilTitle.error = "Task is required"
                    etTitle.requestFocus()
                    return@setOnClickListener
                }
                tilTitle.error = null
                if (todo == null) {
                    val scope = if (currentViewMode == ViewMode.WEEK) TodoScope.WEEK else TodoScope.DAY
                    val newTodo = TodoItem(
                        title = title,
                        date = getDateString(currentDate),
                        weekStartDate = if (scope == TodoScope.WEEK) getDateString(getWeekStartDate(currentDate)) else null,
                        scope = scope,
                        moveToNext = cbMoveToNext.isChecked,
                        isImportant = cbImportant.isChecked,
                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    database.addTodo(newTodo)
                } else {
                    todo.title = title
                    todo.moveToNext = cbMoveToNext.isChecked
                    todo.isImportant = cbImportant.isChecked
                    database.updateTodo(todo)
                }
                loadData()
                dialog.dismiss()
            }
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

    private fun showDatePicker(initialIsoDate: String? = null, onDateSelected: (String) -> Unit) {
        val baseCal = Calendar.getInstance()
        if (initialIsoDate != null) {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(initialIsoDate)
                if (parsed != null) baseCal.time = parsed
            } catch (_: Exception) { /* keep today */ }
        } else {
            baseCal.time = currentDate.time
        }
        DatePickerDialog(this, { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day)
            onDateSelected(getDateString(cal))
        }, baseCal.get(Calendar.YEAR), baseCal.get(Calendar.MONTH), baseCal.get(Calendar.DAY_OF_MONTH)).show()
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

    private fun showMonthTodoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_todo, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_todo_title)
        val tilTitle = dialogView.findViewById<TextInputLayout>(R.id.til_todo_title)
        val cbMoveToNext = dialogView.findViewById<CheckBox>(R.id.cb_move_to_next)
        val cbImportant = dialogView.findViewById<CheckBox>(R.id.cb_important)
        
        cbMoveToNext.text = "Move to next month if not completed"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Monthly Goal")
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnShowListener {
            etTitle.requestFocus()
            etTitle.postDelayed({
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etTitle, InputMethodManager.SHOW_FORCED)
            }, 100)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = etTitle.text.toString().trim()
                if (title.isEmpty()) {
                    tilTitle.error = "Task is required"
                    etTitle.requestFocus()
                    return@setOnClickListener
                }
                tilTitle.error = null
                val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentDate.time)
                val newTodo = TodoItem(
                    title = title,
                    date = monthYear,
                    weekStartDate = null,
                    scope = TodoScope.MONTH,
                    moveToNext = cbMoveToNext.isChecked,
                    isImportant = cbImportant.isChecked,
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                database.addTodo(newTodo)
                loadData()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showYearTodoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_todo, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_todo_title)
        val tilTitle = dialogView.findViewById<TextInputLayout>(R.id.til_todo_title)
        val cbMoveToNext = dialogView.findViewById<CheckBox>(R.id.cb_move_to_next)
        val cbImportant = dialogView.findViewById<CheckBox>(R.id.cb_important)
        
        // Yearly goals should not be pushed forward automatically
        cbMoveToNext.visibility = View.GONE

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Yearly Goal")
            .setView(dialogView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnShowListener {
            etTitle.requestFocus()
            etTitle.postDelayed({
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etTitle, InputMethodManager.SHOW_FORCED)
            }, 100)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = etTitle.text.toString().trim()
                if (title.isEmpty()) {
                    tilTitle.error = "Task is required"
                    etTitle.requestFocus()
                    return@setOnClickListener
                }
                tilTitle.error = null
                val year = currentDate.get(Calendar.YEAR).toString()
                val newTodo = TodoItem(
                    title = title,
                    date = year,
                    weekStartDate = null,
                    scope = TodoScope.YEAR,
                    moveToNext = false,
                    isImportant = cbImportant.isChecked,
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                database.addTodo(newTodo)
                loadData()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
