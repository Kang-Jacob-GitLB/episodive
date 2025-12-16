package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.data.util.updater.RecentFeedRemoteUpdater
import io.jacob.episodive.core.data.util.updater.RecentNewFeedRemoteUpdater
import io.jacob.episodive.core.data.util.updater.SoundbiteRemoteUpdater
import io.jacob.episodive.core.data.util.updater.TrendingFeedRemoteUpdater
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toTrendingFeedEntities
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

class FeedRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<FeedLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<FeedRemoteDataSource>(relaxed = true)
    private val trendingFeedRemoteUpdater = mockk<TrendingFeedRemoteUpdater.Factory>(relaxed = true)
    private val recentFeedRemoteUpdater = mockk<RecentFeedRemoteUpdater.Factory>(relaxed = true)
    private val recentNewFeedRemoteUpdater =
        mockk<RecentNewFeedRemoteUpdater.Factory>(relaxed = true)
    private val soundbiteRemoteUpdater = mockk<SoundbiteRemoteUpdater.Factory>(relaxed = true)

    private val repository: FeedRepository = FeedRepositoryImpl(
        localDataSource = localDataSource,
        remoteDataSource = remoteDataSource,
        trendingFeedRemoteUpdater = trendingFeedRemoteUpdater,
        recentFeedRemoteUpdater = recentFeedRemoteUpdater,
        recentNewFeedRemoteUpdater = recentNewFeedRemoteUpdater,
        soundbiteRemoteUpdater = soundbiteRemoteUpdater,
    )

    @After
    fun teardown() {
        confirmVerified(
            localDataSource,
            remoteDataSource,
            trendingFeedRemoteUpdater,
            recentFeedRemoteUpdater,
            recentNewFeedRemoteUpdater,
            soundbiteRemoteUpdater
        )
    }

    @Test
    fun `Given params, When getTrendingFeeds is called, Then calls localDataSource and RemoteUpdater`() =
        runTest {
            // Given
            val max = 10
            val query = FeedQuery.Trending(
                language = "ko",
                categories = listOf(Category.CAREERS, Category.SELF_IMPROVEMENT)
            )

            val updater = mockk<TrendingFeedRemoteUpdater>(relaxed = true)
            val cachedAt = Clock.System.now()
            val trendingFeedsEntities = trendingFeedTestDataList.toTrendingFeedEntities(
                cacheKey = query.key,
                cachedAt = cachedAt,
            )
            coEvery { updater.getFlowList(max) } returns flowOf(trendingFeedsEntities)
            coEvery {
                trendingFeedRemoteUpdater.create(query)
            } returns updater

            // When
            repository.getTrendingFeeds(
                max = max,
                language = query.language,
                includeCategories = query.categories,
            ).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                trendingFeedRemoteUpdater.create(query)
                updater.getFlowList(max)
            }
        }

    @Test
    fun `Given params, When getRecentFeeds is called, Then calls localDataSource and RemoteUpdater`() =
        runTest {
            // Given
            val max = 10
            val query = FeedQuery.Recent(
                language = "ko",
                categories = listOf(Category.CAREERS, Category.SELF_IMPROVEMENT)
            )

            val updater = mockk<RecentFeedRemoteUpdater>(relaxed = true)
            coEvery { updater.getFlowList(max) } returns flowOf(emptyList())
            coEvery { recentFeedRemoteUpdater.create(query) } returns updater

            // When
            repository.getRecentFeeds(
                max = max,
                language = query.language,
                includeCategories = query.categories,
            ).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                recentFeedRemoteUpdater.create(query)
                updater.getFlowList(max)
            }
        }

    @Test
    fun `Given params, When getRecentNewFeeds is called, Then calls localDataSource and RemoteUpdater`() =
        runTest {
            // Given
            val max = 10
            val query = FeedQuery.RecentNew

            val updater = mockk<RecentNewFeedRemoteUpdater>(relaxed = true)
            coEvery { updater.getFlowList(max) } returns flowOf(emptyList())
            coEvery { recentNewFeedRemoteUpdater.create(query) } returns updater

            // When
            repository.getRecentNewFeeds(max = max).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                recentNewFeedRemoteUpdater.create(query)
                updater.getFlowList(max)
            }
        }

    @Test
    fun `Given params, When getRecentSoundbites is called, Then calls localDataSource and RemoteUpdater`() =
        runTest {
            // Given
            val max = 10
            val query = FeedQuery.Soundbite

            val updater = mockk<SoundbiteRemoteUpdater>(relaxed = true)
            coEvery { updater.getFlowList(max) } returns flowOf(emptyList())
            coEvery { soundbiteRemoteUpdater.create(query) } returns updater

            // When
            repository.getRecentSoundbites(max = max).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                soundbiteRemoteUpdater.create(query)
                updater.getFlowList(max)
            }
        }
}