package com.magictap.data.model

import kotlinx.serialization.Serializable

/**
 * The entire app state is a single JSON document (see the design doc §4). There is
 * no database. This document is what gets encrypted at rest and what export/import
 * round-trips.
 */
@Serializable
data class AppData(
    val version: Int = CURRENT_VERSION,
    val profiles: List<Profile> = emptyList(),
) {
    companion object {
        /** Bump when the schema changes; import validates against this. */
        const val CURRENT_VERSION = 1
    }
}

/**
 * One home / one Pico W agent. PCs belong to a profile and are deleted with it.
 *
 * @param secret Base64-encoded 32-byte HMAC key, shared with the Pico W's config.json.
 *               May be blank when imported from a "secret-excluded" backup.
 */
@Serializable
data class Profile(
    val id: String,
    val alias: String,
    val internalHost: String = "",
    val externalHost: String = "",
    val externalPort: Int = 18080,
    val internalPort: Int = 80,
    val secret: String = "",
    val pcs: List<Pc> = emptyList(),
) {
    val hasInternal: Boolean get() = internalHost.isNotBlank()
    val hasExternal: Boolean get() = externalHost.isNotBlank()
}

/**
 * A wake target, identified solely by MAC address. [mac] is always stored in the
 * canonical `AA:BB:CC:DD:EE:FF` form (see [com.magictap.net.MacUtils]).
 */
@Serializable
data class Pc(
    val id: String,
    val alias: String,
    val mac: String,
)
