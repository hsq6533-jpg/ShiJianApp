@file:OptIn(ExperimentalMaterial3Api::class)

package com.shijian.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.BuildConfig
import com.shijian.app.R
import com.shijian.app.data.UpdatesData
import com.shijian.app.data.prefs.AppSettings
import com.shijian.app.data.prefs.RestMode
import com.shijian.app.ui.components.ListRow
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.components.SjPrimaryButtonSmall
import com.shijian.app.ui.components.TabTopBar
import com.shijian.app.ui.components.WorkModeBadge
import com.shijian.app.ui.navigation.Routes
import com.shijian.app.ui.theme.Brand100
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Brand600
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.Green100
import com.shijian.app.ui.theme.Green500
import com.shijian.app.ui.theme.Purple100
import com.shijian.app.ui.theme.Purple500
import com.shijian.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** 我的 Tab（PRD 1.6）：头像、工作时间、休息模式、美食/新闻设置、隐私、关于、更新 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    container: AppContainer,
    nav: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by remember {
        container.settingsRepo.settings
            .catch { emit(AppSettings()) }
    }.collectAsStateWithLifecycle(initialValue = AppSettings())

    var showWorkTime by remember { mutableStateOf(false) }
    var showRestMode by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showWorkMode by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showUpdates by remember { mutableStateOf(false) }

    val streak = remember { androidx.compose.runtime.derivedStateOf { 0 } }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabTopBar(title = "我的")

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---- 头像卡 ----
            SjCard(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Brand100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("时", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Brand500)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "时笺用户",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "纯本地运行 · 数据不出机",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(
                        label = "连续打卡",
                        value = streak.value.toString(),
                        unit = "天",
                        bg = Brand100,
                        fg = Brand600
                    )
                    StatPill(
                        label = "收藏美食",
                        value = "0",
                        unit = "家",
                        bg = Green100,
                        fg = Green500
                    )
                    StatPill(
                        label = "工作模式",
                        value = if (settings.workMode) "开" else "关",
                        unit = "",
                        bg = Purple100,
                        fg = Purple500
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- 基础设置组 ----
            GroupTitle("基础")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                ListRow(
                    icon = "⏰",
                    label = "工作时间",
                    value = workTimeLabel(settings),
                    onClick = { showWorkTime = true },
                    iconBackground = Brand100
                )
                ListRow(
                    icon = "🌙",
                    label = "休息模式",
                    value = settings.restMode.label,
                    onClick = { showRestMode = true },
                    iconBackground = Purple100
                )
                ListRow(
                    icon = "⚡",
                    label = "专注工作模式",
                    value = if (settings.workMode) "已开启" else "未开启",
                    onClick = { showWorkMode = true },
                    iconBackground = Green100
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---- 子模块 ----
            GroupTitle("模块")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                ListRow(
                    icon = "🍽️",
                    label = "美食设置",
                    value = "搜索中心 / Key / 范围",
                    onClick = { nav(Routes.FOOD_SETTINGS) },
                    iconBackground = Brand100
                )
                ListRow(
                    icon = "📰",
                    label = "新闻设置",
                    value = when {
                        settings.newsChannels.size >= 2 -> "${settings.newsChannels.size} 个频道"
                        settings.newsChannels.size == 1 -> "1 个频道"
                        else -> "默认频道"
                    },
                    onClick = { nav(Routes.NEWS_SETTINGS) },
                    iconBackground = Purple100
                )
                ListRow(
                    icon = "📍",
                    label = "地址管理",
                    value = "家 / 公司 / 常去",
                    onClick = { nav(Routes.ADDRESS_MANAGE) },
                    iconBackground = Green100
                )
                ListRow(
                    icon = "📊",
                    label = "开销统计",
                    value = "月度收支 / 分类",
                    onClick = { nav(Routes.STATS) },
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            GroupTitle("数据")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                ListRow(
                    icon = "⭐",
                    label = "美食收藏",
                    value = "收藏 / 拉黑",
                    onClick = { nav(Routes.FOOD_LIST_FAV) },
                    iconBackground = Brand100
                )
                ListRow(
                    icon = "🔒",
                    label = "隐私与数据",
                    value = "权限 / 导出 / 清空",
                    onClick = { nav(Routes.PRIVACY) },
                    iconBackground = Purple100
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Danger500.copy(alpha = 0.05f), RoundedCornerShape(13.dp))
                        .clickable { showClear = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🗑",
                            fontSize = 20.sp,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Danger500.copy(alpha = 0.1f), CircleShape)
                                .padding(6.dp)
                                .let { Modifier.background(Color.Transparent) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "清空全部数据",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Danger500,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "记账 / 美食 / 设置 · 不可恢复",
                                style = MaterialTheme.typography.bodySmall,
                                color = Danger500.copy(alpha = 0.75f)
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Danger500)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            GroupTitle("关于")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                ListRow(
                    icon = "✨",
                    label = "更新公告",
                    value = "v${BuildConfig.VERSION_NAME}",
                    onClick = { showUpdates = true },
                    iconBackground = Brand100
                )
                ListRow(
                    icon = "ℹ️",
                    label = "关于时笺",
                    value = "版本 / 设计 / 开源",
                    onClick = { showAbout = true },
                    iconBackground = Purple100
                )
                ListRow(
                    icon = "💬",
                    label = "反馈问题",
                    value = "点我跳转 GitHub",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/hsq6533-jpg/ShiJianApp/issues")
                                )
                            )
                        }
                    },
                    iconBackground = Green100
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "时笺手机版 · 纯本地运行",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    // ---- Sheet / Dialog ----
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

    if (showWorkMode) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showWorkMode = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("工作模式", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "开启后首页自动隐藏开销 / 美食 / 新闻入口",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    WorkModeBadge(workMode = settings.workMode)
                }
                Spacer(Modifier.height(16.dp))
                SjPrimaryButtonSmall(
                    text = if (settings.workMode) "关闭工作模式" else "开启工作模式",
                    primary = !settings.workMode,
                    onClick = {
                        scope.launch { container.settingsRepo.setWorkMode(!settings.workMode) }
                        showWorkMode = false
                    }
                )
            }
        }
    }

    if (showUpdates) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showUpdates = false }, sheetState = sheetState) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "更新公告",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME} 版本更新",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                UpdatesData.ITEMS.forEach { item ->
                    if (item.versionCode <= BuildConfig.VERSION_CODE) {
                        UpdatesRow(item)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    if (showAbout) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showAbout = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text("关于时笺", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                AboutPill(title = "一句话介绍", desc = "纯本地运行的手机轻效率工具")
                Spacer(Modifier.height(10.dp))
                AboutPill(title = "数据存储", desc = "所有数据保存在本机 Room / DataStore，不联网上传")
                Spacer(Modifier.height(10.dp))
                AboutPill(
                    title = "联网能力",
                    desc = "仅用于美食（高德）、新闻（配置的 RSS 源）、更新公告（GitHub）"
                )
                Spacer(Modifier.height(18.dp))
                SjPrimaryButtonSmall(
                    text = "查看源码 (GitHub)",
                    primary = true,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hsq6533-jpg/ShiJianApp"))
                            )
                        }
                    }
                )
            }
        }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清空全部数据？") },
            text = {
                Text("将删除本机所有记账 / 美食缓存 / 设置；此操作不可恢复。")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            container.database.clearAllTables()
                            container.settingsRepo.resetAll()
                        }
                    }
                    showClear = false
                }) { Text("确认清空", color = Danger500) }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("取消") }
            }
        )
    }
}

private fun workTimeLabel(s: AppSettings): String {
    val pad = { v: Int -> String.format("%02d", v) }
    return "${pad(s.workStartHour)}:${pad(s.workStartMinute)}–${pad(s.workEndHour)}:${pad(s.workEndMinute)}"
}

@Composable
private fun StatPill(label: String, value: String, unit: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(bg, RoundedCornerShape(13.dp))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp
                )
                if (unit.isNotBlank()) {
                    Spacer(Modifier.width(2.dp))
                    Text(text = unit, style = MaterialTheme.typography.labelSmall, color = fg)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
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
    // 以 30 分钟为步长：一天 24h × 2 = 48 格
    var startIdx by remember { mutableIntStateOf(((currentStart / 30).coerceIn(0, 47))) }
    var endIdx by remember { mutableIntStateOf(((currentEnd / 30).coerceIn(0, 47))) }
    val hhmm = { idx: Int ->
        val mins = (idx * 30).coerceIn(0, 24 * 60 - 30)
        String.format("%02d:%02d", mins / 60, mins % 60)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                text = "工作时间",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "用于首页下班倒计时 · 步长 30 分钟",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("上班", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    text = hhmm(startIdx),
                    color = Brand500,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Slider(
                value = startIdx.toFloat(),
                onValueChange = { startIdx = it.toInt() },
                valueRange = 0f..47f,
                steps = 46
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("下班", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    text = hhmm(endIdx),
                    color = Brand500,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Slider(
                value = endIdx.toFloat(),
                onValueChange = { endIdx = it.toInt() },
                valueRange = 0f..47f,
                steps = 46
            )
            Spacer(Modifier.height(18.dp))
            SjPrimaryButtonSmall(
                text = "完成",
                onClick = { onSave(startIdx * 30, endIdx * 30) },
                primary = true
            )
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
            Spacer(Modifier.height(16.dp))
            options.forEach { option ->
                val sel = option == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (sel) Text("✓", color = Brand500, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UpdatesRow(item: UpdatesData.UpdateItem) {
    SjCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Brand100, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.version,
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand500
                    )
                }
                if (item.desc.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutPill(title: String, desc: String) {
    SjCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Brand500,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
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
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp)
    )
}

private val Routes.Companion.FOOD_LIST_FAV get() = "food_list?type=favorites"
