package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
    fun `Given Trending query with language and categories, When fetchFromRemote, Then calls getTrendingFeeds with correct parameters`() =
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
            val mockResponses = listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getTrendingFeeds(language = language, includeCategories = any())
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify {
                remoteDataSource.getTrendingFeeds(
                    language = language,
                    includeCategories = any()
                )
            }
        }

    @Test
    fun `Given Trending query without language, When fetchFromRemote, Then calls getTrendingFeeds with null language`() =
        runTest {
            // Given
            val query = FeedQuery.Trending(language = null, categories = emptyList())
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getTrendingFeeds(language = null, includeCategories = any())
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify {
                remoteDataSource.getTrendingFeeds(
                    language = null,
                    includeCategories = any()
                )
            }
        }

    @Test
    fun `Given non-Trending query, When fetchFromRemote, Then returns empty list`() =
        runTest {
            // Given
            val query = FeedQuery.Soundbite
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `Given entities, When saveToLocal, Then calls replaceTrendingFeeds`() =
        runTest {
            // Given
            val query = FeedQuery.Trending()
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val entities = listOf(
                mockk<TrendingFeedEntity>(relaxed = true)
            )

            // When
            updater.saveToLocal(entities)

            // Then
            coVerify { localDataSource.replaceTrendingFeeds(entities) }
        }

    @Test
    fun `Given empty output, When isExpired, Then returns true`() =
        runTest {
            // Given
            val query = FeedQuery.Trending()
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )

            // When
            val result = updater.isExpired(emptyList())

            // Then
            assertTrue(result)
        }

    @Test
    fun `Given expired output, When isExpired, Then returns true`() =
        runTest {
            // Given
            val query = FeedQuery.Trending()
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val expiredTime = now - 2.hours
            val entities = listOf(
                mockk<TrendingFeedEntity>(relaxed = true) {
                    coEvery { cachedAt } returns expiredTime
                }
            )

            // When
            val result = updater.isExpired(entities)

            // Then
            assertTrue(result)
        }

    @Test
    fun `Given valid output, When isExpired, Then returns false`() =
        runTest {
            // Given
            val query = FeedQuery.Trending()
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val recentTime = now - 30.minutes
            val entities = listOf(
                mockk<TrendingFeedEntity>(relaxed = true) {
                    coEvery { cachedAt } returns recentTime
                }
            )

            // When
            val result = updater.isExpired(entities)

            // Then
            assertFalse(result)
        }

    @Test
    fun `Given expired data, When load, Then fetches from remote and saves to local`() =
        runTest {
            // Given
            val query = FeedQuery.Trending()
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getTrendingFeeds(language = null, includeCategories = any())
            } returns mockResponses
            val now = Clock.System.now()
            val expiredTime = now - 2.hours
            val entities = listOf(
                mockk<TrendingFeedEntity>(relaxed = true) {
                    coEvery { cachedAt } returns expiredTime
                }
            )

            // When
            updater.load(entities)

            // Then
            coVerify {
                remoteDataSource.getTrendingFeeds(
                    language = null,
                    includeCategories = any()
                )
            }
            coVerify { localDataSource.replaceTrendingFeeds(any()) }
        }

    @Test
    fun `Given valid data, When load, Then does not fetch from remote`() =
        runTest {
            // Given
            val query = FeedQuery.Trending()
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val recentTime = now - 30.minutes
            val entities = listOf(
                mockk<TrendingFeedEntity>(relaxed = true) {
                    coEvery { cachedAt } returns recentTime
                }
            )

            // When
            updater.load(entities)

            // Then
            coVerify(exactly = 0) {
                remoteDataSource.getTrendingFeeds(
                    language = any(),
                    includeCategories = any()
                )
            }
            coVerify(exactly = 0) { localDataSource.replaceTrendingFeeds(any()) }
        }

    @Test
    fun `Given exception during fetch, When load, Then handles exception gracefully`() =
        runTest {
            // Given
            val query = FeedQuery.Trending()
            val updater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                remoteDataSource.getTrendingFeeds(language = any(), includeCategories = any())
            } throws RuntimeException("Network error")

            // When
            updater.load(emptyList())

            // Then - should not throw exception
            coVerify {
                remoteDataSource.getTrendingFeeds(
                    language = any(),
                    includeCategories = any()
                )
            }
            coVerify(exactly = 0) { localDataSource.replaceTrendingFeeds(any()) }
        }
}