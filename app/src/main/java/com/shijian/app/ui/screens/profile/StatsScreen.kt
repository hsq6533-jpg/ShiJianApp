package com.shijian.app.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.db.entity.TYPE_EXPENSE
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Chart1
import com.shijian.app.ui.theme.Chart2
import com.shijian.app.ui.theme.Chart3
import com.shijian.app.ui.theme.Chart4
import com.shijian.app.ui.theme.Chart5
import com.shijian.app.ui.theme.Chart6
import com.shijian.app.ui.theme.Chart7
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.Success500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.categoryEmoji
import kotlinx.coroutines.flow.catch
import java.time.LocalDate

private val CHART_COLORS = listOf(Chart1, Chart2, Chart3, Chart4, Chart5, Chart6, Chart7)

/** 数据统计（PRD 5.6：月度/年度 · 收支柱状图 · 分类占比 · Top 排行） */
@Composable
fun StatsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val all by remember {
        container.transactionRepo.all()
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var isYear by remember { mutableStateOf(false) }
    val now = LocalDate.now()

    val incomeExpense = remember(all, isYear, now) {
        val key = if (isYear) "${now.year}" else "${now.year}-%02d".format(now.monthValue)
        val inList = all.filter { it.type != TYPE_EXPENSE && it.date.startsWith(key) }
        val outList = all.filter { it.type == TYPE_EXPENSE && it.date.startsWith(key) }
        Monthly(now.year, now.monthValue, inList.sumOf { it.amount }, outList.sumOf { it.amount })
    }

    val trend = remember(all, isYear, now) {
        if (isYear) {
            (1..12).map { m ->
                val key = "${now.year}-%02d".format(m)
                val ins = all.filter { it.type != TYPE_EXPENSE && it.date.startsWith(key) }.sumOf { it.amount }
                val outs = all.filter { it.type == TYPE_EXPENSE && it.date.startsWith(key) }.sumOf { it.amount }
                MonthBar("${m}月", ins, outs)
            }
        } else {
            (5 downTo 0).map { i ->
                val d = now.minusMonths(i.toLong())
                val key = "${d.year}-%02d".format(d.monthValue)
                val ins = all.filter { it.type != TYPE_EXPENSE && it.date.startsWith(key) }.sumOf { it.amount }
                val outs = all.filter { it.type == TYPE_EXPENSE && it.date.startsWith(key) }.sumOf { it.amount }
                MonthBar("${d.monthValue}月", ins, outs)
            }
        }
    }

    val topCats = remember(all, isYear, now) {
        val key = if (isYear) "${now.year}" else "${now.year}-%02d".format(now.monthValue)
        val expenseList = all.filter { it.type == TYPE_EXPENSE && it.date.startsWith(key) }
        val total = expenseList.sumOf { it.amount }
        expenseList.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(8)
            .map { Pair(it.first, if (total > 0) it.second / total else 0.0) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageTopBar(title = "数据统计", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // 月度 / 年度切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("月度" to false, "年度" to true).forEach { (label, value) ->
                    val sel = isYear == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) Color.White else TextSecondary,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 汇总三卡
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("收入", incomeExpense.income, Success500, Modifier.weight(1f))
                SummaryCard("支出", incomeExpense.expense, Danger500, Modifier.weight(1f))
                SummaryCard("结余", incomeExpense.income - incomeExpense.expense, Brand500, Modifier.weight(1f))
            }

            Spacer(Modifier.height(18.dp))

            GroupTitle(if (isYear) "全年收支趋势" else "近 6 个月收支趋势")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                LegendRow()
                Spacer(Modifier.height(10.dp))
                TrendBars(trend)
            }

            Spacer(Modifier.height(18.dp))

            GroupTitle(if (isYear) "全年支出分类占比" else "本月支出分类占比")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                if (topCats.isEmpty()) {
                    Text(
                        text = "暂无支出数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    topCats.forEachIndexed { i, (name, ratio) ->
                        if (i > 0) Spacer(Modifier.height(12.dp))
                        CategoryRatioRow(
                            name = name,
                            ratio = ratio,
                            color = CHART_COLORS[i % CHART_COLORS.size]
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ==================== 数据 ====================

private data class Monthly(val year: Int, val month: Int, val income: Double, val expense: Double)
private data class MonthBar(val label: String, val income: Double, val expense: Double)

// ==================== 组件 ====================

@Composable
private fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "¥${java.text.NumberFormat.getInstance().format(amount)}",
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendItem(Chart1, "收入")
        LegendItem(Chart2, "支出")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(10.dp).height(10.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun TrendBars(bars: List<MonthBar>) {
    val max = (bars.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0).takeIf { it > 0 } ?: 1.0
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val slot = size.width / bars.size
        val barW = slot * 0.22f
        val gap = slot * 0.06f
        val baseY = size.height - 20f
        val chartH = size.height - 28f
        bars.forEachIndexed { i, b ->
            val cx = slot * i + slot / 2f
            val inH = (b.income / max).toFloat() * chartH
            val outH = (b.expense / max).toFloat() * chartH
            drawRoundRect(
                color = Chart1,
                topLeft = Offset(cx - barW - gap / 2f, baseY - inH),
                size = Size(barW, inH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = Chart2,
                topLeft = Offset(cx + gap / 2f, baseY - outH),
                size = Size(barW, outH),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        bars.forEach { b ->
            Text(
                text = b.label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CategoryRatioRow(name: String, ratio: Double, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = categoryEmoji(name), fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                text = "${(ratio * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((ratio).toFloat().coerceIn(0f, 1f))
                    .height(7.dp)
                    .background(color, RoundedCornerShape(999.dp))
            )
        }
    }
}
