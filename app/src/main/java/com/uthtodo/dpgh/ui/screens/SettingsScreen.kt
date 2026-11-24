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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.uthtodo.dpgh.ui.components.PrivacyPolicyDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onAddTaskClick: () -> Unit = {},
    onNavClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val settings by AppPreferences.settings.collectAsState()
    val allTasks by TaskRepository.tasks.collectAsState()
    var showPrivacyDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Cài đặt",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Tùy chỉnh giao diện và thông báo",
                    fontSize = 14.sp,
                    color = subTextColor
                )
            }

            // 1. Giao diện Card
            SettingsSectionCard(title = "GIAO DIỆN", cardColor = cardColor, subTextColor = subTextColor) {
                // Theme mode selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Chế độ hiển thị",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Sáng", "Tối", "Tự động").forEach { mode ->
                            val isSel = settings.themeMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { AppPreferences.setThemeMode(mode) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) primaryColor else bgColor
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else textColor
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = bgColor, thickness = 1.dp)

                // Accent Color Selector
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Màu chủ đạo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "#2F5BEA" to Color(0xFF2F5BEA),
                            "#34C759" to Color(0xFF34C759),
                            "#FF9500" to Color(0xFFFF9500),
                            "#AF52DE" to Color(0xFFAF52DE),
                            "#FF2D55" to Color(0xFFFF2D55)
                        ).forEach { (hex, col) ->
                            val isSel = settings.accentColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .clickable { AppPreferences.setAccentColorHex(hex) }
                                    .border(
                                        width = if (isSel) 3.dp else 0.dp,
                                        color = if (isSel) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Text(text = "✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Thông báo Card
            SettingsSectionCard(title = "THÔNG BÁO", cardColor = cardColor, subTextColor = subTextColor) {
                // Nhắc việc
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nhắc việc",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Text(
                            text = "Nhận nhắc nhở trước hạn chót 15 phút",
                            fontSize = 13.sp,
                            color = subTextColor
                        )
                    }
                    Switch(
                        checked = settings.enableReminders,
                        onCheckedChange = {
                            AppPreferences.setEnableReminders(it)
                            ReminderScheduler.syncAllTaskReminders(context, allTasks)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE5E5EA)
                        )
                    )
                }

                HorizontalDivider(color = bgColor, thickness = 1.dp)

                // Âm thanh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Âm thanh",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Text(
                            text = "Phát âm thanh khi có thông báo",
                            fontSize = 13.sp,
                            color = subTextColor
                        )
                    }
                    Switch(
                        checked = settings.enableSound,
                        onCheckedChange = { AppPreferences.setEnableSound(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE5E5EA)
                        )
                    )
                }

                HorizontalDivider(color = bgColor, thickness = 1.dp)

                // Tổng kết cuối ngày
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tổng kết cuối ngày",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Text(
                            text = "Nhắc nhở tổng kết các việc lúc 21:00",
                            fontSize = 13.sp,
                            color = subTextColor
                        )
                    }
                    Switch(
                        checked = settings.enableDailySummary,
                        onCheckedChange = { AppPreferences.setEnableDailySummary(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE5E5EA)
                        )
                    )
                }
            }

            // 3. Chung Card
            SettingsSectionCard(title = "CHUNG", cardColor = cardColor, subTextColor = subTextColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ngôn ngữ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "Tiếng Việt",
                        fontSize = 14.sp,
                        color = subTextColor
                    )
                }

                HorizontalDivider(color = bgColor, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dự án Cloud",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "uth-todo-prod-2026",
                        fontSize = 14.sp,
                        color = subTextColor
                    )
                }

                HorizontalDivider(color = bgColor, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPrivacyDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chính sách quyền riêng tư",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "Xem chi tiết ›",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryColor
                    )
                }

                HorizontalDivider(color = bgColor, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Giới thiệu",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "Phiên bản 1.0 (Production)",
                        fontSize = 14.sp,
                        color = subTextColor
                    )
                }
            }
        }

        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
        }

        // Bottom Navigation Bar
        AppBottomNavigationBar(
            currentRoute = "settings",
            modifier = Modifier.align(Alignment.BottomCenter),
            onAddClick = onAddTaskClick,
            onNavClick = onNavClick
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    cardColor: Color,
    subTextColor: Color,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = subTextColor,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cardColor,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}


