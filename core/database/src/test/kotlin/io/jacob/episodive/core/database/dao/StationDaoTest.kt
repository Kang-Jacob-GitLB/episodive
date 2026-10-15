package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StationDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val dbRule = RoomDatabaseRule()

    private lateinit var dao: StationDao

    private val first = stationIdsTestData[0]
    private val second = stationIdsTestData[1]
    private val third = stationIdsTestData[2]

    @Before
    fun setup() {
        dao = dbRule.db.stationDao()
    }

    @Test
    fun `Given favorites, When getFavoriteStationIds, Then newest first`() = runTest {
        dao.addFavorite(favoriteStationEntityOf(first, 1))
        dao.addFavorite(favoriteStationEntityOf(second, 3))
        dao.addFavorite(favoriteStationEntityOf(third, 2))

        dao.getFavoriteStationIds().test {
            assertEquals(listOf(second, third, first), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given not favorite, When toggleFavorite, Then favorite and emitted`() = runTest {
        dao.isFavorite(first).test {
            assertFalse(awaitItem())

            assertTrue(dao.toggleFavorite(first))
            assertTrue(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given favorite, When toggleFavorite, Then removed and emitted`() = runTest {
        dao.addFavorite(favoriteStationEntityOf(first))

        dao.isFavorite(first).test {
            assertTrue(awaitItem())

            assertFalse(dao.toggleFavorite(first))
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given favorite, When addFavorite again, Then original time is kept`() = runTest {
        dao.addFavorite(favoriteStationEntityOf(first, 1))
        dao.addFavorite(favoriteStationEntityOf(second, 2))
        dao.addFavorite(favoriteStationEntityOf(first, 10))

        dao.getFavoriteStationIds().test {
            assertEquals(listOf(second, first), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given recents within the same second, When getRecentStationIds, Then millisecond order holds`() = runTest {
        dao.upsertRecent(recentStationEntityOf(first, 1))
        dao.upsertRecent(recentStationEntityOf(second, 3))
        dao.upsertRecent(recentStationEntityOf(third, 2))

        dao.getRecentStationIds().test {
            assertEquals(listOf(second, third, first), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given favorites within the same second, When getFavoriteStationIds, Then millisecond order holds`() = runTest {
        // 알파벳 순(first, second)과 반대로 둔다. 동점이 아니라 시각으로 갈려야 한다.
        dao.addFavorite(favoriteStationEntityOf(first, 1))
        dao.addFavorite(favoriteStationEntityOf(second, 2))

        dao.getFavoriteStationIds().test {
            assertEquals(listOf(second, first), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given same timestamp, When getRecentStationIds, Then id descending breaks the tie`() = runTest {
        dao.upsertRecent(recentStationEntityOf(first))
        dao.upsertRecent(recentStationEntityOf(second))

        dao.getRecentStationIds().test {
            assertEquals(listOf(first, second).sortedDescending(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When recordPlayed, Then flow emits once with upsert and trim applied`() = runTest {
        dao.upsertRecent(recentStationEntityOf(first, 1))
        dao.upsertRecent(recentStationEntityOf(second, 2))

        dao.getRecentStationIds().test {
            assertEquals(listOf(second, first), awaitItem())

            dao.recordPlayed(recentStationEntityOf(third, 3), keep = 2)

            // 기록과 정리가 한 트랜잭션이라 중간 상태(3개) 없이 한 번만 방출된다.
            assertEquals(listOf(third, second), awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given recent, When upsert same id, Then playedAt updated and reordered`() = runTest {
        dao.upsertRecent(recentStationEntityOf(first, 1))
        dao.upsertRecent(recentStationEntityOf(second, 2))

        dao.getRecentStationIds().test {
            assertEquals(listOf(second, first), awaitItem())

            dao.upsertRecent(recentStationEntityOf(first, 5))
            assertEquals(listOf(first, second), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given more recents than keep, When trimRecent, Then only newest remain`() = runTest {
        dao.upsertRecent(recentStationEntityOf(first, 1))
        dao.upsertRecent(recentStationEntityOf(second, 3))
        dao.upsertRecent(recentStationEntityOf(third, 2))

        dao.trimRecent(keep = 2)

        dao.getRecentStationIds().test {
            assertEquals(listOf(second, third), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given fewer recents than keep, When trimRecent, Then nothing removed`() = runTest {
        dao.upsertRecent(recentStationEntityOf(first, 1))

        dao.trimRecent(keep = 5)

        dao.getRecentStationIds().test {
            assertEquals(listOf(first), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private val remoteA = remoteStationIdsTestData[0]
    private val remoteB = remoteStationIdsTestData[1]

    @Test
    fun `Given favorites with and without meta, When getFavoriteStationsWithMeta, Then newest first and meta joined`() = runTest {
        val meta = radioBrowserStationEntityOf(remoteA)
        dao.upsertRemoteStation(meta)
        dao.addFavorite(favoriteStationEntityOf(first, 1))
        dao.addFavorite(favoriteStationEntityOf(remoteA, 3))
        dao.addFavorite(favoriteStationEntityOf(remoteB, 2))

        dao.getFavoriteStationsWithMeta().test {
            val items = awaitItem()
            assertEquals(listOf(remoteA, remoteB, first), items.map { it.stationId })
            assertEquals(meta, items[0].meta)
            assertNull(items[1].meta)
            assertNull(items[2].meta)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given recents with and without meta, When getRecentStationsWithMeta, Then newest first and meta joined`() = runTest {
        val meta = radioBrowserStationEntityOf(remoteA)
        dao.upsertRemoteStation(meta)
        dao.upsertRecent(recentStationEntityOf(first, 1))
        dao.upsertRecent(recentStationEntityOf(remoteA, 3))
        dao.upsertRecent(recentStationEntityOf(remoteB, 2))

        dao.getRecentStationsWithMeta().test {
            val items = awaitItem()
            assertEquals(listOf(remoteA, remoteB, first), items.map { it.stationId })
            assertEquals(meta, items[0].meta)
            assertNull(items[1].meta)
            assertNull(items[2].meta)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given meta, When upsertRemoteStation again, Then replaced`() = runTest {
        dao.addFavorite(favoriteStationEntityOf(remoteA))
        dao.upsertRemoteStation(radioBrowserStationEntityOf(remoteA, name = "Old"))
        dao.upsertRemoteStation(radioBrowserStationEntityOf(remoteA, name = "New"))

        dao.getFavoriteStationsWithMeta().test {
            assertEquals("New", awaitItem().single().meta?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given meta only in favorite, When unfavorited, Then meta is deleted`() = runTest {
        dao.toggleFavoriteWithMeta(radioBrowserStationEntityOf(remoteA))

        assertFalse(dao.toggleFavoriteWithMeta(radioBrowserStationEntityOf(remoteA)))

        dao.upsertRecent(recentStationEntityOf(remoteA))
        dao.getRecentStationsWithMeta().test {
            assertNull(awaitItem().single().meta)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given meta also in recent, When unfavorited, Then meta is kept`() = runTest {
        dao.recordPlayedWithMeta(radioBrowserStationEntityOf(remoteA), playedAtMillis = 1L, keep = 10)
        assertTrue(dao.toggleFavoriteWithMeta(radioBrowserStationEntityOf(remoteA)))

        assertFalse(dao.toggleFavorite(remoteA))

        dao.getRecentStationsWithMeta().test {
            assertEquals(remoteA, awaitItem().single().meta?.stationId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given meta in favorite, When recent is trimmed away, Then meta is kept`() = runTest {
        dao.toggleFavoriteWithMeta(radioBrowserStationEntityOf(remoteA))
        dao.recordPlayedWithMeta(radioBrowserStationEntityOf(remoteA), playedAtMillis = 1L, keep = 1)

        dao.recordPlayed(recentStationEntityOf(first, 2), keep = 1)

        dao.getFavoriteStationsWithMeta().test {
            assertEquals(remoteA, awaitItem().single().meta?.stationId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given meta only in recent, When trimmed away by recordPlayed, Then meta is deleted`() = runTest {
        dao.recordPlayedWithMeta(radioBrowserStationEntityOf(remoteA), playedAtMillis = 1L, keep = 1)

        dao.recordPlayed(recentStationEntityOf(first, 2), keep = 1)

        // 메타가 지워졌는지는 다시 즐겨찾기에 올렸을 때 조인 결과로 본다.
        dao.addFavorite(favoriteStationEntityOf(remoteA))
        dao.getFavoriteStationsWithMeta().test {
            assertNull(awaitItem().single().meta)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given orphan meta, When deleteOrphanRemoteStations, Then only unreferenced rows are deleted`() = runTest {
        dao.upsertRemoteStation(radioBrowserStationEntityOf(remoteA))
        dao.upsertRemoteStation(radioBrowserStationEntityOf(remoteB))
        dao.addFavorite(favoriteStationEntityOf(remoteA))

        dao.deleteOrphanRemoteStations()

        dao.addFavorite(favoriteStationEntityOf(remoteB))
        dao.getFavoriteStationsWithMeta().test {
            val byId = awaitItem().associateBy { it.stationId }
            assertEquals(remoteA, byId.getValue(remoteA).meta?.stationId)
            assertNull(byId.getValue(remoteB).meta)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When toggleFavoriteWithMeta adds, Then favorite and meta appear in one emission`() = runTest {
        dao.getFavoriteStationsWithMeta().test {
            assertEquals(emptyList<String>(), awaitItem().map { it.stationId })

            assertTrue(dao.toggleFavoriteWithMeta(radioBrowserStationEntityOf(remoteA)))

            val item = awaitItem().single()
            assertEquals(remoteA, item.meta?.stationId)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When recordPlayedWithMeta, Then recent and meta appear in one emission`() = runTest {
        dao.getRecentStationsWithMeta().test {
            assertEquals(emptyList<String>(), awaitItem().map { it.stationId })

            dao.recordPlayedWithMeta(radioBrowserStationEntityOf(remoteA), playedAtMillis = 1L, keep = 5)

            assertEquals(remoteA, awaitItem().single().meta?.stationId)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given meta only in favorite, When unfavorited with plain toggleFavorite, Then meta is deleted`() = runTest {
        dao.toggleFavoriteWithMeta(radioBrowserStationEntityOf(remoteA))

        assertFalse(dao.toggleFavorite(remoteA))

        // 메타가 지워졌는지는 다시 즐겨찾기에 올렸을 때 조인 결과로 본다.
        dao.addFavorite(favoriteStationEntityOf(remoteA))
        dao.getFavoriteStationsWithMeta().test {
            assertNull(awaitItem().single().meta)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given meta, When getRemoteStation, Then returns it and null for unknown`() = runTest {
        val meta = radioBrowserStationEntityOf(remoteA)
        dao.upsertRemoteStation(meta)

        assertEquals(meta, dao.getRemoteStation(remoteA))
        assertNull(dao.getRemoteStation(remoteB))
    }
}
