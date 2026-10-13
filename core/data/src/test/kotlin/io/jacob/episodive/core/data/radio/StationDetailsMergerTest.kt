package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.network.model.StationDetailsResponse
import io.jacob.episodive.core.testing.model.stationDetailsTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationDetailsMergerTest {
    private val priority = listOf("a", "b")

    private fun response(
        uuid: String,
        homepage: String? = null,
        tags: String? = null,
        codec: String? = null,
        bitrate: Int? = null,
    ) = StationDetailsResponse(
        stationUuid = uuid,
        homepage = homepage,
        tags = tags,
        codec = codec,
        bitrate = bitrate,
    )

    @Test
    fun `Given responses in reverse order, When merge, Then priority wins field by field`() {
        val merged = mergeStationDetails(priority, stationDetailsResponsesTestData)

        assertEquals(stationDetailsTestData, merged)
    }

    @Test
    fun `Given first lacks a field, When merge, Then falls to next candidate for that field`() {
        val merged = mergeStationDetails(
            priority,
            listOf(
                response("a", homepage = "https://a.example.com"),
                response("b", homepage = "https://b.example.com", codec = "MP3", bitrate = 96),
            ),
        )!!

        assertEquals("https://a.example.com", merged.homepage)
        assertEquals("MP3", merged.codec)
        assertEquals(96, merged.bitrateKbps)
    }

    @Test
    fun `Given UNKNOWN or blank codec, When merge, Then excluded`() {
        val merged = mergeStationDetails(
            priority,
            listOf(response("a", codec = "UNKNOWN", bitrate = 64), response("b", codec = " ")),
        )!!

        assertNull(merged.codec)
        assertEquals(64, merged.bitrateKbps)
    }

    @Test
    fun `Given bitrate out of range, When merge, Then rejected`() {
        listOf(0, -1, 513, 613, 656).forEach { bad ->
            assertNull("bitrate $bad", mergeStationDetails(priority, listOf(response("a", bitrate = bad, codec = "AAC")))!!.bitrateKbps)
        }
        listOf(1, 512).forEach { ok ->
            assertEquals(ok, mergeStationDetails(priority, listOf(response("a", bitrate = ok)))!!.bitrateKbps)
        }
    }

    @Test
    fun `Given messy tags, When merge, Then trimmed deduped case-insensitively and capped at 5`() {
        val merged = mergeStationDetails(
            priority,
            listOf(response("a", tags = " News,, news ,Talk,TALK,a,b,c,d")),
        )!!

        assertEquals(listOf("News", "Talk", "a", "b", "c"), merged.tags)
    }

    @Test
    fun `Given tags on several records, When merge, Then only first record with tags is used`() {
        val merged = mergeStationDetails(
            priority,
            listOf(
                response("a", codec = "AAC"),
                response("b", tags = "music,pop"),
            ),
        )!!

        assertEquals(listOf("music", "pop"), merged.tags)

        val both = mergeStationDetails(
            priority,
            listOf(response("a", tags = "news"), response("b", tags = "country")),
        )!!
        assertEquals(listOf("news"), both.tags)
    }

    @Test
    fun `Given non http homepage, When merge, Then rejected`() {
        listOf(
            "javascript:alert(1)",
            "intent://x#Intent;end",
            "file:///etc/passwd",
            "ftp://example.com",
            "example.com",
            "https://",
            "not a url",
            "",
        ).forEach { bad ->
            assertNull(
                "homepage $bad",
                mergeStationDetails(priority, listOf(response("a", homepage = bad, codec = "AAC")))!!.homepage,
            )
        }
    }

    @Test
    fun `Given http and https homepages, When merge, Then kept as is`() {
        assertEquals(
            "http://kbs.example.com/radio",
            mergeStationDetails(priority, listOf(response("a", homepage = " http://kbs.example.com/radio ")))!!.homepage,
        )
        assertEquals(
            "HTTPS://Example.com",
            mergeStationDetails(priority, listOf(response("a", homepage = "HTTPS://Example.com")))!!.homepage,
        )
    }

    @Test
    fun `Given uuid outside priority, When merge, Then ignored`() {
        assertNull(mergeStationDetails(priority, listOf(response("z", codec = "AAC", bitrate = 128))))
    }

    @Test
    fun `Given nothing usable or no responses, When merge, Then null`() {
        assertNull(mergeStationDetails(priority, emptyList()))
        assertNull(
            mergeStationDetails(
                priority,
                listOf(response("a", homepage = "javascript:x", tags = " , ", codec = "UNKNOWN", bitrate = 700)),
            )
        )
    }
}
