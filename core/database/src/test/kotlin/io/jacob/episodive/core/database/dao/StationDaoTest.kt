package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
