package com.magictap.net

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Request authentication (design doc §5.1):
 *
 * ```
 * sig = HMAC-SHA256(secret, mac + "|" + ts)
 * ```
 *
 * `mac` is the canonical address string (see [MacUtils]) exactly as sent in the request
 * body, `ts` is Unix time in seconds, and the signature is lowercase hex. The Pico W
 * recomputes this and additionally enforces a ±60 s window and a replay ring-buffer, so
 * plaintext HTTP is acceptable: the secret never travels and captured requests can't be
 * replayed.
 */
object HmacSigner {

    private const val ALGORITHM = "HmacSHA256"
    private const val SECRET_BYTES = 32

    fun sign(secretBase64: String, mac: String, ts: Long): String {
        val key = Base64.decode(secretBase64, Base64.NO_WRAP)
        val hmac = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(key, ALGORITHM)) }
        val digest = hmac.doFinal("$mac|$ts".toByteArray(Charsets.UTF_8))
        return digest.toHexLower()
    }

    /** Fresh Base64-encoded 32-byte secret for a new profile (§4). */
    fun newSecret(): String {
        val bytes = ByteArray(SECRET_BYTES).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun ByteArray.toHexLower(): String {
        val out = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
