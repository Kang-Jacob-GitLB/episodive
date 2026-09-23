package io.jacob.episodive.core.model.caption

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptionLanguageTest {

    @Test
    fun `matching tags resolve to the expected language`() {
        val cases = mapOf(
            "en" to CaptionLanguage.ENGLISH,
            "en-US" to CaptionLanguage.ENGLISH,
            "en-us" to CaptionLanguage.ENGLISH,
            "EN_gb" to CaptionLanguage.ENGLISH,
            " ko " to CaptionLanguage.KOREAN,
            "ko-KR" to CaptionLanguage.KOREAN,
        )

        cases.forEach { (tag, expected) ->
            assertEquals("tag=$tag", expected, tag.toCaptionLanguage())
        }
    }

    @Test
    fun `unsupported or empty tags resolve to null`() {
        val cases = listOf("", "fr", "eng")

        cases.forEach { tag ->
            assertNull("tag=$tag", tag.toCaptionLanguage())
        }
    }

    @Test
    fun `null tag resolves to null`() {
        val tag: String? = null

        assertNull(tag.toCaptionLanguage())
    }
}
