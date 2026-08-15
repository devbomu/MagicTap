package com.magictap.net

/**
 * MAC-address normalization. The design doc (§4) requires input separators (`:`, `-`,
 * or none) to be normalized on entry. The canonical form used everywhere else — stored,
 * transmitted, and signed — is uppercase, colon-separated: `AA:BB:CC:DD:EE:FF`.
 */
object MacUtils {

    private val HEX = "0123456789ABCDEF".toCharArray().toSet()

    /** Canonical form, or null if [input] is not exactly 12 hex nibbles once cleaned. */
    fun normalize(input: String): String? {
        val cleaned = buildString {
            for (c in input.trim().uppercase()) {
                when {
                    c in HEX -> append(c)
                    c == ':' || c == '-' || c == '.' || c == ' ' -> Unit // allowed separators
                    else -> return null // any other character means malformed input
                }
            }
        }
        if (cleaned.length != 12) return null
        return cleaned.chunked(2).joinToString(":")
    }

    fun isValid(input: String): Boolean = normalize(input) != null
}
