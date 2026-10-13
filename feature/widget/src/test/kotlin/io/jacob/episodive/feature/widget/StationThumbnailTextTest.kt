package io.jacob.episodive.feature.widget

import io.jacob.episodive.feature.widget.component.stationInitialsFontSizeSp
import org.junit.Assert.assertEquals
import org.junit.Test

class StationThumbnailTextTest {

    @Test
    fun `Given 글꼴 배율 1, Then 한 변에 비율을 곱한 크기다`() {
        assertEquals(24.64f, stationInitialsFontSizeSp(sizeDp = 56, textRatio = 0.44f, fontScale = 1f), 0.001f)
    }

    @Test
    fun `Given 글꼴 배율 2, Then 배율로 나눠 화면에서 차지하는 크기는 같다`() {
        val sp1 = stationInitialsFontSizeSp(56, 0.44f, 1f)
        val sp2 = stationInitialsFontSizeSp(56, 0.44f, 2f)

        assertEquals(sp1 / 2f, sp2, 0.001f)
        assertEquals(sp1, sp2 * 2f, 0.001f)
    }

    @Test
    fun `Given 0 이하의 글꼴 배율, Then 나누기에서 터지지 않는다`() {
        assertEquals(true, stationInitialsFontSizeSp(56, 0.44f, 0f).isFinite())
    }
}
