package com.uthtodo.dpgh.ui.screens

import com.uthtodo.dpgh.data.local.AppPreferences
import com.uthtodo.dpgh.data.local.SessionManager
import com.uthtodo.dpgh.data.model.AppNotification
import com.uthtodo.dpgh.data.model.Subtask
import com.uthtodo.dpgh.data.model.Task
import com.uthtodo.dpgh.data.model.buildNotificationsFromTasks
import com.uthtodo.dpgh.data.remote.FirebaseManager
import com.uthtodo.dpgh.data.repository.TaskRepository

import com.uthtodo.dpgh.service.ReminderScheduler
import com.uthtodo.dpgh.ui.components.AppBottomNavigationBar
import com.uthtodo.dpgh.ui.components.LifeIcon
import com.uthtodo.dpgh.ui.components.StudyIcon
import com.uthtodo.dpgh.util.formatRelativeDate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationScreen(
    onAddTaskClick: () -> Unit = {},
    onNavClick: (String) -> Unit = {}
) {
    val allTasks by TaskRepository.tasks.collectAsState()
    val settings by AppPreferences.settings.collectAsState()

    val notifications = buildNotificationsFromTasks(allTasks, settings.readNotificationIds)
    val unreadCount = notifications.count { !it.isRead }

    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Thông báo",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = if (unreadCount > 0) "$unreadCount thông báo mới" else "Đã đọc tất cả",
                        fontSize = 14.sp,
                        color = subTextColor
                    )
                }

                if (unreadCount > 0) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                AppPreferences.markNotificationsRead(notifications.map { it.id })
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = primaryColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "Đánh dấu đã đọc",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (notifications.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = cardColor
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có thông báo nào.",
                            fontSize = 15.sp,
                            color = subTextColor
                        )
                    }
                }
            } else {
                notifications.forEach { item ->
                    NotificationCard(
                        notification = item,
                        cardColor = cardColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        primaryColor = primaryColor,
                        onClick = {
                            if (!item.isRead) {
                                AppPreferences.markNotificationsRead(listOf(item.id))
                            }
                        }
                    )
                }
            }
        }

        // Bottom Navigation Bar
        AppBottomNavigationBar(
            currentRoute = "notifications",
            modifier = Modifier.align(Alignment.BottomCenter),
            onAddClick = onAddTaskClick,
            onNavClick = onNavClick
        )
    }
}

@Composable
fun NotificationCard(
    notification: AppNotification,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val iconBg = when (notification.type) {
        "reminder" -> primaryColor.copy(alpha = 0.15f)
        "overdue" -> Color(0xFFFFE5E5)
        "completed" -> Color(0xFFE6F4EA)
        else -> Color(0xFFF2F2F7)
    }
    val iconColor = when (notification.type) {
        "reminder" -> primaryColor
        "overdue" -> Color(0xFFE5484D)
        "completed" -> Color(0xFF137333)
        else -> subTextColor
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (!notification.isRead) 2.dp else 1.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (notification.type) {
                    "reminder" -> {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            val bellPath = Path().apply {
                                moveTo(size.width * 0.5f, size.height * 0.18f)
                                cubicTo(size.width * 0.3f, size.height * 0.18f, size.width * 0.25f, size.height * 0.5f, size.width * 0.2f, size.height * 0.7f)
                                lineTo(size.width * 0.8f, size.height * 0.7f)
                                cubicTo(size.width * 0.75f, size.height * 0.5f, size.width * 0.7f, size.height * 0.18f, size.width * 0.5f, size.height * 0.18f)
                            }
                            drawPath(bellPath, color = iconColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                    "overdue" -> {
                        Text(
                            text = "!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                    }
                    else -> {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.2f, size.height * 0.55f)
                                lineTo(size.width * 0.45f, size.height * 0.8f)
                                lineTo(size.width * 0.85f, size.height * 0.25f)
                            }
                            drawPath(path, color = iconColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                }
            }

            // Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 15.sp,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor
                    )

                    // Unread dot indicator
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(primaryColor, CircleShape)
                        )
                    }
                }

                Text(
                    text = notification.content,
                    fontSize = 13.sp,
                    fontWeight = if (!notification.isRead) FontWeight.Medium else FontWeight.Normal,
                    color = subTextColor,
                    lineHeight = 18.sp
                )

                Text(
                    text = notification.time,
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}


