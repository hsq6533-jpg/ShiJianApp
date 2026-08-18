package com.shijian.app.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.app.BuildConfig
import com.shijian.app.data.UpdateEntry
import com.shijian.app.data.UpdatesData
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.TextSecondary

/** 更新公告（设计稿：更新公告 + PRD 5.7） */
@Composable
fun UpdatesScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageTopBar(title = "更新公告", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            // 当前版本卡
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Brand500, Color(0xFF2E8DFF))),
                        MaterialTheme.shapes.large
                    )
                    .padding(vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "时笺手机版 · 纯本地运行",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            UpdatesData.HISTORY.forEach { entry ->
                UpdateEntryCard(entry)
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(12.dp))
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
}

@Composable
private fun UpdateEntryCard(entry: UpdateEntry) {
    SjCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.version,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isLatest) Brand500 else MaterialTheme.colorScheme.onSurface
                    )
                    if (entry.isLatest) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "最新",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Brand500, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(text = entry.date, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Brand500.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✓", color = Brand500, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        entry.changes.forEach { change ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(text = "•", color = Brand500, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = change,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
