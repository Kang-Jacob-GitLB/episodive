package io.jacob.episodive.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/** `:core:testing` 의 팩토리를 쓰지 않는다([StationArtTest] 와 같은 사유 — 이 모듈은 JVM 라이브러리다). */
class UserDataTest {
    @Test
    fun `Given locale with a two letter country, Then country is that code`() {
        assertEquals("US", Locale.forLanguageTag("en-US").countryOrNull())
        assertEquals("KR", Locale.forLanguageTag("ko-KR").countryOrNull())
    }

    @Test
    fun `Given locale without country or with a numeric region, Then null`() {
        assertNull(Locale.forLanguageTag("en").countryOrNull())
        assertNull(Locale.forLanguageTag("es-419").countryOrNull())
        assertNull(Locale.ROOT.countryOrNull())
    }
}
