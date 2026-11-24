package com.uthtodo.dpgh.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppNotification(
    val id: Int,
    val type: String, // "reminder" | "overdue" | "completed"
    val title: String,
    val content: String,
    val time: String,
    val isRead: Boolean = false
)

fun buildNotificationsFromTasks(tasks: List<Task>, readIds: Set<Int>): List<AppNotification> {
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val notifications = mutableListOf<AppNotification>()

    tasks.forEach { task ->
        when {
            !task.done && task.date.isNotBlank() && task.date < todayStr -> {
                val notifId = task.id * 10 + 1
                notifications.add(
                    AppNotification(
                        id = notifId,
                        type = "overdue",
                        title = "Việc quá hạn chưa xong",
                        content = "Bạn có việc từ ${task.date} chưa hoàn thành: ${task.title}.",
                        time = task.time.ifBlank { "Hôm qua" },
                        isRead = notifId in readIds
                    )
                )
            }
            !task.done && task.remind -> {
                val notifId = task.id * 10 + 2
                val categoryText = task.categoryLabel.lowercase()
                notifications.add(
                    AppNotification(
                        id = notifId,
                        type = "reminder",
                        title = "Nhắc nhở $categoryText",
                        content = "Sắp đến giờ: ${task.title} (${task.time.ifBlank { "Hôm nay" }}).",
                        time = task.time.ifBlank { "Hôm nay" },
                        isRead = notifId in readIds
                    )
                )
            }
            task.done -> {
                val notifId = task.id * 10 + 3
                notifications.add(
                    AppNotification(
                        id = notifId,
                        type = "completed",
                        title = "Hoàn thành xuất sắc",
                        content = "Tuyệt vời! Bạn đã hoàn thành việc: ${task.title}.",
                        time = task.time.ifBlank { "Đã xong" },
                        isRead = notifId in readIds
                    )
                )
            }
        }
    }
    return notifications
}
