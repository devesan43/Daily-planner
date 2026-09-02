package com.example.ui.screens

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.receiver.NotificationUtils
import com.example.ui.theme.TodoPlannerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure activity to turn screen on and show on top of secure lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val taskId = intent.getLongExtra(NotificationUtils.EXTRA_TASK_ID, 0L)
        val taskTitle = intent.getStringExtra(NotificationUtils.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val category = intent.getStringExtra(NotificationUtils.EXTRA_TASK_CATEGORY) ?: "General"
        val isWakeAlarm = intent.getBooleanExtra(NotificationUtils.EXTRA_IS_WAKE_ALARM, false)
        val notifId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIF_ID, if (isWakeAlarm) 9901 else if (taskId > 0) taskId.toInt() else 9902)
        val isSleepHabit = intent.hasExtra(NotificationUtils.EXTRA_IS_WAKE_ALARM)

        setContent {
            TodoPlannerTheme {
                AlarmAlertScreen(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    category = category,
                    isSleepHabit = isSleepHabit,
                    isWakeAlarm = isWakeAlarm,
                    onMuteSound = {
                        NotificationUtils.stopAlarmSound()
                        Toast.makeText(this, "Alarm sound muted 🔕", Toast.LENGTH_SHORT).show()
                    },
                    onSelectStatus = { isCompleted ->
                        NotificationUtils.stopAlarmSound()
                        dismissNotification(notifId)
                        if (taskId > 0) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val db = AppDatabase.getInstance(applicationContext)
                                db.taskDao().updateTaskCompletion(
                                    id = taskId,
                                    isCompleted = isCompleted,
                                    completedAt = if (isCompleted) System.currentTimeMillis() else null
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@AlarmAlertActivity,
                                        if (isCompleted) "✓ Task status: Completed!" else "○ Task status: Incomplete (Pending)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                            }
                        } else {
                            finish()
                        }
                    },
                    onSnooze = {
                        NotificationUtils.stopAlarmSound()
                        dismissNotification(notifId)
                        val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000
                        if (taskId > 0) {
                            NotificationUtils.scheduleTaskReminder(
                                this,
                                taskId,
                                taskTitle,
                                category,
                                snoozeTime
                            )
                            Toast.makeText(this, "Task snoozed for 10 minutes 💤", Toast.LENGTH_SHORT).show()
                        } else {
                            NotificationUtils.scheduleSleepAlarmInMinutes(this, isWakeAlarm, 10)
                            Toast.makeText(this, "Alarm snoozed for 10 minutes 💤", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    },
                    onStop = {
                        NotificationUtils.stopAlarmSound()
                        dismissNotification(notifId)
                        Toast.makeText(this, "Alarm stopped ⏹", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onOpenApp = {
                        NotificationUtils.stopAlarmSound()
                        dismissNotification(notifId)
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun dismissNotification(id: Int) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(id)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationUtils.stopAlarmSound()
    }
}

@Composable
fun AlarmAlertScreen(
    taskId: Long,
    taskTitle: String,
    category: String,
    isSleepHabit: Boolean,
    isWakeAlarm: Boolean,
    onMuteSound: () -> Unit,
    onSelectStatus: (Boolean) -> Unit,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    onOpenApp: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<Boolean?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSleepHabit && isWakeAlarm) Color(0xFFF59E0B).copy(alpha = 0.2f)
                                else if (isSleepHabit) Color(0xFF6366F1).copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isSleepHabit && isWakeAlarm -> Icons.Default.WbSunny
                                isSleepHabit -> Icons.Default.Bedtime
                                else -> Icons.Default.Alarm
                            },
                            contentDescription = null,
                            tint = when {
                                isSleepHabit && isWakeAlarm -> Color(0xFFF59E0B)
                                isSleepHabit -> Color(0xFF6366F1)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Alert Title
                    Text(
                        text = when {
                            isSleepHabit && isWakeAlarm -> "🌅 Good Morning! Time to Wake Up"
                            isSleepHabit -> "🌙 Bedtime Habit Reminder"
                            else -> "⏰ Task Alarm & Reminder"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Task or Habit Content
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = taskTitle,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            if (!isSleepHabit && category.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Category: $category",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Sound status & Mute quick toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isMuted) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                            .clickable {
                                isMuted = !isMuted
                                if (isMuted) onMuteSound()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMuted) "Alarm Sound Muted 🔕" else "Alarm Ringing Now 🔊",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }

                        Text(
                            text = if (isMuted) "Muted" else "Tap to Mute",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // TASK STATUS SELECTION: Complete or Incomplete (Pending)
                    if (!isSleepHabit && taskId > 0) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Select Task Status:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Mark Complete Button
                                Button(
                                    onClick = {
                                        selectedStatus = true
                                        onSelectStatus(true)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981)
                                    )
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("✓ Complete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Mark Incomplete Button
                                OutlinedButton(
                                    onClick = {
                                        selectedStatus = false
                                        onSelectStatus(false)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("○ Incomplete", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // SNOOZE & STOP CONTROLS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSnooze,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Snooze (10m)", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onStop,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop Alarm", fontSize = 12.sp)
                        }
                    }

                    // OPEN APP BUTTON
                    TextButton(
                        onClick = onOpenApp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Daily Planner App", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
