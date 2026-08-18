package com.shijian.app.util

import java.text.DecimalFormat

object FormatUtils {

    private val amountFmt = DecimalFormat("#,##0.##")

    /** 金额：8000 → "8000"，128.5 → "128.5"，3.14 → "3.14" */
    fun amount(v: Double): String = amountFmt.format(v)

    /** 带符号金额：支出 -¥53.00、收入 +¥8000.00（列表明细用两位小数） */
    fun signedAmount(v: Double, isIncome: Boolean): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign¥${String.format("%.2f", v)}"
    }

    /** 距离：1200 → 1.2km，350 → 350m */
    fun distance(m: Int): String = when {
        m >= 1000 -> String.format("%.1fkm", m / 1000.0)
        else -> "${m}m"
    }

    fun rating(r: Float?): String = r?.let { String.format("%.1f", it) } ?: "-"

    fun cost(c: Int?): String = c?.let { "¥${it}/人" } ?: "人均未知"

    fun percent(v: Double): String = "${(v * 100).toInt()}%"
}
