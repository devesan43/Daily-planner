package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

enum class Priority(val label: String, val colorHex: Long) {
    HIGH("High", 0xFFEF4444),
    MEDIUM("Medium", 0xFFF59E0B),
    LOW("Low", 0xFF10B981),
    NONE("None", 0xFF94A3B8)
}

enum class TaskCategory(val label: String, val icon: String, val colorHex: Long) {
    WORK("Work", "💼", 0xFF6366F1),
    PERSONAL("Personal", "👤", 0xFFEC4899),
    STUDY("Study", "📚", 0xFF8B5CF6),
    FITNESS("Fitness", "🏃", 0xFF10B981),
    HEALTH("Health", "💊", 0xFF14B8A6),
    WISHLIST("Wishlist", "🎁", 0xFFF43F5E),
    SHOPPING("Shopping", "🛒", 0xFFF59E0B),
    ROUTINE("Routine", "⏰", 0xFF3B82F6),
    BIRTHDAY("Anniversary", "🎉", 0xFFA855F7),
    OTHER("Other", "📝", 0xFF64748B);

    companion object {
        fun fromString(name: String): TaskCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.label.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}

/**
 * Room database Entity class 'Category' (and alias CategoryItem for compatibility).
 * Includes auto-generated id, name, and color (hex) for UI customization.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long = 0xFF6366F1, // Color field for UI customization
    val icon: String = "📁",
    val colorHex: Long = color,
    val isCustom: Boolean = true,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

typealias CategoryItem = Category

data class SubTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
)

/**
 * Task Entity with foreign key relationship to Category
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"])
    ]
)
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val categoryId: Long? = null, // Foreign key linking to Category entity
    val category: String = TaskCategory.WORK.label,
    val priority: String = Priority.NONE.name,
    val isStarred: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val dueDate: String, // format: yyyy-MM-dd
    val dueTime: String? = null, // format: HH:mm (e.g. 09:30)
    val durationMinutes: Int = 30, // For schedule time blocking
    val reminderEnabled: Boolean = false,
    val reminderTimeMillis: Long? = null,
    val recurrence: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val subtasksJson: String = "[]",
    val sticker: String? = "⭐️",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getSubtasks(): List<SubTask> {
        val list = mutableListOf<SubTask>()
        try {
            val array = JSONArray(subtasksJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SubTask(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        isDone = obj.optBoolean("isDone", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun copyWithSubtasks(subtasks: List<SubTask>): TaskItem {
        val array = JSONArray()
        subtasks.forEach { sub ->
            val obj = JSONObject()
            obj.put("id", sub.id)
            obj.put("title", sub.title)
            obj.put("isDone", sub.isDone)
            array.put(obj)
        }
        return this.copy(subtasksJson = array.toString())
    }
}

@Entity(tableName = "routines")
data class DailyRoutineItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeOfDay: String, // "MORNING", "AFTERNOON", "EVENING", "NIGHT"
    val targetTime: String = "08:00",
    val isCompleted: Boolean = false,
    val lastCompletedDate: String = "", // yyyy-MM-dd
    val streakDays: Int = 0,
    val iconEmoji: String = "☀️",
    val orderIndex: Int = 0
)

@Entity(tableName = "habits")
data class HabitItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Health",
    val icon: String = "💧",
    val colorHex: Long = 0xFF3B82F6,
    val streak: Int = 0,
    val completedDatesJson: String = "[]", // JSON array of yyyy-MM-dd
    val targetDaysPerWeek: Int = 7
) {
    fun getCompletedDates(): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val array = JSONArray(completedDatesJson)
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return set
    }

    fun isCompletedOn(date: String): Boolean = getCompletedDates().contains(date)

    fun toggleDate(date: String): HabitItem {
        val current = getCompletedDates().toMutableSet()
        if (current.contains(date)) {
            current.remove(date)
        } else {
            current.add(date)
        }
        val array = JSONArray(current.toList())
        return this.copy(
            completedDatesJson = array.toString(),
            streak = if (current.contains(date)) streak + 1 else (streak - 1).coerceAtLeast(0)
        )
    }
}

enum class JournalType(val label: String, val icon: String, val prompt: String) {
    GRATITUDE("Gratitude", "🙏", "What 3 things are you grateful for today?"),
    DIARY("Daily Diary", "📔", "Capture today's story, moments, and personal highlights..."),
    AFFIRMATION("Affirmations", "✨", "Positive affirmations to empower your mindset..."),
    BRAIN_DUMP("Brain Dump", "🧠", "Empty all cluttered thoughts, tasks, and ideas onto the page..."),
    BUJO("Bullet Journal", "🎯", "• Tasks  - Notes  o Events  * Priorities"),
    MINDFULNESS("Mindfulness", "🌿", "How is your mental state? Ground yourself in this moment..."),
    NOTES("Quick Note", "📝", "Jot down insights, meeting takeaways, or study summaries...")
}

@Entity(tableName = "journals")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val templateType: String = JournalType.DIARY.name,
    val mood: String = "CALM", // GREAT, GOOD, CALM, TIRED, ANXIOUS
    val date: String, // yyyy-MM-dd
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class AppTheme(val displayName: String, val primaryColor: Long, val secondaryColor: Long, val isDark: Boolean = false) {
    INDIGO_VIOLET("Modern Indigo", 0xFF6366F1, 0xFF8B5CF6, false),
    LAVENDER_DREAM("Lavender Dream", 0xFF8B5CF6, 0xFFC084FC, false),
    MATCHA_CALM("Matcha Green", 0xFF059669, 0xFF34D399, false),
    SUNSET_CORAL("Sunset Coral", 0xFFF43F5E, 0xFFFB7185, false),
    OCEAN_BLUE("Ocean Breeze", 0xFF0284C7, 0xFF38BDF8, false),
    ROSE_GOLD("Aesthetic Rose", 0xFFDB2777, 0xFFF472B6, false),
    MIDNIGHT_AMOLED("Midnight Dark", 0xFF818CF8, 0xFFA78BFA, true)
}
