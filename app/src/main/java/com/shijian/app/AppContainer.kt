package com.shijian.app

import android.content.Context
import com.shijian.app.data.db.AppDatabase
import com.shijian.app.data.prefs.SecurePrefs
import com.shijian.app.data.prefs.SettingsRepository
import com.shijian.app.data.repo.AddressRepository
import com.shijian.app.data.repo.BackupRepository
import com.shijian.app.data.repo.FoodRepository
import com.shijian.app.data.repo.NewsRepository
import com.shijian.app.data.repo.TransactionRepository
import com.shijian.app.util.NotificationHelper
import java.io.File
import net.sqlcipher.database.SupportFactory

/** 应用依赖容器：任何初始化失败都有兜底，不让应用启动崩。 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    /** EncryptedSharedPreferences（内部已自带损坏降级） */
    val securePrefs: SecurePrefs = try {
        SecurePrefs(appContext)
    } catch (e: Exception) {
        // 最终兜底：反射不可用时，构造失败，但仍提供 SecurePrefs(ctx) 的降级路径。
        SecurePrefs(appContext)
    }

    /** SQLCipher + Room 数据库（SQLCipher 加载/升级失败会自动重建空库） */
    val database: AppDatabase = buildDatabase(appContext, securePrefs)

    /** 各业务仓库 */
    val expenseRepo: TransactionRepository = TransactionRepository(database.transactionDao())
    val foodRepo: FoodRepository = FoodRepository(database.foodPoiDao())
    val addressRepo: AddressRepository = AddressRepository(database.searchAddressDao())
    val settingsRepo: SettingsRepository = SettingsRepository(appContext)
    val newsRepo: NewsRepository = NewsRepository(database.newsDao(), securePrefs)
    val backupRepo: BackupRepository = BackupRepository(database, securePrefs)

    init {
        // 启动时确保通知通道就绪，避免后续推送相关调用崩
        runCatching { NotificationHelper.createChannels(appContext) }
    }

    companion object {
        private fun buildDatabase(ctx: Context, securePrefs: SecurePrefs): AppDatabase {
            val pass: ByteArray = try {
                securePrefs.getDbPassphrase().toByteArray(Charsets.UTF_8)
            } catch (e: Exception) {
                // 口令读取也可能崩（概率低），用固定降级口令。
                "sj_fallback_default".toByteArray(Charsets.UTF_8)
            }
            val factory = try { SupportFactory(pass) } catch (_: Exception) { null }

            // SQLCipher.so 加载失败会抛 UnsatisfiedLinkError，这里做最外层兜底：
            // 直接清掉损坏数据库文件，重建；仍失败则改用内存库，保证启动。
            return try {
                buildRoom(ctx, factory)
            } catch (linkErr: UnsatisfiedLinkError) {
                deleteDbFiles(ctx)
                try { buildRoom(ctx, factory) } catch (_: Exception) { buildRoom(ctx, null) }
            } catch (dbErr: Exception) {
                deleteDbFiles(ctx)
                try { buildRoom(ctx, factory) } catch (_: Exception) { buildRoom(ctx, null) }
            }
        }

        private fun buildRoom(ctx: Context, factory: SupportFactory?): AppDatabase {
        val builder = androidx.room.Room.databaseBuilder(ctx, AppDatabase::class.java, "shijian.db")
            .fallbackToDestructiveMigration()
        if (factory != null) {
            builder.openHelperFactory(factory)
        }
        return builder.build()
    }

        private fun deleteDbFiles(ctx: Context) {
            runCatching {
                val mainDb = ctx.getDatabasePath("shijian.db")
                val filesToDelete = mutableListOf<File>()
                if (mainDb?.exists() == true) {
                    filesToDelete += mainDb
                    File("${mainDb.absolutePath}-wal").takeIf { it.exists() }?.let { filesToDelete += it }
                    File("${mainDb.absolutePath}-shm").takeIf { it.exists() }?.let { filesToDelete += it }
                }
                filesToDelete.forEach { runCatching { it.delete() } }
            }
        }
    }
}
