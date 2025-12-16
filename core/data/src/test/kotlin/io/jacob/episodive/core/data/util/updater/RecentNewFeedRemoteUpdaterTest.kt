package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingConfig
import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.model.RecentNewFeedResponse
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

class RecentNewFeedRemoteUpdaterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<FeedLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<FeedRemoteDataSource>(relaxed = true)

    @After
    fun teardown() {
        confirmVerified(localDataSource, remoteDataSource)
    }

    @Test
    fun `Given dependencies, When RecentNew query, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = FeedQuery.RecentNew
            val updater = RecentNewFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getRecentNewFeedsByCacheKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getRecentNewFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRecentNewFeeds(any(), any())
            } returns listOf(mockk<RecentNewFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceRecentNewFeeds(any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getRecentNewFeedsByCacheKey(any(), 10)
                localDataSource.getRecentNewFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getRecentNewFeeds(1000, null)
                localDataSource.replaceRecentNewFeeds(any())
            }
        }

    @Test
    fun `Given dependencies, When RecentNew query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = FeedQuery.RecentNew
            val updater = RecentNewFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getRecentNewFeedsByCacheKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getRecentNewFeedsOldestCachedAtByCacheKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRecentNewFeeds(any(), any())
            } returns listOf(mockk<RecentNewFeedResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceRecentNewFeeds(any())
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
                localDataSource.getRecentNewFeedsOldestCachedAtByCacheKey(any())
                remoteDataSource.getRecentNewFeeds(1000, null)
                localDataSource.replaceRecentNewFeeds(any())
                localDataSource.getRecentNewFeedsByCacheKeyPaging(any())
            }
        }
}