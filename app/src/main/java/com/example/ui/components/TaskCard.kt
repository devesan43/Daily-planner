package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.Notifications
import com.example.data.model.CategoryItem
import com.example.data.model.Priority
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem

@Composable
fun TaskCard(
    task: TaskItem,
    categories: List<CategoryItem> = emptyList(),
    onToggleComplete: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleReminder: (() -> Unit)? = null,
    onTestReminder: (() -> Unit)? = null,
    onToggleSubTask: (String) -> Unit,
    onAddSubTask: (String) -> Unit,
    onBreakdownWithAi: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subtasks = remember(task.subtasksJson) { task.getSubtasks() }
    val completedSubCount = subtasks.count { it.isDone }
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var newStepInput by remember { mutableStateOf("") }
    var isAddingStep by remember { mutableStateOf(false) }

    // Resolve matching category info (from dynamic DB categories or enum fallback)
    val matchingDbCat = categories.firstOrNull {
        (task.categoryId != null && it.id == task.categoryId) || it.name.equals(task.category, ignoreCase = true)
    }
    val fallbackCat = TaskCategory.fromString(task.category)
    val catIcon = matchingDbCat?.icon ?: fallbackCat.icon
    val catLabel = matchingDbCat?.name ?: fallbackCat.label
    val catColorHex = matchingDbCat?.colorHex ?: fallbackCat.colorHex

    val priorityEnum = try { Priority.valueOf(task.priority) } catch (_: Exception) { Priority.NONE }
    val priorityColor = Color(priorityEnum.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .animateContentSize()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Custom Checkbox
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onToggleComplete() }
                        .testTag("task_check_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!task.sticker.isNullOrBlank()) {
                            Text(text = task.sticker, fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                        }
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (task.isCompleted)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }

                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isExpanded) 10 else 2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Chips row (category, priority, status, time)
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(catColorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$catIcon $catLabel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(catColorHex),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Status Pill (Complete / Incomplete)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (task.isCompleted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onToggleComplete() }
                        ) {
                            Text(
                                text = if (task.isCompleted) "✓ Done" else "○ Pending",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (task.isCompleted) Color(0xFF10B981) else Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Priority Pill
                        if (priorityEnum != Priority.NONE) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = priorityColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = priorityEnum.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = priorityColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Time / Reminder indicator
                        if (!task.dueTime.isNullOrBlank() || task.reminderEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (task.reminderEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onToggleReminder?.invoke() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    if (task.reminderEnabled) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                                    contentDescription = "Reminder status",
                                    tint = if (task.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (!task.dueTime.isNullOrBlank()) "${task.dueTime} (${task.durationMinutes}m)" else if (task.reminderEnabled) "Reminder on" else "No time",
                                    fontSize = 11.sp,
                                    color = if (task.reminderEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (task.reminderEnabled) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Star & More Menu
                IconButton(onClick = onToggleStar, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (task.isStarred) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Star Task",
                        tint = if (task.isStarred) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (task.isCompleted) "Mark as Incomplete (Pending) ○" else "Mark as Complete ✓") },
                            onClick = {
                                showMenu = false
                                onToggleComplete()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (task.isCompleted) Color(0xFFD97706) else Color(0xFF10B981)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("AI Smart Breakdown 🤖") },
                            onClick = {
                                showMenu = false
                                onBreakdownWithAi()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                        if (onToggleReminder != null) {
                            DropdownMenuItem(
                                text = { Text(if (task.reminderEnabled) "Disable Reminder 🔕" else "Enable Reminder ⏰") },
                                onClick = {
                                    showMenu = false
                                    onToggleReminder()
                                },
                                leadingIcon = {
                                    Icon(
                                        if (task.reminderEnabled) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                        if (onTestReminder != null) {
                            DropdownMenuItem(
                                text = { Text("Send Notification Now 🔔") },
                                onClick = {
                                    showMenu = false
                                    onTestReminder()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Edit Task") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            // Subtask progress indicator
            if (subtasks.isNotEmpty()) {
                val progress = completedSubCount.toFloat() / subtasks.size.toFloat()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Checklist ($completedSubCount/${subtasks.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle subtasks",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }

            // Expanded Subtask Checklist
            AnimatedVisibility(visible = isExpanded || (subtasks.isNotEmpty() && !task.isCompleted && isAddingStep)) {
                Column(modifier = Modifier.padding(top = 8.dp, start = 8.dp)) {
                    subtasks.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubTask(sub.id) }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = sub.isDone,
                                onCheckedChange = { onToggleSubTask(sub.id) },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                fontSize = 13.sp,
                                textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (sub.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Inline Add Step
                    if (isAddingStep) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = newStepInput,
                                onValueChange = { newStepInput = it },
                                placeholder = { Text("New checklist step...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (newStepInput.isNotBlank()) {
                                        onAddSubTask(newStepInput)
                                        newStepInput = ""
                                        isAddingStep = false
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    } else {
                        Text(
                            text = "+ Add step",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { isAddingStep = true }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
