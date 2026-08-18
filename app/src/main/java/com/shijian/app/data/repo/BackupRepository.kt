package com.shijian.app.data.repo

import com.shijian.app.data.db.AppDatabase
import com.shijian.app.data.db.entity.CategoryEntity
import com.shijian.app.data.db.entity.FoodPoiEntity
import com.shijian.app.data.db.entity.NewsConfigEntity
import com.shijian.app.data.db.entity.SearchAddressEntity
import com.shijian.app.data.db.entity.TransactionEntity
import com.shijian.app.data.prefs.SecurePrefs
import com.shijian.app.util.CryptoUtil
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 备份与恢复（6.4 / 8.5）
 * - 导出：JSON（可选 AES-256-GCM 加密）
 * - 导入：自动识别加密并解密，覆盖式恢复
 */
class BackupRepository(
    private val db: AppDatabase,
    private val securePrefs: SecurePrefs
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** 导出备份，返回字节流 */
    suspend fun export(encrypted: Boolean): ByteArray {
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            transactions = db.transactionDao().run { observeAll().firstOrNull() ?: emptyList() },
            categories = db.categoryDao().getAll(),
            addresses = db.searchAddressDao().run { observeAll().firstOrNull() ?: emptyList() },
            foodPois = db.foodPoiDao().run { observeAll().firstOrNull() ?: emptyList() },
            newsConfig = db.newsDao().getConfig()
        )
        val bytes = json.encodeToString(BackupData.serializer(), data).toByteArray(Charsets.UTF_8)
        return if (encrypted) CryptoUtil.encrypt(bytes, securePrefs.getBackupKey()) else bytes
    }

    /** 导入备份（覆盖式） */
    suspend fun import(bytes: ByteArray): BackupData {
        val raw = try {
            json.decodeFromString(BackupData.serializer(), bytes.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            val decrypted = CryptoUtil.decrypt(bytes, securePrefs.getBackupKey())
            json.decodeFromString(BackupData.serializer(), decrypted.toString(Charsets.UTF_8))
        }
        db.transactionDao().clearAll()
        db.categoryDao().insertAll(raw.categories)
        db.searchAddressDao().let { dao ->
            val list = dao.observeAll().firstOrNull() ?: emptyList()
            list.forEach { dao.delete(it) }
            raw.addresses.forEach { dao.insert(it) }
        }
        db.foodPoiDao().clearAll()
        db.foodPoiDao().upsertAll(raw.foodPois)
        raw.newsConfig?.let { db.newsDao().saveConfig(it) }
        db.transactionDao().insertAll(raw.transactions)
        return raw
    }

    @Serializable
    data class BackupData(
        val version: Int = 1,
        val exportedAt: Long = 0,
        val transactions: List<TransactionEntity> = emptyList(),
        val categories: List<CategoryEntity> = emptyList(),
        val addresses: List<SearchAddressEntity> = emptyList(),
        val foodPois: List<FoodPoiEntity> = emptyList(),
        val newsConfig: NewsConfigEntity? = null
    )
}
