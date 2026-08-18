package com.shijian.app.util

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份文件加密（AES-256-GCM）
 * 密钥由口令经 PBKDF2 派生；布局：salt(16) + iv(12) + ciphertext
 */
object CryptoUtil {

    private const val ITERATIONS = 100_000
    private const val KEY_LEN = 256
    private const val TAG_BITS = 128

    fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return salt + iv + ct
    }

    fun decrypt(data: ByteArray, password: String): ByteArray {
        require(data.size > 16 + 12) { "数据损坏" }
        val salt = data.copyOfRange(0, 16)
        val iv = data.copyOfRange(16, 28)
        val ct = data.copyOfRange(28, data.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
