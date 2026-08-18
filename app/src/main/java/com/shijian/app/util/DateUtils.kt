package com.shijian.app.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateUtils {

    private val YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val HM = DateTimeFormatter.ofPattern("HH:mm")

    fun today(): String = LocalDate.now().format(YMD)

    fun now(): LocalTime = LocalTime.now()

    fun ymd(date: LocalDate): String = date.format(YMD)

    fun parseYmd(s: String): LocalDate = LocalDate.parse(s, YMD)

    /** yyyy-MM */
    fun monthPrefix(year: Int, month: Int): String = String.format("%04d-%02d", year, month)

    fun monthRange(year: Int, month: Int): Pair<String, String> {
        val first = LocalDate.of(year, month, 1)
        return Pair(ymd(first), ymd(first.with(TemporalAdjusters.lastDayOfMonth())))
    }

    /** 本周周一 ~ 周日 */
    fun weekRange(date: LocalDate): Pair<String, String> {
        val monday = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        return Pair(ymd(monday), ymd(sunday))
    }

    /** 2026年8月 */
    fun monthCn(year: Int, month: Int): String = "${year}年${month}月"

    fun weekdayCn(date: LocalDate): String {
        val names = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return names[(date.dayOfWeek.value - 1) % 7]
    }

    /** 8月18日 今天 / 8月17日 昨天 */
    fun dateLabel(dateStr: String): String {
        val d = parseYmd(dateStr)
        val label = "${d.monthValue}月${d.dayOfMonth}日"
        return when (dateStr) {
            today() -> "$label 今天"
            ymd(LocalDate.now().minusDays(1)) -> "$label 昨天"
            else -> label
        }
    }

    /** X分钟前 / X小时前 / X天前 */
    fun relativeTime(ts: Long): String {
        val diff = System.currentTimeMillis() - ts
        val min = diff / 60000
        return when {
            min < 1 -> "刚刚"
            min < 60 -> "${min}分钟前"
            min < 60 * 24 -> "${min / 60}小时前"
            else -> "${min / 1440}天前"
        }
    }

    /** 下班倒计时 HH:mm:ss */
    fun countdownHms(remainingSeconds: Long): String {
        val h = remainingSeconds / 3600
        val m = (remainingSeconds % 3600) / 60
        val s = remainingSeconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
