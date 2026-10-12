package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.StationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KoreanStationCatalogTest {
    private val entries = KoreanStationCatalog.entries

    @Test
    fun `Given catalog, Then station ids are unique`() {
        val ids = entries.map { it.station.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `Given catalog, Then every entry has radioBrowserIds and none are shared`() {
        assertTrue(entries.all { it.radioBrowserIds.isNotEmpty() })
        val all = entries.flatMap { it.radioBrowserIds }
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `Given catalog, Then fallbackUrl avoids excluded third party domains`() {
        val excluded = listOf("bsod.kr", "qtfm.cn", "duckdns.org")
        entries.mapNotNull { it.fallbackUrl }.forEach { url ->
            assertFalse(url, excluded.any { url.contains(it) })
        }
    }

    @Test
    fun `Given catalog, Then fallbackUrl has no expiring token or chunklist`() {
        // 변형·세션 chunklist 는 CDN 재구성 시 404 가 나므로 master playlist 만 허용한다.
        val tokens = listOf("Policy=", "token=", "_lsu_sa_", "chunklist")
        entries.mapNotNull { it.fallbackUrl }.forEach { url ->
            assertFalse(url, tokens.any { url.contains(it) })
        }
    }

    @Test
    fun `Given catalog, Then every station is radio`() {
        assertTrue(entries.all { it.station.kind == StationKind.RADIO })
    }
}
