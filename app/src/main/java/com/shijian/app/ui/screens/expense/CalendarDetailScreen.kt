@file:OptIn(ExperimentalFoundationApi::class)

package com.shijian.app.ui.screens.expense

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.db.entity.TransactionEntity
import com.shijian.app.ui.components.EmptyState
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.navigation.Routes
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.Orange100
import com.shijian.app.ui.theme.Orange500
import com.shijian.app.ui.theme.Success100
import com.shijian.app.ui.theme.Success500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.DateUtils
import com.shijian.app.util.FormatUtils
import com.shijian.app.util.categoryEmoji
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate

private val PERIODS = listOf("上午", "下午", "晚上")

/** 按时间段划分：05:00-11:59 上午 / 12:00-17:59 下午 / 其余 晚上 */
private fun periodOf(time: String): String {
    val h = time.substringBefore(":").toIntOrNull() ?: 12
    return when {
        h in 5..11 -> "上午"
        h in 12..17 -> "下午"
        else -> "晚上"
    }
}

/** 当日账单明细（设计稿：日历当天明细） */
@Composable
fun CalendarDetailScreen(
    container: AppContainer,
    date: String,
    onBack: () -> Unit,
    nav: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val list by remember(date) {
        container.transactionRepo.byDate(date)
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    var deleteTarget by remember { mutableStateOf<TransactionEntity?>(null) }

    val day = remember(date) { runCatching { DateUtils.parseYmd(date) }.getOrNull() }
    val title = day?.let { "${it.monthValue}月${it.dayOfMonth}日" } ?: date

    val expense = list.filter { it.type == "EXPENSE" && !it.isReimbursable }.sumOf { it.amount }
    val income = list.filter { it.type == "INCOME" }.sumOf { it.amount }
    val pending = list.filter { it.isReimbursable && !it.isReimbursed }
    val pendingSum = pending.sumOf { it.amount }

    val grouped = remember(list) { list.groupBy { periodOf(it.time) } }

    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageTopBar(title = title, onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // 三栏汇总卡
            SjCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
                    SummaryCell(Modifier.weight(1f), "支出", "−${FormatUtils.amount(expense)}", Danger500)
                    SummaryCell(Modifier.weight(1f), "收入", "+${FormatUtils.amount(income)}", Success500)
                    SummaryCell(Modifier.weight(1f), "待报销", "¥${FormatUtils.amount(pendingSum)}", Orange500)
                }
            }

            Spacer(Modifier.height(12.dp))
            // 记一笔
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .combinedClickable(onClick = {
                        nav(Routes.ADD_RECORD
                            .replace("{editId}", "-1")
                            .replace("{type}", "")
                            .replace("{date}", date))
                    }),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "＋ 记一笔", color = Color.White, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))

            if (list.isEmpty()) {
                EmptyState(emoji = "🧾", title = "这一天还没有记录", subtitle = "点击上方「记一笔」开始记账")
            } else {
                SjCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PERIODS.forEach { period ->
                            val items = grouped[period].orEmpty()
                            if (items.isNotEmpty()) {
                                Text(
                                    text = period,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                                items.forEach { t ->
                                    CalendarRow(
                                        t = t,
                                        onClick = {
                                            nav(Routes.ADD_RECORD
                                                .replace("{editId}", t.id.toString())
                                                .replace("{type}", "")
                                                .replace("{date}", ""))
                                        },
                                        onLongClick = { deleteTarget = t }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "时笺手机版 · 纯本地运行",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除记录") },
            text = { Text("确定删除这笔「${target.category} ¥${FormatUtils.amount(target.amount)}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { container.transactionRepo.delete(target) }
                    }
                    deleteTarget = null
                }) { Text("删除", color = Danger500) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SummaryCell(modifier: Modifier, label: String, value: String, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
private fun CalendarRow(
    t: TransactionEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isPending = t.isReimbursable && !t.isReimbursed
    val isIncome = t.type == "INCOME"
    val amountColor = when {
        isIncome -> Success500
        isPending -> Orange500
        else -> Danger500
    }
    val amountText = when {
        isIncome -> "+${FormatUtils.amount(t.amount)}"
        isPending -> "¥${FormatUtils.amount(t.amount)}"
        else -> "-${FormatUtils.amount(t.amount)}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    when {
                        isIncome -> Success100
                        isPending -> Orange100
                        else -> Brand500.copy(alpha = 0.12f)
                    },
                    RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = categoryEmoji(t.category), fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            val sub = listOf(t.remark, t.merchant).filter { it.isNotBlank() }.joinToString(" · ")
            Text(
                text = if (sub.isNotBlank()) "${t.category} · $sub" else t.category,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPending) {
                    SmallTag("待报销", Orange500, Orange100)
                } else {
                    SmallTag(if (isIncome) "收入" else "支出", if (isIncome) Success500 else Danger500, if (isIncome) Success100 else Danger500.copy(alpha = 0.10f))
                }
                if (t.isMilkTea) {
                    Spacer(Modifier.width(6.dp))
                    Text(text = "☕", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = amountText,
            color = amountColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
private fun SmallTag(text: String, color: Color, bg: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
