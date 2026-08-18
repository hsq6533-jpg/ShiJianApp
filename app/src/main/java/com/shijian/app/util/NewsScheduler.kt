package com.shijian.app.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shijian.app.data.repo.NewsRepository
import com.shijian.app.data.prefs.SettingsRepository
import com.shijian.app.worker.NewsWorker
import java.util.concurrent.TimeUnit

/** 新闻定时任务调度（WorkManager） */
object NewsScheduler {

    private const val WORK_NAME = "shijian_news"

    suspend fun schedule(context: Context, newsRepo: NewsRepository, settings: SettingsRepository) {
        val cfg = newsRepo.getConfig()
        if (!cfg.enabled || !cfg.pushEnabled) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            return
        }
        val hours = when (cfg.pushFrequency) {
            "WEEKLY" -> 7 * 24L
            "MONTHLY" -> 30 * 24L
            else -> 24L
        }
        val request = PeriodicWorkRequestBuilder<NewsWorker>(hours, TimeUnit.HOURS)
            .setInitialDelay(hours, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
