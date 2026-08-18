package com.shijian.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.db.entity.TransactionEntity
import com.shijian.app.data.prefs.AppSettings
import com.shijian.app.data.prefs.RestMode
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.TabTopBar
import com.shijian.app.ui.navigation.Routes
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.CountdownEnd
import com.shijian.app.ui.theme.CountdownStart
import com.shijian.app.ui.theme.Danger100
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.MilkTeaGradientEnd
import com.shijian.app.ui.theme.MilkTeaGradientStart
import com.shijian.app.ui.theme.Orange500
import com.shijian.app.ui.theme.ReimburseStart
import com.shijian.app.ui.theme.Success100
import com.shijian.app.ui.theme.Success500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.AmapIntents
import com.shijian.app.util.DateUtils
import com.shijian.app.util.FormatUtils
import com.shijian.app.util.categoryEmoji
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(container: AppContainer, nav: (String) -> Unit) {
    val settings by remember {
        container.settingsRepo.settings
            .catch { emit(AppSettings()) }
    }.collectAsStateWithLifecycle(initialValue = AppSettings())
    val today = remember { LocalDate.now() }

    // 页面月份（默认为当月）
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var month by rememberSaveable { mutableIntStateOf(today.monthValue) }

    // 本月与全量数据（所有 Flow 均 catch 异常，防止数据库/SQLCipher 崩溃导致闪退）
    val monthList by remember(year, month) {
        container.transactionRepo.month(year, month)
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val allList by remember {
        container.transactionRepo.all()
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // 本周数据
    val (monday, sunday) = remember(today) { DateUtils.weekRange(today) }
    val weekList by remember(monday, sunday) {
        container.transactionRepo.weekRange(monday, sunday)
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // 随机美食弹窗
    var showRandom by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabTopBar(
            title = "时笺",
            actions = {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "新闻设置",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { nav(Routes.NEWS_SETTINGS) }
                )
            }
        )

        ScreenHomeContent(
            settings = settings,
            today = today,
            year = year,
            month = month,
            onPrevMonth = { month--; if (month == 0) { month = 12; year-- } },
            onNextMonth = { month++; if (month == 13) { month = 1; year++ } },
            onToday = { year = today.year; month = today.monthValue },
            monthList = monthList,
            weekList = weekList,
            allList = allList,
            onQuickAdd = { type -> nav(Routes.ADD_RECORD.replace("{editId}", "-1").replace("{type}", type).replace("{date}", "")) },
            onReimburse = { nav("${Routes.EXPENSE_FILTER.replace("{filter}", "reimburse")}") },
            onRandomFood = { showRandom = true },
            onTodayNews = { nav(Routes.NEWS) },
            onDateClick = { d -> nav(Routes.CALENDAR_DETAIL.replace("{date}", d)) },
            onStats = { nav(Routes.STATS) }
        )
    }

    if (showRandom) {
        RandomFoodDialog(container = container, onDismiss = { showRandom = false })
    }
}

/** 可滚动内容区 */
@Composable
private fun ScreenHomeContent(
    settings: com.shijian.app.data.prefs.AppSettings,
    today: LocalDate,
    year: Int,
    month: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    monthList: List<TransactionEntity>,
    weekList: List<TransactionEntity>,
    allList: List<TransactionEntity>,
    onQuickAdd: (String) -> Unit,
    onReimburse: () -> Unit,
    onRandomFood: () -> Unit,
    onTodayNews: () -> Unit,
    onDateClick: (String) -> Unit,
    onStats: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        CountdownCard(settings, today)

        Spacer(Modifier.height(16.dp))
        QuickGrid(
            onQuickAdd = onQuickAdd,
            onReimburse = onReimburse,
            onRandomFood = onRandomFood,
            onTodayNews = onTodayNews
        )

        Spacer(Modifier.height(16.dp))
        MonthStatsRow(monthList, allList, onQuickAdd)

        val reimburseList = allList.filter { it.isReimbursable && !it.isReimbursed }
        Spacer(Modifier.height(16.dp))
        ReimburseCard(reimburseList, onReimburse)

        Spacer(Modifier.height(16.dp))
        CalendarCard(
            year = year, month = month, today = today,
            monthList = monthList,
            onPrevMonth = onPrevMonth, onNextMonth = onNextMonth, onToday = onToday,
            onDateClick = onDateClick
        )

        Spacer(Modifier.height(16.dp))
        WeekBreakdownCard(weekList, onStats)

        Spacer(Modifier.height(16.dp))
        DoubleCard(onRandomFood, onStats)

        Spacer(Modifier.height(24.dp))
        Text(
            text = "时笺手机版 · 纯本地运行",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ==================== 下班倒计时卡 ====================
@Composable
private fun CountdownCard(
    settings: com.shijian.app.data.prefs.AppSettings,
    today: LocalDate
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000)
        }
    }

    val start = LocalTime.of(settings.workStartHour, settings.workStartMinute)
    val end = LocalTime.of(settings.workEndHour, settings.workEndMinute)
    val isRestDay = settings.restMode != RestMode.NO_REST &&
        today.dayOfWeek.value !in settings.restMode.weekdays

    val label: String
    val progress: Float
    val showCountdown: Boolean
    when {
        isRestDay -> { label = "今日休息"; progress = 0f; showCountdown = false }
        now.isBefore(start) -> { label = "距上班 ${formatHm(start)}"; progress = 0f; showCountdown = false }
        !now.isBefore(end) -> { label = "已下班 🎉"; progress = 1f; showCountdown = false }
        else -> {
            val total = (end.toSecondOfDay() - start.toSecondOfDay()).toFloat()
            val elapsed = (now.toSecondOfDay() - start.toSecondOfDay()).toFloat()
            progress = (elapsed / total).coerceIn(0f, 1f)
            label = DateUtils.countdownHms((end.toSecondOfDay() - now.toSecondOfDay()).toLong())
            showCountdown = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(CountdownStart, CountdownEnd)),
                RoundedCornerShape(19.2.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Text(text = "距下班", color = Color(0xFF8E8E93), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = if (showCountdown) 44.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                        .height(6.dp)
                        .background(Brand500, CircleShape)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (showCountdown) "上班 ${formatHm(start)} · 下班 ${formatHm(end)} · ${(progress * 100).toInt()}%" else
                    "上班 ${formatHm(start)} · 下班 ${formatHm(end)}",
                color = Color(0xFF8E8E93),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatHm(t: LocalTime): String = String.format("%02d:%02d", t.hour, t.minute)

// ==================== 四宫格 ====================
@Composable
private fun QuickGrid(
    onQuickAdd: (String) -> Unit,
    onReimburse: () -> Unit,
    onRandomFood: () -> Unit,
    onTodayNews: () -> Unit
) {
    val items = listOf(
        Triple("记一笔", Icons.Filled.AccountBalanceWallet, { onQuickAdd("EXPENSE") }),
        Triple("待报销", Icons.Filled.ReceiptLong, { onReimburse() }),
        Triple("随机美食", Icons.Filled.RestaurantMenu, { onRandomFood() }),
        Triple("今日要闻", Icons.Filled.Newspaper, { onTodayNews() })
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(2).forEach { columnItems ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                columnItems.forEach { (label, icon, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .clickable { action() }
                            .padding(vertical = 14.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Brand500,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ==================== 本月统计 ====================
@Composable
private fun MonthStatsRow(
    monthList: List<TransactionEntity>,
    allList: List<TransactionEntity>,
    onQuickAdd: (String) -> Unit
) {
    val income = monthList.filter { it.type == "INCOME" }.sumOf { it.amount }
    val incomeCount = monthList.count { it.type == "INCOME" }
    val expense = monthList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val expenseCount = monthList.count { it.type == "EXPENSE" }
    val reimburse = allList.filter { it.isReimbursable && !it.isReimbursed }.sumOf { it.amount }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStatCard(
                modifier = Modifier.weight(1f),
                title = "本月收入",
                amount = income,
                count = incomeCount,
                color = Success500,
                tintBg = Success100,
                onAdd = { onQuickAdd("INCOME") }
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                title = "本月支出",
                amount = expense,
                count = expenseCount,
                color = Danger500,
                tintBg = Danger100,
                onAdd = { onQuickAdd("EXPENSE") }
            )
        }
        MiniStatCard(
            modifier = Modifier.fillMaxWidth(),
            title = "待报销",
            amount = reimburse,
            count = allList.count { it.isReimbursable && !it.isReimbursed },
            color = Orange500,
            tintBg = ReimburseStart
        )
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier,
    title: String,
    amount: Double,
    count: Int,
    color: Color,
    tintBg: Color,
    onAdd: (() -> Unit)? = null
) {
    SjCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            if (onAdd != null) {
                Text(
                    text = "+",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onAdd() }
                        .padding(horizontal = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "¥${FormatUtils.amount(amount)}",
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
        )
        Spacer(Modifier.height(2.dp))
        Text(text = "共 $count 笔", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

// ==================== 待报销卡 ====================
@Composable
private fun ReimburseCard(list: List<TransactionEntity>, onClick: () -> Unit) {
    val sum = list.sumOf { it.amount }
    SjCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Brush.horizontalGradient(listOf(MilkTeaGradientStart, MilkTeaGradientEnd)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🧾", fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "待报销 ¥${FormatUtils.amount(sum)}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "尚有 ${list.size} 笔未报销",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(text = "›", fontSize = 22.sp, color = TextSecondary)
        }
    }
}

// ==================== 日历卡 ====================
private data class DayInfo(val income: Double, val expense: Double, val milkTea: Boolean)

@Composable
private fun CalendarCard(
    year: Int,
    month: Int,
    today: LocalDate,
    monthList: List<TransactionEntity>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onDateClick: (String) -> Unit
) {
    val dayMap = remember(monthList) {
        val m = mutableMapOf<String, DayInfo>()
        monthList.forEach { t ->
            val info = m.getOrPut(t.date) { DayInfo(0.0, 0.0, false) }
            if (t.type == "INCOME") {
                m[t.date] = info.copy(income = info.income + t.amount)
            } else {
                m[t.date] = info.copy(expense = info.expense + t.amount)
            }
            if (t.isMilkTea) m[t.date] = m[t.date]!!.copy(milkTea = true)
        }
        m
    }
    val todayStr = DateUtils.ymd(today)
    val first = LocalDate.of(year, month, 1)
    val daysInMonth = first.lengthOfMonth()
    val leading = first.dayOfWeek.value - 1

    SjCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "‹",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onPrevMonth() }.padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Text(
                text = DateUtils.monthCn(year, month),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "今天",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onToday() }.padding(horizontal = 8.dp)
            )
            Text(
                text = "›",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNextMonth() }.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        val weekdayNames = listOf("日", "一", "二", "三", "四", "五", "六")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayNames.forEach { w ->
                Text(
                    text = w,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        // 周行：周一开头对齐（leading）
        val totalCells = leading + daysInMonth
        val rows = (totalCells + 6) / 7
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val cell = r * 7 + c
                    val dayNum = cell - leading + 1
                    val inMonth = dayNum in 1..daysInMonth
                    val dateStr = if (inMonth) String.format("%04d-%02d-%02d", year, month, dayNum) else ""
                    DayCell(
                        dayNum = if (inMonth) dayNum else 0,
                        info = dayMap[dateStr],
                        isToday = dateStr == todayStr,
                        modifier = Modifier.weight(1f),
                        onClick = { if (inMonth) onDateClick(dateStr) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNum: Int,
    info: DayInfo?,
    isToday: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(if (info == null) 44.dp else 58.dp)
            .padding(2.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (dayNum == 0) return@Column
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(if (isToday) Brand500 else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayNum.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (info != null) {
                if (info.income > 0) {
                    Text(
                        text = "+${FormatUtils.amount(info.income)}",
                        fontSize = 9.sp,
                        color = Success500,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
                    )
                }
                if (info.expense > 0) {
                    Text(
                        text = "-${FormatUtils.amount(info.expense)}",
                        fontSize = 9.sp,
                        color = Danger500,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
        // 奶茶角标
        if (info?.milkTea == true) {
            Text(
                text = "☕",
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 0.dp)
            )
        }
    }
}

// ==================== 本周支出构成 ====================
@Composable
private fun WeekBreakdownCard(weekList: List<TransactionEntity>, onClick: () -> Unit) {
    val byCategory = remember(weekList) {
        weekList.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)
    }
    val max = byCategory.maxOfOrNull { it.second } ?: 0.0
    val chartColors = listOf(Brand500, Orange500, Success500, ChartPurple)

    SjCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        CardTitleCompat("本周支出构成", trailing = "详情 ›")
        Spacer(Modifier.height(12.dp))
        if (byCategory.isEmpty()) {
            Text(text = "本周暂无支出", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        } else {
            byCategory.forEachIndexed { i, (cat, amount) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = categoryEmoji(cat),
                        fontSize = 14.sp,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((amount / max).toFloat().coerceIn(0.05f, 1f))
                                .height(8.dp)
                                .background(chartColors[i % chartColors.size], CircleShape)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "¥${String.format("%.2f", amount)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (i < byCategory.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

private val ChartPurple = androidx.compose.ui.graphics.Color(0xFF5856D6)

@Composable
private fun CardTitleCompat(title: String, trailing: String?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ==================== 双卡 ====================
@Composable
private fun DoubleCard(onRandomFood: () -> Unit, onStats: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HalfCard(
            modifier = Modifier.weight(1f),
            emoji = "🍜",
            title = "今天吃什么",
            subtitle = "随机一家",
            gradient = listOf(MilkTeaGradientStart, MilkTeaGradientEnd),
            onClick = onRandomFood
        )
        HalfCard(
            modifier = Modifier.weight(1f),
            emoji = "📊",
            title = "本月支出拆解",
            subtitle = "看看钱都哪儿了",
            gradient = listOf(Brand500, ChartPurple),
            onClick = onStats
        )
    }
}

@Composable
private fun HalfCard(
    modifier: Modifier,
    emoji: String,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Brush.linearGradient(gradient), RoundedCornerShape(19.2.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(text = emoji, fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text(text = title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ==================== 随机美食弹窗 ====================
@Composable
fun RandomFoodDialog(container: AppContainer, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var poi by remember { mutableStateOf<com.shijian.app.data.db.entity.FoodPoiEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val pick = {
        loading = true
        scope.launch {
            poi = container.foodRepo.randomPick()
            loading = false
        }
    }
    LaunchedEffect(Unit) { pick() }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("知道了", color = MaterialTheme.colorScheme.primary)
            }
        },
        title = { Text("今天吃什么？") },
        text = {
            when {
                loading -> Text("正在挑选中…", style = MaterialTheme.typography.bodyMedium)
                poi == null -> Text("还没有美食数据，先去美食页搜一搜吧", style = MaterialTheme.typography.bodyMedium)
                else -> {
                    val p = poi!!
                    Column {
                        Text(
                            text = "${p.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${p.type} · ${FormatUtils.distance(p.distance)} · ${FormatUtils.cost(p.cost)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SjSmallBtn(text = "去这里", filled = true) {
                                AmapIntents.openNavigation(context, p.latitude, p.longitude, p.name)
                            }
                            SjSmallBtn(text = "换一个", filled = false) { pick() }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SjSmallBtn(text: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (filled) Brand500 else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
