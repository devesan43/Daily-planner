package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyRoutineItem
import com.example.data.model.HabitItem
import com.example.ui.viewmodel.FocusMode
import com.example.ui.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusAdhdScreen(viewModel: PlannerViewModel) {
    val routines by viewModel.allRoutines.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val focusMode by viewModel.focusMode.collectAsState()
    val remainingSeconds by viewModel.timerSecondsRemaining.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val focusMinutesLogged by viewModel.focusMinutesLoggedToday.collectAsState()
    val sleepSettings by viewModel.sleepSettings.collectAsState()
    val sleepRecords by viewModel.sleepRecords.collectAsState()

    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showLogSleepDialog by remember { mutableStateOf(false) }
    var isAlarmSoundTesting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val totalDurationSeconds = focusMode.durationMinutes * 60
    val progress = 1f - (remainingSeconds.toFloat() / totalDurationSeconds.toFloat())
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "timer_progress"
    )

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    // Last 5 days for habit grid
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val lastDays = remember {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -4)
        for (i in 0 until 5) {
            list.add(Pair(sdf.format(cal.time), dayNameFormat.format(cal.time)))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // ---------------------------------------------------------------------
        // 1. POMODORO FOCUS TIMER CARD
        // ---------------------------------------------------------------------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_focus_timer"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🧠 Focus & Flow Timer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "⚡️ $focusMinutesLogged min today",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FocusMode.entries.forEach { mode ->
                        FilterChip(
                            selected = focusMode == mode,
                            onClick = { viewModel.setFocusMode(mode) },
                            label = { Text("${mode.durationMinutes}m ${mode.label}", fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Circular Progress Timer Display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(170.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(170.dp),
                        strokeWidth = 10.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isRunning) "Stay Focused!" else "Ready",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timer Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .width(130.dp)
                            .testTag("btn_timer_toggle"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRunning) "Pause" else "Start", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---------------------------------------------------------------------
        // 2. ADHD VISUAL ROUTINE PLANNER
        // ---------------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("☀️ ADHD Visual Routines", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Step-by-step routines to eliminate executive dysfunction", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showAddRoutineDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Routine", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val groupedRoutines = routines.groupBy { it.timeOfDay }
        val routineSections = listOf("MORNING" to "🌅 Morning Momentum", "AFTERNOON" to "☀️ Midday Reset", "EVENING" to "🌙 Evening Wind-Down")

        routineSections.forEach { (timeOfDay, sectionTitle) ->
            val sectionItems = groupedRoutines[timeOfDay] ?: emptyList()
            if (sectionItems.isNotEmpty()) {
                Text(
                    text = sectionTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        sectionItems.forEach { routine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleRoutine(routine) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (routine.isCompleted) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (routine.isCompleted) {
                                        Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(text = routine.iconEmoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = routine.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (routine.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${routine.targetTime} • 🔥 ${routine.streakDays} day streak",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteRoutine(routine) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------------------------------------------------------------
        // 3. HABIT TRACKER GRID
        // ---------------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("📈 Daily Habit Tracker", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Build consistency 1 day at a time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showAddHabitDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header row of days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Habit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    lastDays.forEach { (_, dayName) ->
                        Text(
                            text = dayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(34.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                habits.forEach { habit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = habit.icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = habit.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("🔥 ${habit.streak}d streak", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Day checkboxes
                        lastDays.forEach { (dateStr, _) ->
                            val isDone = habit.isCompletedOn(dateStr)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isDone) Color(habit.colorHex)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.toggleHabit(habit, dateStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // 4. SLEEP TIME MONITORING & HABIT ALARMS
        // ---------------------------------------------------------------------
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sleep Time Monitoring & Alarms", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("Habit reminder alarms with sound & tracking", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = { showLogSleepDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ Log Sleep", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Bedtime and Wake-up target boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bedtime selector
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val parts = sleepSettings.targetBedtime.split(":")
                                val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 22
                                val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 30
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        val newBedtime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                        viewModel.updateSleepSchedule(
                                            bedtime = newBedtime,
                                            wakeTime = sleepSettings.targetWakeTime,
                                            bedtimeReminder = sleepSettings.bedtimeReminderEnabled,
                                            wakeAlarm = sleepSettings.wakeAlarmEnabled,
                                            soundAlarm = sleepSettings.soundAlarmEnabled
                                        )
                                    },
                                    initialH,
                                    initialM,
                                    true
                                ).show()
                            },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Target Bedtime", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(sleepSettings.targetBedtime, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Tap to edit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Wake-up selector
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val parts = sleepSettings.targetWakeTime.split(":")
                                val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 6
                                val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 30
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        val newWakeTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                        viewModel.updateSleepSchedule(
                                            bedtime = sleepSettings.targetBedtime,
                                            wakeTime = newWakeTime,
                                            bedtimeReminder = sleepSettings.bedtimeReminderEnabled,
                                            wakeAlarm = sleepSettings.wakeAlarmEnabled,
                                            soundAlarm = sleepSettings.soundAlarmEnabled
                                        )
                                    },
                                    initialH,
                                    initialM,
                                    true
                                ).show()
                            },
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Target Wake-up", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(sleepSettings.targetWakeTime, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("Tap to edit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bedtime reminder alarm toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Bedtime Reminder Alarm", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Alerts you at ${sleepSettings.targetBedtime} to start winding down", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = sleepSettings.bedtimeReminderEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateSleepSchedule(
                                bedtime = sleepSettings.targetBedtime,
                                wakeTime = sleepSettings.targetWakeTime,
                                bedtimeReminder = enabled,
                                wakeAlarm = sleepSettings.wakeAlarmEnabled,
                                soundAlarm = sleepSettings.soundAlarmEnabled
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Wake-up Alarm Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Wake-up Alarm & Notification", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Rings at ${sleepSettings.targetWakeTime} to build morning habit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = sleepSettings.wakeAlarmEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateSleepSchedule(
                                bedtime = sleepSettings.targetBedtime,
                                wakeTime = sleepSettings.targetWakeTime,
                                bedtimeReminder = sleepSettings.bedtimeReminderEnabled,
                                wakeAlarm = enabled,
                                soundAlarm = sleepSettings.soundAlarmEnabled
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sound Alarm Audio Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (sleepSettings.soundAlarmEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Audible Sound Alarm", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Play ringtone sound on habit reminder alarm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = sleepSettings.soundAlarmEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateSleepSchedule(
                                bedtime = sleepSettings.targetBedtime,
                                wakeTime = sleepSettings.targetWakeTime,
                                bedtimeReminder = sleepSettings.bedtimeReminderEnabled,
                                wakeAlarm = sleepSettings.wakeAlarmEnabled,
                                soundAlarm = enabled
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test sound alarm buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isAlarmSoundTesting = true
                            viewModel.testSleepAlarmSound()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Alarm Sound", fontSize = 12.sp)
                    }

                    if (isAlarmSoundTesting) {
                        Button(
                            onClick = {
                                isAlarmSoundTesting = false
                                viewModel.stopSleepAlarmSound()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("⏹ Stop Alarm", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recent Sleep Logs
        Text("💤 Recent Sleep Tracking Log", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))

        if (sleepRecords.isEmpty()) {
            Text("No sleep sessions recorded yet. Tap '+ Log Sleep' to log today's rest!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sleepRecords.take(5).forEach { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌙", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("${rec.bedtime} → ${rec.wakeTime}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("${rec.date} • ${rec.notes.ifBlank { "Sleep tracked" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "${String.format(Locale.getDefault(), "%.1f", rec.durationHours)} hrs (${rec.quality})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRoutineDialog) {
        var rTitle by remember { mutableStateOf("") }
        var rTimeOfDay by remember { mutableStateOf("MORNING") }
        var rTargetTime by remember { mutableStateOf("08:00") }
        var rIcon by remember { mutableStateOf("☀️") }

        AlertDialog(
            onDismissRequest = { showAddRoutineDialog = false },
            title = { Text("New Visual Routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rTitle,
                        onValueChange = { rTitle = it },
                        label = { Text("Routine Name") },
                        placeholder = { Text("e.g. 500ml Water + Sunlight") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rTargetTime,
                        onValueChange = { rTargetTime = it },
                        label = { Text("Target Time") },
                        placeholder = { Text("08:00") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MORNING", "AFTERNOON", "EVENING").forEach { tod ->
                            FilterChip(
                                selected = rTimeOfDay == tod,
                                onClick = { rTimeOfDay = tod },
                                label = { Text(tod.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rTitle.isNotBlank()) {
                            val emoji = when (rTimeOfDay) {
                                "MORNING" -> "☀️"
                                "AFTERNOON" -> "⚡️"
                                else -> "🌙"
                            }
                            viewModel.addRoutine(rTitle, rTimeOfDay, rTargetTime, emoji)
                            showAddRoutineDialog = false
                        }
                    }
                ) {
                    Text("Add Routine")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoutineDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Habit Dialog
    if (showAddHabitDialog) {
        var hName by remember { mutableStateOf("") }
        var hCategory by remember { mutableStateOf("Health") }
        var hIcon by remember { mutableStateOf("💧") }

        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text("New Daily Habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hName,
                        onValueChange = { hName = it },
                        label = { Text("Habit Name") },
                        placeholder = { Text("e.g. Read 10 Pages") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("💧 Water", "📖 Read", "🧘 Meditate", "🏃 Exercise", "🥑 Eat Clean").forEach { iconItem ->
                            val parts = iconItem.split(" ")
                            FilterChip(
                                selected = hIcon == parts[0],
                                onClick = {
                                    hIcon = parts[0]
                                    hName = if (hName.isBlank()) parts[1] else hName
                                },
                                label = { Text(iconItem, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hName.isNotBlank()) {
                            viewModel.addHabit(hName, hCategory, hIcon, 0xFF6366F1)
                            showAddHabitDialog = false
                        }
                    }
                ) {
                    Text("Save Habit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Log Sleep Dialog
    if (showLogSleepDialog) {
        var logBedtime by remember { mutableStateOf(sleepSettings.targetBedtime) }
        var logWakeTime by remember { mutableStateOf(sleepSettings.targetWakeTime) }
        var logQuality by remember { mutableStateOf("Restful") }
        var logNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLogSleepDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌙", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Sleep Session")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Record your bedtime & wake-up time to track recovery habit progress:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                val parts = logBedtime.split(":")
                                TimePickerDialog(
                                    context,
                                    { _, h, m -> logBedtime = String.format(Locale.getDefault(), "%02d:%02d", h, m) },
                                    parts.getOrNull(0)?.toIntOrNull() ?: 22,
                                    parts.getOrNull(1)?.toIntOrNull() ?: 30,
                                    true
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Bedtime", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(logBedtime, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            onClick = {
                                val parts = logWakeTime.split(":")
                                TimePickerDialog(
                                    context,
                                    { _, h, m -> logWakeTime = String.format(Locale.getDefault(), "%02d:%02d", h, m) },
                                    parts.getOrNull(0)?.toIntOrNull() ?: 6,
                                    parts.getOrNull(1)?.toIntOrNull() ?: 30,
                                    true
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Wake Time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(logWakeTime, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Sleep Quality:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Restful", "Good", "Fair", "Tired").forEach { q ->
                            FilterChip(
                                selected = logQuality == q,
                                onClick = { logQuality = q },
                                label = { Text(q, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = logNotes,
                        onValueChange = { logNotes = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("e.g. Slept peacefully without waking up") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logSleepSession(
                            bedtime = logBedtime,
                            wakeTime = logWakeTime,
                            quality = logQuality,
                            notes = logNotes
                        )
                        showLogSleepDialog = false
                    }
                ) {
                    Text("Save Sleep Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogSleepDialog = false }) { Text("Cancel") }
            }
        )
    }
}
