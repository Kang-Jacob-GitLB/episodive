package io.jacob.episodive.feature.radio

import io.jacob.episodive.core.testing.model.kbsFirstRadioStationTestData
import io.jacob.episodive.core.testing.model.kbsHappyRadioStationTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.radioStationsTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationRingTest {

    private val a = radioStationTestData
    private val b = kbsFirstRadioStationTestData
    private val c = kbsHappyRadioStationTestData
    private val all = radioStationsTestData

    @Test
    fun `Given 즐겨찾기 없음, Then 카탈로그 전체를 돈다`() {
        assertEquals(all, stationRing(a.id, emptyList(), all))
    }

    @Test
    fun `Given 즐겨찾기가 하나뿐, Then 돌 곳이 없어 카탈로그 전체를 돈다`() {
        assertEquals(all, stationRing(a.id, listOf(a), all))
    }

    @Test
    fun `Given 기준이 즐겨찾기 안이고 둘 이상, Then 즐겨찾기를 돈다`() {
        val favorites = listOf(c, a)

        assertEquals(favorites, stationRing(a.id, favorites, all))
    }

    @Test
    fun `Given 기준이 즐겨찾기 밖, Then 즐겨찾기가 둘 이상이어도 카탈로그 전체를 돈다`() {
        val favorites = listOf(c, a)

        assertEquals(all, stationRing(b.id, favorites, all))
    }

    @Test
    fun `Given 중간 스테이션, Then 이전과 다음이 이웃이다`() {
        val ring = listOf(a, b, c)

        assertEquals(c, ring.neighbor(b.id, 1))
        assertEquals(a, ring.neighbor(b.id, -1))
    }

    @Test
    fun `Given 끝 스테이션, Then 반대 끝으로 순환한다`() {
        val ring = listOf(a, b, c)

        assertEquals(a, ring.neighbor(c.id, 1))
        assertEquals(c, ring.neighbor(a.id, -1))
    }

    @Test
    fun `Given 하나뿐이거나 비었거나 기준이 없음, Then null`() {
        assertNull(listOf(a).neighbor(a.id, 1))
        assertNull(emptyList<io.jacob.episodive.core.model.Station>().neighbor(a.id, 1))
        assertNull(listOf(a, b).neighbor(regionalRadioStationTestData.id, 1))
    }

    @Test
    fun `Given 기준이 전체 목록에 없고 최근에 둘 이상, Then 최근을 돈다`() {
        val recents = listOf(b, c)

        assertEquals(recents.sortedBy { it.id }, stationRing(b.id, emptyList(), listOf(a), recents))
    }

    @Test
    fun `Given 최근 순서가 재생마다 바뀜, Then 최근 링의 순서는 같다`() {
        val abc = listOf(a, b, c)
        val reordered = listOf(c, a, b)

        assertEquals(
            stationRing(a.id, emptyList(), emptyList(), abc),
            stationRing(a.id, emptyList(), emptyList(), reordered),
        )
    }

    @Test
    fun `Given 기준이 전체 목록에도 없고 최근은 하나뿐, Then 전체다`() {
        assertEquals(listOf(a), stationRing(b.id, emptyList(), listOf(a), listOf(b)))
    }
}
