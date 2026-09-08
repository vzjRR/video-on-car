package com.iptvcar.core.provider.xtream

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin HTTP layer over Xtream Codes' `player_api.php`. Never logs full
 * request URLs (they contain the password) — see core/security/Redaction.
 */
class XtreamClient(
    private val credentials: XtreamCredentials,
    private val http: OkHttpClient = defaultClient(),
) {
    private fun apiUrl(action: String?, extraParams: Map<String, String> = emptyMap()): String {
        val base = credentials.baseUrl.trimEnd('/')
        val sb = StringBuilder("$base/player_api.php?username=${credentials.username}&password=${credentials.password}")
        if (action != null) sb.append("&action=$action")
        extraParams.forEach { (k, v) -> sb.append("&$k=$v") }
        return sb.toString()
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).get().build()
        try {
            http.newCall(request).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    throw XtreamError.AuthenticationFailed("HTTP ${response.code} from provider")
                }
                if (!response.isSuccessful) {
                    throw XtreamError.NetworkFailure("HTTP ${response.code}", null)
                }
                return response.body?.string() ?: throw XtreamError.MalformedResponse("Empty response body")
            }
        } catch (e: IOException) {
            throw XtreamError.NetworkFailure(e.message ?: "network error", e)
        }
    }

    fun authenticate(): Boolean {
        val json = get(apiUrl(action = null))
        if (!XtreamParser.isAuthenticated(json)) {
            throw XtreamError.AuthenticationFailed("Invalid Xtream credentials")
        }
        return true
    }

    fun getLiveCategoriesRaw(): String = get(apiUrl("get_live_categories"))
    fun getLiveStreamsRaw(categoryId: String? = null): String =
        get(apiUrl("get_live_streams", categoryId?.let { mapOf("category_id" to it) } ?: emptyMap()))

    fun getVodCategoriesRaw(): String = get(apiUrl("get_vod_categories"))
    fun getVodStreamsRaw(categoryId: String? = null): String =
        get(apiUrl("get_vod_streams", categoryId?.let { mapOf("category_id" to it) } ?: emptyMap()))
    fun getVodInfoRaw(vodId: String): String = get(apiUrl("get_vod_info", mapOf("vod_id" to vodId)))

    fun getSeriesCategoriesRaw(): String = get(apiUrl("get_series_categories"))
    fun getSeriesRaw(categoryId: String? = null): String =
        get(apiUrl("get_series", categoryId?.let { mapOf("category_id" to it) } ?: emptyMap()))
    fun getSeriesInfoRaw(seriesId: String): String = get(apiUrl("get_series_info", mapOf("series_id" to seriesId)))

    fun getShortEpgRaw(streamId: String, limit: Int = 4): String =
        get(apiUrl("get_short_epg", mapOf("stream_id" to streamId, "limit" to limit.toString())))

    companion object {
        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
