package io.jacob.episodive.core.player.datasource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IcyTitleDecoderTest {
    private val eucKr = charset("EUC-KR")

    private fun raw(title: ByteArray, tail: String = "';StreamUrl='';"): ByteArray =
        "StreamTitle='".toByteArray() + title + tail.toByteArray()

    @Test
    fun `Given UTF-8 hangul bytes, Then it is decoded as UTF-8`() {
        assertEquals("가수 - 노래", decodeIcyTitle(raw("가수 - 노래".toByteArray()), fallbackTitle = "깨짐"))
    }

    @Test
    fun `Given EUC-KR hangul bytes, Then it is decoded as Korean not Latin-1`() {
        assertEquals("가수 - 노래", decodeIcyTitle(raw("가수 - 노래".toByteArray(eucKr)), fallbackTitle = "깨짐"))
    }

    @Test
    fun `Given a title with quotes and semicolons, Then only the closing quote-semicolon ends it`() {
        assertEquals(
            "Don't Stop; Believin' (Live)",
            decodeIcyTitle(raw("Don't Stop; Believin' (Live)".toByteArray()), fallbackTitle = null),
        )
    }

    @Test
    fun `Given an empty StreamTitle, Then an empty title is returned`() {
        assertEquals("", decodeIcyTitle(raw(ByteArray(0)), fallbackTitle = "x"))
    }

    @Test
    fun `Given no closing quote, Then the last quote or the end of the bytes closes it`() {
        assertEquals("제목", decodeIcyTitle("StreamTitle='제목'".toByteArray(), fallbackTitle = null))
        assertEquals("제목", decodeIcyTitle("StreamTitle='제목\u0000\u0000".toByteArray(), fallbackTitle = null))
    }

    @Test
    fun `Given bytes neither strict UTF-8 nor Korean, Then the fallback title is used`() {
        // 0xFF 는 UTF-8 에서 부정이고 CP949 에서도 매핑되지 않는다.
        assertEquals("fallback", decodeIcyTitle(raw(byteArrayOf(0xFF.toByte(), 0xFF.toByte())), fallbackTitle = "fallback"))
    }

    @Test
    fun `Given no StreamTitle in the raw bytes, Then the fallback title is returned as is`() {
        assertEquals("fallback", decodeIcyTitle("StreamUrl='';".toByteArray(), fallbackTitle = "fallback"))
        assertNull(decodeIcyTitle(ByteArray(0), fallbackTitle = null))
    }
}
