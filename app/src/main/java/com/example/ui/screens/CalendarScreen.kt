package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.ui.components.AddTaskBottomSheet
import com.example.ui.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: PlannerViewModel,
    onOpenAiPlanner: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val selectedDayTasks by viewModel.calendarSelectedDateTasks.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var prefilledTime by remember { mutableStateOf("06:00") }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayHeaderFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    val currentCal = remember(selectedDate) {
        val cal = Calendar.getInstance()
        try {
            cal.time = sdf.parse(selectedDate) ?: Date()
        } catch (_: Exception) {}
        cal
    }

    // Days for the 7-day strip
    val weekDays = remember(selectedDate) {
        val list = mutableListOf<Pair<String, Calendar>>()
        val tempCal = Calendar.getInstance().apply {
            time = currentCal.time
            add(Calendar.DAY_OF_MONTH, -3)
        }
        for (i in 0 until 7) {
            list.add(Pair(sdf.format(tempCal.time), tempCal.clone() as Calendar))
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    // Hourly Time Blocking: 12:00 AM (00:00) to 11:00 PM (23:00) - Full 24 Hours
    val hours = (0..23).map { String.format(Locale.getDefault(), "%02d:00", it) }

    fun formatHourDisplay(hourStr: String): String {
        val h = hourStr.split(":").getOrNull(0)?.toIntOrNull() ?: return hourStr
        return when {
            h == 0 -> "12 AM"
            h < 12 -> "$h AM"
            h == 12 -> "12 PM"
            else -> "${h - 12} PM"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Month Header & Date Navigation
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentCal.add(Calendar.DAY_OF_MONTH, -1)
                            viewModel.setSelectedDate(sdf.format(currentCal.time))
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displayFormat.format(currentCal.time),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dayHeaderFormat.format(currentCal.time),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            currentCal.add(Calendar.DAY_OF_MONTH, 1)
                            viewModel.setSelectedDate(sdf.format(currentCal.time))
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 7-day horizontal calendar strip
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    items(weekDays) { (dateStr, cal) ->
                        val isSelected = dateStr == selectedDate
                        val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
                        val dayOfMonth = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)
                        val hasTasks = allTasks.any { it.dueDate == dateStr }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { viewModel.setSelectedDate(dateStr) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = dayOfWeek,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dayOfMonth,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (hasTasks) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }

        // Time Blocking Action Banner
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Time-Blocked Schedule (${selectedDayTasks.size} tasks)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Surface(
                    onClick = onOpenAiPlanner,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "AI Optimize",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Hourly Timeline (06:00 to 22:00)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(hours) { hourStr ->
                val hourInt = hourStr.split(":")[0].toInt()
                val tasksInSlot = selectedDayTasks.filter {
                    val taskHour = it.dueTime?.split(":")?.getOrNull(0)?.toIntOrNull()
                    taskHour == hourInt
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Hour label
                    Column(
                        modifier = Modifier.width(62.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = formatHourDisplay(hourStr),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = hourStr,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // Slot content (Tasks or empty slot button)
                    if (tasksInSlot.isNotEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tasksInSlot.forEach { task ->
                                val cat = TaskCategory.fromString(task.category)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.toggleTaskCompletion(task)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(cat.colorHex).copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp, 28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(cat.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${task.sticker ?: ""} ${task.title}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${task.dueTime ?: hourStr} (${task.durationMinutes}m) • ${task.category}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (task.isCompleted) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty time block tap affordance
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable {
                                    prefilledTime = hourStr
                                    showAddTaskDialog = true
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Plan time slot",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Plan ${formatHourDisplay(hourStr)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        val categories by viewModel.allCategories.collectAsState()
        AddTaskBottomSheet(
            targetDate = selectedDate,
            initialTime = prefilledTime,
            availableCategories = categories,
            onDismiss = { showAddTaskDialog = false },
            onSaveTask = { title, desc, cat, priority, dueDate, dueTime, duration, isStarred, reminder, recurrence, subtasks, sticker ->
                viewModel.addTask(
                    title = title,
                    description = desc,
                    category = cat,
                    priority = priority,
                    dueDate = dueDate,
                    dueTime = dueTime ?: prefilledTime,
                    durationMinutes = duration,
                    isStarred = isStarred,
                    reminderEnabled = reminder,
                    recurrence = recurrence,
                    subtasks = subtasks,
                    sticker = sticker
                )
            }
        )
    }
}
