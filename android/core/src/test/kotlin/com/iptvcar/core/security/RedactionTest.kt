package com.iptvcar.core.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class RedactionTest {

    @Test
    fun `redacts query-string credentials`() {
        val url = "http://example.test/player_api.php?username=bob&password=hunter2&action=get_live_streams"
        val redacted = redact(url)
        assertFalse(redacted.contains("hunter2"))
        assertFalse(redacted.contains("bob"))
    }

    @Test
    fun `redacts path-embedded xtream credentials`() {
        val url = "http://example.test/movie/bob/hunter2/1001.mp4"
        val redacted = redact(url)
        assertFalse(redacted.contains("bob"))
        assertFalse(redacted.contains("hunter2"))
    }
}
