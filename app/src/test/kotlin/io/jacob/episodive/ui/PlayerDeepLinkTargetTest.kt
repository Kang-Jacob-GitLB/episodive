package io.jacob.episodive.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerDeepLinkTargetTest {

    @Test
    fun `스테이션이 올라 있으면 라디오 시트를 연다`() {
        assertEquals(PlayerDeepLinkTarget.StationSheet, playerDeepLinkTarget(isStationActive = true))
    }

    @Test
    fun `스테이션이 없으면 에피소드 시트를 연다`() {
        assertEquals(PlayerDeepLinkTarget.EpisodeSheet, playerDeepLinkTarget(isStationActive = false))
    }
}
