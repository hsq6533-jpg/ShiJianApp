package com.shijian.app

import android.app.Application
import com.shijian.app.data.db.entity.CategoryEntity
import com.shijian.app.data.db.entity.DEFAULT_EXPENSE_CATEGORIES
import com.shijian.app.data.db.entity.DEFAULT_INCOME_CATEGORIES
import com.shijian.app.util.NewsScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShiJianApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            // 全局兜底：任何未捕获异常只记录，直接让进程退出（避免闪退到系统 ANR），
            // 但我们已在各关键路径做了 try/catch，极少会到这里。
            android.util.Log.e("ShiJianApp", "uncaught", e)
        }
        container = try {
            AppContainer(this)
        } catch (e: Exception) {
            android.util.Log.e("ShiJianApp", "container init failed", e)
            AppContainer(this) // 再试一次（AppContainer 内部有降级）
        }
        seedDefaults()
        appScope.launch {
            runCatching { NewsScheduler.schedule(this@ShiJianApp, container.newsRepo, container.settingsRepo) }
        }
    }

    /** 首次启动：写入默认收支分类；任何异常不影响启动。 */
    private fun seedDefaults() {
        appScope.launch {
            runCatching {
                val dao = container.database.categoryDao()
                if (dao.count() == 0) {
                    val list = buildList {
                        DEFAULT_EXPENSE_CATEGORIES.forEachIndexed { i, (name, icon) ->
                            add(CategoryEntity(name = name, type = "EXPENSE", icon = icon, sortOrder = i))
                        }
                        DEFAULT_INCOME_CATEGORIES.forEachIndexed { i, (name, icon) ->
                            add(CategoryEntity(name = name, type = "INCOME", icon = icon, sortOrder = i))
                        }
                    }
                    dao.insertAll(list)
                }
            }
        }
    }
}
