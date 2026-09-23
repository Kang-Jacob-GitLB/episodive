package io.jacob.episodive.core.caption.engine

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleDeviceLanguageProviderTest {
    private val original = Locale.getDefault()

    @After
    fun teardown() {
        Locale.setDefault(original)
    }

    @Test
    fun `Given default locale, When languageTag called, Then returns its BCP 47 tag`() {
        // Given
        Locale.setDefault(Locale.KOREA)

        // When
        val tag = LocaleDeviceLanguageProvider().languageTag()

        // Then
        assertEquals("ko-KR", tag)
    }
}
