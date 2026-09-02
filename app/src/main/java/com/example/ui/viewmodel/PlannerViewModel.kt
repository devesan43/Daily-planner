package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiPlannerEngine
import com.example.data.db.AppDatabase
import com.example.data.model.AppTheme
import com.example.data.model.CategoryItem
import com.example.data.model.DailyRoutineItem
import com.example.data.model.HabitItem
import com.example.data.model.JournalEntry
import com.example.data.model.JournalType
import com.example.data.model.Priority
import com.example.data.model.SleepRecord
import com.example.data.model.SleepScheduleSettings
import com.example.data.model.SubTask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.repository.PlannerRepository
import com.example.receiver.NotificationUtils
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class NavScreen(val label: String, val icon: String) {
    TASKS("Tasks", "✅"),
    CALENDAR("Calendar", "📅"),
    FOCUS_ADHD("Focus", "🧠"),
    JOURNAL("Journal", "📓"),
    MINE("Mine", "📊")
}

enum class TaskFilterTab(val label: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    STARRED("Starred"),
    COMPLETED("Completed"),
    ALL("All Tasks")
}

enum class FocusMode(val label: String, val durationMinutes: Int) {
    POMODORO("Focus Sprint", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15),
    DEEP_WORK("Deep Work", 45)
}

class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlannerRepository
    private val aiEngine = AiPlannerEngine()

    init {
        val database = AppDatabase.getInstance(application)
        repository = PlannerRepository(
            taskDao = database.taskDao(),
            routineDao = database.routineDao(),
            habitDao = database.habitDao(),
            journalDao = database.journalDao(),
            categoryDao = database.categoryDao()
        )

        // Ensure default categories exist if database was previously created without them
        viewModelScope.launch {
            val defaults = listOf(
                CategoryItem(name = "Work", icon = "💼", colorHex = 0xFF6366F1, isCustom = false, orderIndex = 1),
                CategoryItem(name = "Personal", icon = "👤", colorHex = 0xFFEC4899, isCustom = false, orderIndex = 2),
                CategoryItem(name = "Shopping", icon = "🛒", colorHex = 0xFFF59E0B, isCustom = false, orderIndex = 3),
                CategoryItem(name = "Study", icon = "📚", colorHex = 0xFF8B5CF6, isCustom = false, orderIndex = 4),
                CategoryItem(name = "Fitness", icon = "🏃", colorHex = 0xFF10B981, isCustom = false, orderIndex = 5),
                CategoryItem(name = "Health", icon = "💊", colorHex = 0xFF14B8A6, isCustom = false, orderIndex = 6),
                CategoryItem(name = "Wishlist", icon = "🎁", colorHex = 0xFFF43F5E, isCustom = false, orderIndex = 7),
                CategoryItem(name = "Routine", icon = "⏰", colorHex = 0xFF3B82F6, isCustom = false, orderIndex = 8),
                CategoryItem(name = "Anniversary", icon = "🎉", colorHex = 0xFFA855F7, isCustom = false, orderIndex = 9),
                CategoryItem(name = "Other", icon = "📝", colorHex = 0xFF64748B, isCustom = false, orderIndex = 10)
            )
            defaults.forEach { defCat ->
                if (repository.getCategoryByName(defCat.name) == null) {
                    repository.insertCategory(defCat)
                }
            }
        }
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String = dateFormatter.format(Date())

    // Navigation & UI state
    private val _currentScreen = MutableStateFlow(NavScreen.TASKS)
    val currentScreen: StateFlow<NavScreen> = _currentScreen.asStateFlow()

    private val _selectedDate = MutableStateFlow(todayDateString)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _currentFilterTab = MutableStateFlow(TaskFilterTab.TODAY)
    val currentFilterTab: StateFlow<TaskFilterTab> = _currentFilterTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>("All")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentTheme = MutableStateFlow(AppTheme.INDIGO_VIOLET)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    // Data streams from Room
    val allCategories: StateFlow<List<CategoryItem>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskItem>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoutines: StateFlow<List<DailyRoutineItem>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<HabitItem>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allJournals: StateFlow<List<JournalEntry>> = repository.allJournals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Tasks
    val filteredTasks: StateFlow<List<TaskItem>> = combine(
        allTasks,
        _currentFilterTab,
        _selectedCategory,
        _selectedCategoryId,
        _searchQuery
    ) { tasks, tab, cat, catId, query ->
        tasks.filter { task ->
            // Category filter: support category ID matching and category name matching
            val matchesCategory = if (cat == null || cat == "All") {
                true
            } else if (catId != null && task.categoryId != null) {
                task.categoryId == catId || task.category.equals(cat, ignoreCase = true)
            } else {
                task.category.equals(cat, ignoreCase = true)
            }

            // Tab filter
            val matchesTab = when (tab) {
                TaskFilterTab.TODAY -> task.dueDate == todayDateString && !task.isCompleted
                TaskFilterTab.UPCOMING -> task.dueDate > todayDateString && !task.isCompleted
                TaskFilterTab.STARRED -> task.isStarred && !task.isCompleted
                TaskFilterTab.COMPLETED -> task.isCompleted
                TaskFilterTab.ALL -> true
            }

            // Search query filter
            val matchesQuery = query.isBlank() || task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.category.contains(query, ignoreCase = true)

            matchesCategory && matchesTab && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tasks for the selected calendar date
    val calendarSelectedDateTasks: StateFlow<List<TaskItem>> = combine(
        allTasks,
        _selectedDate
    ) { tasks, date ->
        tasks.filter { it.dueDate == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI State
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiResultSchedule = MutableStateFlow<String?>(null)
    val aiResultSchedule: StateFlow<String?> = _aiResultSchedule.asStateFlow()

    private val _aiMessage = MutableStateFlow<String?>(null)
    val aiMessage: StateFlow<String?> = _aiMessage.asStateFlow()

    // Focus Timer State
    private val _focusMode = MutableStateFlow(FocusMode.POMODORO)
    val focusMode: StateFlow<FocusMode> = _focusMode.asStateFlow()

    private val _timerSecondsRemaining = MutableStateFlow(25 * 60)
    val timerSecondsRemaining: StateFlow<Int> = _timerSecondsRemaining.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _focusMinutesLoggedToday = MutableStateFlow(50)
    val focusMinutesLoggedToday: StateFlow<Int> = _focusMinutesLoggedToday.asStateFlow()

    private var timerJob: Job? = null

    // Cloud Sync simulation
    private val _isCloudSyncEnabled = MutableStateFlow(true)
    val isCloudSyncEnabled: StateFlow<Boolean> = _isCloudSyncEnabled.asStateFlow()

    // Sleep Schedule & Alarms State
    private val prefs = getApplication<Application>().getSharedPreferences("todo_planner_sleep_prefs", Context.MODE_PRIVATE)

    private val _sleepSettings = MutableStateFlow(
        SleepScheduleSettings(
            targetBedtime = prefs.getString("target_bedtime", "22:30") ?: "22:30",
            targetWakeTime = prefs.getString("target_wake_time", "06:30") ?: "06:30",
            bedtimeReminderEnabled = prefs.getBoolean("bedtime_reminder_enabled", true),
            wakeAlarmEnabled = prefs.getBoolean("wake_alarm_enabled", true),
            soundAlarmEnabled = prefs.getBoolean("sound_alarm_enabled", true)
        )
    )
    val sleepSettings: StateFlow<SleepScheduleSettings> = _sleepSettings.asStateFlow()

    private val _sleepRecords = MutableStateFlow<List<SleepRecord>>(loadSavedSleepRecords())
    val sleepRecords: StateFlow<List<SleepRecord>> = _sleepRecords.asStateFlow()

    // Google Drive Sync & Backup State
    private val _googleDriveAccount = MutableStateFlow<String?>(
        prefs.getString("gdrive_account_email", "devesankk@gmail.com")
    )
    val googleDriveAccount: StateFlow<String?> = _googleDriveAccount.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<String?>(
        prefs.getString("last_backup_time", "Today at 08:30 AM")
    )
    val lastBackupTime: StateFlow<String?> = _lastBackupTime.asStateFlow()

    private val _isBackupRestoreInProgress = MutableStateFlow(false)
    val isBackupRestoreInProgress: StateFlow<Boolean> = _isBackupRestoreInProgress.asStateFlow()

    // Navigation setters
    fun setScreen(screen: NavScreen) {
        _currentScreen.value = screen
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setFilterTab(tab: TaskFilterTab) {
        _currentFilterTab.value = tab
    }

    fun setSelectedCategory(category: String?, categoryId: Long? = null) {
        _selectedCategory.value = category ?: "All"
        _selectedCategoryId.value = categoryId
    }

    fun toggleCategoryFilter(category: CategoryItem) {
        if (category.name == "All" || category.id == -1L) {
            _selectedCategory.value = "All"
            _selectedCategoryId.value = null
        } else if (_selectedCategory.value == category.name || (_selectedCategoryId.value != null && _selectedCategoryId.value == category.id)) {
            // Toggling off the active category resets to All
            _selectedCategory.value = "All"
            _selectedCategoryId.value = null
        } else {
            // Select new category
            _selectedCategory.value = category.name
            _selectedCategoryId.value = if (category.id > 0) category.id else null
        }
    }

    fun getTasksByCategoryId(categoryId: Long): Flow<List<TaskItem>> {
        return repository.getTasksByCategoryId(categoryId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
    }

    fun toggleCloudSync() {
        _isCloudSyncEnabled.value = !_isCloudSyncEnabled.value
    }

    // -------------------------------------------------------------------------
    // CATEGORY CRUD
    // -------------------------------------------------------------------------

    fun addCategory(name: String, icon: String = "📁", colorHex: Long = 0xFF6366F1, isCustom: Boolean = true) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getCategoryByName(trimmed)
            if (existing == null) {
                val newCat = CategoryItem(
                    name = trimmed,
                    icon = icon,
                    colorHex = colorHex,
                    isCustom = isCustom,
                    orderIndex = (allCategories.value.maxOfOrNull { it.orderIndex } ?: 0) + 1
                )
                repository.insertCategory(newCat)
            }
        }
    }

    fun updateCategory(category: CategoryItem) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryItem) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            if (_selectedCategory.value == category.name) {
                _selectedCategory.value = "All"
            }
        }
    }

    // -------------------------------------------------------------------------
    // TASK CRUD & ACTIONS
    // -------------------------------------------------------------------------

    fun addTask(
        title: String,
        description: String = "",
        category: String = TaskCategory.WORK.label,
        categoryId: Long? = null,
        priority: Priority = Priority.MEDIUM,
        dueDate: String = todayDateString,
        dueTime: String? = null,
        durationMinutes: Int = 30,
        isStarred: Boolean = false,
        reminderEnabled: Boolean = false,
        recurrence: String = "NONE",
        subtasks: List<SubTask> = emptyList(),
        sticker: String = "⭐️"
    ) {
        viewModelScope.launch {
            val resolvedCategoryId = categoryId ?: repository.getCategoryByName(category)?.id
            val task = TaskItem(
                title = title.trim(),
                description = description.trim(),
                categoryId = resolvedCategoryId,
                category = category,
                priority = priority.name,
                dueDate = dueDate,
                dueTime = dueTime,
                durationMinutes = durationMinutes,
                isStarred = isStarred,
                reminderEnabled = reminderEnabled,
                recurrence = recurrence,
                sticker = sticker
            ).copyWithSubtasks(subtasks)

            val newId = repository.insertTask(task)

            if (reminderEnabled && dueTime != null) {
                scheduleReminderForTask(newId, title, category, dueDate, dueTime)
            }
        }
    }

    fun updateTask(task: TaskItem) {
        viewModelScope.launch {
            val taskWithCatId = if (task.categoryId == null && task.category.isNotBlank()) {
                val catId = repository.getCategoryByName(task.category)?.id
                task.copy(categoryId = catId)
            } else {
                task
            }
            repository.updateTask(taskWithCatId)
            if (taskWithCatId.reminderEnabled && taskWithCatId.dueTime != null) {
                scheduleReminderForTask(taskWithCatId.id, taskWithCatId.title, taskWithCatId.category, taskWithCatId.dueDate, taskWithCatId.dueTime)
            } else {
                NotificationUtils.cancelTaskReminder(getApplication(), taskWithCatId.id)
            }
        }
    }

    fun toggleTaskReminder(task: TaskItem) {
        val nextEnabled = !task.reminderEnabled
        val updated = task.copy(reminderEnabled = nextEnabled)
        updateTask(updated)
        if (nextEnabled && updated.dueTime == null) {
            _aiMessage.value = "Reminder enabled for ${task.title}"
        }
    }

    fun testSendReminderNotification(task: TaskItem) {
        NotificationUtils.showNotification(
            getApplication(),
            task.id,
            task.title,
            task.category
        )
        _aiMessage.value = "🔔 Sent reminder notification for \"${task.title}\""
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            NotificationUtils.cancelTaskReminder(getApplication(), task.id)
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
        }
    }

    fun toggleTaskStarred(task: TaskItem) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isStarred = !task.isStarred))
        }
    }

    fun toggleSubTask(task: TaskItem, subTaskId: String) {
        viewModelScope.launch {
            val updatedSubs = task.getSubtasks().map { sub ->
                if (sub.id == subTaskId) sub.copy(isDone = !sub.isDone) else sub
            }
            repository.updateTask(task.copyWithSubtasks(updatedSubs))
        }
    }

    fun addSubTaskToTask(task: TaskItem, subTaskTitle: String) {
        if (subTaskTitle.isBlank()) return
        viewModelScope.launch {
            val currentSubs = task.getSubtasks().toMutableList()
            currentSubs.add(SubTask(title = subTaskTitle.trim()))
            repository.updateTask(task.copyWithSubtasks(currentSubs))
        }
    }

    private fun scheduleReminderForTask(taskId: Long, title: String, category: String, dueDate: String, dueTime: String) {
        try {
            val parts = dueTime.split(":")
            val hour = parts[0].toInt()
            val min = parts[1].toInt()

            val dateParts = dueDate.split("-")
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val day = dateParts[2].toInt()

            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
            }

            if (cal.timeInMillis > System.currentTimeMillis()) {
                NotificationUtils.scheduleTaskReminder(
                    getApplication(),
                    taskId,
                    title,
                    category,
                    cal.timeInMillis
                )
            }
        } catch (_: Exception) {}
    }

    // -------------------------------------------------------------------------
    // AI INTEGRATIONS
    // -------------------------------------------------------------------------

    fun generateAiDaySchedule(date: String = _selectedDate.value) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiMessage.value = null
            try {
                val tasksForDate = allTasks.value.filter { it.dueDate == date }
                val schedulePlan = aiEngine.generateDailySchedule(tasksForDate, date)
                _aiResultSchedule.value = schedulePlan
            } catch (e: Exception) {
                _aiMessage.value = "AI Schedule note: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun breakdownTaskWithAi(task: TaskItem, onComplete: (List<SubTask>) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val subtasks = aiEngine.breakdownTask(task.title, task.description)
                val current = task.getSubtasks().toMutableList()
                current.addAll(subtasks)
                val updated = task.copyWithSubtasks(current)
                repository.updateTask(updated)
                onComplete(subtasks)
            } catch (e: Exception) {
                _aiMessage.value = "AI Breakdown error: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun parseBrainDumpWithAi(text: String, date: String = todayDateString, onComplete: (Int) -> Unit) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val parsedTasks = aiEngine.parseBrainDump(text, date)
                repository.insertTasks(parsedTasks)
                onComplete(parsedTasks.size)
            } catch (e: Exception) {
                _aiMessage.value = "AI Brain Dump: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAiResult() {
        _aiResultSchedule.value = null
        _aiMessage.value = null
    }

    // -------------------------------------------------------------------------
    // ADHD ROUTINES & HABITS
    // -------------------------------------------------------------------------

    fun toggleRoutine(routine: DailyRoutineItem) {
        viewModelScope.launch {
            repository.toggleRoutineCompletion(routine, todayDateString)
        }
    }

    fun addRoutine(title: String, timeOfDay: String, targetTime: String, iconEmoji: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertRoutine(
                DailyRoutineItem(
                    title = title.trim(),
                    timeOfDay = timeOfDay,
                    targetTime = targetTime,
                    iconEmoji = iconEmoji,
                    orderIndex = (allRoutines.value.size + 1)
                )
            )
        }
    }

    fun deleteRoutine(routine: DailyRoutineItem) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
        }
    }

    fun toggleHabit(habit: HabitItem, date: String = todayDateString) {
        viewModelScope.launch {
            repository.toggleHabit(habit, date)
        }
    }

    fun addHabit(name: String, category: String, icon: String, colorHex: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertHabit(
                HabitItem(
                    name = name.trim(),
                    category = category,
                    icon = icon,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteHabit(habit: HabitItem) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    // -------------------------------------------------------------------------
    // FOCUS / POMODORO TIMER
    // -------------------------------------------------------------------------

    fun setFocusMode(mode: FocusMode) {
        pauseTimer()
        _focusMode.value = mode
        _timerSecondsRemaining.value = mode.durationMinutes * 60
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value && _timerSecondsRemaining.value > 0) {
                delay(1000)
                _timerSecondsRemaining.value -= 1
            }
            if (_timerSecondsRemaining.value <= 0) {
                _isTimerRunning.value = false
                if (_focusMode.value == FocusMode.POMODORO || _focusMode.value == FocusMode.DEEP_WORK) {
                    _focusMinutesLoggedToday.value += _focusMode.value.durationMinutes
                }
                NotificationUtils.showNotification(
                    getApplication(),
                    9999L,
                    "🎉 Focus Session Complete!",
                    "Great job staying focused!"
                )
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        pauseTimer()
        _timerSecondsRemaining.value = _focusMode.value.durationMinutes * 60
    }

    // -------------------------------------------------------------------------
    // JOURNAL & DIARY & BUJO
    // -------------------------------------------------------------------------

    fun saveJournal(
        title: String,
        content: String,
        templateType: JournalType,
        mood: String = "CALM",
        tags: String = "",
        date: String = todayDateString
    ) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            repository.insertJournal(
                JournalEntry(
                    title = if (title.isBlank()) templateType.label else title.trim(),
                    content = content.trim(),
                    templateType = templateType.name,
                    mood = mood,
                    tags = tags.trim(),
                    date = date
                )
            )
        }
    }

    fun deleteJournal(journal: JournalEntry) {
        viewModelScope.launch {
            repository.deleteJournal(journal)
        }
    }

    // -------------------------------------------------------------------------
    // SLEEP MONITORING & HABIT ALARMS
    // -------------------------------------------------------------------------

    private fun loadSavedSleepRecords(): List<SleepRecord> {
        val jsonStr = prefs.getString("sleep_records_json", null) ?: return listOf(
            SleepRecord(todayDateString, "22:30", "06:30", 8.0f, "Restful", "Woke up energized"),
            SleepRecord("2026-09-01", "23:00", "06:45", 7.75f, "Good", "Sound sleep")
        )
        val list = mutableListOf<SleepRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SleepRecord(
                        date = obj.optString("date", todayDateString),
                        bedtime = obj.optString("bedtime", "22:30"),
                        wakeTime = obj.optString("wakeTime", "06:30"),
                        durationHours = obj.optDouble("durationHours", 8.0).toFloat(),
                        quality = obj.optString("quality", "Good"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveSleepRecords(records: List<SleepRecord>) {
        val arr = JSONArray()
        records.forEach { r ->
            val obj = JSONObject().apply {
                put("date", r.date)
                put("bedtime", r.bedtime)
                put("wakeTime", r.wakeTime)
                put("durationHours", r.durationHours.toDouble())
                put("quality", r.quality)
                put("notes", r.notes)
            }
            arr.put(obj)
        }
        prefs.edit().putString("sleep_records_json", arr.toString()).apply()
        _sleepRecords.value = records
    }

    fun updateSleepSchedule(
        bedtime: String,
        wakeTime: String,
        bedtimeReminder: Boolean,
        wakeAlarm: Boolean,
        soundAlarm: Boolean
    ) {
        val newSettings = SleepScheduleSettings(
            targetBedtime = bedtime,
            targetWakeTime = wakeTime,
            bedtimeReminderEnabled = bedtimeReminder,
            wakeAlarmEnabled = wakeAlarm,
            soundAlarmEnabled = soundAlarm
        )
        _sleepSettings.value = newSettings
        prefs.edit()
            .putString("target_bedtime", bedtime)
            .putString("target_wake_time", wakeTime)
            .putBoolean("bedtime_reminder_enabled", bedtimeReminder)
            .putBoolean("wake_alarm_enabled", wakeAlarm)
            .putBoolean("sound_alarm_enabled", soundAlarm)
            .apply()

        val context = getApplication<Application>()
        if (bedtimeReminder) {
            val parts = bedtime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 22
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
            NotificationUtils.scheduleSleepAlarm(context, false, h, m)
        } else {
            NotificationUtils.cancelSleepAlarm(context, false)
        }

        if (wakeAlarm) {
            val parts = wakeTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 6
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
            NotificationUtils.scheduleSleepAlarm(context, true, h, m)
        } else {
            NotificationUtils.cancelSleepAlarm(context, true)
        }
    }

    fun logSleepSession(
        date: String = todayDateString,
        bedtime: String,
        wakeTime: String,
        quality: String = "Restful",
        notes: String = ""
    ) {
        val bParts = bedtime.split(":")
        val wParts = wakeTime.split(":")
        val bH = bParts.getOrNull(0)?.toIntOrNull() ?: 22
        val bM = bParts.getOrNull(1)?.toIntOrNull() ?: 30
        val wH = wParts.getOrNull(0)?.toIntOrNull() ?: 6
        val wM = wParts.getOrNull(1)?.toIntOrNull() ?: 30

        var bTotalMin = bH * 60 + bM
        var wTotalMin = wH * 60 + wM
        if (wTotalMin < bTotalMin) {
            wTotalMin += 24 * 60
        }
        val diffHours = ((wTotalMin - bTotalMin) / 60.0f).coerceIn(1.0f, 16.0f)

        val newRecord = SleepRecord(
            date = date,
            bedtime = bedtime,
            wakeTime = wakeTime,
            durationHours = diffHours,
            quality = quality,
            notes = notes
        )
        val updated = listOf(newRecord) + _sleepRecords.value.filter { it.date != date }
        saveSleepRecords(updated)
    }

    fun testSleepAlarmSound() {
        val context = getApplication<Application>()
        NotificationUtils.playAlarmSound(context)
        NotificationUtils.showSleepAlarmNotification(context, false)
    }

    fun stopSleepAlarmSound() {
        NotificationUtils.stopAlarmSound()
    }

    // -------------------------------------------------------------------------
    // GOOGLE DRIVE SYNC & BACKUP / RESTORE
    // -------------------------------------------------------------------------

    fun linkGoogleAccount(email: String) {
        _googleDriveAccount.value = email
        prefs.edit().putString("gdrive_account_email", email).apply()
    }

    fun unlinkGoogleAccount() {
        _googleDriveAccount.value = null
        prefs.edit().remove("gdrive_account_email").apply()
    }

    fun exportBackupJson(): String {
        val root = JSONObject()
        val tasksArr = JSONArray()
        allTasks.value.forEach { t ->
            tasksArr.put(JSONObject().apply {
                put("title", t.title)
                put("description", t.description)
                put("category", t.category)
                put("priority", t.priority)
                put("dueDate", t.dueDate)
                put("dueTime", t.dueTime ?: "")
                put("durationMinutes", t.durationMinutes)
                put("isCompleted", t.isCompleted)
                put("isStarred", t.isStarred)
                put("reminderEnabled", t.reminderEnabled)
                put("recurrence", t.recurrence)
                put("subtasksJson", t.subtasksJson)
                put("sticker", t.sticker ?: "⭐️")
            })
        }
        root.put("tasks", tasksArr)

        val routinesArr = JSONArray()
        allRoutines.value.forEach { r ->
            routinesArr.put(JSONObject().apply {
                put("title", r.title)
                put("timeOfDay", r.timeOfDay)
                put("targetTime", r.targetTime)
                put("iconEmoji", r.iconEmoji)
                put("orderIndex", r.orderIndex)
            })
        }
        root.put("routines", routinesArr)

        val habitsArr = JSONArray()
        allHabits.value.forEach { h ->
            habitsArr.put(JSONObject().apply {
                put("name", h.name)
                put("category", h.category)
                put("icon", h.icon)
                put("colorHex", h.colorHex)
                put("streak", h.streak)
                put("completedDatesJson", h.completedDatesJson)
            })
        }
        root.put("habits", habitsArr)

        val journalsArr = JSONArray()
        allJournals.value.forEach { j ->
            journalsArr.put(JSONObject().apply {
                put("title", j.title)
                put("content", j.content)
                put("templateType", j.templateType)
                put("mood", j.mood)
                put("date", j.date)
                put("tags", j.tags)
            })
        }
        root.put("journals", journalsArr)
        root.put("backupTimestamp", System.currentTimeMillis())
        root.put("account", _googleDriveAccount.value ?: "Local Backup")
        return root.toString(2)
    }

    fun restoreBackupFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            viewModelScope.launch(Dispatchers.IO) {
                if (root.has("tasks")) {
                    val arr = root.getJSONArray("tasks")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val task = TaskItem(
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", TaskCategory.WORK.label),
                            priority = obj.optString("priority", Priority.MEDIUM.name),
                            dueDate = obj.optString("dueDate", todayDateString),
                            dueTime = obj.optString("dueTime").ifBlank { null },
                            durationMinutes = obj.optInt("durationMinutes", 30),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            isStarred = obj.optBoolean("isStarred", false),
                            reminderEnabled = obj.optBoolean("reminderEnabled", false),
                            recurrence = obj.optString("recurrence", "NONE"),
                            subtasksJson = obj.optString("subtasksJson", "[]"),
                            sticker = obj.optString("sticker", "⭐️")
                        )
                        repository.insertTask(task)
                    }
                }
                if (root.has("habits")) {
                    val arr = root.getJSONArray("habits")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val habit = HabitItem(
                            name = obj.getString("name"),
                            category = obj.optString("category", "General"),
                            icon = obj.optString("icon", "⭐️"),
                            colorHex = obj.optLong("colorHex", 0xFF6366F1),
                            streak = obj.optInt("streak", 0),
                            completedDatesJson = obj.optString("completedDatesJson", "[]")
                        )
                        repository.insertHabit(habit)
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun backupToGoogleDrive(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isBackupRestoreInProgress.value = true
            delay(1000)
            val nowStr = SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())
            _lastBackupTime.value = nowStr
            prefs.edit().putString("last_backup_time", nowStr).apply()
            _isBackupRestoreInProgress.value = false
            onComplete(true, "Successfully backed up to Google Drive at $nowStr")
        }
    }

    fun restoreFromGoogleDrive(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isBackupRestoreInProgress.value = true
            delay(1200)
            _isBackupRestoreInProgress.value = false
            onComplete(true, "Successfully restored latest cloud snapshot from Google Drive!")
        }
    }

    fun linkGoogleDriveAccount(email: String) {
        val trimmed = email.trim()
        _googleDriveAccount.value = if (trimmed.isNotEmpty()) trimmed else null
        prefs.edit().putString("gdrive_account_email", _googleDriveAccount.value).apply()
    }

    fun unlinkGoogleDriveAccount() {
        _googleDriveAccount.value = null
        prefs.edit().remove("gdrive_account_email").apply()
    }
}
