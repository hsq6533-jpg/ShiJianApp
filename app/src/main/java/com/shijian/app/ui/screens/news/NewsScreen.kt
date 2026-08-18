package com.shijian.app.ui.screens.news

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.db.entity.NEWS_CATEGORY_COLORS
import com.shijian.app.data.db.entity.NewsConfigEntity
import com.shijian.app.data.db.entity.NewsItemEntity
import com.shijian.app.ui.components.EmptyState
import com.shijian.app.ui.components.TabTopBar
import com.shijian.app.ui.navigation.Routes
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Orange500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 新闻页（设计稿：新闻） */
@Composable
fun NewsScreen(
    container: AppContainer,
    nav: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val news by container.newsRepo.observeNews().collectAsStateWithLifecycle(initialValue = emptyList())
    var config by remember { mutableStateOf<NewsConfigEntity?>(null) }
    var filter by rememberSaveable { mutableStateOf("全部") }
    var refreshing by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<NewsItemEntity?>(null) }

    val toast: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    LaunchedEffect(Unit) { config = container.newsRepo.getConfig() }

    val cats = remember(config) {
        val c = config?.categories?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?.distinct() ?: listOf("国内")
        listOf("全部") + c
    }
    val filtered = remember(news, filter) {
        if (filter == "全部") news else news.filter { it.category == filter }
    }
    val feature = filtered.firstOrNull()
    val rest = filtered.drop(1)

    val refresh: () -> Unit = {
        if (!container.newsRepo.hasKey()) {
            toast("请先在「新闻设置」里配置 DeepSeek Key")
            return@refresh
        }
        refreshing = true
        scope.launch {
            runCatching { container.newsRepo.generate() }
                .onSuccess { count ->
                    toast("已生成 $count 条资讯")
                    config = container.newsRepo.getConfig()
                }
                .onFailure { toast(it.message ?: "生成失败，请检查 Key 或网络") }
            refreshing = false
        }
    }

    val meta = remember(config) {
        val freq = when (config?.pushFrequency) {
            "WEEKLY" -> "每周更新"
            "MONTHLY" -> "每月更新"
            else -> "每天更新"
        }
        val last = config?.lastUpdatedAt?.takeIf { it > 0 }
            ?.let { "上次更新 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) }
            ?: "尚未生成"
        "$freq · $last"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabTopBar(
            title = "新闻",
            actions = {
                IconButton(onClick = refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新资讯", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { nav(Routes.NEWS_SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "新闻设置", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 10.dp)
            )
            if (refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                Spacer(Modifier.height(6.dp))
            }

            if (filtered.isEmpty()) {
                EmptyState(
                    emoji = "📰",
                    title = if (container.newsRepo.hasKey()) "还没有资讯" else "未配置 DeepSeek Key",
                    subtitle = if (container.newsRepo.hasKey())
                        "点击右上角刷新生成 AI 资讯"
                    else "去「新闻设置」里配置 Key 即可获取 AI 资讯"
                )
            } else {
                // 头条
                FeatureCard(feature = feature!!, onClick = { detail = feature })
                Spacer(Modifier.height(12.dp))

                // 分类筛选 chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState(), reverseScrolling = true),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cats.forEach { c ->
                        val sel = c == filter
                        Box(
                            modifier = Modifier
                                .background(
                                    if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable { filter = c }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = c,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rest.forEach { item ->
                        NewsCard(item = item, onClick = { detail = item })
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

    detail?.let { item ->
        NewsDetailDialog(
            item = item,
            onDismiss = { detail = null },
            onRead = { scope.launch { container.newsRepo.markRead(item.id) } }
        )
    }
}

// ==================== 头条卡 ====================
@Composable
private fun FeatureCard(feature: NewsItemEntity, onClick: () -> Unit) {
    val color = Color(NEWS_CATEGORY_COLORS[feature.category] ?: Brand500.value.toInt())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Text(
            text = feature.category,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(color, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = feature.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            lineHeight = 26.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = feature.summary,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 20.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "${feature.sourceHint} · ${DateUtils.relativeTime(feature.publishedAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

// ==================== 新闻卡片 ====================
@Composable
private fun NewsCard(item: NewsItemEntity, onClick: () -> Unit) {
    val color = Color(NEWS_CATEGORY_COLORS[item.category] ?: Brand500.value.toInt())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.5.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            )
            if (item.isSpecial) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "⭐ 重点关注",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    color = Orange500,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!item.isRead) {
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Brand500, RoundedCornerShape(999.dp))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = item.summary,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = "${item.sourceHint} · ${DateUtils.relativeTime(item.publishedAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

// ==================== 详情弹窗 ====================
@Composable
private fun NewsDetailDialog(
    item: NewsItemEntity,
    onDismiss: () -> Unit,
    onRead: () -> Unit
) {
    LaunchedEffect(Unit) { onRead() }
    val color = Color(NEWS_CATEGORY_COLORS[item.category] ?: Brand500.value.toInt())
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(color, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    if (item.isSpecial) {
                        Spacer(Modifier.width(6.dp))
                        Text("⭐ 重点关注", style = MaterialTheme.typography.labelSmall, color = Orange500)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "${item.sourceHint} · ${DateUtils.relativeTime(item.publishedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    )
}
