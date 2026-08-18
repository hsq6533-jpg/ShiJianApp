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
        container = AppContainer(this)
        seedDefaults()
        appScope.launch { NewsScheduler.schedule(this@ShiJianApp, container.newsRepo, container.settingsRepo) }
    }

    /** 首次启动：写入默认收支分类 */
    private fun seedDefaults() {
        appScope.launch {
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
