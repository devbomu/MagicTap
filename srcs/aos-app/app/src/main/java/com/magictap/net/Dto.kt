package com.magictap.net

import kotlinx.serialization.Serializable

/** `POST /wake` request body (design doc §5.2). */
@Serializable
data class WakeRequest(
    val mac: String,
    val ts: Long,
    val sig: String,
)

/** `POST /wake` response body. */
@Serializable
data class WakeResponse(
    val ok: Boolean = false,
    val err: String? = null,
)

/** `GET /ping` response body — used for connection tests and internal-network detection. */
@Serializable
data class PingResponse(
    val ok: Boolean = false,
    val uptime: Long = 0,
    val fw: String = "",
)
