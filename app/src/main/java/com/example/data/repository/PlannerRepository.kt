package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.HabitDao
import com.example.data.dao.JournalDao
import com.example.data.dao.RoutineDao
import com.example.data.dao.TaskDao
import com.example.data.model.CategoryItem
import com.example.data.model.DailyRoutineItem
import com.example.data.model.HabitItem
import com.example.data.model.JournalEntry
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.Flow

class PlannerRepository(
    private val taskDao: TaskDao,
    private val routineDao: RoutineDao,
    private val habitDao: HabitDao,
    private val journalDao: JournalDao,
    private val categoryDao: CategoryDao
) {
    // Categories
    val allCategories: Flow<List<CategoryItem>> = categoryDao.getAllCategories()
    suspend fun getCategoryByName(name: String): CategoryItem? = categoryDao.getCategoryByName(name)
    suspend fun insertCategory(category: CategoryItem): Long = categoryDao.insertCategory(category)
    suspend fun insertCategories(categories: List<CategoryItem>) = categoryDao.insertCategories(categories)
    suspend fun updateCategory(category: CategoryItem) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: CategoryItem) = categoryDao.deleteCategory(category)
    suspend fun deleteCategoryById(id: Long) = categoryDao.deleteCategoryById(id)

    // Tasks
    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks()
    val starredTasks: Flow<List<TaskItem>> = taskDao.getStarredTasks()

    fun getTasksForDate(date: String): Flow<List<TaskItem>> = taskDao.getTasksForDate(date)
    fun getTasksByCategory(category: String): Flow<List<TaskItem>> = taskDao.getTasksByCategory(category)
    fun getTasksByCategoryId(categoryId: Long): Flow<List<TaskItem>> = taskDao.getTasksByCategoryId(categoryId)
    suspend fun getTaskById(id: Long): TaskItem? = taskDao.getTaskById(id)

    suspend fun insertTask(task: TaskItem): Long = taskDao.insertTask(task)
    suspend fun insertTasks(tasks: List<TaskItem>): List<Long> = taskDao.insertTasks(tasks)
    suspend fun updateTask(task: TaskItem) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskItem) = taskDao.deleteTask(task)
    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)
    suspend fun toggleTaskCompletion(task: TaskItem) {
        val nextCompleted = !task.isCompleted
        taskDao.updateTaskCompletion(
            id = task.id,
            isCompleted = nextCompleted,
            completedAt = if (nextCompleted) System.currentTimeMillis() else null
        )
    }

    // Routines
    val allRoutines: Flow<List<DailyRoutineItem>> = routineDao.getAllRoutines()
    suspend fun insertRoutine(routine: DailyRoutineItem): Long = routineDao.insertRoutine(routine)
    suspend fun updateRoutine(routine: DailyRoutineItem) = routineDao.updateRoutine(routine)
    suspend fun deleteRoutine(routine: DailyRoutineItem) = routineDao.deleteRoutine(routine)
    suspend fun toggleRoutineCompletion(routine: DailyRoutineItem, date: String) {
        val nextState = !routine.isCompleted
        val nextStreak = if (nextState) routine.streakDays + 1 else (routine.streakDays - 1).coerceAtLeast(0)
        routineDao.updateCompletion(routine.id, nextState, date, nextStreak)
    }

    // Habits
    val allHabits: Flow<List<HabitItem>> = habitDao.getAllHabits()
    suspend fun insertHabit(habit: HabitItem): Long = habitDao.insertHabit(habit)
    suspend fun updateHabit(habit: HabitItem) = habitDao.updateHabit(habit)
    suspend fun deleteHabit(habit: HabitItem) = habitDao.deleteHabit(habit)
    suspend fun toggleHabit(habit: HabitItem, date: String) {
        val updated = habit.toggleDate(date)
        habitDao.updateHabit(updated)
    }

    // Journals
    val allJournals: Flow<List<JournalEntry>> = journalDao.getAllJournals()
    fun getJournalsByType(type: String): Flow<List<JournalEntry>> = journalDao.getJournalsByType(type)
    suspend fun insertJournal(journal: JournalEntry): Long = journalDao.insertJournal(journal)
    suspend fun updateJournal(journal: JournalEntry) = journalDao.updateJournal(journal)
    suspend fun deleteJournal(journal: JournalEntry) = journalDao.deleteJournal(journal)
}
