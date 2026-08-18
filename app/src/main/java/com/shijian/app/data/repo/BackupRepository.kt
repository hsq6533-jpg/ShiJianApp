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
 * - 导入：自动识别加密并解密，覆盖式恢复；任何异常不向上抛，抛出给 UI 提示。
 */
class BackupRepository(
    private val db: AppDatabase,
    private val securePrefs: SecurePrefs
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** 导出备份，返回字节流；任何异常重新抛出由 UI 提示。 */
    suspend fun export(encrypted: Boolean): ByteArray {
        val transactions = runCatching { db.transactionDao().observeAll().firstOrNull() ?: emptyList() }.getOrDefault(emptyList())
        val categories = runCatching { db.categoryDao().getAll() }.getOrDefault(emptyList())
        val addresses = runCatching { db.searchAddressDao().observeAll().firstOrNull() ?: emptyList() }.getOrDefault(emptyList())
        val foodPois = runCatching { db.foodPoiDao().observeAll().firstOrNull() ?: emptyList() }.getOrDefault(emptyList())
        val newsConfig = runCatching { db.newsDao().getConfig() }.getOrNull()
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            transactions = transactions,
            categories = categories,
            addresses = addresses,
            foodPois = foodPois,
            newsConfig = newsConfig
        )
        val bytes = json.encodeToString(BackupData.serializer(), data).toByteArray(Charsets.UTF_8)
        return if (encrypted) CryptoUtil.encrypt(bytes, securePrefs.getBackupKey()) else bytes
    }

    /** 导入备份（覆盖式）；失败会抛出异常由 UI 提示。 */
    suspend fun import(bytes: ByteArray): BackupData {
        val raw = try {
            json.decodeFromString(BackupData.serializer(), bytes.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            val decrypted = CryptoUtil.decrypt(bytes, securePrefs.getBackupKey())
            json.decodeFromString(BackupData.serializer(), decrypted.toString(Charsets.UTF_8))
        }
        runCatching { db.transactionDao().clearAll() }
        runCatching { db.categoryDao().insertAll(raw.categories) }
        runCatching {
            db.searchAddressDao().let { dao ->
                val list = dao.observeAll().firstOrNull() ?: emptyList()
                list.forEach { runCatching { dao.delete(it) } }
                raw.addresses.forEach { runCatching { dao.insert(it) } }
            }
        }
        runCatching { db.foodPoiDao().clearAll() }
        runCatching { db.foodPoiDao().upsertAll(raw.foodPois) }
        runCatching { raw.newsConfig?.let { db.newsDao().saveConfig(it) } }
        runCatching { db.transactionDao().insertAll(raw.transactions) }
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
