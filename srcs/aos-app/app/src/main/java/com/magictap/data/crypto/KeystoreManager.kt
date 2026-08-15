package com.magictap.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * At-rest encryption for the app document, keyed by a non-exportable AES-256 key
 * held in the Android Keystore.
 *
 * The design doc (§8) explicitly rules out the deprecated `androidx.security-crypto`
 * (EncryptedSharedPreferences) library and requires using the Keystore API directly.
 *
 * The key is created without a user-authentication requirement on purpose: home-screen
 * widgets must be able to read the PC list and send a wake without the device being
 * unlocked. The threat model here is "someone dumps app files off the device", which
 * a Keystore-bound key defends against; it is not "attacker has an unlocked phone".
 */
object KeystoreManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "magictap_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    /** Encrypts [plain], returning `IV || ciphertext||tag`. */
    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv // GCM generates a fresh 12-byte IV per operation
        val body = cipher.doFinal(plain)
        return iv + body
    }

    /** Inverse of [encrypt]. Throws if the blob is corrupt or the key was invalidated. */
    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > IV_LENGTH) { "ciphertext too short" }
        val iv = blob.copyOfRange(0, IV_LENGTH)
        val body = blob.copyOfRange(IV_LENGTH, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(body)
    }

    private fun getOrCreateKey(): SecretKey {
        val keystore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keystore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
