package com.iptvcar.core.provider.xtream

import com.iptvcar.core.model.Category
import com.iptvcar.core.model.ContentType
import com.iptvcar.core.model.Episode
import com.iptvcar.core.model.LiveChannel
import com.iptvcar.core.model.Movie
import com.iptvcar.core.model.Season
import com.iptvcar.core.model.Series
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses raw Xtream Codes `player_api.php` JSON per shared/parsing-spec/xtream-and-m3u.md.
 * Every accessor is lenient: a missing or mistyped field degrades to null/default
 * rather than throwing, because Xtream server implementations vary widely.
 */
object XtreamParser {

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val v = get(key)) {
            is Int -> v
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return when (val v = get(key)) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }

    fun parseCategories(providerId: String, contentType: ContentType, json: String): List<Category> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Category(
                id = o.getString("category_id"),
                providerId = providerId,
                name = o.optString("category_name", "Unnamed"),
                contentType = contentType,
            )
        }
    }

    fun parseLiveStreams(
        providerId: String,
        credentials: XtreamCredentials,
        json: String,
    ): List<LiveChannel> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val streamId = o.get("stream_id").toString()
            LiveChannel(
                id = streamId,
                providerId = providerId,
                categoryId = o.optStringOrNull("category_id") ?: "uncategorized",
                name = o.optString("name", "Unnamed channel"),
                logoUrl = o.optStringOrNull("stream_icon"),
                streamUrl = liveStreamUrl(credentials, streamId, extension = "m3u8"),
                epgChannelId = o.optStringOrNull("epg_channel_id"),
                catchupAvailable = o.optIntOrNull("tv_archive") == 1,
                catchupDays = o.optIntOrNull("tv_archive_duration"),
            )
        }
    }

    fun parseVodStreams(
        providerId: String,
        credentials: XtreamCredentials,
        json: String,
    ): List<Movie> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val streamId = o.get("stream_id").toString()
            val ext = o.optStringOrNull("container_extension") ?: "mp4"
            Movie(
                id = streamId,
                providerId = providerId,
                categoryId = o.optStringOrNull("category_id") ?: "uncategorized",
                title = o.optString("name", "Untitled"),
                posterUrl = o.optStringOrNull("stream_icon"),
                rating = o.optDoubleOrNull("rating"),
                streamUrl = vodStreamUrl(credentials, streamId, ext),
                containerExtension = ext,
            )
        }
    }

    /** Enriches a Movie with detail from `get_vod_info`. */
    fun parseVodInfo(movie: Movie, json: String): Movie {
        val root = JSONObject(json)
        val info = root.optJSONObject("info") ?: return movie
        return movie.copy(
            description = info.optStringOrNull("plot") ?: movie.description,
            genre = info.optStringOrNull("genre") ?: movie.genre,
            durationSeconds = info.optIntOrNull("duration_secs") ?: movie.durationSeconds,
            year = info.optStringOrNull("releasedate")?.take(4)?.toIntOrNull() ?: movie.year,
        )
    }

    fun parseSeriesList(providerId: String, json: String): List<Series> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Series(
                id = o.get("series_id").toString(),
                providerId = providerId,
                categoryId = o.optStringOrNull("category_id") ?: "uncategorized",
                title = o.optString("name", "Untitled"),
                posterUrl = o.optStringOrNull("cover"),
                rating = o.optDoubleOrNull("rating"),
            )
        }
    }

    /** Parses `get_series_info` into full Season/Episode trees for one series. */
    fun parseSeriesInfo(series: Series, credentials: XtreamCredentials, json: String): Series {
        val root = JSONObject(json)
        val info = root.optJSONObject("info")
        val enriched = if (info != null) {
            series.copy(
                description = info.optStringOrNull("plot") ?: series.description,
                genre = info.optStringOrNull("genre") ?: series.genre,
                posterUrl = info.optStringOrNull("cover") ?: series.posterUrl,
            )
        } else series

        val episodesByseason = root.optJSONObject("episodes") ?: JSONObject()
        val seasons = episodesByseason.keys().asSequence().map { seasonKey ->
            val seasonNumber = seasonKey.toIntOrNull() ?: 0
            val episodesArray = episodesByseason.getJSONArray(seasonKey)
            val episodes = (0 until episodesArray.length()).map { i ->
                val eo = episodesArray.getJSONObject(i)
                val epInfo = eo.optJSONObject("info")
                val ext = eo.optStringOrNull("container_extension") ?: "mp4"
                val episodeId = eo.get("id").toString()
                Episode(
                    id = episodeId,
                    seasonId = "${series.id}-s$seasonNumber",
                    episodeNumber = eo.optIntOrNull("episode_num") ?: 0,
                    title = eo.optString("title", "Episode"),
                    durationSeconds = epInfo?.optIntOrNull("duration_secs"),
                    description = epInfo?.optStringOrNull("plot"),
                    streamUrl = episodeStreamUrl(credentials, episodeId, ext),
                    containerExtension = ext,
                )
            }
            Season(
                id = "${series.id}-s$seasonNumber",
                seriesId = series.id,
                seasonNumber = seasonNumber,
                episodes = episodes,
            )
        }.sortedBy { it.seasonNumber }.toList()

        return enriched.copy(seasons = seasons)
    }

    fun liveStreamUrl(c: XtreamCredentials, streamId: String, extension: String) =
        "${c.baseUrl.trimEnd('/')}/live/${c.username}/${c.password}/$streamId.$extension"

    fun vodStreamUrl(c: XtreamCredentials, streamId: String, extension: String) =
        "${c.baseUrl.trimEnd('/')}/movie/${c.username}/${c.password}/$streamId.$extension"

    fun episodeStreamUrl(c: XtreamCredentials, episodeId: String, extension: String) =
        "${c.baseUrl.trimEnd('/')}/series/${c.username}/${c.password}/$episodeId.$extension"

    /** Returns true only when the auth JSON positively confirms a valid session. */
    fun isAuthenticated(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val userInfo = root.optJSONObject("user_info") ?: return false
            userInfo.optString("auth", "0") == "1"
        } catch (e: Exception) {
            false
        }
    }
}
