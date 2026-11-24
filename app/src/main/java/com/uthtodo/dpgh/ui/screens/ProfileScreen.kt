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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onLinkGoogleClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val session by SessionManager.session.collectAsState()
    val allTasks by TaskRepository.tasks.collectAsState()
    val errorMessage by TaskRepository.errorMessage.collectAsState()
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    val totalTasks = allTasks.size
    val completedCount = allTasks.count { it.done }
    val pendingCount = allTasks.count { !it.done }
    val completionRate = if (totalTasks > 0) (completedCount * 100) / totalTasks else 0

    val statDoneStr = if (errorMessage != null && allTasks.isEmpty()) "–" else "$completedCount"
    val statPendingStr = if (errorMessage != null && allTasks.isEmpty()) "–" else "$pendingCount"
    val statRateStr = if (errorMessage != null && allTasks.isEmpty()) "–" else "$completionRate%"

    val bgColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top App Bar
            ProfileTopBar(onBackClick = onBackClick, primaryColor = primaryColor, textColor = textColor)

            // User Info Section
            ProfileUserInfo(
                displayName = session.displayName,
                email = session.email,
                isGuest = session.isGuest,
                avatarInitials = session.avatarInitials,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                primaryColor = primaryColor,
                onLinkGoogleClick = onLinkGoogleClick
            )

            // 3 Quick Stat Cards
            ProfileStatsRow(
                doneCount = statDoneStr,
                pendingCount = statPendingStr,
                rate = statRateStr,
                cardColor = cardColor,
                subTextColor = subTextColor,
                primaryColor = primaryColor
            )

            // Weekly Activity Chart Card (7 bars Mon - Sun computed from real tasks)
            WeeklyChartCard(
                tasks = allTasks,
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = subTextColor,
                primaryColor = primaryColor
            )

            // Logout Button
            ProfileLogoutButton(
                onClick = {
                    SessionManager.logout()
                    Toast.makeText(context, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                    onLogoutClick()
                }
            )

            // Delete Account & Data Button (Required by Google Play Account Deletion Policy)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !isDeletingAccount) {
                        showDeleteAccountDialog = true
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDeletingAccount) "Đang xóa tài khoản & dữ liệu…" else "Xóa tài khoản & toàn bộ dữ liệu",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = subTextColor
                    )
                }
            }
        }

        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
                containerColor = cardColor,
                title = {
                    Text(
                        text = "Xóa vĩnh viễn tài khoản?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                text = {
                    Text(
                        text = "Toàn bộ công việc, dữ liệu đám mây trên Firestore và tài khoản hiện tại của bạn sẽ bị xóa vĩnh viễn và không thể khôi phục.",
                        fontSize = 14.sp,
                        color = subTextColor,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        enabled = !isDeletingAccount,
                        onClick = {
                            isDeletingAccount = true
                            coroutineScope.launch {
                                SessionManager.deleteAccountAndData()
                                isDeletingAccount = false
                                showDeleteAccountDialog = false
                                Toast.makeText(context, "Đã xóa tài khoản và toàn bộ dữ liệu", Toast.LENGTH_SHORT).show()
                                onLogoutClick()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D))
                    ) {
                        Text("Xóa vĩnh viễn", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isDeletingAccount,
                        onClick = { showDeleteAccountDialog = false }
                    ) {
                        Text("Hủy", color = subTextColor)
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileTopBar(onBackClick: () -> Unit, primaryColor: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBackClick() }
                .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.65f, size.height * 0.15f)
                    lineTo(size.width * 0.3f, size.height * 0.5f)
                    lineTo(size.width * 0.65f, size.height * 0.85f)
                }
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            Text(
                text = "Quay lại",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = primaryColor
            )
        }

        Text(
            text = "Hồ sơ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun ProfileUserInfo(
    displayName: String,
    email: String,
    isGuest: Boolean,
    avatarInitials: String,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    primaryColor: Color,
    onLinkGoogleClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitials,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }

            // Name
            Text(
                text = displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            // Email / Status
            if (!isGuest && email.isNotBlank()) {
                Text(
                    text = email,
                    fontSize = 14.sp,
                    color = subTextColor
                )
            }

            // Account status badge
            if (!isGuest) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(Color(0xFFDDF5EA), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.15f, size.height * 0.5f)
                            lineTo(size.width * 0.42f, size.height * 0.78f)
                            lineTo(size.width * 0.88f, size.height * 0.22f)
                        }
                        drawPath(path, color = Color(0xFF0F7A4F), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    Text(
                        text = "Đã liên kết tài khoản Google",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F7A4F)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Đang dùng phiên Khách (đồng bộ Cloud ẩn danh)",
                        fontSize = 13.sp,
                        color = subTextColor
                    )
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLinkGoogleClick() },
                        shape = RoundedCornerShape(8.dp),
                        color = primaryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Liên kết Google",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatsRow(
    doneCount: String,
    pendingCount: String,
    rate: String,
    cardColor: Color,
    subTextColor: Color,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProfileStatCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            value = doneCount,
            label = "Việc đã xong",
            color = Color(0xFF137333),
            cardColor = cardColor,
            subTextColor = subTextColor
        )
        ProfileStatCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            value = pendingCount,
            label = "Đang chờ",
            color = primaryColor,
            cardColor = cardColor,
            subTextColor = subTextColor
        )
        ProfileStatCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            value = rate,
            label = "Hoàn thành",
            color = Color(0xFFB25E00),
            cardColor = cardColor,
            subTextColor = subTextColor
        )
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    color: Color,
    cardColor: Color,
    subTextColor: Color
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = subTextColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WeeklyChartCard(
    tasks: List<Task>,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    primaryColor: Color
) {
    val cal = Calendar.getInstance()
    val currentDayIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val mondayCal = (cal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -currentDayIndex)
    }

    val dayCounts = (0..6).map { offset ->
        val dayCal = (mondayCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, offset)
        }
        val dayStr = sdf.format(dayCal.time)
        tasks.count { it.date == dayStr }
    }
    val maxCount = (dayCounts.maxOrNull() ?: 1).coerceAtLeast(1)

    val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(20.dp),
        color = cardColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tuần này",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "${tasks.size} công việc",
                    fontSize = 13.sp,
                    color = subTextColor
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { index, day ->
                    val isToday = index == currentDayIndex
                    val count = dayCounts[index]
                    val hRatio = if (count > 0) {
                        (count.toFloat() / maxCount.toFloat()).coerceIn(0.25f, 1.0f)
                    } else {
                        0.15f
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            val barW = 12.dp.toPx()
                            val barH = size.height * hRatio
                            val startX = (size.width - barW) / 2
                            val startY = size.height - barH

                            drawRoundRect(
                                color = if (isToday) primaryColor else Color(0xFFE5E5EA),
                                topLeft = Offset(startX, startY),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )
                        }

                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) primaryColor else subTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileLogoutButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFFFD0D2), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF5F5)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Đăng xuất",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE5484D)
            )
        }
    }
}


