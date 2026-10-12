package io.jacob.episodive

import io.jacob.episodive.core.testing.model.radioStationTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationControlsTest {
    @Test
    fun `Given no station, Then custom buttons are shown with like state`() {
        assertEquals(
            NotificationControls(showCustomButtons = true, isLiked = true),
            notificationControls(station = null, isLiked = true),
        )
        assertTrue(notificationControls(station = null, isLiked = false).showCustomButtons)
    }

    @Test
    fun `Given station, Then custom buttons are hidden regardless of like state`() {
        val controls = notificationControls(station = radioStationTestData, isLiked = true)

        assertFalse(controls.showCustomButtons)
        assertFalse(controls.isLiked)
    }

    @Test
    fun `Given station then episode, Then buttons come back with the episode like state`() {
        assertFalse(notificationControls(radioStationTestData, isLiked = false).showCustomButtons)
        assertEquals(
            NotificationControls(showCustomButtons = true, isLiked = true),
            notificationControls(station = null, isLiked = true),
        )
    }
}
