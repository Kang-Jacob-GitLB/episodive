package io.jacob.episodive.core.player.datasource

import android.graphics.BitmapFactory
import io.jacob.episodive.core.model.art
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StationArtworkRendererTest {

    @Test
    fun `Given art, When render called, Then decodes to a square of sizePx`() {
        val bytes = StationArtworkRenderer.render(radioStationTestData.art, sizePx = 128)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull(bitmap)
        assertEquals(128, bitmap.width)
        assertEquals(128, bitmap.height)
    }

    @Test
    fun `Given default size, When render called, Then size is 320`() {
        val bytes = StationArtworkRenderer.render(regionalRadioStationTestData.art)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals(StationArtworkRenderer.DefaultSizePx, bitmap.width)
    }
}
