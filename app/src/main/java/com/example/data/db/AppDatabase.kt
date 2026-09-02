package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.HabitDao
import com.example.data.dao.JournalDao
import com.example.data.dao.RoutineDao
import com.example.data.dao.TaskDao
import com.example.data.model.CategoryItem
import com.example.data.model.DailyRoutineItem
import com.example.data.model.HabitItem
import com.example.data.model.JournalEntry
import com.example.data.model.JournalType
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        TaskItem::class,
        DailyRoutineItem::class,
        HabitItem::class,
        JournalEntry::class,
        CategoryItem::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun routineDao(): RoutineDao
    abstract fun habitDao(): HabitDao
    abstract fun journalDao(): JournalDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo_planner.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            populateInitialData(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Default Categories
            val defaultCategories = listOf(
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
            database.categoryDao().insertCategories(defaultCategories)

            // Sample Tasks
            val sampleTasks = listOf(
                TaskItem(
                    title = "Review Weekly Goals & Priorities",
                    description = "Align top 3 focus outcomes for study and productivity.",
                    category = TaskCategory.WORK.label,
                    priority = Priority.HIGH.name,
                    isStarred = true,
                    dueDate = todayStr,
                    dueTime = "09:00",
                    durationMinutes = 45,
                    sticker = "🎯"
                ).copyWithSubtasks(
                    listOf(
                        SubTask(title = "Check task backlog", isDone = true),
                        SubTask(title = "Schedule deep focus blocks", isDone = false),
                        SubTask(title = "Prepare checklist for tomorrow", isDone = false)
                    )
                ),
                TaskItem(
                    title = "Team Sprint Planning & Demo",
                    description = "Discuss app features and roadmap milestones.",
                    category = TaskCategory.WORK.label,
                    priority = Priority.HIGH.name,
                    isStarred = true,
                    dueDate = todayStr,
                    dueTime = "11:00",
                    durationMinutes = 60,
                    reminderEnabled = true,
                    sticker = "🚀"
                ),
                TaskItem(
                    title = "30-min Evening Cardio / Gym",
                    description = "Stay active, stretch, and log daily workout progress.",
                    category = TaskCategory.FITNESS.label,
                    priority = Priority.MEDIUM.name,
                    isStarred = false,
                    dueDate = todayStr,
                    dueTime = "17:30",
                    durationMinutes = 45,
                    sticker = "🏃"
                ),
                TaskItem(
                    title = "Pick up Grocery & Healthy Snacks",
                    description = "Almonds, oat milk, fresh berries, green tea.",
                    category = TaskCategory.SHOPPING.label,
                    priority = Priority.LOW.name,
                    isStarred = false,
                    dueDate = todayStr,
                    dueTime = "19:00",
                    durationMinutes = 30,
                    sticker = "🛒"
                ),
                TaskItem(
                    title = "Family Dinner & Catch-up Call",
                    description = "Celebrate weekend anniversary plans.",
                    category = TaskCategory.BIRTHDAY.label,
                    priority = Priority.HIGH.name,
                    isStarred = true,
                    dueDate = todayStr,
                    dueTime = "20:00",
                    durationMinutes = 60,
                    sticker = "🎉"
                )
            )
            database.taskDao().insertTasks(sampleTasks)

            // ADHD Routines
            val sampleRoutines = listOf(
                DailyRoutineItem(title = "Morning Sunlight & Big Glass of Water", timeOfDay = "MORNING", targetTime = "07:30", iconEmoji = "☀️", streakDays = 5, orderIndex = 1),
                DailyRoutineItem(title = "5-Minute Brain Dump / Priority Review", timeOfDay = "MORNING", targetTime = "08:00", iconEmoji = "🧠", streakDays = 4, orderIndex = 2),
                DailyRoutineItem(title = "Midday Hydration & Desk Stretch", timeOfDay = "AFTERNOON", targetTime = "13:00", iconEmoji = "💧", streakDays = 7, orderIndex = 3),
                DailyRoutineItem(title = "Afternoon Pomodoro Focus Sprint", timeOfDay = "AFTERNOON", targetTime = "15:00", iconEmoji = "⚡️", streakDays = 3, orderIndex = 4),
                DailyRoutineItem(title = "Evening Digital Sunset & Room Tidy", timeOfDay = "EVENING", targetTime = "21:30", iconEmoji = "🌙", streakDays = 6, orderIndex = 5),
                DailyRoutineItem(title = "Gratitude Log & Tomorrow's 3 Goals", timeOfDay = "EVENING", targetTime = "22:00", iconEmoji = "✨", streakDays = 5, orderIndex = 6)
            )
            database.routineDao().insertRoutines(sampleRoutines)

            // Initial Habits
            val sampleHabits = listOf(
                HabitItem(name = "Drink 2.5L Water", category = "Health", icon = "💧", colorHex = 0xFF0284C7, streak = 6),
                HabitItem(name = "Read 15 Pages", category = "Mind", icon = "📖", colorHex = 0xFF8B5CF6, streak = 4),
                HabitItem(name = "Daily Meditation", category = "Mindfulness", icon = "🧘", colorHex = 0xFF10B981, streak = 8),
                HabitItem(name = "Deep Focus Block (45m)", category = "Productivity", icon = "⚡️", colorHex = 0xFFF59E0B, streak = 5)
            )
            database.habitDao().insertHabits(sampleHabits)

            // Initial Journal
            database.journalDao().insertJournal(
                JournalEntry(
                    title = "A Fresh Mindful Start",
                    content = "1. Grateful for quiet morning coffee.\n2. Energized by the new schedule planner!\n3. Looking forward to completing all high-priority tasks with focus.",
                    templateType = JournalType.GRATITUDE.name,
                    mood = "GREAT",
                    date = todayStr,
                    tags = "Focus, Gratitude, New Start"
                )
            )
        }
    }
}
