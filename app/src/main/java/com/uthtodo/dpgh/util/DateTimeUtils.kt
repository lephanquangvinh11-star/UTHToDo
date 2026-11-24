package com.uthtodo.dpgh.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Format date rule:
 * Hôm nay "Hôm nay", +1 ngày "Ngày mai", đến hết Chủ nhật "Tuần này", ghép giờ: "Hôm nay, 14:00"
 */
fun formatRelativeDate(dateStr: String, timeStr: String): String {
    val timePart = if (timeStr.isNotBlank()) ", $timeStr" else ""
    if (dateStr.isBlank()) return timeStr

    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val taskDate = sdf.parse(dateStr) ?: return "$dateStr$timePart"

        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffDays = ((taskDate.time - calToday.time.time) / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays == 0 -> "Hôm nay$timePart"
            diffDays == 1 -> "Ngày mai$timePart"
            diffDays in 2..6 -> "Tuần này$timePart"
            else -> {
                val dayMonth = SimpleDateFormat("dd/MM", Locale.getDefault()).format(taskDate)
                "$dayMonth$timePart"
            }
        }
    } catch (_: Exception) {
        return "$dateStr$timePart"
    }
}
