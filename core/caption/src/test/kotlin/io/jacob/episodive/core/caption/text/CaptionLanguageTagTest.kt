package io.jacob.episodive.core.caption.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptionLanguageTagTest {

    @Test
    fun `plain primary subtag passes through lowercased`() {
        assertEquals("en", "en".toPrimaryLanguageSubtag())
        assertEquals("ko", "ko".toPrimaryLanguageSubtag())
    }

    @Test
    fun `region and casing are stripped`() {
        assertEquals("en", "en-US".toPrimaryLanguageSubtag())
        assertEquals("en", "en-us".toPrimaryLanguageSubtag())
        assertEquals("en", "EN_gb".toPrimaryLanguageSubtag())
        assertEquals("ko", "ko-KR".toPrimaryLanguageSubtag())
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("ko", " ko ".toPrimaryLanguageSubtag())
    }

    @Test
    fun `blank or null tag is null`() {
        assertNull(null.toPrimaryLanguageSubtag())
        assertNull("".toPrimaryLanguageSubtag())
        assertNull("   ".toPrimaryLanguageSubtag())
    }
}
