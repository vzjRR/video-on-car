package com.iptvcar.core.security

private val SENSITIVE_QUERY_KEYS = listOf("username", "password", "token", "pass", "user")

/**
 * Masks credential-bearing query parameters before a URL is ever logged.
 * Never log a provider/stream URL without passing it through this first.
 */
fun redactUrl(url: String): String {
    val queryStart = url.indexOf('?')
    if (queryStart == -1) return url
    val base = url.substring(0, queryStart)
    val query = url.substring(queryStart + 1)
    val redactedQuery = query.split('&').joinToString("&") { pair ->
        val eq = pair.indexOf('=')
        if (eq == -1) return@joinToString pair
        val key = pair.substring(0, eq)
        if (SENSITIVE_QUERY_KEYS.any { it.equals(key, ignoreCase = true) }) {
            "$key=***"
        } else {
            pair
        }
    }
    return "$base?$redactedQuery"
}

/** Also strips Xtream's path-embedded credentials: /live/USER/PASS/id.ext */
fun redactPathCredentials(url: String): String {
    val pathCredentialRegex = Regex("/(live|movie|series)/[^/]+/[^/]+/")
    return pathCredentialRegex.replace(url) { m -> "/${m.groupValues[1]}/***/***/ " }.trim()
}

fun redact(url: String): String = redactPathCredentials(redactUrl(url))
