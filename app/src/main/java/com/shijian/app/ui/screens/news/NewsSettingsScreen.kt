package com.shijian.app.ui.screens.news

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.shijian.app.AppContainer
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.components.SwitchRow
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Orange500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.NewsScheduler
import kotlinx.coroutines.launch

private val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val LENGTH_OPTIONS = listOf("短", "中", "长")
private val LENGTH_VALUES = mapOf("短" to "SHORT", "中" to "MEDIUM", "长" to "LONG")
private val PERIOD_LABEL = mapOf("DAILY" to "每天", "WEEKLY" to "每周", "MONTHLY" to "每月")

/** 新闻设置（设计稿：新闻设置 + PRD 4.5） */
@Composable
fun NewsSettingsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var period by remember { mutableStateOf("DAILY") }
    var hour by remember { mutableIntStateOf(8) }
    var weekday by remember { mutableIntStateOf(1) }
    var day by remember { mutableIntStateOf(1) }
    var keywords by remember { mutableStateOf<List<Pair<String, Boolean>>>(emptyList()) }
    var kwInput by remember { mutableStateOf("") }
    var contentLength by remember { mutableStateOf("MEDIUM") }
    var pushEnabled by remember { mutableStateOf(false) }
    var deepSeekKey by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        runCatching {
            val cfg = container.newsRepo.getConfig()
            period = cfg.pushFrequency
            hour = cfg.pushHour
            weekday = cfg.pushWeekday
            day = cfg.pushDay
            keywords = cfg.specialKeywords.split(",").map { it.trim() }
                .filter { it.isNotBlank() }.map { it to true }
            contentLength = cfg.contentLength
            pushEnabled = cfg.pushEnabled
            deepSeekKey = container.securePrefs.getDeepSeekKey().orEmpty()
        }
    }

    val persist: () -> Unit = {
        scope.launch {
            runCatching {
                val cfg = container.newsRepo.getConfig()
                container.newsRepo.saveConfig(
                    cfg.copy(
                        pushFrequency = period,
                        pushHour = hour,
                        pushMinute = 0,
                        pushWeekday = weekday,
                        pushDay = day,
                        specialKeywords = keywords.joinToString(",") { it.first },
                        contentLength = contentLength,
                        pushEnabled = pushEnabled,
                        enabled = true
                    )
                )
                NewsScheduler.schedule(context, container.newsRepo, container.settingsRepo)
            }
        }
    }

    fun resetAll() {
        period = "DAILY"; hour = 8; weekday = 1; day = 1
        keywords = emptyList(); kwInput = ""
        contentLength = "MEDIUM"; pushEnabled = false
        runCatching { container.securePrefs.setDeepSeekKey(null) }
        deepSeekKey = ""
        persist()
        Toast.makeText(context, "已恢复默认设置", Toast.LENGTH_SHORT).show()
    }

    val schedulePreview = when (period) {
        "WEEKLY" -> "每周${WEEKDAYS[weekday - 1]} $hour:00 更新"
        "MONTHLY" -> "每月 $day 日 $hour:00 更新"
        else -> "每天 $hour:00 更新"
    }
    val specialCount = keywords.count { it.second }

    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageTopBar(
            title = "新闻设置",
            onBack = onBack,
            actions = {
                TextButton(onClick = { resetAll() }) { Text("恢复默认") }
            }
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "当前：${schedulePreview} · 关注 ${keywords.size} 个关键词 · 重点关注 $specialCount 个",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp)
            )

            // ---- 更新频率 ----
            GroupTitle("更新频率")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PERIOD_LABEL.forEach { (key, label) ->
                        val sel = key == period
                        Box(
                            modifier = Modifier
                                .background(
                                    if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable { period = key; persist() }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "更新时间", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = "$hour 点", style = MaterialTheme.typography.bodyMedium, color = Brand500)
                }
                Slider(
                    value = hour.toFloat(),
                    onValueChange = { hour = it.toInt() },
                    onValueChangeFinished = { persist() },
                    valueRange = 0f..23f
                )
                if (period == "WEEKLY") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WEEKDAYS.forEachIndexed { i, w ->
                            val sel = weekday == i + 1
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { weekday = i + 1; persist() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = w,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                if (period == "MONTHLY") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "每月第", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(text = "$day 日", style = MaterialTheme.typography.bodyMedium, color = Brand500)
                    }
                    Slider(
                        value = day.toFloat(),
                        onValueChange = { day = it.toInt() },
                        onValueChangeFinished = { persist() },
                        valueRange = 1f..31f
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = schedulePreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---- 关注哪些新闻 ----
            GroupTitle("关注哪些新闻")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kwInput,
                        onValueChange = { kwInput = it },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("输入想关注的新闻方向，如：人工智能", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true
                    )
                    TextButton(onClick = {
                        val kw = kwInput.trim()
                        if (kw.isNotBlank() && keywords.none { it.first == kw }) {
                            keywords = keywords + (kw to false)
                            kwInput = ""
                            persist()
                        } else if (keywords.any { it.first == kw }) {
                            Toast.makeText(context, "关键词已存在", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("添加") }
                }
                Spacer(Modifier.height(8.dp))
                if (keywords.isEmpty()) {
                    Text(
                        text = "还没有关注的关键词，添加一个试试",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        keywords.forEach { (kw, special) ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (special) Orange500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable {
                                        keywords = keywords.map { if (it.first == kw) it.first to !it.second else it }
                                        persist()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (special) "⭐ $kw" else kw,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (special) Orange500 else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                                TextButton(onClick = {
                                    keywords = keywords.filterNot { it.first == kw }
                                    persist()
                                }) { Text("×", color = TextSecondary) }
                            }
                        }
                    }
                    Text(
                        text = "点击关键词切换「重点关注」，相关新闻会标记置顶展示",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- 内容长度 ----
            GroupTitle("内容长度")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LENGTH_OPTIONS.forEach { opt ->
                        val sel = contentLength == LENGTH_VALUES[opt]
                        Box(
                            modifier = Modifier
                                .background(
                                    if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable { contentLength = LENGTH_VALUES.getValue(opt); persist() }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "影响 AI 生成摘要的详细程度",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---- 定时推送 ----
            GroupTitle("定时推送")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                SwitchRow(
                    icon = "🔔",
                    label = "开启定时推送",
                    subtitle = "按上方频率自动生成资讯并通知（需要通知权限）",
                    checked = pushEnabled,
                    onCheckedChange = {
                        pushEnabled = it
                        persist()
                        Toast.makeText(context, if (it) "已开启定时推送" else "已关闭定时推送", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---- DeepSeek Key ----
            GroupTitle("DeepSeek Key")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = deepSeekKey,
                    onValueChange = { deepSeekKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("DeepSeek API Key") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand500)
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                        .clickable {
                            container.securePrefs.setDeepSeekKey(deepSeekKey.trim().ifEmpty { null })
                            Toast.makeText(
                                context,
                                if (deepSeekKey.isNotBlank()) "DeepSeek Key 已保存" else "已清空 Key",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "保存 Key", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "设置仅保存在本机 · 新闻页将按你的偏好展示与排列",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
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
