package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryItem
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.ui.components.AddTaskBottomSheet
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.ManageCategoriesBottomSheet
import com.example.ui.components.TaskCard
import com.example.ui.viewmodel.PlannerViewModel
import com.example.ui.viewmodel.TaskFilterTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: PlannerViewModel,
    tasks: List<TaskItem>,
    currentTab: TaskFilterTab,
    selectedCategory: String?,
    onOpenAiPlanner: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showManageCategories by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var quickTitle by remember { mutableStateOf("") }

    val filterTabs = TaskFilterTab.entries

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter Tabs Row
            ScrollableTabRow(
                selectedTabIndex = filterTabs.indexOf(currentTab).coerceAtLeast(0),
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                filterTabs.forEach { tab ->
                    val isSelected = currentTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setFilterTab(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name}")
                    )
                }
            }

            // Categories horizontal filter toggle
            CategoryFilterRow(
                categories = categories,
                selectedCategory = selectedCategory,
                selectedCategoryId = selectedCategoryId,
                onToggleCategory = { viewModel.toggleCategoryFilter(it) },
                onManageCategories = { showManageCategories = true }
            )

            // Quick 2-Step Add Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickTitle,
                        onValueChange = { quickTitle = it },
                        placeholder = { Text("Quick add task (e.g. Call mom tonight)...", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (quickTitle.isNotBlank()) {
                                viewModel.addTask(
                                    title = quickTitle.trim(),
                                    category = selectedCategory ?: TaskCategory.WORK.label,
                                    priority = Priority.MEDIUM,
                                    dueDate = viewModel.todayDateString
                                )
                                quickTitle = ""
                            } else {
                                showAddDialog = true
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("btn_quick_add_submit")
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Task List or Empty State
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "✨", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (currentTab) {
                                TaskFilterTab.TODAY -> "All clear for today!"
                                TaskFilterTab.STARRED -> "No starred tasks yet"
                                TaskFilterTab.COMPLETED -> "No completed tasks yet"
                                else -> "No tasks in this view"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button or use AI Planner to organize your daily schedule effortlessly.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 4.dp, bottom = 80.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            categories = categories,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onToggleStar = { viewModel.toggleTaskStarred(task) },
                            onToggleReminder = { viewModel.toggleTaskReminder(task) },
                            onTestReminder = { viewModel.testSendReminderNotification(task) },
                            onToggleSubTask = { subId -> viewModel.toggleSubTask(task, subId) },
                            onAddSubTask = { subTitle -> viewModel.addSubTaskToTask(task, subTitle) },
                            onBreakdownWithAi = {
                                viewModel.breakdownTaskWithAi(task) {}
                            },
                            onEdit = {
                                taskToEdit = task
                                showAddDialog = true
                            },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                taskToEdit = null
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_task"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add New Task", modifier = Modifier.size(26.dp))
        }

        // Add/Edit BottomSheet
        if (showAddDialog) {
            AddTaskBottomSheet(
                initialTask = taskToEdit,
                targetDate = viewModel.todayDateString,
                availableCategories = categories,
                onDismiss = {
                    showAddDialog = false
                    taskToEdit = null
                },
                onSaveTask = { title, desc, cat, priority, dueDate, dueTime, duration, isStarred, reminder, recurrence, subtasks, sticker ->
                    if (taskToEdit == null) {
                        viewModel.addTask(
                            title = title,
                            description = desc,
                            category = cat,
                            priority = priority,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            durationMinutes = duration,
                            isStarred = isStarred,
                            reminderEnabled = reminder,
                            recurrence = recurrence,
                            subtasks = subtasks,
                            sticker = sticker
                        )
                    } else {
                        viewModel.updateTask(
                            taskToEdit!!.copy(
                                title = title,
                                description = desc,
                                category = cat,
                                priority = priority.name,
                                dueDate = dueDate,
                                dueTime = dueTime,
                                durationMinutes = duration,
                                isStarred = isStarred,
                                reminderEnabled = reminder,
                                recurrence = recurrence,
                                sticker = sticker
                            ).copyWithSubtasks(subtasks)
                        )
                    }
                }
            )
        }

        // Manage Categories BottomSheet
        if (showManageCategories) {
            ManageCategoriesBottomSheet(
                categories = categories,
                onDismiss = { showManageCategories = false },
                onAddCategory = { name, icon, colorHex ->
                    viewModel.addCategory(name = name, icon = icon, colorHex = colorHex, isCustom = true)
                },
                onDeleteCategory = { categoryItem ->
                    viewModel.deleteCategory(categoryItem)
                }
            )
        }
    }
}
