package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.data.util.updater.RecentFeedRemoteUpdater
import io.jacob.episodive.core.data.util.updater.RecentNewFeedRemoteUpdater
import io.jacob.episodive.core.data.util.updater.SoundbiteRemoteUpdater
import io.jacob.episodive.core.data.util.updater.TrendingFeedRemoteUpdater
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class FeedRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<FeedLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<FeedRemoteDataSource>(relaxed = true)

    private val repository: FeedRepository = FeedRepositoryImpl(
        localDataSource = localDataSource,
        remoteDataSource = remoteDataSource,
    )

    @After
    fun teardown() {
        unmockkAll()
        confirmVerified(localDataSource, remoteDataSource)
    }

    @Test
    fun `Given params, When getTrendingFeeds is called, Then calls localDataSource and RemoteUpdater`() =
        runTest {
            // Given
            val query = FeedQuery.Trending(
                language = "ko",
                categories = listOf(Category.CAREERS, Category.SELF_IMPROVEMENT)
            )
            mockkConstructor(TrendingFeedRemoteUpdater::class)
            coEvery {
                anyConstructed<TrendingFeedRemoteUpdater>().load(any<List<TrendingFeedEntity>>())
            } returns Unit
            coEvery {
                localDataSource.getTrendingFeedsByCacheKey(any())
            } returns flowOf(emptyList())

            // When
            repository.getTrendingFeeds(
                language = query.language,
                includeCategories = query.categories,
            ).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerify {
                localDataSource.getTrendingFeedsByCacheKey(any())
            }
        }

    @Test
    fun `Given params, When getRecentFeeds is called, Then calls localDataSource and RemoteUpdater`() =
        runTest {
            // Given
            val query = FeedQuery.Recent(
                language = "ko",
                categories = listOf(Category.CAREERS, Category.SELF_IMPROVEMENT)
            )
            mockkConstructor(RecentFeedRemoteUpdater::class)
            coEvery {
                anyConstructed<RecentFeedRemoteUpdater>().load(any<List<RecentFeedEntity>>())
            } returns Unit
            coEvery {
                localDataSource.getRecentFeedsByCacheKey(any())
            } returns flowOf(emptyList())

            // When
            repository.getRecentFeeds(
                language = query.language,
                includeCategories = query.categories,
            ).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerify {
                localDataSource.getRecentFeedsByCacheKey(any())
            }
        }

    @Test
    fun `Given params, When getRecentNewFeeds is called, Then calls localDataSource and RemoteUpdater`() =
        runTest {
            // Given
            val query = FeedQuery.RecentNew
            mockkConstructor(RecentNewFeedRemoteUpdater::class)
            coEvery {
                anyConstructed<RecentNewFeedRemoteUpdater>().load(any<List<RecentNewFeedEntity>>())
            } returns Unit
            coEvery {
                localDataSource.getRecentNewFeedsByCacheKey(any())
            } returns flowOf(emptyList())

            // When
            repository.getRecentNewFeeds().test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerify {
                localDataSource.getRecentNewFeedsByCacheKey(any())
            }
        }

    @Test
    fun `Given params, When getRecentSoundbites is called, Then calls localDataSource and RemoteUpdater`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            mockkConstructor(SoundbiteRemoteUpdater::class)
            coEvery {
                anyConstructed<SoundbiteRemoteUpdater>().load(any<List<SoundbiteEntity>>())
            } returns Unit
            coEvery {
                localDataSource.getSoundbitesByCacheKey(any())
            } returns flowOf(emptyList())

            // When
            repository.getRecentSoundbites().test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerify {
                localDataSource.getSoundbitesByCacheKey(any())
            }
        }
}