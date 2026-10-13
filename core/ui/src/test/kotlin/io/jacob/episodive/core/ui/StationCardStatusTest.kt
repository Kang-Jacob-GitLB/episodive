package io.jacob.episodive.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StationCardStatusTest {
    private fun status(
        nowPlayingId: String? = null,
        isPlaying: Boolean = false,
        isBuffering: Boolean = false,
        pendingId: String? = null,
    ) = stationCardStatus(
        id = "a",
        nowPlayingId = nowPlayingId,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        pendingId = pendingId,
    )

    @Test
    fun `Given 아무것도 아님, Then Idle`() {
        assertEquals(StationCardStatus.Idle, status())
        assertEquals(StationCardStatus.Idle, status(nowPlayingId = "b", isPlaying = true, pendingId = "c"))
    }

    @Test
    fun `Given 주소를 받는 중, Then Preparing`() {
        assertEquals(StationCardStatus.Preparing, status(pendingId = "a"))
    }

    @Test
    fun `Given 올라 있고 버퍼링, Then Preparing`() {
        assertEquals(StationCardStatus.Preparing, status(nowPlayingId = "a", isBuffering = true))
    }

    @Test
    fun `Given 올라 있고 재생 중, Then Playing`() {
        assertEquals(StationCardStatus.Playing, status(nowPlayingId = "a", isPlaying = true))
    }

    @Test
    fun `Given 올라 있고 멈춤, Then Paused`() {
        assertEquals(StationCardStatus.Paused, status(nowPlayingId = "a"))
    }

    @Test
    fun `Given pending 과 재생 중이 겹침, Then pending 이 우선`() {
        assertEquals(
            StationCardStatus.Preparing,
            status(nowPlayingId = "a", isPlaying = true, pendingId = "a"),
        )
    }
}
