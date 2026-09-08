package com.iptvcar.core.provider.xtream

data class XtreamCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)

sealed class XtreamError : Exception() {
    data class AuthenticationFailed(override val message: String) : XtreamError()
    data class NetworkFailure(override val message: String, override val cause: Throwable?) : XtreamError()
    data class MalformedResponse(override val message: String) : XtreamError()
}
