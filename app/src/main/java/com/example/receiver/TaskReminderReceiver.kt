package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationUtils.EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(NotificationUtils.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val category = intent.getStringExtra(NotificationUtils.EXTRA_TASK_CATEGORY) ?: "General"

        NotificationUtils.showNotification(context, taskId, title, category)
    }
}
