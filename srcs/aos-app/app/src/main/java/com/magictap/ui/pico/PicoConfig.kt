package com.magictap.ui.pico

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Mirror of the Pico W firmware's `config.json` schema (see `backend-api/config.py`). The
 * app generates this from a profile's secret/host plus Wi-Fi and network details, so the
 * user never hand-edits JSON or copies the secret by hand.
 */
@Serializable
data class PicoConfig(
    @SerialName("wifi_ssid") val wifiSsid: String,
    @SerialName("wifi_pass") val wifiPass: String,
    @SerialName("static_ip") val staticIp: String,
    val subnet: String,
    val gateway: String,
    val dns: String,
    @SerialName("http_port") val httpPort: Int,
    val secret: String,
    @SerialName("duckdns_domain") val duckdnsDomain: String,
    @SerialName("duckdns_token") val duckdnsToken: String,
    @SerialName("ntp_host") val ntpHost: String,
)

private val prettyJson = Json { prettyPrint = true; encodeDefaults = true }

fun PicoConfig.toJson(): String = prettyJson.encodeToString(this)

/**
 * Best-effort network defaults read from the phone's current Wi-Fi. Gateway/subnet/DNS
 * need no permission; the SSID needs location access, so it may come back blank (the user
 * then types it). [staticIpGuess] just reuses the gateway's /24 with a high host octet.
 */
data class NetSuggestion(
    val ssid: String = "",
    val gateway: String = "",
    val subnet: String = "255.255.255.0",
    val dns: String = "8.8.8.8",
    val staticIpGuess: String = "",
)

@Suppress("DEPRECATION")
fun suggestNetwork(context: Context): NetSuggestion {
    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        ?: return NetSuggestion()

    fun fmt(a: Int): String =
        "${a and 0xff}.${(a shr 8) and 0xff}.${(a shr 16) and 0xff}.${(a shr 24) and 0xff}"

    val dhcp = try {
        wifi.dhcpInfo
    } catch (e: Exception) {
        null
    }
    val gateway = if (dhcp != null && dhcp.gateway != 0) fmt(dhcp.gateway) else ""
    val subnet = if (dhcp != null && dhcp.netmask != 0) fmt(dhcp.netmask) else "255.255.255.0"
    // Always default to a public resolver. The phone's own DNS is usually the home router,
    // which the Pico frequently can't use for NTP / DuckDNS name lookups (a stuck clock is
    // the #1 setup failure). The user can still change it.
    val dns = "8.8.8.8"
    val staticGuess = if (gateway.contains('.')) "${gateway.substringBeforeLast('.')}.50" else ""

    val rawSsid = try {
        wifi.connectionInfo?.ssid.orEmpty()
    } catch (e: Exception) {
        ""
    }
    val ssid = rawSsid.trim('"').let { if (it.isBlank() || it.contains("unknown", ignoreCase = true)) "" else it }

    return NetSuggestion(ssid = ssid, gateway = gateway, subnet = subnet, dns = dns, staticIpGuess = staticGuess)
}
