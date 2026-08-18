@file:OptIn(ExperimentalFoundationApi::class)

package com.shijian.app.ui.screens.expense

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.db.entity.TransactionEntity
import com.shijian.app.ui.components.ChipRow
import com.shijian.app.ui.components.EmptyState
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.TabTopBar
import com.shijian.app.ui.navigation.Routes
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.MilkTeaGradientEnd
import com.shijian.app.ui.theme.MilkTeaGradientStart
import com.shijian.app.ui.theme.Orange500
import com.shijian.app.ui.theme.Success500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.DateUtils
import com.shijian.app.util.FormatUtils
import com.shijian.app.util.categoryEmoji
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ExpenseScreen(
    container: AppContainer,
    nav: (String) -> Unit,
    initialFilter: String = ""
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var month by rememberSaveable { mutableIntStateOf(today.monthValue) }
    var filter by rememberSaveable { mutableStateOf(initialFilter.ifEmpty { "全部" }) }
    var deleteTarget by remember { mutableStateOf<TransactionEntity?>(null) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    val monthList by remember(year, month) {
        container.transactionRepo.month(year, month)
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val allList by remember {
        container.transactionRepo.all()
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val income = monthList.filter { it.type == "INCOME" }.sumOf { it.amount }
    val incomeCount = monthList.count { it.type == "INCOME" }
    val expense = monthList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val expenseCount = monthList.count { it.type == "EXPENSE" }
    val pending = allList.filter { it.isReimbursable && !it.isReimbursed }
    val pendingSum = pending.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabTopBar(
            title = "开销",
            actions = {
                IconButton(onClick = {
                    nav(Routes.ADD_RECORD.replace("{editId}", "-1").replace("{type}", "").replace("{date}", ""))
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "记一笔", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // 月份切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            month--; if (month == 0) { month = 12; year-- }
                        }
                        .padding(12.dp)
                )
                Text(
                    text = DateUtils.monthCn(year, month),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showMonthPicker = true }
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "今天",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { year = today.year; month = today.monthValue }
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = "›",
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            month++; if (month == 13) { month = 1; year++ }
                        }
                        .padding(12.dp)
                )
            }

            // 三栏汇总
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCell(Modifier.weight(1f), "本月收入", income, incomeCount, Success500)
                StatCell(Modifier.weight(1f), "本月支出", expense, expenseCount, Danger500)
                StatCell(Modifier.weight(1f), "待报销", pendingSum, pending.size, Orange500)
            }

            Spacer(Modifier.height(12.dp))
            ReimburseHint(pendingSum, pending.size) { filter = "待报销" }

            Spacer(Modifier.height(16.dp))
            SjPrimaryButtonCompat("＋ 记一笔") {
                nav(Routes.ADD_RECORD.replace("{editId}", "-1").replace("{type}", "").replace("{date}", ""))
            }

            Spacer(Modifier.height(16.dp))
            ChipRow(
                options = listOf("全部", "支出", "收入", "待报销"),
                selected = filter,
                onSelect = { filter = it }
            )

            Spacer(Modifier.height(12.dp))
            val filtered = when (filter) {
                "支出" -> monthList.filter { it.type == "EXPENSE" && !it.isReimbursable }
                "收入" -> monthList.filter { it.type == "INCOME" }
                "待报销" -> monthList.filter { it.isReimbursable && !it.isReimbursed }
                else -> monthList
            }
            if (filtered.isEmpty()) {
                EmptyState(emoji = "🧾", title = "暂无记录", subtitle = "点击上方「记一笔」开始记账")
                Spacer(Modifier.height(12.dp))
                FooterText()
            } else {
                TransactionGroupedList(
                    list = filtered,
                    onItemClick = { t ->
                        nav(Routes.ADD_RECORD.replace("{editId}", t.id.toString()).replace("{type}", "").replace("{date}", ""))
                    },
                    onItemLongClick = { deleteTarget = it },
                    onReimburse = { t ->
                        scope.launch {
                            runCatching { container.transactionRepo.setReimbursed(t.id, true) }
                                .onSuccess { Toast.makeText(context, "已标记为报销完成", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, "报销失败，请重试", Toast.LENGTH_SHORT).show() }
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
                FooterText()
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除记录") },
            text = { Text("确定删除这笔「${target.category} ¥${FormatUtils.amount(target.amount)}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.transactionRepo.delete(target) }
                    deleteTarget = null
                }) { Text("删除", color = Danger500) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }

    // ---- 月份选择器：点击月份文字弹出，可快速跳转任意年月 ----
    if (showMonthPicker) {
        val monthStartMillis = java.time.LocalDate.of(year, month, 1)
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val dateState = rememberDatePickerState(initialSelectedDateMillis = monthStartMillis)
        DatePickerDialog(
            onDismissRequest = { showMonthPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val d = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        year = d.year
                        month = d.monthValue
                    }
                    showMonthPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showMonthPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = dateState, showModeToggle = false, title = { Text("选择月份", style = MaterialTheme.typography.labelLarge) })
        }
    }
}

@Composable
private fun StatCell(modifier: Modifier, title: String, amount: Double, count: Int, color: androidx.compose.ui.graphics.Color) {
    SjCard(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "¥${FormatUtils.amount(amount)}",
            color = color,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
        )
        Spacer(Modifier.height(2.dp))
        Text(text = "共 $count 笔", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun ReimburseHint(sum: Double, count: Int, onClick: () -> Unit) {
    val visible = sum > 0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(MilkTeaGradientStart.copy(alpha = 0.9f), MilkTeaGradientEnd.copy(alpha = 0.9f))),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (visible) "● 有 $count 笔待报销，合计 ¥${FormatUtils.amount(sum)} ›"
            else "● 暂无待报销",
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SjPrimaryButtonCompat(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
    }
}

@Composable
private fun FooterText() {
    Text(
        text = "时笺手机版 · 纯本地运行",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

/** 按日期分组的账单列表 */
@Composable
fun TransactionGroupedList(
    list: List<TransactionEntity>,
    onItemClick: (TransactionEntity) -> Unit,
    onItemLongClick: (TransactionEntity) -> Unit,
    onReimburse: (TransactionEntity) -> Unit = {}
) {
    val grouped = remember(list) {
        list.groupBy { it.date }.toSortedMap(Comparator.reverseOrder())
    }
    Column {
        grouped.forEach { (date, items) ->
            val dayExpense = items.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            Text(
                text = if (dayExpense > 0) "${DateUtils.dateLabel(date)} · 支出 -¥${String.format("%.2f", dayExpense)}"
                else DateUtils.dateLabel(date),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 10.dp)
            )
            SjCard(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { i, t ->
                    TransactionRow(t, onClick = { onItemClick(t) }, onLongClick = { onItemLongClick(t) }, onReimburse = { onReimburse(t) })
                    if (i < items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    t: TransactionEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReimburse: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = categoryEmoji(t.category), fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = t.category,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (t.isMilkTea) {
                    Text(text = " ☕", fontSize = 12.sp)
                }
                if (t.isReimbursable) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (t.isReimbursed) "已报销" else "待报销",
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .background(
                                if (t.isReimbursed) TextSecondary else Orange500,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            val sub = listOf(t.remark, t.merchant).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = FormatUtils.signedAmount(t.amount, t.type == "INCOME"),
                color = if (t.type == "INCOME") Success500 else Danger500,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
            )
            // 待报销记录：提供「报销」按钮，一键标记为已报销
            if (t.isReimbursable && !t.isReimbursed && onReimburse != null) {
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .background(Orange500.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onReimburse)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "报销",
                        style = MaterialTheme.typography.labelSmall,
                        color = Orange500,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
