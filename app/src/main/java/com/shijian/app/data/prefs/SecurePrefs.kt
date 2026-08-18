package com.shijian.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * 密钥安全存储（8.2）
 * - 高德 Key / DeepSeek Key / 数据库口令 使用 EncryptedSharedPreferences 加密
 * - AES-256-GCM，主密钥由 Android Keystore 保管，不出设备
 * - EncryptedSharedPreferences 损坏时自动降级为明文 SharedPreferences（安全等级略降但不崩）
 *
 * 构造入口：
 * - SecurePrefs(ctx)            → 默认：尝试加密 → 删除重建 → 明文降级
 * - SecurePrefs(ctx, forcePlain = true) → 强制明文模式（AppContainer 终极兜底，不递归）
 */
class SecurePrefs {

    private val appContext: Context
    private val prefs: SharedPreferences

    /** 默认构造：尝试加密 -> 重建 -> 明文降级 */
    constructor(context: Context) {
        appContext = context.applicationContext
        prefs = try {
            createEncryptedPrefs(appContext)
        } catch (e: Exception) {
            // EncryptedSharedPreferences 损坏（如 Keystore 不可用或文件损坏）时，删除损坏文件并重建；
            // 重建仍失败则降级为普通 SharedPreferences。
            try {
                val badDir = File(appContext.filesDir.parentFile, "shared_prefs")
                File(badDir, "shijian_secure.xml").takeIf { it.exists() }?.delete()
                File(badDir, "shijian_secure.bak").takeIf { it.exists() }?.delete()
            } catch (_: Exception) { /* ignore */ }
            try {
                createEncryptedPrefs(appContext)
            } catch (_: Exception) {
                appContext.getSharedPreferences("shijian_secure_plain", Context.MODE_PRIVATE)
            }
        }
    }

    /** 强制明文模式（AppContainer 终极兜底用，不做任何加密尝试，确保 100% 不抛） */
    constructor(context: Context, @Suppress("UNUSED_PARAMETER") forcePlain: Boolean) {
        appContext = context.applicationContext
        prefs = try {
            // 强制直接用明文，不走加密路径，用于 Keystore 彻底损坏时仍能启动
            appContext.getSharedPreferences("shijian_secure_plain", Context.MODE_PRIVATE)
        } catch (_: Throwable) {
            // 极端终极兜底：任何 getSharedPreferences 都失败时，改用无磁盘的 MemoryPrefs（内存模式）
            MemoryPrefs()
        }
    }

    private fun createEncryptedPrefs(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx,
            "shijian_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAmapKey(): String? = runCatching {
        prefs.getString(KEY_AMAP, null)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_AMAP_KEY.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun setAmapKey(key: String?) {
        runCatching { prefs.edit().putString(KEY_AMAP, key).apply() }
    }

    fun getDeepSeekKey(): String? = runCatching {
        prefs.getString(KEY_DEEPSEEK, null)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun setDeepSeekKey(key: String?) {
        runCatching { prefs.edit().putString(KEY_DEEPSEEK, key).apply() }
    }

    /** 数据库口令：首次生成随机值并加密保存（8.1） */
    fun getDbPassphrase(): String {
        val existing = runCatching { prefs.getString(KEY_DB_PASS, null)?.takeIf { it.isNotBlank() } }.getOrNull()
        if (existing != null) return existing
        val generated = "sj_" + (0..5).joinToString("") { ALPHABET.random().toString() }
        runCatching { prefs.edit().putString(KEY_DB_PASS, generated).apply() }
        return generated
    }

    /** 备份加密密钥（与数据库同源） */
    fun getBackupKey(): String = getDbPassphrase()

    companion object {
        private const val KEY_AMAP = "key_amap"
        private const val KEY_DEEPSEEK = "key_deepseek"
        private const val KEY_DB_PASS = "key_db_pass"
        private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        /** 预置的高德 Web 服务 Key（用户提供）；读取 EncryptedPrefs 用户自定义 Key 优先。 */
        private const val DEFAULT_AMAP_KEY = "b2f697671d23f65dcbf6f74d0ae3ec0c"
    }
}

/**
 * 纯内存 SharedPreferences 兜底（极端情况：连 getSharedPreferences 都失败时，保证应用仍能启动）。
 * 仅实现 SecurePrefs 用到的 getString / edit 相关方法，其余返回空值或空集合。
 */
internal class MemoryPrefs : SharedPreferences {
    private val map = java.util.concurrent.ConcurrentHashMap<String, Any?>()
    override fun getAll(): MutableMap<String, *> = map.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = map[key]?.toString() ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (map[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = MemoryEditor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
}

private class MemoryEditor(private val map: java.util.concurrent.ConcurrentHashMap<String, Any?>) : SharedPreferences.Editor {
    private val pending = LinkedHashMap<String, Any?>()
    private val removes = LinkedHashSet<String>()
    private var clearAll = false
    override fun putString(key: String?, value: String?): SharedPreferences.Editor { key?.let { pending[it] = value }; return this }
    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { key?.let { pending[it] = values }; return this }
    override fun putInt(key: String?, value: Int): SharedPreferences.Editor { key?.let { pending[it] = value }; return this }
    override fun putLong(key: String?, value: Long): SharedPreferences.Editor { key?.let { pending[it] = value }; return this }
    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { key?.let { pending[it] = value }; return this }
    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { key?.let { pending[it] = value }; return this }
    override fun remove(key: String?): SharedPreferences.Editor { key?.let { removes.add(it) }; return this }
    override fun clear(): SharedPreferences.Editor { clearAll = true; return this }
    override fun commit(): Boolean { apply(); return true }
    override fun apply() {
        if (clearAll) map.clear()
        removes.forEach { map.remove(it) }
        map.putAll(pending)
    }
}
