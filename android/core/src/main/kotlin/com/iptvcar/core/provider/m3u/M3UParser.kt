package com.iptvcar.core.provider.m3u

import com.iptvcar.core.model.Category
import com.iptvcar.core.model.ContentType
import com.iptvcar.core.model.Episode
import com.iptvcar.core.model.LiveChannel
import com.iptvcar.core.model.Movie
import com.iptvcar.core.model.Season
import com.iptvcar.core.model.Series

data class M3UParseResult(
    val categories: List<Category>,
    val liveChannels: List<LiveChannel>,
    val movies: List<Movie>,
    val series: List<Series>,
    val warnings: List<String>,
)

private data class RawEntry(
    val attrs: Map<String, String>,
    val displayName: String,
    val url: String,
)

/**
 * Parses M3U / M3U Plus playlists per shared/parsing-spec/xtream-and-m3u.md.
 * Never assumes attribute order or completeness; unclassifiable entries
 * default to Live rather than being dropped.
 */
object M3UParser {

    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
    private val episodeTagRegex = Regex("""(?i)\bS(\d{1,2})E(\d{1,3})\b""")

    fun parse(providerId: String, content: String): M3UParseResult {
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val warnings = mutableListOf<String>()
        val entries = mutableListOf<RawEntry>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXTINF")) {
                val commaIndex = line.lastIndexOf(',')
                if (commaIndex == -1) {
                    warnings.add("Skipped malformed #EXTINF at line ${i + 1}: no display name")
                    i++
                    continue
                }
                val attrsPart = line.substring("#EXTINF:".length, commaIndex)
                val displayName = line.substring(commaIndex + 1).trim()
                val attrs = attrRegex.findAll(attrsPart).associate { it.groupValues[1].lowercase() to it.groupValues[2] }

                val next = lines.getOrNull(i + 1)
                if (next == null || next.startsWith("#")) {
                    warnings.add("Skipped '$displayName': no stream URL followed #EXTINF")
                    i++
                    continue
                }
                entries.add(RawEntry(attrs, displayName, next))
                i += 2
            } else {
                i++
            }
        }

        val categoriesByName = LinkedHashMap<String, Category>()
        val liveChannels = mutableListOf<LiveChannel>()
        val movies = mutableListOf<Movie>()
        val seriesByTitle = LinkedHashMap<String, MutableMap<Int, MutableList<Episode>>>()
        val seriesMeta = LinkedHashMap<String, RawEntry>()

        fun categoryFor(name: String?, contentType: ContentType): Category {
            val resolvedName = name?.takeIf { it.isNotBlank() } ?: "Uncategorized"
            val key = "$contentType::$resolvedName"
            return categoriesByName.getOrPut(key) {
                Category(id = key, providerId = providerId, name = resolvedName, contentType = contentType)
            }
        }

        for (entry in entries) {
            val groupTitle = entry.attrs["group-title"]
            val episodeMatch = episodeTagRegex.find(entry.displayName)

            val classification = classify(entry.url, groupTitle, episodeMatch != null)
            when (classification) {
                ContentType.MOVIE -> {
                    val category = categoryFor(groupTitle, ContentType.MOVIE)
                    movies.add(
                        Movie(
                            id = entry.url,
                            providerId = providerId,
                            categoryId = category.id,
                            title = stripYearNoise(entry.displayName),
                            year = extractYear(entry.displayName),
                            posterUrl = entry.attrs["tvg-logo"],
                            streamUrl = entry.url,
                        )
                    )
                }
                ContentType.SERIES -> {
                    val seasonNum = episodeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    val episodeNum = episodeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
                    val seriesTitle = episodeMatch?.let {
                        entry.displayName.substring(0, it.range.first).trim().trimEnd('-', ' ')
                    }?.ifBlank { null } ?: entry.displayName

                    seriesMeta.putIfAbsent(seriesTitle, entry)
                    val seasonsMap = seriesByTitle.getOrPut(seriesTitle) { mutableMapOf() }
                    val episodeList = seasonsMap.getOrPut(seasonNum) { mutableListOf() }
                    episodeList.add(
                        Episode(
                            id = entry.url,
                            seasonId = "$seriesTitle-s$seasonNum",
                            episodeNumber = episodeNum,
                            title = entry.displayName,
                            streamUrl = entry.url,
                        )
                    )
                    categoryFor(groupTitle, ContentType.SERIES)
                }
                ContentType.LIVE -> {
                    val category = categoryFor(groupTitle, ContentType.LIVE)
                    liveChannels.add(
                        LiveChannel(
                            id = entry.url,
                            providerId = providerId,
                            categoryId = category.id,
                            name = entry.displayName,
                            logoUrl = entry.attrs["tvg-logo"],
                            streamUrl = entry.url,
                            epgChannelId = entry.attrs["tvg-id"],
                            catchupAvailable = entry.attrs.containsKey("catchup"),
                            catchupDays = entry.attrs["catchup-days"]?.toIntOrNull(),
                        )
                    )
                }
            }
        }

        val seriesList = seriesByTitle.map { (title, seasonsMap) ->
            val metaEntry = seriesMeta[title]
            val category = categoryFor(metaEntry?.attrs?.get("group-title"), ContentType.SERIES)
            Series(
                id = title,
                providerId = providerId,
                categoryId = category.id,
                title = title,
                posterUrl = metaEntry?.attrs?.get("tvg-logo"),
                seasons = seasonsMap.map { (num, episodes) ->
                    Season(id = "$title-s$num", seriesId = title, seasonNumber = num, episodes = episodes.sortedBy { it.episodeNumber })
                }.sortedBy { it.seasonNumber },
            )
        }

        return M3UParseResult(
            categories = categoriesByName.values.toList(),
            liveChannels = liveChannels,
            movies = movies,
            series = seriesList,
            warnings = warnings,
        )
    }

    private fun classify(url: String, groupTitle: String?, looksLikeEpisode: Boolean): ContentType {
        val group = groupTitle?.lowercase() ?: ""
        if (group.contains("series") && looksLikeEpisode) return ContentType.SERIES
        if (group.contains("movie") || group.contains("vod")) return ContentType.MOVIE
        if (url.contains("/movie/")) return ContentType.MOVIE
        if (url.contains("/series/")) return ContentType.SERIES
        return ContentType.LIVE
    }

    private val yearRegex = Regex("""\((\d{4})\)""")
    private fun extractYear(name: String): Int? = yearRegex.find(name)?.groupValues?.get(1)?.toIntOrNull()
    private fun stripYearNoise(name: String): String = yearRegex.replace(name, "").trim()
}
