package com.iptvcar.app.data

import android.content.Context
import com.iptvcar.app.security.SecureCredentialStore
import com.iptvcar.core.model.Category
import com.iptvcar.core.model.ContentType
import com.iptvcar.core.model.LiveChannel
import com.iptvcar.core.model.Movie
import com.iptvcar.core.model.Provider
import com.iptvcar.core.model.ProviderKind
import com.iptvcar.core.model.Series
import com.iptvcar.core.provider.m3u.M3UParser
import com.iptvcar.core.provider.xtream.XtreamClient
import com.iptvcar.core.provider.xtream.XtreamCredentials
import com.iptvcar.core.provider.xtream.XtreamParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Bridges the platform-neutral :core parsing/client layer to a single
 * app-facing repository. Holds no UI state; ViewModels layer above this.
 */
class ProviderRepository(context: Context) {

    private val appContext = context.applicationContext
    private val credentialStore = SecureCredentialStore(appContext)
    private val providerListPrefs = appContext.getSharedPreferences("iptv_car_providers", Context.MODE_PRIVATE)
    private val http = OkHttpClient()

    fun listProviders(): List<Provider> {
        val raw = providerListPrefs.getString("providers", null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Provider(
                id = o.getString("id"),
                displayName = o.getString("displayName"),
                kind = ProviderKind.valueOf(o.getString("kind")),
                baseUrl = o.getString("baseUrl"),
                createdAt = o.getLong("createdAt"),
            )
        }
    }

    private fun persistProviderList(providers: List<Provider>) {
        val array = JSONArray()
        providers.forEach { p ->
            array.put(
                JSONObject()
                    .put("id", p.id)
                    .put("displayName", p.displayName)
                    .put("kind", p.kind.name)
                    .put("baseUrl", p.baseUrl)
                    .put("createdAt", p.createdAt)
            )
        }
        providerListPrefs.edit().putString("providers", array.toString()).apply()
    }

    fun addXtreamProvider(displayName: String, baseUrl: String, username: String, password: String): Provider {
        val provider = Provider(id = UUID.randomUUID().toString(), displayName = displayName, kind = ProviderKind.XTREAM, baseUrl = baseUrl, createdAt = System.currentTimeMillis())
        credentialStore.saveXtreamCredentials(provider.id, username, password)
        persistProviderList(listProviders() + provider)
        return provider
    }

    fun addM3uProvider(displayName: String, url: String): Provider {
        val provider = Provider(id = UUID.randomUUID().toString(), displayName = displayName, kind = ProviderKind.M3U, baseUrl = url, createdAt = System.currentTimeMillis())
        credentialStore.saveM3uUrl(provider.id, url)
        persistProviderList(listProviders() + provider)
        return provider
    }

    fun deleteProvider(providerId: String) {
        credentialStore.clear(providerId)
        persistProviderList(listProviders().filterNot { it.id == providerId })
    }

    private fun xtreamClientFor(provider: Provider): XtreamClient =
        XtreamClient(xtreamCredentialsFor(provider))

    private fun xtreamCredentialsFor(provider: Provider): XtreamCredentials {
        val (username, password) = credentialStore.readXtreamCredentials(provider.id)
            ?: error("No stored credentials for provider ${provider.id}")
        return XtreamCredentials(provider.baseUrl, username, password)
    }

    fun authenticate(provider: Provider): Boolean = when (provider.kind) {
        ProviderKind.XTREAM -> xtreamClientFor(provider).authenticate()
        ProviderKind.M3U -> true // M3U has no separate auth step; a fetch failure surfaces at load time.
    }

    fun loadLiveCategories(provider: Provider): List<Category> = when (provider.kind) {
        ProviderKind.XTREAM -> XtreamParser.parseCategories(provider.id, ContentType.LIVE, xtreamClientFor(provider).getLiveCategoriesRaw())
        ProviderKind.M3U -> loadM3u(provider).categories.filter { it.contentType == ContentType.LIVE }
    }

    fun loadLiveChannels(provider: Provider, categoryId: String): List<LiveChannel> = when (provider.kind) {
        ProviderKind.XTREAM -> XtreamParser.parseLiveStreams(provider.id, xtreamCredentialsFor(provider), xtreamClientFor(provider).getLiveStreamsRaw(categoryId))
        ProviderKind.M3U -> loadM3u(provider).liveChannels.filter { it.categoryId == categoryId }
    }

    fun loadMovieCategories(provider: Provider): List<Category> = when (provider.kind) {
        ProviderKind.XTREAM -> XtreamParser.parseCategories(provider.id, ContentType.MOVIE, xtreamClientFor(provider).getVodCategoriesRaw())
        ProviderKind.M3U -> loadM3u(provider).categories.filter { it.contentType == ContentType.MOVIE }
    }

    fun loadMovies(provider: Provider, categoryId: String): List<Movie> = when (provider.kind) {
        ProviderKind.XTREAM -> XtreamParser.parseVodStreams(provider.id, xtreamCredentialsFor(provider), xtreamClientFor(provider).getVodStreamsRaw(categoryId))
        ProviderKind.M3U -> loadM3u(provider).movies.filter { it.categoryId == categoryId }
    }

    fun loadMovieDetail(provider: Provider, movie: Movie): Movie = when (provider.kind) {
        ProviderKind.XTREAM -> XtreamParser.parseVodInfo(movie, xtreamClientFor(provider).getVodInfoRaw(movie.id))
        ProviderKind.M3U -> movie // M3U carries no separate detail endpoint.
    }

    fun loadSeriesCategories(provider: Provider): List<Category> = when (provider.kind) {
        ProviderKind.XTREAM -> XtreamParser.parseCategories(provider.id, ContentType.SERIES, xtreamClientFor(provider).getSeriesCategoriesRaw())
        ProviderKind.M3U -> loadM3u(provider).categories.filter { it.contentType == ContentType.SERIES }
    }

    fun loadSeries(provider: Provider, categoryId: String): List<Series> = when (provider.kind) {
        ProviderKind.XTREAM -> XtreamParser.parseSeriesList(provider.id, xtreamClientFor(provider).getSeriesRaw(categoryId))
        ProviderKind.M3U -> loadM3u(provider).series.filter { it.categoryId == categoryId }
    }

    /**
     * Looks up a series by id alone (no known category), for callers like
     * the Android Auto media-browse tree that only have a media id to work
     * from. For Xtream this only needs `series.id` to build the detail
     * request, so the placeholder title is immediately overwritten by
     * [loadSeriesDetail]'s own `get_series_info` call.
     */
    fun findSeriesById(provider: Provider, seriesId: String): Series? = when (provider.kind) {
        ProviderKind.XTREAM -> loadSeriesDetail(provider, Series(id = seriesId, providerId = provider.id, categoryId = "", title = ""))
        ProviderKind.M3U -> loadM3u(provider).series.firstOrNull { it.id == seriesId }?.let { loadSeriesDetail(provider, it) }
    }

    fun loadSeriesDetail(provider: Provider, series: Series): Series = when (provider.kind) {
        ProviderKind.XTREAM -> XtreamParser.parseSeriesInfo(series, xtreamCredentialsFor(provider), xtreamClientFor(provider).getSeriesInfoRaw(series.id))
        ProviderKind.M3U -> loadM3u(provider).series.firstOrNull { it.id == series.id } ?: series
    }

    // M3U playlists are fetched and parsed in full and cached in-memory per
    // provider for the process lifetime; this app does not proxy or
    // re-host the playlist, it only parses the provider's own bytes.
    private val m3uCache = mutableMapOf<String, com.iptvcar.core.provider.m3u.M3UParseResult>()

    private fun loadM3u(provider: Provider): com.iptvcar.core.provider.m3u.M3UParseResult {
        return m3uCache.getOrPut(provider.id) {
            val content = if (provider.baseUrl.startsWith("http")) {
                val request = Request.Builder().url(provider.baseUrl).get().build()
                http.newCall(request).execute().use { it.body?.string() ?: "" }
            } else {
                File(provider.baseUrl).readText()
            }
            M3UParser.parse(provider.id, content)
        }
    }
}
