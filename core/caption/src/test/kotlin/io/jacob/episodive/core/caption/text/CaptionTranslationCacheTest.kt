package io.jacob.episodive.core.caption.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptionTranslationCacheTest {

    @Test
    fun `miss returns null`() {
        val cache = CaptionTranslationCache(capacity = 2)

        assertNull(cache.get("hello"))
    }

    @Test
    fun `hit returns what was put`() {
        val cache = CaptionTranslationCache(capacity = 2)

        cache.put("hello", "안녕")

        assertEquals("안녕", cache.get("hello"))
    }

    @Test
    fun `oldest entry is evicted once over capacity`() {
        val cache = CaptionTranslationCache(capacity = 2)

        cache.put("a", "1")
        cache.put("b", "2")
        cache.put("c", "3")

        assertNull(cache.get("a"))
        assertEquals("2", cache.get("b"))
        assertEquals("3", cache.get("c"))
    }

    @Test
    fun `reading an entry keeps it fresh against eviction`() {
        val cache = CaptionTranslationCache(capacity = 2)

        cache.put("a", "1")
        cache.put("b", "2")
        cache.get("a") // a 를 최근 사용으로 되돌린다
        cache.put("c", "3")

        assertEquals("1", cache.get("a"))
        assertNull(cache.get("b"))
    }
}
