package com.uthtodo.dpgh.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Điều khoản & Chính sách quyền riêng tư",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Dữ liệu thu thập & Mục đích sử dụng",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "UTH Todo chỉ lưu trữ tên hiển thị, địa chỉ email (khi bạn đăng nhập bằng Google) và danh sách công việc/nhắc nhở do chính bạn tạo nhằm mục đích đồng bộ dữ liệu trên các thiết bị của bạn.",
                    fontSize = 13.sp,
                    color = subTextColor,
                    lineHeight = 18.sp
                )

                Text(
                    text = "2. Lưu trữ & Bảo mật đám mây",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Dữ liệu được mã hóa và lưu trữ an toàn trên Google Cloud Firestore (dự án uth-todo-prod-2026) với quy tắc bảo mật cô lập tuyệt đối theo từng định danh tài khoản (UID). Không ai khác có thể truy cập dữ liệu công việc của bạn.",
                    fontSize = 13.sp,
                    color = subTextColor,
                    lineHeight = 18.sp
                )

                Text(
                    text = "3. Quyền kiểm soát & Xóa tài khoản",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Bạn có toàn quyền chỉnh sửa hoặc xóa vĩnh viễn tài khoản cùng toàn bộ dữ liệu đám mây bất kỳ lúc nào ngay trong ứng dụng tại mục Hồ sơ > Xóa tài khoản & dữ liệu.",
                    fontSize = 13.sp,
                    color = subTextColor,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Đã hiểu", color = Color.White)
            }
        }
    )
}
