package com.shijian.app.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.BuildConfig
import com.shijian.app.data.db.entity.TransactionEntity
import com.shijian.app.data.prefs.DarkMode
import com.shijian.app.data.prefs.RestMode
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.SwitchRow
import com.shijian.app.ui.components.TabTopBar
import com.shijian.app.ui.navigation.Routes
import com.shijian.app.ui.theme.Brand100
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Danger100
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.Orange100
import com.shijian.app.ui.theme.Orange500
import com.shijian.app.ui.theme.Purple100
import com.shijian.app.ui.theme.Purple500
import com.shijian.app.ui.theme.Success100
import com.shijian.app.ui.theme.Success500
import com.shijian.app.ui.theme.Teal100
import com.shijian.app.ui.theme.Teal500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 我的页（设计稿：我的 + PRD 4.5） */
@Composable
fun ProfileScreen(
    container: AppContainer,
    nav: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by container.settingsRepo.settings.collectAsStateWithLifecycle()

    var recordCount by remember { mutableStateOf(0) }
    var dayCount by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }
    var newsSummary by remember { mutableStateOf("每天 8:00") }

    val favs by container.foodRepo.observeFavorites().collectAsStateWithLifecycle(initialValue = emptyList())
    val blocks by container.foodRepo.observeBlacklisted().collectAsStateWithLifecycle(initialValue = emptyList())

    // 弹窗状态
    var showBackup by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showWorkTime by remember { mutableStateOf(false) }
    var showRestMode by remember { mutableStateOf(false) }
    var showDarkMode by remember { mutableStateOf(false) }
    var showUserName by remember { mutableStateOf(false) }

    val toast: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    LaunchedEffect(Unit) {
        val all = container.transactionRepo.all().first()
        recordCount = all.size
        dayCount = all.map { it.date }.distinct().size
        streak = streakDays(all)
        container.newsRepo.getConfig()?.let { cfg ->
            newsSummary = when (cfg.pushFrequency) {
                "WEEKLY" -> "每周${"一二三四五六日"[cfg.pushWeekday - 1]} ${cfg.pushHour}:00"
                "MONTHLY" -> "每月 ${cfg.pushDay} 日 ${cfg.pushHour}:00"
                else -> "每天 ${cfg.pushHour}:00"
            }
        }
    }

    // ---- 备份导入导出 ----
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val bytes = container.backupRepo.export(encrypted = true)
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            }.onSuccess { toast("备份已导出") }
                .onFailure { toast("导出失败，请重试") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("读取失败")
                container.backupRepo.import(bytes)
            }.onSuccess { toast("导入成功") }
                .onFailure { toast("导入失败，文件格式不正确") }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabTopBar(
            title = "我的",
            actions = {
                TextButton(onClick = { nav(Routes.UPDATES) }) { Text("更新") }
            }
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---- 用户信息卡 ----
            SjCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(
                                Brush.linearGradient(listOf(Brand500, Color(0xFF2E8DFF))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "时", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = settings.userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showUserName = true }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "✎",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { showUserName = true }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Success100, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🛡 纯本地数据 · 隐私优先",
                                style = MaterialTheme.typography.labelSmall,
                                color = Success500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row {
                    ProfileStat(dayCount, "记账天数")
                    ProfileStat(recordCount, "记录总数")
                    ProfileStat(streak, "连续打卡")
                }
            }

            // ---- 外观与习惯 ----
            GroupTitle("外观与习惯")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                ProfileRow("🌙", Brand100, Brand500, "深色模式", settings.darkMode.label) { showDarkMode = true }
                DividerLine()
                ProfileRow("⏰", Purple100, Purple500, "工作时间", workTimeLabel(settings)) { showWorkTime = true }
                DividerLine()
                ProfileRow("📅", Teal100, Teal500, "休息模式", settings.restMode.label) { showRestMode = true }
                DividerLine()
                SwitchRow(
                    icon = "🔔",
                    label = "待报销提醒",
                    subtitle = "有未报销支出时本地通知提醒",
                    checked = settings.reimburseReminderEnabled,
                    onCheckedChange = { container.settingsRepo.setReimburseReminder(it) },
                    iconBackground = Orange100
                )
                DividerLine()
                ProfileRow("📰", Brand100, Brand500, "新闻设置", newsSummary) { nav(Routes.NEWS_SETTINGS) }
                DividerLine()
                ProfileRow("📢", Purple100, Purple500, "更新公告", "v${BuildConfig.VERSION_NAME}") { nav(Routes.UPDATES) }
            }

            // ---- 美食设置 ----
            GroupTitle("美食设置")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                ProfileRow("❤️", Danger100, Danger500, "收藏管理", "${favs.size} 家") { nav(Routes.FOOD_LIST.replace("{type}", "favorites")) }
                DividerLine()
                ProfileRow("🚫", MaterialTheme.colorScheme.surfaceVariant, TextSecondary, "拉黑管理", "${blocks.size} 家") { nav(Routes.FOOD_LIST.replace("{type}", "blocked")) }
                DividerLine()
                ProfileRow("📍", Brand100, Brand500, "地址管理", "常用地址") { nav(Routes.ADDRESS_MANAGE) }
                DividerLine()
                ProfileRow("🍜", Orange100, Orange500, "美食设置", "高德 Key · 范围") { nav(Routes.FOOD_SETTINGS) }
            }

            // ---- 数据与备份 ----
            GroupTitle("数据与备份")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                ProfileRow("💾", Brand100, Brand500, "备份与恢复", "导出 · 导入") { showBackup = true }
                DividerLine()
                ProfileRow("📊", Success100, Success500, "数据统计", "月报 · 年报") { nav(Routes.STATS) }
                DividerLine()
                ProfileRow("🗑", MaterialTheme.colorScheme.surfaceVariant, Danger500, "清空数据", null, danger = true) { showClear = true }
            }

            // ---- 关于 ----
            GroupTitle("关于")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                ProfileRow("ℹ️", Purple100, Purple500, "更新与关于", "v${BuildConfig.VERSION_NAME} 手机版") { nav(Routes.UPDATES) }
                DividerLine()
                ProfileRow("🔒", Teal100, Teal500, "隐私说明", "数据不出设备") { nav(Routes.PRIVACY) }
            }

            Spacer(Modifier.height(20.dp))
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

    if (showBackup) {
        BackupSheet(
            onDismiss = { showBackup = false },
            onExport = {
                val now = java.util.Date()
                val fname = "shijian-backup-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(
                    now.toInstant().atZone(java.time.ZoneId.systemDefault())
                ) + ".json"
                exportLauncher.launch(fname)
                showBackup = false
            },
            onImport = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "text/*"))
                showBackup = false
            },
            onCopy = {
                scope.launch {
                    runCatching {
                        val bytes = container.backupRepo.export(encrypted = true)
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("shijian-backup", base64))
                    }.onSuccess { toast("已复制，请粘贴保存到文本文件") }
                        .onFailure { toast("复制失败，请改用导出文件") }
                }
            }
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清空全部数据？") },
            text = { Text("将删除所有账单、美食收藏、地址与新闻记录，此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showClear = false
                    scope.launch {
                        container.database.transactionDao().clearAll()
                        container.database.foodPoiDao().clearAll()
                        container.addressRepo.clearAll()
                        container.database.newsDao().clearAll()
                        toast("本地数据已清空")
                    }
                }) { Text("清空", color = Danger500) }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("取消") }
            }
        )
    }

    if (showUserName) {
        EditNameDialog(
            current = settings.userName,
            onDismiss = { showUserName = false },
            onSave = { name ->
                container.settingsRepo.setUserName(name)
                showUserName = false
            }
        )
    }

    if (showWorkTime) {
        WorkTimeSheet(
            currentStart = settings.workStartHour * 60 + settings.workStartMinute,
            currentEnd = settings.workEndHour * 60 + settings.workEndMinute,
            onDismiss = { showWorkTime = false },
            onSave = { s, e ->
                container.settingsRepo.setWorkStart(s / 60, s % 60)
                container.settingsRepo.setWorkEnd(e / 60, e % 60)
                showWorkTime = false
            }
        )
    }

    if (showRestMode) {
        OptionSheet(
            title = "休息模式",
            options = RestMode.entries.map { it.label },
            selected = settings.restMode.label,
            onDismiss = { showRestMode = false },
            onSelect = { label ->
                RestMode.entries.firstOrNull { it.label == label }?.let { container.settingsRepo.setRestMode(it) }
                showRestMode = false
            }
        )
    }

    if (showDarkMode) {
        OptionSheet(
            title = "深色模式",
            options = DarkMode.entries.map { it.label },
            selected = settings.darkMode.label,
            onDismiss = { showDarkMode = false },
            onSelect = { label ->
                DarkMode.entries.firstOrNull { it.label == label }?.let { container.settingsRepo.setDarkMode(it) }
                showDarkMode = false
            }
        )
    }
}

// ==================== 小组件 ====================

private fun workTimeLabel(s: com.shijian.app.data.prefs.AppSettings): String {
    val pad = { v: Int -> String.format("%02d", v) }
    return "${pad(s.workStartHour)}:${pad(s.workStartMinute)}–${pad(s.workEndHour)}:${pad(s.workEndMinute)}"
}

/** 连续打卡：今天有记录则从今天起算，否则从昨天起算（今天尚未结束时仍算连续） */
private fun streakDays(all: List<TransactionEntity>): Int {
    val recorded = all.map { it.date }.toSet()
    if (recorded.isEmpty()) return 0
    var day = LocalDate.parse(DateUtils.today())
    if (!recorded.contains(DateUtils.ymd(day))) {
        day = day.minusDays(1)
    }
    var streak = 0
    while (recorded.contains(DateUtils.ymd(day))) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

@Composable
private fun ProfileStat(value: Int, label: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum"
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun ProfileRow(
    icon: String,
    iconBackground: Color,
    iconColor: Color,
    label: String,
    value: String?,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconBackground, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 15.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (danger) Danger500 else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.width(4.dp))
        Text(text = "›", color = MaterialTheme.colorScheme.outline, fontSize = 20.sp)
    }
}

// ==================== 弹窗 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupSheet(
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onCopy: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                text = "备份与恢复",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "所有数据仅保存在本机。建议定期导出备份：换机、卸载或恢复出厂前请先导出，之后可通过导入一键还原。备份文件已加密。",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(18.dp))
            SjPrimaryButtonSmall(text = "导出备份文件", onClick = onExport, primary = true)
            Spacer(Modifier.height(10.dp))
            SjPrimaryButtonSmall(text = "从文件导入", onClick = onImport, primary = false)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCopy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("复制备份内容（手动保存）", color = Brand500)
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("完成")
            }
        }
    }
}

@Composable
private fun SjPrimaryButtonSmall(text: String, onClick: () -> Unit, primary: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EditNameDialog(
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改昵称") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(12) },
                singleLine = true,
                label = { Text("昵称") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim().ifEmpty { "时笺用户" }) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkTimeSheet(
    currentStart: Int,
    currentEnd: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var start by remember { mutableIntStateOf(currentStart) }
    var end by remember { mutableIntStateOf(currentEnd) }
    val hh = { m: Int -> String.format("%02d", m / 60) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                text = "工作时间",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "用于首页下班倒计时",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("上班", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${hh(start)}:00", color = Brand500, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
            }
            Slider(value = (start / 60).toFloat(), onValueChange = { start = it.toInt() * 60 }, valueRange = 0f..23f)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("下班", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${hh(end)}:00", color = Brand500, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
            }
            Slider(value = (end / 60).toFloat(), onValueChange = { end = it.toInt() * 60 }, valueRange = 0f..23f)
            Spacer(Modifier.height(18.dp))
            SjPrimaryButtonSmall(text = "完成", onClick = { onSave(start, end) }, primary = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionSheet(
    title: String,
    options: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            options.forEach { opt ->
                val sel = opt == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (sel) Brand100 else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(opt) }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (sel) "✓  $opt" else opt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (sel) Brand500 else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
