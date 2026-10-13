package io.jacob.episodive.feature.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerBarVisibilityTest {

    @Test
    fun `Given 에피소드가 올라 있음, When 억제하지 않으면, Then 보인다`() {
        assertTrue(isPlayerBarVisible(hasEpisode = true, suppressed = false))
    }

    @Test
    fun `Given 에피소드가 올라 있음, When 라디오 바가 자리를 차지하면, Then 숨긴다`() {
        assertFalse(isPlayerBarVisible(hasEpisode = true, suppressed = true))
    }

    @Test
    fun `Given 올라 있는 에피소드가 없음, Then 억제와 무관하게 숨긴다`() {
        assertFalse(isPlayerBarVisible(hasEpisode = false, suppressed = false))
        assertFalse(isPlayerBarVisible(hasEpisode = false, suppressed = true))
    }
}
