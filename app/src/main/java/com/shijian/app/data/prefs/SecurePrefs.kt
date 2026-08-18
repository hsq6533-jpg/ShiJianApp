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
 */
class SecurePrefs(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences = try {
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
