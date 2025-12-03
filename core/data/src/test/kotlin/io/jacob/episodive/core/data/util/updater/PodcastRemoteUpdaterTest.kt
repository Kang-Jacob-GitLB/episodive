package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastDtos
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.model.PodcastResponse
import io.jacob.episodive.core.testing.model.podcastTestDataList
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
import kotlin.time.Duration.Companion.minutes

class PodcastRemoteUpdaterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<PodcastLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<PodcastRemoteDataSource>(relaxed = true)

    @After
    fun teardown() {
        confirmVerified(localDataSource, remoteDataSource)
    }

    @Test
    fun `Given Search query, When fetchFromRemote, Then calls searchPodcasts`() =
        runTest {
            // Given
            val searchQuery = "test podcast"
            val query = PodcastQuery.Search(searchQuery)
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                remoteDataSource.searchPodcasts(searchQuery)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.searchPodcasts(searchQuery) }
        }

    @Test
    fun `Given Medium query, When fetchFromRemote, Then calls getPodcastsByMedium`() =
        runTest {
            // Given
            val medium = "podcast"
            val query = PodcastQuery.Medium(medium)
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getPodcastsByMedium(medium)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.getPodcastsByMedium(medium) }
        }

    @Test
    fun `Given FeedId query with existing podcast, When fetchFromRemote, Then returns single podcast`() =
        runTest {
            // Given
            val feedId = 123L
            val query = PodcastQuery.FeedId(feedId)
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponse = mockk<PodcastResponse>(relaxed = true)
            coEvery {
                remoteDataSource.getPodcastByFeedId(feedId)
            } returns mockResponse

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.size == 1)
            coVerify { remoteDataSource.getPodcastByFeedId(feedId) }
        }

    @Test
    fun `Given FeedId query with null response, When fetchFromRemote, Then returns empty list`() =
        runTest {
            // Given
            val feedId = 123L
            val query = PodcastQuery.FeedId(feedId)
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                remoteDataSource.getPodcastByFeedId(feedId)
            } returns null

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isEmpty())
            coVerify { remoteDataSource.getPodcastByFeedId(feedId) }
        }

    @Test
    fun `Given entities, When saveToLocal, Then calls replacePodcasts`() =
        runTest {
            // Given
            val query = PodcastQuery.Search("test")
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val entities = podcastTestDataList.toPodcastEntities("test_key")

            // When
            updater.saveToLocal(entities)

            // Then
            coVerify { localDataSource.replacePodcasts(entities) }
        }

    @Test
    fun `Given empty output, When isExpired, Then returns true`() =
        runTest {
            // Given
            val query = PodcastQuery.Search("test")
            val updater = PodcastRemoteUpdater(
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
            val query = PodcastQuery.Search("test")
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val expiredTime = now - 40.minutes
            val dtos = podcastTestDataList.toPodcastDtos("test_key")
                .map { it.copy(podcast = it.podcast.copy(cachedAt = expiredTime)) }

            // When
            val result = updater.isExpired(dtos)

            // Then
            assertTrue(result)
        }

    @Test
    fun `Given valid output, When isExpired, Then returns false`() =
        runTest {
            // Given
            val query = PodcastQuery.Search("test")
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val recentTime = now - 20.minutes
            val dtos = podcastTestDataList.toPodcastDtos("test_key")
                .map { it.copy(podcast = it.podcast.copy(cachedAt = recentTime)) }

            // When
            val result = updater.isExpired(dtos)

            // Then
            assertFalse(result)
        }

    @Test
    fun `Given expired data, When load, Then fetches from remote and saves to local`() =
        runTest {
            // Given
            val searchQuery = "test podcast"
            val query = PodcastQuery.Search(searchQuery)
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                remoteDataSource.searchPodcasts(searchQuery)
            } returns mockResponses
            val now = Clock.System.now()
            val expiredTime = now - 40.minutes
            val dtos = podcastTestDataList.toPodcastDtos("test_key")
                .map { it.copy(podcast = it.podcast.copy(cachedAt = expiredTime)) }

            // When
            updater.load(dtos)

            // Then
            coVerify { remoteDataSource.searchPodcasts(searchQuery) }
            coVerify { localDataSource.replacePodcasts(any()) }
        }

    @Test
    fun `Given valid data, When load, Then does not fetch from remote`() =
        runTest {
            // Given
            val query = PodcastQuery.Search("test")
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val recentTime = now - 20.minutes
            val dtos = podcastTestDataList.toPodcastDtos("test_key")
                .map { it.copy(podcast = it.podcast.copy(cachedAt = recentTime)) }

            // When
            updater.load(dtos)

            // Then
            coVerify(exactly = 0) { remoteDataSource.searchPodcasts(any()) }
            coVerify(exactly = 0) { localDataSource.replacePodcasts(any()) }
        }

    @Test
    fun `Given exception during fetch, When load, Then handles exception gracefully`() =
        runTest {
            // Given
            val searchQuery = "test podcast"
            val query = PodcastQuery.Search(searchQuery)
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                remoteDataSource.searchPodcasts(searchQuery)
            } throws RuntimeException("Network error")

            // When
            updater.load(emptyList())

            // Then - should not throw exception
            coVerify { remoteDataSource.searchPodcasts(searchQuery) }
            coVerify(exactly = 0) { localDataSource.replacePodcasts(any()) }
        }

    @Test
    fun `Given ByChannel query, When fetchFromRemote, Then calls getPodcastsByGuids`() =
        runTest {
            // Given
            val channel = Channel(
                id = 1,
                title = "Test Channel",
                description = "Test Description",
                image = "https://example.com/image.jpg",
                link = "https://example.com",
                count = 3,
                podcastGuids = listOf("guid1", "guid2", "guid3")
            )
            val query = PodcastQuery.ByChannel(channel)
            val updater = PodcastRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getPodcastsByGuids(channel.podcastGuids)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.getPodcastsByGuids(channel.podcastGuids) }
        }
}