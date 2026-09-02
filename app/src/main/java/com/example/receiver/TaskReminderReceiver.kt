package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        when (action) {
            NotificationUtils.ACTION_REMINDER -> {
                val taskId = intent.getLongExtra(NotificationUtils.EXTRA_TASK_ID, 0L)
                val title = intent.getStringExtra(NotificationUtils.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val category = intent.getStringExtra(NotificationUtils.EXTRA_TASK_CATEGORY) ?: "General"
                NotificationUtils.showNotification(context, taskId, title, category, playSound = true)
            }

            NotificationUtils.ACTION_SLEEP_ALARM -> {
                val isWakeAlarm = intent.getBooleanExtra(NotificationUtils.EXTRA_IS_WAKE_ALARM, false)
                NotificationUtils.showSleepAlarmNotification(context, isWakeAlarm)
            }

            NotificationUtils.ACTION_MUTE_ALARM -> {
                NotificationUtils.stopAlarmSound()
                Toast.makeText(context, "Alarm sound muted 🔕", Toast.LENGTH_SHORT).show()
            }

            NotificationUtils.ACTION_STOP_ALARM -> {
                NotificationUtils.stopAlarmSound()
                val notifId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIF_ID, 0)
                if (notifId != 0) {
                    notifManager?.cancel(notifId)
                }
                Toast.makeText(context, "Alarm stopped ⏹", Toast.LENGTH_SHORT).show()
            }

            NotificationUtils.ACTION_SNOOZE_TASK -> {
                NotificationUtils.stopAlarmSound()
                val notifId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIF_ID, 0)
                if (notifId != 0) {
                    notifManager?.cancel(notifId)
                }
                val taskId = intent.getLongExtra(NotificationUtils.EXTRA_TASK_ID, 0L)
                val title = intent.getStringExtra(NotificationUtils.EXTRA_TASK_TITLE) ?: "Task"
                val category = intent.getStringExtra(NotificationUtils.EXTRA_TASK_CATEGORY) ?: "General"
                val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000L)
                NotificationUtils.scheduleTaskReminder(context, taskId, title, category, snoozeTime)
                Toast.makeText(context, "Task reminder snoozed for 10 minutes 💤", Toast.LENGTH_SHORT).show()
            }

            NotificationUtils.ACTION_SNOOZE_SLEEP -> {
                NotificationUtils.stopAlarmSound()
                val notifId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIF_ID, 9901)
                notifManager?.cancel(notifId)
                val isWakeAlarm = intent.getBooleanExtra(NotificationUtils.EXTRA_IS_WAKE_ALARM, true)
                NotificationUtils.scheduleSleepAlarmInMinutes(context, isWakeAlarm, 10)
                Toast.makeText(context, "Alarm snoozed for 10 minutes 💤", Toast.LENGTH_SHORT).show()
            }

            NotificationUtils.ACTION_COMPLETE_TASK -> {
                NotificationUtils.stopAlarmSound()
                val notifId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIF_ID, 0)
                if (notifId != 0) {
                    notifManager?.cancel(notifId)
                }
                val taskId = intent.getLongExtra(NotificationUtils.EXTRA_TASK_ID, 0L)
                if (taskId > 0L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getInstance(context)
                        db.taskDao().updateTaskCompletion(taskId, true, System.currentTimeMillis())
                    }
                }
                Toast.makeText(context, "✓ Task marked as Completed!", Toast.LENGTH_SHORT).show()
            }

            NotificationUtils.ACTION_INCOMPLETE_TASK -> {
                NotificationUtils.stopAlarmSound()
                val notifId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIF_ID, 0)
                if (notifId != 0) {
                    notifManager?.cancel(notifId)
                }
                val taskId = intent.getLongExtra(NotificationUtils.EXTRA_TASK_ID, 0L)
                if (taskId > 0L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getInstance(context)
                        db.taskDao().updateTaskCompletion(taskId, false, null)
                    }
                }
                Toast.makeText(context, "○ Task status: Incomplete (Pending)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

