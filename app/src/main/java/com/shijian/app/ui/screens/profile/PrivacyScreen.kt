package com.shijian.app.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Success500
import com.shijian.app.ui.theme.TextSecondary

/** 隐私说明（设计稿：隐私政策 + PRD 5.8） */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageTopBar(title = "隐私说明", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            SjCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🛡️", fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "数据不出设备",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Success500
                    )
                }
                Spacer(Modifier.height(12.dp))
                PrivacySection(
                    title = "1. 本地存储",
                    body = "时笺的所有数据——账单、分类、美食收藏、搜索地址、新闻偏好——均仅保存在你的设备本地，使用 SQLCipher 全库加密，任何情况下都不会上传到任何服务器。"
                )
                PrivacySection(
                    title = "2. 绝不收集",
                    body = "App 不注册账号、不收集个人信息、不采集行为数据、不接入任何统计 SDK。卸载即彻底删除，不留任何云端痕迹。"
                )
                PrivacySection(
                    title = "3. API Key 加密",
                    body = "你在「美食设置」「新闻设置」中填写的高德、DeepSeek Key 会通过 Android Keystore 加密后保存，仅在你主动发起搜索/生成时用于调用对应服务，不会写入普通存储或日志。"
                )
                PrivacySection(
                    title = "4. 网络请求",
                    body = "仅在以下场景联网：美食搜索调用高德 Web 服务、新闻生成调用 DeepSeek API、定时推送的通知展示。除此之外 App 完全离线可用。"
                )
                PrivacySection(
                    title = "5. 权限说明",
                    body = "定位权限仅用于美食页「搜附近」，未授权时不影响其他功能；通知权限用于新闻定时推送与待报销提醒，可随时在系统设置中关闭。"
                )
                PrivacySection(
                    title = "6. 你的控制权",
                    body = "你可以随时在「我的 → 数据与备份」导出加密备份文件或一键清空全部数据。导出文件同样由你保管，App 无法读取到任何云端。"
                )
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
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Brand500
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 21.sp
        )
    }
}
