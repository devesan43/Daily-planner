package com.example

import com.example.data.model.Category
import com.example.data.model.CategoryItem
import com.example.data.model.HabitItem
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerUnitTest {

    @Test
    fun testCategoryEntity_creationAndColorFields() {
        val category = Category(
            id = 1L,
            name = "Work",
            color = 0xFF6366F1,
            icon = "💼",
            isCustom = false
        )

        assertEquals(1L, category.id)
        assertEquals("Work", category.name)
        assertEquals(0xFF6366F1, category.color)
        assertEquals(0xFF6366F1, category.colorHex)
        assertEquals("💼", category.icon)
        assertFalse(category.isCustom)
    }

    @Test
    fun testTaskItem_foreignKeyRelationshipToCategory() {
        val taskWithCategory = TaskItem(
            id = 10L,
            title = "Prepare Project Proposal",
            dueDate = "2026-09-02",
            categoryId = 1L,
            category = "Work",
            priority = Priority.HIGH.name
        )

        assertEquals(1L, taskWithCategory.categoryId)
        assertEquals("Work", taskWithCategory.category)

        val taskWithoutCategory = TaskItem(
            id = 11L,
            title = "Unassigned Quick Note",
            dueDate = "2026-09-02"
        )
        assertNull(taskWithoutCategory.categoryId)
    }

    @Test
    fun testCategoryItem_creationAndProperties() {
        val workCat = CategoryItem(name = "Work", icon = "💼", colorHex = 0xFF6366F1, isCustom = false)
        val customShoppingCat = CategoryItem(name = "Shopping", icon = "🛒", colorHex = 0xFFF59E0B, isCustom = true)

        assertEquals("Work", workCat.name)
        assertEquals("💼", workCat.icon)
        assertFalse(workCat.isCustom)

        assertEquals("Shopping", customShoppingCat.name)
        assertEquals("🛒", customShoppingCat.icon)
        assertTrue(customShoppingCat.isCustom)
    }

    @Test
    fun testTaskItem_subtasksSerialization() {
        val subtasks = listOf(
            SubTask(id = "1", title = "Step 1", isDone = false),
            SubTask(id = "2", title = "Step 2", isDone = true)
        )
        val task = TaskItem(
            title = "Sample Task",
            dueDate = "2026-09-01",
            priority = Priority.HIGH.name,
            category = TaskCategory.WORK.label
        ).copyWithSubtasks(subtasks)

        val retrieved = task.getSubtasks()
        assertEquals(2, retrieved.size)
        assertEquals("Step 1", retrieved[0].title)
        assertFalse(retrieved[0].isDone)
        assertEquals("Step 2", retrieved[1].title)
        assertTrue(retrieved[1].isDone)
    }

    @Test
    fun testHabit_toggleDateAndStreak() {
        val habit = HabitItem(
            name = "Drink 2L Water",
            category = "Health",
            streak = 2
        )
        val updated = habit.toggleDate("2026-09-01")
        assertTrue(updated.isCompletedOn("2026-09-01"))
        assertEquals(3, updated.streak)

        val toggledOff = updated.toggleDate("2026-09-01")
        assertFalse(toggledOff.isCompletedOn("2026-09-01"))
        assertEquals(2, toggledOff.streak)
    }

    @Test
    fun testTaskItem_categoryFilteringAndReminder() {
        val workTask1 = TaskItem(
            id = 101L,
            title = "Sprint Planning",
            categoryId = 1L,
            category = "Work",
            dueDate = "2026-09-01",
            dueTime = "09:30",
            reminderEnabled = true
        )
        val workTask2 = TaskItem(
            id = 102L,
            title = "Review PRs",
            categoryId = 1L,
            category = "Work",
            dueDate = "2026-09-01",
            dueTime = "14:00",
            reminderEnabled = false
        )
        val personalTask = TaskItem(
            id = 103L,
            title = "Buy Groceries",
            categoryId = 2L,
            category = "Personal",
            dueDate = "2026-09-01"
        )

        val allTasks = listOf(workTask1, workTask2, personalTask)

        // Filter by categoryId 1L (Work)
        val filteredByCat1 = allTasks.filter { it.categoryId == 1L }
        assertEquals(2, filteredByCat1.size)
        assertTrue(filteredByCat1.contains(workTask1))
        assertTrue(filteredByCat1.contains(workTask2))

        // Filter by categoryId 2L (Personal)
        val filteredByCat2 = allTasks.filter { it.categoryId == 2L }
        assertEquals(1, filteredByCat2.size)
        assertEquals("Buy Groceries", filteredByCat2[0].title)

        // Reminder property checks
        assertTrue(workTask1.reminderEnabled)
        assertEquals("09:30", workTask1.dueTime)
        assertFalse(workTask2.reminderEnabled)
    }

    @Test
    fun testTaskCategory_fallback() {
        val category = TaskCategory.fromString("Unknown Category")
        assertEquals(TaskCategory.OTHER, category)

        val work = TaskCategory.fromString("Work")
        assertEquals(TaskCategory.WORK, work)
    }
}
