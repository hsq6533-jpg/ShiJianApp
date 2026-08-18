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
        // 全局兜底：保存系统原 Handler，先记录日志，再交给系统处理（避免 ANR，确保能正常崩溃上报）
        val sysHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            android.util.Log.e("ShiJianApp", "uncaught on thread=${t.name}", e)
            // 关键：交给系统原有 handler 处理（弹出崩溃对话框 / 上报 / 杀进程）
            // 如果系统 handler 不可用，就直接杀进程防止 ANR
            runCatching { sysHandler?.uncaughtException(t, e) }.onFailure {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(1)
            }
        }
        container = try {
            AppContainer(this)
        } catch (e: Exception) {
            android.util.Log.e("ShiJianApp", "container init failed, retry once", e)
            runCatching { AppContainer(this) }.getOrElse { e2 ->
                // 两次都失败：极端情况（Keystore/SQLCipher/磁盘全挂），让进程退出而不是在后续生命周期里随机崩
                android.util.Log.e("ShiJianApp", "container init retry failed, abort", e2)
                throw RuntimeException("AppContainer init failed after retry", e2)
            }
        }
        runCatching { seedDefaults() }
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
