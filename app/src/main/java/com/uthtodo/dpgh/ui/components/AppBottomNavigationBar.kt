package com.uthtodo.dpgh.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBottomNavigationBar(
    currentRoute: String,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit = {},
    onNavClick: (String) -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardColor = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            color = cardColor,
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Trang chủ",
                    isSelected = currentRoute == "home",
                    iconType = "home",
                    onClick = { onNavClick("home") }
                )

                BottomNavItem(
                    label = "Danh sách",
                    isSelected = currentRoute == "tasks",
                    iconType = "list",
                    onClick = { onNavClick("tasks") }
                )

                Spacer(modifier = Modifier.width(48.dp))

                BottomNavItem(
                    label = "Thông báo",
                    isSelected = currentRoute == "notifications",
                    iconType = "bell",
                    onClick = { onNavClick("notifications") }
                )

                BottomNavItem(
                    label = "Cài đặt",
                    isSelected = currentRoute == "settings",
                    iconType = "settings",
                    onClick = { onNavClick("settings") }
                )
            }
        }

        // Center Floating Action Button (+)
        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(56.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    spotColor = primaryColor.copy(alpha = 0.45f)
                )
                .background(primaryColor, CircleShape)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    iconType: String,
    onClick: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            when (iconType) {
                "home" -> {
                    val roof = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.5f)
                        lineTo(size.width * 0.5f, size.height * 0.15f)
                        lineTo(size.width * 0.85f, size.height * 0.5f)
                    }
                    drawPath(roof, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    val body = Path().apply {
                        moveTo(size.width * 0.25f, size.height * 0.45f)
                        lineTo(size.width * 0.25f, size.height * 0.85f)
                        lineTo(size.width * 0.75f, size.height * 0.85f)
                        lineTo(size.width * 0.75f, size.height * 0.45f)
                    }
                    drawPath(body, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                "list" -> {
                    drawLine(tint, Offset(size.width * 0.15f, size.height * 0.3f), Offset(size.width * 0.85f, size.height * 0.3f), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(tint, Offset(size.width * 0.15f, size.height * 0.55f), Offset(size.width * 0.85f, size.height * 0.55f), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(tint, Offset(size.width * 0.15f, size.height * 0.8f), Offset(size.width * 0.85f, size.height * 0.8f), 2.dp.toPx(), StrokeCap.Round)
                }
                "bell" -> {
                    val bellPath = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.18f)
                        cubicTo(size.width * 0.3f, size.height * 0.18f, size.width * 0.25f, size.height * 0.5f, size.width * 0.2f, size.height * 0.7f)
                        lineTo(size.width * 0.8f, size.height * 0.7f)
                        cubicTo(size.width * 0.75f, size.height * 0.5f, size.width * 0.7f, size.height * 0.18f, size.width * 0.5f, size.height * 0.18f)
                    }
                    drawPath(bellPath, color = tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
                "settings" -> {
                    drawCircle(color = tint, radius = size.width * 0.25f, style = Stroke(width = 2.dp.toPx()))
                    drawCircle(color = tint, radius = size.width * 0.4f, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint
        )
    }
}
