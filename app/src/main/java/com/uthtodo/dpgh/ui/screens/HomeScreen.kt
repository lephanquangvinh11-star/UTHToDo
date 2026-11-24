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

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(
    onAvatarClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onTaskClick: (Int) -> Unit = {},
    onAddTaskClick: () -> Unit = {},
    onNavClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val session by SessionManager.session.collectAsState()
    val allTasks by TaskRepository.tasks.collectAsState()
    val isLoading by TaskRepository.isLoading.collectAsState()
    val errorMessage by TaskRepository.errorMessage.collectAsState()
    val settings by AppPreferences.settings.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(Unit) {
        if (allTasks.isEmpty()) {
            TaskRepository.refreshTasks()
        }
    }

    // Date computation
    val calendar = Calendar.getInstance()
    val dayOfWeekStr = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "Chủ Nhật"
        Calendar.MONDAY -> "Thứ Hai"
        Calendar.TUESDAY -> "Thứ Ba"
        Calendar.WEDNESDAY -> "Thứ Tư"
        Calendar.THURSDAY -> "Thứ Năm"
        Calendar.FRIDAY -> "Thứ Sáu"
        Calendar.SATURDAY -> "Thứ Bảy"
        else -> "Hôm nay"
    }
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val monthOfYear = calendar.get(Calendar.MONTH) + 1
    val formattedDate = "$dayOfWeekStr, $dayOfMonth tháng $monthOfYear"

    val todayDateStr = TaskRepository.getTodayDateString()
    val todayTasks = allTasks.filter {
        it.date == todayDateStr || it.date.isBlank()
    }.sortedBy { it.time }

    val hasUnreadNotifications = buildNotificationsFromTasks(allTasks, settings.readNotificationIds)
        .any { !it.isRead }

    val todayTotal = todayTasks.size
    val todayCompleted = todayTasks.count { it.done }
    val progressPercent = if (todayTotal > 0) (todayCompleted * 100) / todayTotal else 0

    val motivationText = when {
        progressPercent == 100 -> "Xong hết rồi, nghỉ ngơi thôi!"
        progressPercent >= 50 -> "Đã hơn nửa chặng, cố lên!"
        else -> "Bắt đầu từ việc nhỏ nhất nhé."
    }

    // Category remaining (count of unfinished tasks for all dates)
    val remainingStudy = allTasks.count { it.category == "study" && !it.done }
    val remainingLife = allTasks.count { it.category == "life" && !it.done }

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Section
            HomeHeaderSection(
                avatarInitials = session.avatarInitials,
                greeting = if (session.isGuest) "Chào bạn!" else "Chào ${session.displayName}!",
                dateText = formattedDate,
                hasUnreadNotifications = hasUnreadNotifications,
                onAvatarClick = onAvatarClick,
                onNotificationClick = onNotificationClick
            )

            // Progress Card
            HomeProgressCard(
                completedCount = todayCompleted,
                totalCount = todayTotal,
                progressPercent = progressPercent,
                motivationText = motivationText
            )

            // Category Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryCard(
                    modifier = Modifier.weight(1f),
                    title = "Học tập",
                    subtitle = "$remainingStudy việc còn lại",
                    iconType = "study",
                    badgeColor = Color(0xFFE6EDFF),
                    iconColor = Color(0xFF1F4FD1),
                    onClick = { onCategoryClick("study") }
                )

                CategoryCard(
                    modifier = Modifier.weight(1f),
                    title = "Cuộc sống",
                    subtitle = "$remainingLife việc còn lại",
                    iconType = "life",
                    badgeColor = Color(0xFFFFEBD9),
                    iconColor = Color(0xFFA24A08),
                    onClick = { onCategoryClick("life") }
                )
            }

            // Tasks Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Việc hôm nay",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Xem tất cả",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onViewAllClick() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }

                if (isLoading && allTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2F5BEA), strokeWidth = 2.5.dp)
                    }
                } else if (errorMessage != null && allTasks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = errorMessage ?: "Lỗi tải dữ liệu", color = Color(0xFFE5484D), fontSize = 14.sp)
                        Button(
                            onClick = { coroutineScope.launch { TaskRepository.refreshTasks() } },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F5BEA))
                        ) {
                            Text("Thử lại", color = Color.White)
                        }
                    }
                } else if (todayTasks.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Hôm nay chưa có việc nào.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "Bấm + để thêm nhé.",
                                fontSize = 13.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                } else {
                    todayTasks.forEach { task ->
                        HomeTaskCard(
                            task = task,
                            onToggle = {
                                coroutineScope.launch {
                                    val result = TaskRepository.toggleTaskDone(task.id)
                                    if (result.isSuccess) {
                                        val isNowDone = result.getOrNull()?.done ?: !task.done
                                        if (isNowDone) {
                                            Toast.makeText(context, "Tuyệt, đã xong một việc!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar
        AppBottomNavigationBar(
            currentRoute = "home",
            modifier = Modifier.align(Alignment.BottomCenter),
            onAddClick = onAddTaskClick,
            onNavClick = onNavClick
        )
    }
}

@Composable
private fun HomeHeaderSection(
    avatarInitials: String,
    greeting: String,
    dateText: String,
    hasUnreadNotifications: Boolean,
    onAvatarClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6EDFF), CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitials,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F4FD1)
                )
            }

            // Date & Greeting
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = dateText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = greeting,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp
                )
            }
        }

        // Notification Bell Button with Red Badge Dot
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { onNotificationClick() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val bellPath = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.15f)
                        cubicTo(
                            size.width * 0.3f, size.height * 0.15f,
                            size.width * 0.25f, size.height * 0.45f,
                            size.width * 0.2f, size.height * 0.65f
                        )
                        lineTo(size.width * 0.8f, size.height * 0.65f)
                        cubicTo(
                            size.width * 0.75f, size.height * 0.45f,
                            size.width * 0.7f, size.height * 0.15f,
                            size.width * 0.5f, size.height * 0.15f
                        )
                    }
                    drawPath(
                        path = bellPath,
                        color = Color(0xFF1C1C1E),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFF1C1C1E),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.42f, size.height * 0.72f),
                        size = Size(size.width * 0.16f, size.height * 0.16f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-10).dp, y = 10.dp)
                            .background(Color(0xFFE5484D), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeProgressCard(
    completedCount: Int,
    totalCount: Int,
    progressPercent: Int,
    motivationText: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(22.dp), spotColor = primaryColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(22.dp),
        color = primaryColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tiến độ hôm nay",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = "$completedCount/$totalCount việc",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = motivationText,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 7.dp.toPx()
                    val radius = (size.minDimension - strokeW) / 2
                    val centerPt = Offset(size.width / 2, size.height / 2)

                    drawCircle(
                        color = Color.White.copy(alpha = 0.28f),
                        radius = radius,
                        center = centerPt,
                        style = Stroke(width = strokeW)
                    )

                    val sweep = (progressPercent / 100f) * 360f
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(centerPt.x - radius, centerPt.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$progressPercent",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    iconType: String,
    badgeColor: Color,
    iconColor: Color,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(badgeColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (iconType == "study") {
                    StudyIcon(color = iconColor)
                } else {
                    LifeIcon(color = iconColor)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HomeTaskCard(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit = {}
) {
    val isStudy = task.category.lowercase() == "study"
    val chipBg = if (isStudy) Color(0xFFE6EDFF) else Color(0xFFFFEBD9)
    val chipText = if (isStudy) Color(0xFF1F4FD1) else Color(0xFFA24A08)
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.done) primaryColor else Color.Transparent,
                        CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (task.done) primaryColor else Color(0xFFC7C7CC),
                        shape = CircleShape
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (task.done) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.2f, size.height * 0.55f)
                            lineTo(size.width * 0.45f, size.height * 0.8f)
                            lineTo(size.width * 0.85f, size.height * 0.25f)
                        }
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // Task Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.done) Color(0xFF8E8E93) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(chipBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.categoryLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = chipText
                        )
                    }

                    if (task.time.isNotBlank()) {
                        Text(
                            text = task.time,
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }
    }
}


