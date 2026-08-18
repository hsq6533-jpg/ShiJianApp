package com.shijian.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 密钥安全存储（8.2）
 * - 高德 Key / DeepSeek Key / 数据库口令 使用 EncryptedSharedPreferences 加密
 * - AES-256-GCM，主密钥由 Android Keystore 保管，不出设备
 */
class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "shijian_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAmapKey(): String? = prefs.getString(KEY_AMAP, null)?.takeIf { it.isNotBlank() }

    fun setAmapKey(key: String?) {
        prefs.edit().putString(KEY_AMAP, key).apply()
    }

    fun getDeepSeekKey(): String? = prefs.getString(KEY_DEEPSEEK, null)?.takeIf { it.isNotBlank() }

    fun setDeepSeekKey(key: String?) {
        prefs.edit().putString(KEY_DEEPSEEK, key).apply()
    }

    /** 数据库口令：首次生成随机值并加密保存（8.1） */
    fun getDbPassphrase(): String {
        prefs.getString(KEY_DB_PASS, null)?.let { if (it.isNotBlank()) return it }
        val generated = "sj_" + (0..5).joinToString("") { ALPHABET.random().toString() }
        prefs.edit().putString(KEY_DB_PASS, generated).apply()
        return generated
    }

    /** 备份加密密钥（与数据库同源） */
    fun getBackupKey(): String = getDbPassphrase()

    companion object {
        private const val KEY_AMAP = "key_amap"
        private const val KEY_DEEPSEEK = "key_deepseek"
        private const val KEY_DB_PASS = "key_db_pass"
        private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }
}
