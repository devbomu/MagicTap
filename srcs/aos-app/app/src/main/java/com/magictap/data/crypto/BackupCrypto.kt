package com.magictap.data.crypto

import android.util.Base64
import com.magictap.data.model.AppData
import com.magictap.data.model.Profile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Export / import format for backups (design doc §9). A single JSON envelope wraps the
 * app document. Three modes:
 *
 *  - **encrypted** (default): PBKDF2-HMAC-SHA256 (200k iterations) derives an AES-256
 *    key from the user's passphrase; the document is sealed with AES-256-GCM.
 *  - **secret-excluded**: plaintext structure with every [Profile.secret] blanked.
 *  - **plain**: plaintext structure as-is (offered, but never the default).
 */
object BackupCrypto {

    private const val APP = "MagicTap"
    private const val TYPE = "backup"
    const val SCHEMA = 1

    private const val ENC_AES = "pbkdf2-hmacsha256-aes256-gcm"
    private const val ENC_NONE = "none"

    private const val PBKDF2_ITERATIONS = 200_000

    // Upper bound on the iteration count accepted from an (untrusted) backup envelope, so a
    // malicious file can't pin the app in a multi-billion-round PBKDF2 on import.
    private const val MAX_ITERATIONS = 1_000_000
    private const val KEY_BITS = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Serializable
    private data class Envelope(
        val app: String = APP,
        val type: String = TYPE,
        val schema: Int = SCHEMA,
        val encryption: String,
        val salt: String? = null,
        val iterations: Int? = null,
        val iv: String? = null,
        val ciphertext: String? = null,
        val data: AppData? = null,
    )

    class BackupException(val kind: Kind, cause: Throwable? = null) : Exception(kind.name, cause) {
        enum class Kind { WRONG_PASSWORD, MALFORMED, UNSUPPORTED_VERSION }
    }

    // ---- Export ----

    fun exportEncrypted(data: AppData, password: CharArray): String {
        val salt = randomBytes(SALT_LENGTH)
        val key = deriveKey(password, salt)
        val iv = randomBytes(IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val plaintext = json.encodeToString(AppData.serializer(), data).encodeToByteArray()
        val ciphertext = cipher.doFinal(plaintext)
        return json.encodeToString(
            Envelope.serializer(),
            Envelope(
                encryption = ENC_AES,
                salt = salt.b64(),
                iterations = PBKDF2_ITERATIONS,
                iv = iv.b64(),
                ciphertext = ciphertext.b64(),
            ),
        )
    }

    fun exportPlain(data: AppData): String =
        json.encodeToString(Envelope.serializer(), Envelope(encryption = ENC_NONE, data = data))

    fun exportWithoutSecrets(data: AppData): String =
        exportPlain(data.copy(profiles = data.profiles.map { it.copy(secret = "") }))

    // ---- Import ----

    /** True if [text] is an encrypted backup and a password is required to decode it. */
    fun isEncrypted(text: String): Boolean = runCatching { parse(text).encryption == ENC_AES }
        .getOrDefault(false)

    /**
     * Decodes a backup document. [password] is required iff [isEncrypted] returns true.
     * Throws [BackupException] on bad password, malformed input, or unsupported version.
     */
    fun decode(text: String, password: CharArray? = null): AppData {
        val envelope = parse(text)
        if (envelope.schema > SCHEMA) {
            throw BackupException(BackupException.Kind.UNSUPPORTED_VERSION)
        }
        return when (envelope.encryption) {
            ENC_NONE -> envelope.data
                ?: throw BackupException(BackupException.Kind.MALFORMED)

            ENC_AES -> decrypt(envelope, password)
            else -> throw BackupException(BackupException.Kind.MALFORMED)
        }
    }

    private fun decrypt(envelope: Envelope, password: CharArray?): AppData {
        val salt = envelope.salt?.unb64()
        val iv = envelope.iv?.unb64()
        val ciphertext = envelope.ciphertext?.unb64()
        val iterations = envelope.iterations ?: PBKDF2_ITERATIONS
        if (password == null || salt == null || iv == null || ciphertext == null ||
            iterations !in 1..MAX_ITERATIONS
        ) {
            throw BackupException(BackupException.Kind.MALFORMED)
        }
        return try {
            val key = deriveKey(password, salt, iterations)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            val plaintext = cipher.doFinal(ciphertext).decodeToString()
            json.decodeFromString(AppData.serializer(), plaintext)
        } catch (e: Exception) {
            // A wrong passphrase surfaces as a GCM tag mismatch (AEADBadTagException).
            throw BackupException(BackupException.Kind.WRONG_PASSWORD, e)
        }
    }

    private fun parse(text: String): Envelope = try {
        json.decodeFromString(Envelope.serializer(), text).also {
            if (it.app != APP || it.type != TYPE) {
                throw BackupException(BackupException.Kind.MALFORMED)
            }
        }
    } catch (e: BackupException) {
        throw e
    } catch (e: Exception) {
        throw BackupException(BackupException.Kind.MALFORMED, e)
    }

    // ---- helpers ----

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun randomBytes(n: Int): ByteArray = ByteArray(n).also { SecureRandom().nextBytes(it) }

    private fun ByteArray.b64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.unb64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
