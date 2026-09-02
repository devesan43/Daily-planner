package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryItem
import com.example.data.model.DailyRoutineItem
import com.example.data.model.HabitItem
import com.example.data.model.JournalEntry
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, dueDate ASC, id DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date ORDER BY isCompleted ASC, priority DESC, id DESC")
    fun getTasksForDate(date: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE isStarred = 1 ORDER BY isCompleted ASC, dueDate ASC")
    fun getStarredTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY isCompleted ASC, dueDate ASC")
    fun getTasksByCategory(category: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId ORDER BY isCompleted ASC, dueDate ASC")
    fun getTasksByCategoryId(categoryId: Long): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskItem>): List<Long>

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean, completedAt: Long?)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY orderIndex ASC, id ASC")
    fun getAllRoutines(): Flow<List<DailyRoutineItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: DailyRoutineItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<DailyRoutineItem>)

    @Update
    suspend fun updateRoutine(routine: DailyRoutineItem)

    @Delete
    suspend fun deleteRoutine(routine: DailyRoutineItem)

    @Query("UPDATE routines SET isCompleted = :isCompleted, lastCompletedDate = :date, streakDays = :streak WHERE id = :id")
    suspend fun updateCompletion(id: Long, isCompleted: Boolean, date: String, streak: Int)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitItem>)

    @Update
    suspend fun updateHabit(habit: HabitItem)

    @Delete
    suspend fun deleteHabit(habit: HabitItem)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals ORDER BY createdAt DESC")
    fun getAllJournals(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journals WHERE templateType = :templateType ORDER BY createdAt DESC")
    fun getJournalsByType(templateType: String): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: JournalEntry): Long

    @Update
    suspend fun updateJournal(journal: JournalEntry)

    @Delete
    suspend fun deleteJournal(journal: JournalEntry)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC, id ASC")
    fun getAllCategories(): Flow<List<CategoryItem>>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryItem?

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryItem>)

    @Update
    suspend fun updateCategory(category: CategoryItem)

    @Delete
    suspend fun deleteCategory(category: CategoryItem)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
}

