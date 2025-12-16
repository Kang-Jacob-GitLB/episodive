package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingConfig
import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class TrendingFeedRemoteUpdaterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<FeedLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<FeedRemoteDataSource>(relaxed = true)

    @After
    fun teardown() {
        confirmVerified(localDataSource, remoteDataSource)
    }

    @Test
    fun `Given dependencies, When Trending query with language and categories, Then call dataSource's functions`() =
        runTest {
            // Given
            val language = "en"
            val categories = listOf(Category.BUSINESS, Category.EDUCATION)
            val query = FeedQuery.Trending(language = language, categories = categories)
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getTrendingFeedsByCacheKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getTrendingFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getTrendingFeeds(any(), any(), any(), any())
            } returns listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceTrendingFeeds(any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getTrendingFeedsByCacheKey(any(), 10)
                localDataSource.getTrendingFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getTrendingFeeds(1000, null, language, any())
                localDataSource.replaceTrendingFeeds(any())
            }
        }

    @Test
    fun `Given dependencies, When Trending query with language and categories paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val language = "en"
            val categories = listOf(Category.BUSINESS, Category.EDUCATION)
            val query = FeedQuery.Trending(language = language, categories = categories)
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getTrendingFeedsByCacheKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getTrendingFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getTrendingFeeds(any(), any(), any(), any())
            } returns listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceTrendingFeeds(any())
            } just Runs

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getTrendingFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getTrendingFeeds(1000, null, language, any())
                localDataSource.replaceTrendingFeeds(any())
                localDataSource.getTrendingFeedsByCacheKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When Trending query without language, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = FeedQuery.Trending(language = null, categories = emptyList())
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getTrendingFeedsByCacheKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getTrendingFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getTrendingFeeds(any(), any(), any(), any())
            } returns listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceTrendingFeeds(any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getTrendingFeedsByCacheKey(any(), 10)
                localDataSource.getTrendingFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getTrendingFeeds(1000, null, null, any())
                localDataSource.replaceTrendingFeeds(any())
            }
        }

    @Test
    fun `Given dependencies, When Trending query without language paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = FeedQuery.Trending(language = null, categories = emptyList())
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getTrendingFeedsByCacheKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getTrendingFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getTrendingFeeds(any(), any(), any(), any())
            } returns listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceTrendingFeeds(any())
            } just Runs

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getTrendingFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getTrendingFeeds(1000, null, null, any())
                localDataSource.replaceTrendingFeeds(any())
                localDataSource.getTrendingFeedsByCacheKeyPaging(any())
            }
        }
}