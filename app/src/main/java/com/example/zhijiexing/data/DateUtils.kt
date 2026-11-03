package com.example.zhijiexing.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val serverFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    private val pointFormat = SimpleDateFormat("MM.dd", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)

    fun todayKey(): String = dayFormat.format(Date())

    fun dayKey(millis: Long): String = dayFormat.format(Date(millis))

    fun parseServerTime(value: String): Long? = try {
        serverFormat.parse(value)?.time
    } catch (_: Exception) {
        null
    }

    fun formatPoint(millis: Long): String = pointFormat.format(Date(millis))

    fun formatPointRange(startMillis: Long, endMillis: Long): String =
        "${pointFormat.format(Date(startMillis))} - ${pointFormat.format(Date(endMillis))}"

    fun formatClock(millis: Long): String = timeFormat.format(Date(millis))

    fun startOfDayOffset(offsetDays: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return cal.timeInMillis
    }

    fun lastNDayStarts(count: Int): List<Long> =
        (count - 1 downTo 0).map { startOfDayOffset(-it) }

    fun weekdayLabel(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> "周日"
        }
    }

    fun monthDayLabel(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    }

    fun shortMonthDay(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return "${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

    fun formatMinutes(minutes: Int): String {
        if (minutes < 60) return "$minutes 分钟"
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0) "$hours 小时" else "$hours 小时 $rest 分"
    }

    fun formatHours(minutes: Int): String {
        val hours = minutes / 60f
        return if (hours >= 10f) hours.toInt().toString() else String.format(Locale.CHINA, "%.1f", hours)
    }

    fun formatDuration(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return if (h > 0) String.format(Locale.CHINA, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.CHINA, "%02d:%02d", m, s)
    }

    fun formatUsedTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m} 分 ${s} 秒" else "${s} 秒"
    }

    fun todayRange(): Pair<Long, Long> {
        val start = startOfDayOffset(0)
        return start to start + 24 * 60 * 60 * 1000L
    }

    fun weekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val start = cal.timeInMillis
        return start to start + 7 * 24 * 60 * 60 * 1000L
    }
}
