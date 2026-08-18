package com.shijian.app.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.app.BuildConfig
import com.shijian.app.data.UpdatesData
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.TextSecondary

/** 新版本更新弹窗（5.7：版本号变化时首次启动展示） */
@Composable
fun UpdateDialog(onDismiss: () -> Unit) {
    val latest = UpdatesData.HISTORY.firstOrNull { it.isLatest } ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
        title = {
            Text(
                text = "v${BuildConfig.VERSION_NAME} 更新内容",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = latest.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                latest.changes.forEach { change ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = "•", color = Brand500)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = change,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    )
}
