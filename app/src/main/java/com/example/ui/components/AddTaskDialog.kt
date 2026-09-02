package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    initialTask: TaskItem? = null,
    targetDate: String,
    initialTime: String? = null,
    availableCategories: List<CategoryItem> = emptyList(),
    onDismiss: () -> Unit,
    onSaveTask: (
        title: String,
        desc: String,
        category: String,
        priority: Priority,
        dueDate: String,
        dueTime: String?,
        duration: Int,
        isStarred: Boolean,
        reminder: Boolean,
        recurrence: String,
        subtasks: List<SubTask>,
        sticker: String,
        isCompleted: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val categoryList = if (availableCategories.isNotEmpty()) {
        availableCategories
    } else {
        TaskCategory.entries.map {
            CategoryItem(name = it.label, icon = it.icon, colorHex = it.colorHex, isCustom = false)
        }
    }

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            initialTask?.category ?: categoryList.firstOrNull()?.name ?: TaskCategory.WORK.label
        )
    }
    var selectedPriority by remember {
        mutableStateOf(
            try { Priority.valueOf(initialTask?.priority ?: "MEDIUM") } catch (_: Exception) { Priority.MEDIUM }
        )
    }
    var dueDate by remember { mutableStateOf(initialTask?.dueDate ?: targetDate) }
    var dueTime by remember { mutableStateOf(initialTask?.dueTime ?: initialTime ?: "06:00") }
    var durationMinutes by remember { mutableIntStateOf(initialTask?.durationMinutes ?: 30) }
    var isStarred by remember { mutableStateOf(initialTask?.isStarred ?: false) }
    var isCompleted by remember { mutableStateOf(initialTask?.isCompleted ?: false) }
    var reminderEnabled by remember { mutableStateOf(initialTask?.reminderEnabled ?: false) }
    var recurrence by remember { mutableStateOf(initialTask?.recurrence ?: "NONE") }
    var selectedSticker by remember { mutableStateOf(initialTask?.sticker ?: "⭐️") }

    val subtasksList = remember {
        mutableStateListOf<SubTask>().apply {
            initialTask?.let { addAll(it.getSubtasks()) }
        }
    }
    var newSubtaskInput by remember { mutableStateOf("") }

    var isCategoryDropdownOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTask == null) "New Task" else "Edit Task",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { isStarred = !isStarred }) {
                    Icon(
                        imageVector = if (isStarred) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Star",
                        tint = if (isStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What would you like to do?") },
                placeholder = { Text("e.g., Finalize project report") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category & Priority
            Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownOpen,
                    onExpandedChange = { isCategoryDropdownOpen = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownOpen) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownOpen,
                        onDismissRequest = { isCategoryDropdownOpen = false }
                    ) {
                        categoryList.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${cat.icon}  ${cat.name}")
                                        if (cat.isCustom) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "(custom)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCategory = cat.name
                                    isCategoryDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Priority
            Text("Priority", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            PrioritySelector(
                selectedPriority = selectedPriority,
                onSelectPriority = { selectedPriority = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Schedule & Time Blocking
            Text("Date & Time Schedule", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Date and Time picker buttons side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Picker Button
                Surface(
                    onClick = {
                        val cal = Calendar.getInstance()
                        try {
                            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dueDate)
                            if (parsedDate != null) cal.time = parsedDate
                        } catch (_: Exception) {}
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                dueDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = dueDate, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                // Time Picker Button
                Surface(
                    onClick = {
                        val parts = dueTime.split(":")
                        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 6
                        val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                dueTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                            },
                            initialHour,
                            initialMin,
                            true
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = dueTime, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Time Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("06:00", "09:00", "12:00", "15:00", "18:00", "21:00").forEach { preset ->
                    FilterChip(
                        selected = dueTime == preset,
                        onClick = { dueTime = preset },
                        label = { Text(preset, fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Duration Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Duration:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val durations = listOf(15, 30, 45, 60, 90)
                durations.forEach { d ->
                    FilterChip(
                        selected = durationMinutes == d,
                        onClick = { durationMinutes = d },
                        label = { Text("${d}m", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reminder Notification Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Reminder",
                            tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Reminder Alarm", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Notify at scheduled due time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        modifier = Modifier.testTag("reminder_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Task Status Selector: Complete or Not
            Text("Task Status", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isCompleted,
                    onClick = { isCompleted = false },
                    label = { Text("○ Incomplete (Pending)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isCompleted,
                    onClick = { isCompleted = true },
                    label = { Text("✓ Completed") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aesthetic Sticker
            Text("Planner Sticker Badge", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            StickerPickerRow(
                selectedSticker = selectedSticker,
                onSelectSticker = { selectedSticker = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Checklist & Subtasks
            Text("Checklist / Subtasks (${subtasksList.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))

            subtasksList.forEachIndexed { index, sub ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(text = "• ${sub.title}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { subtasksList.removeAt(index) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove step", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskInput,
                    onValueChange = { newSubtaskInput = it },
                    placeholder = { Text("Add sub-task / checklist step...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                IconButton(
                    onClick = {
                        if (newSubtaskInput.isNotBlank()) {
                            subtasksList.add(SubTask(title = newSubtaskInput.trim()))
                            newSubtaskInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add subtask", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSaveTask(
                            title,
                            description,
                            selectedCategory,
                            selectedPriority,
                            dueDate,
                            dueTime,
                            durationMinutes,
                            isStarred,
                            reminderEnabled,
                            recurrence,
                            subtasksList.toList(),
                            selectedSticker,
                            isCompleted
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (initialTask == null) "Create Task" else "Save Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
