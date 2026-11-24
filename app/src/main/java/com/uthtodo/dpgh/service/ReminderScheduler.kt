package com.uthtodo.dpgh.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.uthtodo.dpgh.data.local.AppPreferences
import com.uthtodo.dpgh.data.model.Task
import java.text.SimpleDateFormat
import java.util.Locale

object ReminderScheduler {
    fun syncAllTaskReminders(context: Context, tasks: List<Task>) {
        val remindersEnabled = AppPreferences.settings.value.enableReminders
        tasks.forEach { task ->
            if (remindersEnabled && task.remind && !task.done) {
                scheduleTaskReminder(context, task)
            } else {
                cancelTaskReminder(context, task.id)
            }
        }
    }

    fun scheduleTaskReminder(context: Context, task: Task) {
        if (task.date.isBlank() || task.time.isBlank()) return
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dueDate = sdf.parse("${task.date} ${task.time}") ?: return
            val triggerAtMillis = dueDate.time - 15 * 60 * 1000L
            if (triggerAtMillis <= System.currentTimeMillis()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                putExtra("task_id", task.id)
                putExtra("title", "Sắp đến giờ: ${task.title}")
                putExtra("body", "${task.categoryLabel} • Hạn lúc ${task.time}")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.w("ReminderScheduler", "Failed to schedule reminder for task ${task.id}: ${e.message}")
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (_: Exception) {
        }
    }
}
