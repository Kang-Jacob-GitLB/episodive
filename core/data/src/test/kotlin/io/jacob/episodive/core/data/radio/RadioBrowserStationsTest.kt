package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.network.model.StationDetailsResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioBrowserStationsTest {

    private fun idsOf(vararg responses: StationDetailsResponse) =
        responses.toList().toRemoteStations().map { it.uuid }

    @Test
    fun `Given a passing response, When toRemoteStations, Then mapped to station`() {
        val remote = listOf(
            remoteResponseTestData(
                "u1", "  Radio One ", url = "https://s.example/live", state = " California ",
                languageCodes = "EN,es", hls = 0,
            )
        ).toRemoteStations().single()

        assertEquals("rb:u1", remote.station.id)
        assertEquals("Radio One", remote.station.name)
        assertEquals("", remote.station.broadcaster)
        assertEquals(StationKind.RADIO, remote.station.kind)
        assertEquals("California", remote.station.region)
        assertEquals("u1", remote.uuid)
        assertEquals("https://s.example/live", remote.urlResolved)
        assertFalse(remote.isHls)
        assertEquals("en", remote.station.language)
    }

    @Test
    fun `Given blank state and language codes, Then region and language are null`() {
        val remote = listOf(remoteResponseTestData("u1", "A", state = " ", languageCodes = "")).toRemoteStations().single()

        assertNull(remote.station.region)
        assertNull(remote.station.language)
    }

    @Test
    fun `Given hls flag or m3u8 url, Then isHls`() {
        val remotes = listOf(
            remoteResponseTestData("a", "A", hls = 1, url = "https://s/a"),
            remoteResponseTestData("b", "B", hls = 0, url = "https://s/b.m3u8?token=1"),
            remoteResponseTestData("c", "C", hls = 0, url = "https://s/c"),
        ).toRemoteStations()

        assertEquals(listOf(true, true, false), remotes.map { it.isHls })
    }

    @Test
    fun `Given failing health checks, Then excluded`() {
        assertEquals(
            listOf("ok"),
            idsOf(
                remoteResponseTestData("ok", "Ok"),
                remoteResponseTestData("dead", "Dead", lastCheckOk = 0),
                remoteResponseTestData("nocheck", "NoCheck", lastCheckOk = null),
                remoteResponseTestData("ssl", "Ssl", sslError = 1),
                remoteResponseTestData("nossl", "NoSsl", sslError = null),
            ),
        )
    }

    @Test
    fun `Given non https urls, Then excluded`() {
        assertEquals(
            listOf("ok"),
            idsOf(
                remoteResponseTestData("ok", "Ok", url = "HTTPS://s/ok"),
                remoteResponseTestData("http", "Http", url = "http://s/x"),
                remoteResponseTestData("none", "None", url = null),
                remoteResponseTestData("blank", "Blank", url = "  "),
                remoteResponseTestData("bare", "Bare", url = "https://"),
            ),
        )
    }

    @Test
    fun `Given codecs, Then only playable ones pass and UNKNOWN needs hls`() {
        assertEquals(
            listOf("mp3", "aac", "aacp", "ogg", "opus", "flac", "unk-hls"),
            idsOf(
                remoteResponseTestData("mp3", "A1", codec = "mp3"),
                remoteResponseTestData("aac", "A2", codec = "AAC"),
                remoteResponseTestData("aacp", "A3", codec = "AAC+"),
                remoteResponseTestData("ogg", "A4", codec = "OGG"),
                remoteResponseTestData("opus", "A5", codec = "OPUS"),
                remoteResponseTestData("flac", "A6", codec = "FLAC"),
                remoteResponseTestData("unk-hls", "A7", codec = "UNKNOWN", hls = 1),
                remoteResponseTestData("unk", "A8", codec = "UNKNOWN", hls = 0),
                remoteResponseTestData("wma", "A9", codec = "WMA"),
                remoteResponseTestData("empty", "B1", codec = ""),
                remoteResponseTestData("null", "B2", codec = null),
                remoteResponseTestData("video", "B3", codec = "MP3,H.264"),
                remoteResponseTestData("video2", "B4", codec = "H.264"),
            ),
        )
    }

    @Test
    fun `Given blank names or uuids, Then excluded`() {
        assertEquals(
            listOf("ok"),
            idsOf(
                remoteResponseTestData("ok", "Ok"),
                remoteResponseTestData("noname", "   "),
                remoteResponseTestData(" ", "Blank uuid"),
            ),
        )
    }

    @Test
    fun `Given same name after normalization, Then the more popular one stays`() {
        assertEquals(
            listOf("pop"),
            idsOf(
                remoteResponseTestData("low", "Radio  One!", clickCount = 5),
                remoteResponseTestData("pop", "radio-one", clickCount = 50),
            ),
        )
    }

    @Test
    fun `Given same url, Then the more popular one stays`() {
        assertEquals(
            listOf("pop"),
            idsOf(
                remoteResponseTestData("low", "Alpha", url = "https://s/x", clickCount = 5),
                remoteResponseTestData("pop", "Beta", url = "https://S/X", clickCount = 50),
            ),
        )
    }

    @Test
    fun `Given symbol only names, Then they do not dedupe each other by name`() {
        assertEquals(listOf("a", "b"), idsOf(remoteResponseTestData("a", "***"), remoteResponseTestData("b", "!!!")))
    }

    @Test
    fun `Given equal clicks, Then server order is kept`() {
        assertEquals(
            listOf("a", "b", "c"),
            idsOf(
                remoteResponseTestData("a", "A", clickCount = 10),
                remoteResponseTestData("b", "B", clickCount = 10),
                remoteResponseTestData("c", "C", clickCount = null),
            ),
        )
    }

    @Test
    fun `Given more than the limit, Then capped at 50 by popularity`() {
        val responses = (1..120).map { remoteResponseTestData("u$it", "Station $it", clickCount = it) }

        val remotes = responses.toRemoteStations()

        assertEquals(50, remotes.size)
        assertEquals("u120", remotes.first().uuid)
        assertEquals("u71", remotes.last().uuid)
    }

    @Test
    fun `Given rb id helpers, Then prefix round trips`() {
        assertEquals("rb:abc", radioBrowserStationId("abc"))
        assertTrue("rb:abc".isRadioBrowserStationId())
        assertEquals("abc", "rb:abc".radioBrowserUuid())
        assertFalse("rb:".isRadioBrowserStationId())
        assertFalse("kbs-1radio".isRadioBrowserStationId())
    }
}
