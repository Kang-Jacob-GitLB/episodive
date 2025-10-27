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
            dao.getTrendingFeedsByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, trendingFeedEntities.size)
                cancel()
            }

            // When
            dao.deleteTrendingFeed(trendingFeedEntities.first().id)
            dao.getTrendingFeedsByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, trendingFeedEntities.size - 1)
                cancel()
            }

            // When
            dao.deleteTrendingFeeds()
            dao.getTrendingFeedsByCacheKey(cacheKey).test {
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
            dao.getTrendingFeedsByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
            dao.getTrendingFeedsByCacheKey("test_cache1").test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, trendingFeedEntities.size)
                cancel()
            }
        }

    @Test
    fun `Given recent feeds, When upsertRecentFeeds, Then upserted correctly`() =
        runTest {
            // Given
            dao.upsertRecentFeeds(recentFeedEntities)

            // When
            dao.getRecentFeedsByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentFeedEntities.size)
                cancel()
            }

            // When
            dao.deleteRecentFeed(recentFeedEntities.first().id)
            dao.getRecentFeedsByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentFeedEntities.size - 1)
                cancel()
            }

            // When
            dao.deleteRecentFeeds()
            dao.getRecentFeedsByCacheKey(cacheKey).test {
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
            dao.getRecentFeedsByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
            dao.getRecentFeedsByCacheKey("test_cache1").test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentFeedEntities.size)
                cancel()
            }
        }

    @Test
    fun `Given recent new feeds, When upsertRecentNewFeeds, Then upserted correctly`() =
        runTest {
            // Given
            dao.upsertRecentNewFeeds(recentNewFeedEntities)

            // When
            dao.getRecentNewFeedsByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentNewFeedEntities.size)
                cancel()
            }

            // When
            dao.deleteRecentNewFeed(recentNewFeedEntities.first().id)
            dao.getRecentNewFeedsByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentNewFeedEntities.size - 1)
                cancel()
            }

            // When
            dao.deleteRecentNewFeeds()
            dao.getRecentNewFeedsByCacheKey(cacheKey).test {
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
            dao.getRecentNewFeedsByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
            dao.getRecentNewFeedsByCacheKey("test_cache1").test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, recentNewFeedEntities.size)
                cancel()
            }
        }

    @Test
    fun `Given soundbites, When upsertSoundbites, Then upserted correctly`() =
        runTest {
            // Given
            dao.upsertSoundbites(soundbiteEntities)

            // When
            dao.getSoundbitesByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, soundbiteEntities.size)
                cancel()
            }

            // When
            dao.deleteSoundbite(soundbiteEntities.first().episodeId)
            dao.getSoundbitesByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, soundbiteEntities.size - 1)
                cancel()
            }

            // When
            dao.deleteSoundbites()
            dao.getSoundbitesByCacheKey(cacheKey).test {
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
            dao.getSoundbitesByCacheKey(cacheKey).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
            dao.getSoundbitesByCacheKey("test_cache1").test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, soundbiteEntities.size)
                cancel()
            }
        }
}