package com.iptvcar.core.provider.m3u

import com.iptvcar.core.model.ContentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class M3UParserTest {

    private fun fixture(): String =
        javaClass.classLoader.getResourceAsStream("fixtures/sample.m3u")!!.bufferedReader().readText()

    @Test
    fun `classifies live movie and series entries correctly`() {
        val result = M3UParser.parse("p1", fixture())

        assertEquals(2, result.liveChannels.size) // news + sports
        assertEquals(1, result.movies.size)
        assertEquals(1, result.series.size)

        val movie = result.movies.first()
        assertEquals(2021, movie.year)
        assertEquals("Example Movie One", movie.title)

        val series = result.series.first()
        assertEquals(1, series.seasons.size)
        assertEquals(2, series.seasons.first().episodes.size)
    }

    @Test
    fun `parses catchup attributes on live channels`() {
        val result = M3UParser.parse("p1", fixture())
        val sports = result.liveChannels.first { it.name == "Example Sports Channel" }
        assertTrue(sports.catchupAvailable)
        assertEquals(7, sports.catchupDays)
    }

    @Test
    fun `malformed entry without a url is skipped with a warning, not a crash`() {
        val result = M3UParser.parse("p1", fixture())
        assertTrue(result.warnings.any { it.contains("Malformed Entry") })
    }

    @Test
    fun `unclassifiable entries default to live rather than being dropped`() {
        val content = "#EXTM3U\n#EXTINF:-1 group-title=\"Odd\",Something Unusual\nhttp://example.test/odd/stream\n"
        val result = M3UParser.parse("p1", content)
        assertEquals(1, result.liveChannels.size)
        assertEquals(ContentType.LIVE, result.categories.first().contentType)
    }
}
