package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.database.mapper.toRecentFeedEntities
import io.jacob.episodive.core.database.mapper.toRecentNewFeedEntities
import io.jacob.episodive.core.database.mapper.toSoundbiteEntities
import io.jacob.episodive.core.database.mapper.toTrendingFeedEntities
import io.jacob.episodive.core.testing.model.recentFeedTestDataList
import io.jacob.episodive.core.testing.model.recentNewFeedTestDataList
import io.jacob.episodive.core.testing.model.soundbiteTestDataList
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val dbRule = RoomDatabaseRule()

    private lateinit var dao: FeedDao

    @Before
    fun setup() {
        dao = dbRule.db.feedDao()
    }

    private val cacheKey = "test_cache"
    private val trendingFeedEntities = trendingFeedTestDataList.toTrendingFeedEntities(cacheKey)
    private val recentFeedEntities = recentFeedTestDataList.toRecentFeedEntities(cacheKey)
    private val recentNewFeedEntities = recentNewFeedTestDataList.toRecentNewFeedEntities(cacheKey)
    private val soundbiteEntities = soundbiteTestDataList.toSoundbiteEntities(cacheKey)

    @Test
    fun `Given trending feeds, When upsertTrendingFeeds, Then upserted correctly`() =
        runTest {
            // Given
            dao.upsertTrendingFeeds(trendingFeedEntities)

            // When
            dao.getTrendingFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, trendingFeedEntities.size)
                cancel()
            }

            // When
            dao.deleteTrendingFeed(trendingFeedEntities.first().id)
            dao.getTrendingFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, trendingFeedEntities.size - 1)
                cancel()
            }

            // When
            dao.deleteTrendingFeeds()
            dao.getTrendingFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
        }

    @Test
    fun `Given some trending feeds, When deleteTrendingFeedsByCacheKey, Then deleted correctly`() =
        runTest {
            // Given
            dao.upsertTrendingFeeds(trendingFeedEntities)
            dao.upsertTrendingFeeds(trendingFeedEntities.map { it.copy(cacheKey = "test_cache1") })

            // When
            dao.deleteTrendingFeedsByCacheKey(cacheKey)
            dao.getTrendingFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
            dao.getTrendingFeedsByCacheKey("test_cache1", 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, trendingFeedEntities.size)
                cancel()
            }
        }

    @Test
    fun `Given trending feeds with different cache keys, When replaceTrendingFeeds, Then replaced by cache key`() =
        runTest {
            // Given - Insert initial feeds
            val initialFeeds = trendingFeedEntities.take(3).map { it.copy(cacheKey = "key1") }
            dao.upsertTrendingFeeds(initialFeeds)

            // When - Replace with new feeds
            val newFeeds = listOf(
                trendingFeedEntities[5].copy(id = 100L, cacheKey = "key1"),
                trendingFeedEntities[6].copy(id = 101L, cacheKey = "key1")
            )
            dao.replaceTrendingFeeds(newFeeds)

            // Then
            dao.getTrendingFeedsByCacheKey("key1", 10).test {
                val items = awaitItem()
                assertEquals(2, items.size)
                assertTrue(items.any { it.id == 100L })
                assertTrue(items.any { it.id == 101L })
                cancel()
            }
        }

    @Test
    fun `Given recent feeds, When upsertRecentFeeds, Then upserted correctly`() =
        runTest {
            // Given
            dao.upsertRecentFeeds(recentFeedEntities)

            // When
            dao.getRecentFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentFeedEntities.size)
                cancel()
            }

            // When
            dao.deleteRecentFeed(recentFeedEntities.first().id)
            dao.getRecentFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentFeedEntities.size - 1)
                cancel()
            }

            // When
            dao.deleteRecentFeeds()
            dao.getRecentFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
        }

    @Test
    fun `Given some recent feeds, When deleteRecentFeedsByCacheKey, Then deleted correctly`() =
        runTest {
            // Given
            dao.upsertRecentFeeds(recentFeedEntities)
            dao.upsertRecentFeeds(recentFeedEntities.map { it.copy(cacheKey = "test_cache1") })

            // When
            dao.deleteRecentFeedsByCacheKey(cacheKey)
            dao.getRecentFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
            dao.getRecentFeedsByCacheKey("test_cache1", 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentFeedEntities.size)
                cancel()
            }
        }

    @Test
    fun `Given recent feeds with different cache keys, When replaceRecentFeeds, Then replaced by cache key`() =
        runTest {
            // Given - Insert initial feeds
            val initialFeeds = recentFeedEntities.take(3).map { it.copy(cacheKey = "key1") }
            dao.upsertRecentFeeds(initialFeeds)

            // When - Replace with new feeds
            val newFeeds = listOf(
                recentFeedEntities[5].copy(id = 200L, cacheKey = "key1"),
                recentFeedEntities[6].copy(id = 201L, cacheKey = "key1")
            )
            dao.replaceRecentFeeds(newFeeds)

            // Then
            dao.getRecentFeedsByCacheKey("key1", 10).test {
                val items = awaitItem()
                assertEquals(2, items.size)
                assertTrue(items.any { it.id == 200L })
                assertTrue(items.any { it.id == 201L })
                cancel()
            }
        }

    @Test
    fun `Given recent new feeds, When upsertRecentNewFeeds, Then upserted correctly`() =
        runTest {
            // Given
            dao.upsertRecentNewFeeds(recentNewFeedEntities)

            // When
            dao.getRecentNewFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentNewFeedEntities.size)
                cancel()
            }

            // When
            dao.deleteRecentNewFeed(recentNewFeedEntities.first().id)
            dao.getRecentNewFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentNewFeedEntities.size - 1)
                cancel()
            }

            // When
            dao.deleteRecentNewFeeds()
            dao.getRecentNewFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
        }

    @Test
    fun `Given some recent new feeds, When deleteRecentNewFeedsByCacheKey, Then deleted correctly`() =
        runTest {
            // Given
            dao.upsertRecentNewFeeds(recentNewFeedEntities)
            dao.upsertRecentNewFeeds(recentNewFeedEntities.map { it.copy(cacheKey = "test_cache1") })

            // When
            dao.deleteRecentNewFeedsByCacheKey(cacheKey)
            dao.getRecentNewFeedsByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
            dao.getRecentNewFeedsByCacheKey("test_cache1", 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentNewFeedEntities.size)
                cancel()
            }
        }

    @Test
    fun `Given recent new feeds with different cache keys, When replaceRecentNewFeeds, Then replaced by cache key`() =
        runTest {
            // Given - Insert initial feeds
            val initialFeeds = recentNewFeedEntities.take(3).map { it.copy(cacheKey = "key1") }
            dao.upsertRecentNewFeeds(initialFeeds)

            // When - Replace with new feeds
            val newFeeds = listOf(
                recentNewFeedEntities[5].copy(id = 300L, cacheKey = "key1"),
                recentNewFeedEntities[6].copy(id = 301L, cacheKey = "key1")
            )
            dao.replaceRecentNewFeeds(newFeeds)

            // Then
            dao.getRecentNewFeedsByCacheKey("key1", 10).test {
                val items = awaitItem()
                assertEquals(2, items.size)
                assertTrue(items.any { it.id == 300L })
                assertTrue(items.any { it.id == 301L })
                cancel()
            }
        }

    @Test
    fun `Given soundbites, When upsertSoundbites, Then upserted correctly`() =
        runTest {
            // Given
            dao.upsertSoundbites(soundbiteEntities)

            // When
            dao.getSoundbitesByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, soundbiteEntities.size)
                cancel()
            }

            // When
            dao.deleteSoundbite(soundbiteEntities.first().episodeId)
            dao.getSoundbitesByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, soundbiteEntities.size - 1)
                cancel()
            }

            // When
            dao.deleteSoundbites()
            dao.getSoundbitesByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
        }

    @Test
    fun `Given some soundbites, When deleteSoundbitesByCacheKey, Then deleted correctly`() =
        runTest {
            // Given
            dao.upsertSoundbites(soundbiteEntities)
            dao.upsertSoundbites(soundbiteEntities.map { it.copy(cacheKey = "test_cache1") })

            // When
            dao.deleteSoundbitesByCacheKey(cacheKey)
            dao.getSoundbitesByCacheKey(cacheKey, 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
            dao.getSoundbitesByCacheKey("test_cache1", 10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, soundbiteEntities.size)
                cancel()
            }
        }

    @Test
    fun `Given soundbites with different cache keys, When replaceSoundbites, Then replaced by cache key`() =
        runTest {
            // Given - Insert initial soundbites
            val initialSoundbites = soundbiteEntities.take(3).map { it.copy(cacheKey = "key1") }
            dao.upsertSoundbites(initialSoundbites)

            // When - Replace with new soundbites
            val newSoundbites = listOf(
                soundbiteEntities[5].copy(episodeId = 400L, cacheKey = "key1"),
                soundbiteEntities[6].copy(episodeId = 401L, cacheKey = "key1")
            )
            dao.replaceSoundbites(newSoundbites)

            // Then
            dao.getSoundbitesByCacheKey("key1", 10).test {
                val items = awaitItem()
                assertEquals(2, items.size)
                assertTrue(items.any { it.episodeId == 400L })
                assertTrue(items.any { it.episodeId == 401L })
                cancel()
            }
        }
}