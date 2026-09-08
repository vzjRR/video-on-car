package com.iptvcar.core.provider.xtream

import com.iptvcar.core.model.ContentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class XtreamParserTest {

    private val credentials = XtreamCredentials(baseUrl = "http://example.test", username = "user", password = "pass")

    private fun fixture(name: String): String =
        javaClass.classLoader.getResourceAsStream("fixtures/$name")!!.bufferedReader().readText()

    @Test
    fun `parses vod streams leniently including missing fields`() {
        val movies = XtreamParser.parseVodStreams("p1", credentials, fixture("vod_streams.json"))
        assertEquals(2, movies.size)

        val first = movies[0]
        assertEquals("1001", first.id)
        assertEquals("Example Movie One", first.title)
        assertEquals(7.5, first.rating)
        assertEquals("http://example.test/movie/user/pass/1001.mp4", first.streamUrl)

        val second = movies[1]
        assertNull(second.rating)
        assertEquals("http://example.test/movie/user/pass/1002.mkv", second.streamUrl)
    }

    @Test
    fun `parses series info into seasons and episodes`() {
        val base = com.iptvcar.core.model.Series(
            id = "77", providerId = "p1", categoryId = "10", title = "placeholder",
        )
        val series = XtreamParser.parseSeriesInfo(base, credentials, fixture("series_info.json"))

        assertEquals("A test series used for parser fixtures.", series.description)
        assertEquals("placeholder", series.title)
        assertEquals(1, series.seasons.size)
        val season = series.seasons.first()
        assertEquals(1, season.seasonNumber)
        assertEquals(2, season.episodes.size)
        assertEquals("http://example.test/series/user/pass/20001.mp4", season.episodes[0].streamUrl)
        assertEquals(1500, season.episodes[0].durationSeconds)
        assertNull(season.episodes[1].description)
    }

    @Test
    fun `auth check requires explicit auth=1`() {
        assertTrue(XtreamParser.isAuthenticated("""{"user_info":{"auth":"1"}}"""))
        assertEquals(false, XtreamParser.isAuthenticated("""{"user_info":{"auth":"0"}}"""))
        assertEquals(false, XtreamParser.isAuthenticated("""not even json"""))
    }

    @Test
    fun `category parsing tags content type explicitly`() {
        val json = """[{"category_id":"10","category_name":"Action"}]"""
        val categories = XtreamParser.parseCategories("p1", ContentType.MOVIE, json)
        assertEquals(ContentType.MOVIE, categories.first().contentType)
    }
}
