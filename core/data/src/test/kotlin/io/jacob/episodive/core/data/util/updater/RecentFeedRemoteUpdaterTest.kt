package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingConfig
import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.model.RecentFeedResponse
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

class RecentFeedRemoteUpdaterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<FeedLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<FeedRemoteDataSource>(relaxed = true)

    @After
    fun teardown() {
        confirmVerified(localDataSource, remoteDataSource)
    }

    @Test
    fun `Given dependencies, When Recent query with language and categories, Then call dataSource's functions`() =
        runTest {
            // Given
            val language = "en"
            val categories = listOf(Category.BUSINESS, Category.EDUCATION)
            val query = FeedQuery.Recent(language = language, categories = categories)
            val updater = RecentFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getRecentFeedsByCacheKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getRecentFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRecentFeeds(any(), any(), any(), any())
            } returns listOf(mockk<RecentFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceRecentFeeds(any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getRecentFeedsByCacheKey(any(), 10)
                localDataSource.getRecentFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getRecentFeeds(1000, null, language, any())
                localDataSource.replaceRecentFeeds(any())
            }
        }

    @Test
    fun `Given dependencies, When Recent query with language and categories paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val language = "en"
            val categories = listOf(Category.BUSINESS, Category.EDUCATION)
            val query = FeedQuery.Recent(language = language, categories = categories)
            val updater = RecentFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getRecentFeedsByCacheKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getRecentFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRecentFeeds(any(), any(), any(), any())
            } returns listOf(mockk<RecentFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceRecentFeeds(any())
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
                localDataSource.getRecentFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getRecentFeeds(1000, null, language, any())
                localDataSource.replaceRecentFeeds(any())
                localDataSource.getRecentFeedsByCacheKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When Recent query without language, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = FeedQuery.Recent(language = null, categories = emptyList())
            val updater = RecentFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getRecentFeedsByCacheKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getRecentFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRecentFeeds(any(), any(), any(), any())
            } returns listOf(mockk<RecentFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceRecentFeeds(any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getRecentFeedsByCacheKey(any(), 10)
                localDataSource.getRecentFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getRecentFeeds(1000, null, null, any())
                localDataSource.replaceRecentFeeds(any())
            }
        }

    @Test
    fun `Given dependencies, When Recent query without language paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = FeedQuery.Recent(language = null, categories = emptyList())
            val updater = RecentFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getRecentFeedsByCacheKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getRecentFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRecentFeeds(any(), any(), any(), any())
            } returns listOf(mockk<RecentFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceRecentFeeds(any())
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
                localDataSource.getRecentFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getRecentFeeds(1000, null, null, any())
                localDataSource.replaceRecentFeeds(any())
                localDataSource.getRecentFeedsByCacheKeyPaging(any())
            }
        }
}