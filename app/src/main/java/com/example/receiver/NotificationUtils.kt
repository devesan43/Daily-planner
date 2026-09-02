package com.example.receiver

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ui.screens.AlarmAlertActivity
import java.util.Calendar

object NotificationUtils {
    const val CHANNEL_ID = "todo_planner_reminders"
    const val CHANNEL_NAME = "To-Do List & Schedule Reminders"

    const val SLEEP_CHANNEL_ID = "sleep_habit_alarm_channel"
    const val SLEEP_CHANNEL_NAME = "Sleep Habit & Wake Alarms"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_CATEGORY = "extra_task_category"
    const val EXTRA_IS_WAKE_ALARM = "extra_is_wake_alarm"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    const val ACTION_REMINDER = "com.aistudio.todolist.ACTION_REMINDER"
    const val ACTION_SLEEP_ALARM = "com.aistudio.todolist.ACTION_SLEEP_ALARM"
    const val ACTION_MUTE_ALARM = "com.aistudio.todolist.ACTION_MUTE_ALARM"
    const val ACTION_STOP_ALARM = "com.aistudio.todolist.ACTION_STOP_ALARM"
    const val ACTION_SNOOZE_TASK = "com.aistudio.todolist.ACTION_SNOOZE_TASK"
    const val ACTION_SNOOZE_SLEEP = "com.aistudio.todolist.ACTION_SNOOZE_SLEEP"
    const val ACTION_COMPLETE_TASK = "com.aistudio.todolist.ACTION_COMPLETE_TASK"
    const val ACTION_INCOMPLETE_TASK = "com.aistudio.todolist.ACTION_INCOMPLETE_TASK"

    private var activeRingtone: Ringtone? = null

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // Task Reminders Channel (Heads up pop-up on lock screen)
            val taskChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pop-up reminders for scheduled tasks, daily planners, and routines"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(alarmSound, audioAttributes)
            }
            manager.createNotificationChannel(taskChannel)

            // Sleep & Wake Alarm Channel (High priority with alarm sound attributes and lock screen pop-up)
            val sleepChannel = NotificationChannel(
                SLEEP_CHANNEL_ID,
                SLEEP_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Audible alarm & lock screen notifications for bedtime and wake-up habits"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 250, 600, 250, 600)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(alarmSound, audioAttributes)
            }
            manager.createNotificationChannel(sleepChannel)
        }
    }

    fun scheduleTaskReminder(
        context: Context,
        taskId: Long,
        taskTitle: String,
        category: String,
        triggerTimeMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_TASK_CATEGORY, category)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, taskId.toInt(), intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleSleepAlarm(context: Context, isWakeAlarm: Boolean, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_SLEEP_ALARM
            putExtra(EXTRA_IS_WAKE_ALARM, isWakeAlarm)
        }

        val requestCode = if (isWakeAlarm) 9901 else 9902
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    fun scheduleSleepAlarmInMinutes(context: Context, isWakeAlarm: Boolean, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_SLEEP_ALARM
            putExtra(EXTRA_IS_WAKE_ALARM, isWakeAlarm)
        }
        val requestCode = if (isWakeAlarm) 9901 else 9902
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    fun cancelSleepAlarm(context: Context, isWakeAlarm: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_SLEEP_ALARM
        }
        val requestCode = if (isWakeAlarm) 9901 else 9902
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun playAlarmSound(context: Context) {
        try {
            stopAlarmSound()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            ringtone?.play()
            activeRingtone = ringtone
        } catch (_: Exception) {}
    }

    fun stopAlarmSound() {
        try {
            activeRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            activeRingtone = null
        } catch (_: Exception) {}
    }

    fun showNotification(context: Context, taskId: Long, title: String, category: String, playSound: Boolean = true) {
        createNotificationChannel(context)
        if (playSound) {
            playAlarmSound(context)
        }

        val notifId = taskId.toInt()

        // App Launch Intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Lock Screen Full-Screen Pop-up Intent (Over Keyguard & Heads-up)
        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_CATEGORY, category)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (taskId + 10000).toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Mark Complete
        val completeIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + 20000).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 10m
        val snoozeIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_CATEGORY, category)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + 30000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Mute Sound / Stop
        val stopIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + 40000).toInt(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ To-Do Reminder: $title")
            .setContentText("[$category] • Tap for full alarm or select status below")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "✓ Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "💤 Snooze (10m)", snoozePendingIntent)
            .addAction(android.R.drawable.ic_lock_silent_mode, "🔕 Mute / Stop", stopPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }

    fun showSleepAlarmNotification(context: Context, isWakeAlarm: Boolean) {
        createNotificationChannel(context)
        playAlarmSound(context)

        val notifId = if (isWakeAlarm) 9901 else 9902

        // App Launch Intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Lock Screen Full-Screen Pop-up Intent
        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_IS_WAKE_ALARM, isWakeAlarm)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notifId + 100,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Snooze 10m
        val snoozeIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_SLEEP
            putExtra(EXTRA_IS_WAKE_ALARM, isWakeAlarm)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 200,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Mute Sound
        val muteIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_MUTE_ALARM
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val mutePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 300,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Stop Alarm
        val stopIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 400,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isWakeAlarm) "🌅 Good Morning! Time to Wake Up" else "🌙 Bedtime Habit Reminder"
        val body = if (isWakeAlarm) "Time to rise and start your day!" else "Time to wind down for restful sleep!"

        val notification = NotificationCompat.Builder(context, SLEEP_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "💤 Snooze (10m)", snoozePendingIntent)
            .addAction(android.R.drawable.ic_lock_silent_mode, "🔕 Mute Sound", mutePendingIntent)
            .addAction(android.R.drawable.ic_delete, "⏹ Stop Alarm", stopPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }
}
