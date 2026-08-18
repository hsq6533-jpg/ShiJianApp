package com.shijian.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shijian.app.ShiJianApp
import com.shijian.app.util.Notifications

/** 定时生成新闻并推送本地通知 */
class NewsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ShiJianApp
        val repo = app.container.newsRepo
        val cfg = repo.getConfig()
        if (!cfg.enabled || !cfg.pushEnabled) return Result.success()
        if (!repo.hasKey()) return Result.success() // 未配置 Key，静默跳过
        return try {
            val count = repo.generate()
            Notifications.showNews(applicationContext, "时笺 · 今日要闻", "已生成 $count 条资讯，点击查看")
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
