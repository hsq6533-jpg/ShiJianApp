package com.shijian.app

import android.content.Context
import androidx.room.Room
import com.shijian.app.data.db.AppDatabase
import com.shijian.app.data.prefs.SecurePrefs
import com.shijian.app.data.prefs.SettingsRepository
import com.shijian.app.data.repo.AddressRepository
import com.shijian.app.data.repo.BackupRepository
import com.shijian.app.data.repo.FoodRepository
import com.shijian.app.data.repo.NewsRepository
import com.shijian.app.data.repo.TransactionRepository
import net.zetetic.android.database.sqlcipher.SupportFactory

/**
 * 手动依赖容器（无 Hilt，保持轻量）
 * 数据库口令由 Keystore 加密保管，SQLCipher 加密落盘（8.1）
 */
class AppContainer(context: Context) {

    val securePrefs = SecurePrefs(context)

    val settingsRepo = SettingsRepository(context)

    val database: AppDatabase = run {
        val passphrase = securePrefs.getDbPassphrase()
        Room.databaseBuilder(context, AppDatabase::class.java, "shijian.db")
            .openHelperFactory(SupportFactory(passphrase.toByteArray()))
            .fallbackToDestructiveMigration()
            .build()
    }

    val transactionRepo = TransactionRepository(database.transactionDao())
    val addressRepo = AddressRepository(database.searchAddressDao())
    val foodRepo = FoodRepository(database.foodPoiDao())
    val newsRepo = NewsRepository(database.newsDao(), securePrefs)
    val backupRepo = BackupRepository(database, securePrefs)
}
