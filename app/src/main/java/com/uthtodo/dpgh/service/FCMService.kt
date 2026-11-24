package com.uthtodo.dpgh.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.uthtodo.dpgh.MainActivity
import com.uthtodo.dpgh.R
import com.uthtodo.dpgh.data.local.AppPreferences

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMService", "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "UTH Todo Nhắc Nhở"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Bạn có công việc cần hoàn thành hôm nay!"

        showNotification(this, title, body)
    }

    companion object {
        const val CHANNEL_ID = "uth_todo_reminders"
        const val SILENT_CHANNEL_ID = "uth_todo_reminders_silent"

        fun showNotification(context: Context, title: String, message: String) {
            val settings = AppPreferences.settings.value
            val useSound = settings.enableSound
            val activeChannelId = if (useSound) CHANNEL_ID else SILENT_CHANNEL_ID

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val soundChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở công việc",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Kênh thông báo nhắc nhở bài tập và công việc UTH Todo"
                    enableVibration(true)
                }
                val silentChannel = NotificationChannel(
                    SILENT_CHANNEL_ID,
                    "Nhắc nhở công việc (Im lặng)",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Kênh thông báo nhắc nhở không phát âm thanh"
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(soundChannel)
                notificationManager.createNotificationChannel(silentChannel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, activeChannelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(if (useSound) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (useSound) {
                builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            } else {
                builder.setSilent(true)
            }

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        }
    }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!AppPreferences.settings.value.enableReminders) return
        val title = intent.getStringExtra("title") ?: "Nhắc việc UTH Todo"
        val body = intent.getStringExtra("body") ?: "Sắp đến hạn công việc của bạn!"
        FCMService.showNotification(context, title, body)
    }
}

class TestNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "UTH Todo Nhắc Nhở"
        val message = intent.getStringExtra("body") ?: "Bạn có bài tập cần hoàn thành hôm nay!"
        FCMService.showNotification(context, title, message)
    }
}
