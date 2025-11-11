package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisodeDtos
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.model.EpisodeResponse
import io.jacob.episodive.core.testing.model.episodeTestDataList
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

class EpisodeRemoteUpdaterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<EpisodeLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<EpisodeRemoteDataSource>(relaxed = true)

    @After
    fun teardown() {
        confirmVerified(localDataSource, remoteDataSource)
    }

    @Test
    fun `Given Person query, When fetchFromRemote, Then calls searchEpisodesByPerson`() =
        runTest {
            // Given
            val person = "John Doe"
            val query = EpisodeQuery.Person(person)
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                remoteDataSource.searchEpisodesByPerson(person, 10000)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.searchEpisodesByPerson(person, 10000) }
        }

    @Test
    fun `Given FeedId query, When fetchFromRemote, Then calls getEpisodesByFeedId`() =
        runTest {
            // Given
            val feedId = 123L
            val query = EpisodeQuery.FeedId(feedId)
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getEpisodesByFeedId(feedId, 10000)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.getEpisodesByFeedId(feedId, 10000) }
        }

    @Test
    fun `Given FeedUrl query, When fetchFromRemote, Then calls getEpisodesByFeedUrl`() =
        runTest {
            // Given
            val feedUrl = "https://example.com/feed.xml"
            val query = EpisodeQuery.FeedUrl(feedUrl)
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getEpisodesByFeedUrl(feedUrl, 10000)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.getEpisodesByFeedUrl(feedUrl, 10000) }
        }

    @Test
    fun `Given PodcastGuid query, When fetchFromRemote, Then calls getEpisodesByPodcastGuid`() =
        runTest {
            // Given
            val podcastGuid = "test-guid"
            val query = EpisodeQuery.PodcastGuid(podcastGuid)
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getEpisodesByPodcastGuid(podcastGuid, 10000)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.getEpisodesByPodcastGuid(podcastGuid, 10000) }
        }

    @Test
    fun `Given Live query, When fetchFromRemote, Then calls getLiveEpisodes`() =
        runTest {
            // Given
            val query = EpisodeQuery.Live
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getLiveEpisodes(6)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.getLiveEpisodes(6) }
        }

    @Test
    fun `Given Random query, When fetchFromRemote, Then calls getRandomEpisodes`() =
        runTest {
            // Given
            val query = EpisodeQuery.Random
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getRandomEpisodes(6)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.getRandomEpisodes(6) }
        }

    @Test
    fun `Given Recent query, When fetchFromRemote, Then calls getRecentEpisodes`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getRecentEpisodes(6)
            } returns mockResponses

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isNotEmpty())
            coVerify { remoteDataSource.getRecentEpisodes(6) }
        }

    @Test
    fun `Given EpisodeId query with existing episode, When fetchFromRemote, Then returns single episode`() =
        runTest {
            // Given
            val episodeId = 123L
            val query = EpisodeQuery.EpisodeId(episodeId)
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponse = mockk<EpisodeResponse>(relaxed = true)
            coEvery {
                remoteDataSource.getEpisodeById(episodeId)
            } returns mockResponse

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.size == 1)
            coVerify { remoteDataSource.getEpisodeById(episodeId) }
        }

    @Test
    fun `Given EpisodeId query with null response, When fetchFromRemote, Then returns empty list`() =
        runTest {
            // Given
            val episodeId = 123L
            val query = EpisodeQuery.EpisodeId(episodeId)
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                remoteDataSource.getEpisodeById(episodeId)
            } returns null

            // When
            val result = updater.fetchFromRemote()

            // Then
            assertTrue(result.isEmpty())
            coVerify { remoteDataSource.getEpisodeById(episodeId) }
        }

    @Test
    fun `Given entities, When saveToLocal, Then calls replaceEpisodes`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val entities = episodeTestDataList.toEpisodeEntities("test_key")

            // When
            updater.saveToLocal(entities)

            // Then
            coVerify { localDataSource.replaceEpisodes(entities) }
        }

    @Test
    fun `Given empty output, When isExpired, Then returns true`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
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
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val expiredTime = now - 15.minutes
            val dtos = episodeTestDataList.toEpisodeDtos("test_key")
                .map { it.copy(episode = it.episode.copy(cachedAt = expiredTime)) }

            // When
            val result = updater.isExpired(dtos)

            // Then
            assertTrue(result)
        }

    @Test
    fun `Given valid output, When isExpired, Then returns false`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val recentTime = now - 5.minutes
            val dtos = episodeTestDataList.toEpisodeDtos("test_key")
                .map { it.copy(episode = it.episode.copy(cachedAt = recentTime)) }

            // When
            val result = updater.isExpired(dtos)

            // Then
            assertFalse(result)
        }

    @Test
    fun `Given expired data, When load, Then fetches from remote and saves to local`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val mockResponses = listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                remoteDataSource.getRecentEpisodes(6)
            } returns mockResponses
            val now = Clock.System.now()
            val expiredTime = now - 15.minutes
            val dtos = episodeTestDataList.toEpisodeDtos("test_key")
                .map { it.copy(episode = it.episode.copy(cachedAt = expiredTime)) }

            // When
            updater.load(dtos)

            // Then
            coVerify { remoteDataSource.getRecentEpisodes(6) }
            coVerify { localDataSource.replaceEpisodes(any()) }
        }

    @Test
    fun `Given valid data, When load, Then does not fetch from remote`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            val now = Clock.System.now()
            val recentTime = now - 5.minutes
            val dtos = episodeTestDataList.toEpisodeDtos("test_key")
                .map { it.copy(episode = it.episode.copy(cachedAt = recentTime)) }

            // When
            updater.load(dtos)

            // Then
            coVerify(exactly = 0) { remoteDataSource.getRecentEpisodes(any()) }
            coVerify(exactly = 0) { localDataSource.replaceEpisodes(any()) }
        }

    @Test
    fun `Given exception during fetch, When load, Then handles exception gracefully`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query,
            )
            coEvery {
                remoteDataSource.getRecentEpisodes(6)
            } throws RuntimeException("Network error")

            // When
            updater.load(emptyList())

            // Then - should not throw exception
            coVerify { remoteDataSource.getRecentEpisodes(6) }
            coVerify(exactly = 0) { localDataSource.replaceEpisodes(any()) }
        }
}