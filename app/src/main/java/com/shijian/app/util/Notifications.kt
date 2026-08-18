package com.shijian.app.util

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shijian.app.MainActivity
import com.shijian.app.R

/** 本地通知（新闻推送） */
object Notifications {

    const val CHANNEL_NEWS = "news"
    const val NOTIF_NEWS = 1001

    fun ensureChannel(context: Context) {
        val channel = android.app.NotificationChannel(
            CHANNEL_NEWS,
            "新闻推送",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "定时生成 AI 资讯后的提醒" }
        context.getSystemService(android.app.NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showNews(context: Context, title: String, text: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val pending = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("shijian://app/news")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_NEWS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_NEWS, notif)
    }
}
