package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.FeedDao
import io.jacob.episodive.core.database.mapper.toRecentFeedEntities
import io.jacob.episodive.core.database.mapper.toRecentNewFeedEntities
import io.jacob.episodive.core.database.mapper.toSoundbiteEntities
import io.jacob.episodive.core.database.mapper.toTrendingFeedEntities
import io.jacob.episodive.core.testing.model.recentFeedTestDataList
import io.jacob.episodive.core.testing.model.recentNewFeedTestDataList
import io.jacob.episodive.core.testing.model.soundbiteTestDataList
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class FeedLocalDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val feedDao = mockk<FeedDao>(relaxed = true)

    private val dataSource: FeedLocalDataSource = FeedLocalDataSourceImpl(
        feedDao = feedDao,
    )

    private val cacheKey = "test_cache"
    private val trendingFeedEntities = trendingFeedTestDataList.toTrendingFeedEntities(cacheKey)
    private val recentFeedEntities = recentFeedTestDataList.toRecentFeedEntities(cacheKey)
    private val recentNewFeedEntities = recentNewFeedTestDataList.toRecentNewFeedEntities(cacheKey)
    private val soundbiteEntities = soundbiteTestDataList.toSoundbiteEntities()

    @After
    fun teardown() {
        confirmVerified(feedDao)
    }

    @Test
    fun `Given dependencies, When upsertTrendingFeeds, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.upsertTrendingFeeds(any()) } just Runs

            // When
            dataSource.upsertTrendingFeeds(trendingFeedEntities)

            // Then
            coVerify { feedDao.upsertTrendingFeeds(trendingFeedEntities) }
        }

    @Test
    fun `Given dependencies, When deleteTrendingFeed, Then call dao's method`() =
        runTest {
            // Given
            val id = 1L
            coEvery { feedDao.deleteTrendingFeed(any()) } just Runs

            // When
            dataSource.deleteTrendingFeed(id)

            // Then
            coVerify { feedDao.deleteTrendingFeed(id) }
        }

    @Test
    fun `Given dependencies, When deleteTrendingFeeds, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.deleteTrendingFeeds() } just Runs

            // When
            dataSource.deleteTrendingFeeds()

            // Then
            coVerify { feedDao.deleteTrendingFeeds() }
        }

    @Test
    fun `Given dependencies, When deleteTrendingFeedsByCacheKey, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.deleteTrendingFeedsByCacheKey(any()) } just Runs

            // When
            dataSource.deleteTrendingFeedsByCacheKey(cacheKey)

            // Then
            coVerify { feedDao.deleteTrendingFeedsByCacheKey(cacheKey) }
        }

    @Test
    fun `Given dependencies, When replaceTrendingFeeds, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.replaceTrendingFeeds(any()) } just Runs

            // When
            dataSource.replaceTrendingFeeds(trendingFeedEntities)

            // Then
            coVerify { feedDao.replaceTrendingFeeds(trendingFeedEntities) }
        }

    @Test
    fun `Given dependencies, When getTrendingFeedsByCacheKey, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getTrendingFeedsByCacheKey(any(), 10) } returns mockk()

            // When
            dataSource.getTrendingFeedsByCacheKey(cacheKey, 10)

            // Then
            coVerify { feedDao.getTrendingFeedsByCacheKey(cacheKey, 10) }
        }

    @Test
    fun `Given dependencies, When getTrendingFeedsByCacheKeyPaging, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getTrendingFeedsByCacheKeyPaging(any()) } returns mockk()

            // When
            dataSource.getTrendingFeedsByCacheKeyPaging(cacheKey)

            // Then
            coVerify { feedDao.getTrendingFeedsByCacheKeyPaging(cacheKey) }
        }

    @Test
    fun `Given dependencies, When getTrendingFeedsOldestCachedAtByCacheKey, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getTrendingFeedsOldestCachedAtByCacheKey(any()) } returns mockk()

            // When
            dataSource.getTrendingFeedsOldestCachedAtByCacheKey(cacheKey)

            // Then
            coVerify { feedDao.getTrendingFeedsOldestCachedAtByCacheKey(cacheKey) }
        }

    @Test
    fun `Given dependencies, When upsertRecentFeeds, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.upsertRecentFeeds(any()) } just Runs

            // When
            dataSource.upsertRecentFeeds(recentFeedEntities)

            // Then
            coVerify { feedDao.upsertRecentFeeds(recentFeedEntities) }
        }

    @Test
    fun `Given dependencies, When deleteRecentFeed, Then call dao's method`() =
        runTest {
            // Given
            val id = 1L
            coEvery { feedDao.deleteRecentFeed(any()) } just Runs

            // When
            dataSource.deleteRecentFeed(id)

            // Then
            coVerify { feedDao.deleteRecentFeed(id) }
        }

    @Test
    fun `Given dependencies, When deleteRecentFeeds, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.deleteRecentFeeds() } just Runs

            // When
            dataSource.deleteRecentFeeds()

            // Then
            coVerify { feedDao.deleteRecentFeeds() }
        }

    @Test
    fun `Given dependencies, When deleteRecentFeedsByCacheKey, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.deleteRecentFeedsByCacheKey(any()) } just Runs

            // When
            dataSource.deleteRecentFeedsByCacheKey(cacheKey)

            // Then
            coVerify { feedDao.deleteRecentFeedsByCacheKey(cacheKey) }
        }

    @Test
    fun `Given dependencies, When replaceRecentFeeds, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.replaceRecentFeeds(any()) } just Runs

            // When
            dataSource.replaceRecentFeeds(recentFeedEntities)

            // Then
            coVerify { feedDao.replaceRecentFeeds(recentFeedEntities) }
        }

    @Test
    fun `Given dependencies, When getRecentFeedsByCacheKey, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getRecentFeedsByCacheKey(any(), 10) } returns mockk()

            // When
            dataSource.getRecentFeedsByCacheKey(cacheKey, 10)

            // Then
            coVerify { feedDao.getRecentFeedsByCacheKey(cacheKey, 10) }
        }

    @Test
    fun `Given dependencies, When getRecentFeedsByCacheKeyPaging, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getRecentFeedsByCacheKeyPaging(any()) } returns mockk()

            // When
            dataSource.getRecentFeedsByCacheKeyPaging(cacheKey)

            // Then
            coVerify { feedDao.getRecentFeedsByCacheKeyPaging(cacheKey) }
        }

    @Test
    fun `Given dependencies, When getRecentFeedsOldestCachedAtByCacheKey, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getRecentFeedsOldestCachedAtByCacheKey(any()) } returns mockk()

            // When
            dataSource.getRecentFeedsOldestCachedAtByCacheKey(cacheKey)

            // Then
            coVerify { feedDao.getRecentFeedsOldestCachedAtByCacheKey(cacheKey) }
        }

    @Test
    fun `Given dependencies, When upsertRecentNewFeeds, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.upsertRecentNewFeeds(any()) } just Runs

            // When
            dataSource.upsertRecentNewFeeds(recentNewFeedEntities)

            // Then
            coVerify { feedDao.upsertRecentNewFeeds(recentNewFeedEntities) }
        }

    @Test
    fun `Given dependencies, When deleteRecentNewFeed, Then call dao's method`() =
        runTest {
            // Given
            val id = 1L
            coEvery { feedDao.deleteRecentNewFeed(any()) } just Runs

            // When
            dataSource.deleteRecentNewFeed(id)

            // Then
            coVerify { feedDao.deleteRecentNewFeed(id) }
        }

    @Test
    fun `Given dependencies, When deleteRecentNewFeeds, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.deleteRecentNewFeeds() } just Runs

            // When
            dataSource.deleteRecentNewFeeds()

            // Then
            coVerify { feedDao.deleteRecentNewFeeds() }
        }

    @Test
    fun `Given dependencies, When deleteRecentNewFeedsByCacheKey, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.deleteRecentNewFeedsByCacheKey(any()) } just Runs

            // When
            dataSource.deleteRecentNewFeedsByCacheKey(cacheKey)

            // Then
            coVerify { feedDao.deleteRecentNewFeedsByCacheKey(cacheKey) }
        }

    @Test
    fun `Given dependencies, When replaceRecentNewFeeds, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.replaceRecentNewFeeds(any()) } just Runs

            // When
            dataSource.replaceRecentNewFeeds(recentNewFeedEntities)

            // Then
            coVerify { feedDao.replaceRecentNewFeeds(recentNewFeedEntities) }
        }

    @Test
    fun `Given dependencies, When getRecentNewFeedsByCacheKey, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getRecentNewFeedsByCacheKey(any(), 10) } returns mockk()

            // When
            dataSource.getRecentNewFeedsByCacheKey(cacheKey, 10)

            // Then
            coVerify { feedDao.getRecentNewFeedsByCacheKey(cacheKey, 10) }
        }

    @Test
    fun `Given dependencies, When getRecentNewFeedsByCacheKeyPaging, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getRecentNewFeedsByCacheKeyPaging(any()) } returns mockk()

            // When
            dataSource.getRecentNewFeedsByCacheKeyPaging(cacheKey)

            // Then
            coVerify { feedDao.getRecentNewFeedsByCacheKeyPaging(cacheKey) }
        }

    @Test
    fun `Given dependencies, When getRecentNewFeedsOldestCachedAtByCacheKey, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getRecentNewFeedsOldestCachedAtByCacheKey(any()) } returns mockk()

            // When
            dataSource.getRecentNewFeedsOldestCachedAtByCacheKey(cacheKey)

            // Then
            coVerify { feedDao.getRecentNewFeedsOldestCachedAtByCacheKey(cacheKey) }
        }

    @Test
    fun `Given dependencies, When upsertSoundbites, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.upsertSoundbites(any()) } just Runs

            // When
            dataSource.upsertSoundbites(soundbiteEntities)

            // Then
            coVerify { feedDao.upsertSoundbites(soundbiteEntities) }
        }

    @Test
    fun `Given dependencies, When deleteSoundbite, Then call dao's method`() =
        runTest {
            // Given
            val episodeId = 1L
            coEvery { feedDao.deleteSoundbite(any()) } just Runs

            // When
            dataSource.deleteSoundbite(episodeId)

            // Then
            coVerify { feedDao.deleteSoundbite(episodeId) }
        }

    @Test
    fun `Given dependencies, When deleteSoundbites, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.deleteSoundbites() } just Runs

            // When
            dataSource.deleteSoundbites()

            // Then
            coVerify { feedDao.deleteSoundbites() }
        }

    @Test
    fun `Given dependencies, When replaceSoundbites, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.replaceSoundbites(any()) } just Runs

            // When
            dataSource.replaceSoundbites(soundbiteEntities)

            // Then
            coVerify { feedDao.replaceSoundbites(soundbiteEntities) }
        }

    @Test
    fun `Given dependencies, When getSoundbites, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getSoundbites(10) } returns mockk()

            // When
            dataSource.getSoundbites(10)

            // Then
            coVerify { feedDao.getSoundbites(10) }
        }

    @Test
    fun `Given dependencies, When getSoundbitesPaging, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getSoundbitesPaging() } returns mockk()

            // When
            dataSource.getSoundbitesPaging()

            // Then
            coVerify { feedDao.getSoundbitesPaging() }
        }

    @Test
    fun `Given dependencies, When getSoundbitesOldestCachedAt, Then call dao's method`() =
        runTest {
            // Given
            coEvery { feedDao.getSoundbitesOldestCachedAt() } returns mockk()

            // When
            dataSource.getSoundbitesOldestCachedAt()

            // Then
            coVerify { feedDao.getSoundbitesOldestCachedAt() }
        }
}