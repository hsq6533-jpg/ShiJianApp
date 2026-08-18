package com.shijian.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** 通知通道工具：App 启动时一次性创建，确保后续推送可用 */
object NotificationHelper {

    private const val NEWS_CHANNEL_ID = "shijian_news"
    private const val REMIND_CHANNEL_ID = "shijian_remind"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            val news = NotificationChannel(
                NEWS_CHANNEL_ID,
                "新闻推送",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "每日定时新闻摘要" }
            val remind = NotificationChannel(
                REMIND_CHANNEL_ID,
                "时笺提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "记账提醒、休息提醒等" }
            nm.createNotificationChannels(listOf(news, remind))
        }
    }

    fun newsChannelId(): String = NEWS_CHANNEL_ID
    fun remindChannelId(): String = REMIND_CHANNEL_ID
}
