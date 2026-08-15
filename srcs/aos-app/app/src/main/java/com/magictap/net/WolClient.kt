package com.magictap.net

import android.os.SystemClock
import com.magictap.data.model.Pc
import com.magictap.data.model.Profile
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/** Result of a wake attempt. The UI maps these to localized toasts. */
sealed interface WakeOutcome {
    data object Success : WakeOutcome
    data object AuthFailed : WakeOutcome
    data object Unreachable : WakeOutcome
    data object Timeout : WakeOutcome
    data class Error(val message: String) : WakeOutcome
}

/** Result of a `GET /ping`, used by the connection test and internal-network probe. */
sealed interface PingOutcome {
    data class Ok(val latencyMs: Long, val firmware: String) : PingOutcome
    data class Failed(val reason: String) : PingOutcome
}

/**
 * Result of a `POST /verify`. [Rejected] means a genuine secret mismatch, while
 * [ClockError] means the Pico's clock isn't NTP-synced (a stale-timestamp rejection) —
 * distinct causes that otherwise both look like "wrong secret".
 */
enum class VerifyOutcome { Verified, Rejected, ClockError, Unknown }

/**
 * Sends wake requests to the Pico W and probes it with `/ping`.
 *
 * The internal/external switch (design doc §5.3) is automatic and invisible to the user:
 * a fast internal `/ping` decides whether we're on the home LAN; if so we wake via the
 * internal address, otherwise we fall back to the external DDNS address. Internal is
 * always tried first because home Wi-Fi + no NAT-hairpin would make the external address
 * fail.
 *
 * A fresh timestamp+signature is generated per HTTP attempt, so the internal→external
 * fallback can never trip the Pico W's replay filter.
 */
class WolClient(baseClient: OkHttpClient = OkHttpClient()) {

    // newBuilder() shares the connection pool and dispatcher across the per-timeout clients.
    private val base = baseClient
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun wake(profile: Profile, pc: Pc): WakeOutcome {
        if (profile.secret.isBlank()) return WakeOutcome.Error("secret not set")

        if (profile.hasInternal) {
            val probe = ping(profile.internalHost, profile.internalPort, INTERNAL_PING_TIMEOUT_MS)
            if (probe is PingOutcome.Ok) {
                val result = postWake(
                    profile.internalHost, profile.internalPort, profile.secret, pc.mac,
                    INTERNAL_WAKE_TIMEOUT_MS,
                )
                // A definitive answer from the Pico W ends it; only a transport failure
                // falls through to the external address.
                when (result) {
                    is WakeOutcome.Success, is WakeOutcome.AuthFailed -> return result
                    else -> Unit
                }
            }
        }

        if (profile.hasExternal) {
            return postWake(
                profile.externalHost, profile.externalPort, profile.secret, pc.mac,
                EXTERNAL_WAKE_TIMEOUT_MS,
            )
        }

        return WakeOutcome.Unreachable
    }

    suspend fun ping(host: String, port: Int, timeoutMs: Long): PingOutcome = withContext(Dispatchers.IO) {
        val client = clientWithTimeout(timeoutMs)
        val start = SystemClock.elapsedRealtime()
        try {
            val request = Request.Builder().url("http://$host:$port/ping").get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    PingOutcome.Failed("HTTP ${response.code}")
                } else {
                    val parsed = runCatching {
                        json.decodeFromString(PingResponse.serializer(), body)
                    }.getOrNull()
                    if (parsed?.ok == true) {
                        PingOutcome.Ok(SystemClock.elapsedRealtime() - start, parsed.fw)
                    } else {
                        PingOutcome.Failed("bad response")
                    }
                }
            }
        } catch (e: Exception) {
            PingOutcome.Failed(e.reason())
        }
    }

    /**
     * Confirms the profile's secret against the Pico W's `/verify` (no side effect). A
     * fixed sentinel MAC is signed — verification only needs to prove the shared key. An
     * older firmware without `/verify` answers 404, surfaced as [VerifyOutcome.Unknown] so
     * the caller can treat "secret unproven" differently from "secret rejected".
     */
    suspend fun verify(host: String, port: Int, secret: String, timeoutMs: Long): VerifyOutcome =
        withContext(Dispatchers.IO) {
            if (secret.isBlank()) return@withContext VerifyOutcome.Unknown
            val ts = System.currentTimeMillis() / 1000
            // Random nonce in the MAC field so parallel internal+external verifies never
            // sign the same (mac, ts): an identical signature trips the Pico's replay filter
            // and surfaces as a false "secret mismatch". /verify ignores the MAC value.
            val mac = randomNonceMac()
            val sig = HmacSigner.sign(secret, mac, ts)
            val payload = json.encodeToString(WakeRequest.serializer(), WakeRequest(mac, ts, sig))
            val client = clientWithTimeout(timeoutMs)
            try {
                val request = Request.Builder()
                    .url("http://$host:$port/verify")
                    .post(payload.toRequestBody(jsonMedia))
                    .build()
                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> VerifyOutcome.Verified
                        response.code == 401 -> {
                            val err = runCatching {
                                json.decodeFromString(WakeResponse.serializer(), response.body?.string().orEmpty()).err
                            }.getOrNull()
                            if (err == "clock") VerifyOutcome.ClockError else VerifyOutcome.Rejected
                        }

                        else -> VerifyOutcome.Unknown
                    }
                }
            } catch (e: Exception) {
                VerifyOutcome.Unknown
            }
        }

    private suspend fun postWake(
        host: String,
        port: Int,
        secret: String,
        mac: String,
        timeoutMs: Long,
    ): WakeOutcome = withContext(Dispatchers.IO) {
        val ts = System.currentTimeMillis() / 1000
        val sig = HmacSigner.sign(secret, mac, ts)
        val payload = json.encodeToString(WakeRequest.serializer(), WakeRequest(mac, ts, sig))
        val client = clientWithTimeout(timeoutMs)
        try {
            val request = Request.Builder()
                .url("http://$host:$port/wake")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> WakeOutcome.Success
                    response.code == 401 -> WakeOutcome.AuthFailed
                    else -> WakeOutcome.Error("HTTP ${response.code}")
                }
            }
        } catch (e: SocketTimeoutException) {
            WakeOutcome.Timeout
        } catch (e: InterruptedIOException) {
            // OkHttp's callTimeout surfaces as InterruptedIOException("timeout").
            WakeOutcome.Timeout
        } catch (e: Exception) {
            WakeOutcome.Unreachable
        }
    }

    private fun clientWithTimeout(timeoutMs: Long): OkHttpClient = base.newBuilder()
        .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build()

    private fun Exception.reason(): String = when (this) {
        is SocketTimeoutException, is InterruptedIOException -> "timeout"
        else -> message ?: this::class.java.simpleName
    }

    /** Throwaway random MAC-shaped nonce for [verify] — keeps each verify signature unique. */
    private fun randomNonceMac(): String =
        Random.nextBytes(6).joinToString(":") { "%02x".format(it.toInt() and 0xFF) }

    companion object {
        const val INTERNAL_PING_TIMEOUT_MS = 300L
        const val INTERNAL_WAKE_TIMEOUT_MS = 3_000L
        const val EXTERNAL_WAKE_TIMEOUT_MS = 5_000L

        /** Generous timeouts for the explicit connection test in the profile editor. */
        const val TEST_INTERNAL_TIMEOUT_MS = 1_500L
        const val TEST_EXTERNAL_TIMEOUT_MS = 5_000L
    }
}
