package com.example.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts sensitive credentials (API keys and session cookies) at rest using
 * Android Keystore AES-256-GCM before storing them in DataStore.
 *
 * Both directions return null when the operation could not be performed. A failure to
 * encrypt must never be stored as if it were a credential (that would replace a working
 * key with an empty string), and a failure to decrypt must be distinguishable from "no
 * credential saved".
 */
object CryptoHelper {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "itsaplan_credentials_aes_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SEPARATOR = ":"
    private const val GCM_TAG_BITS = 128

    /** Guards first-use key creation so two threads cannot each generate a key. */
    private val keyLock = Any()

    private fun loadExistingKey(keyStore: KeyStore): SecretKey? =
        runCatching { keyStore.getKey(KEY_ALIAS, null) as? SecretKey }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        loadExistingKey(keyStore)?.let { return it }

        synchronized(keyLock) {
            // Re-check inside the lock: another thread may have just created it.
            loadExistingKey(keyStore)?.let { return it }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
    }

    /** @return the stored form, or null when encryption is unavailable. */
    fun encrypt(plainText: String): String? {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val dataB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            "$ivB64$IV_SEPARATOR$dataB64"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * @return the plaintext, "" when nothing was stored, or null when the ciphertext
     * could not be read (wrong key, corrupted value, keystore unavailable).
     */
    fun decrypt(cipherText: String): String? {
        if (cipherText.isEmpty()) return ""
        val parts = cipherText.split(IV_SEPARATOR)
        if (parts.size != 2) return null
        return try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
