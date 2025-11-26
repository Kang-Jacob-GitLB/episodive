package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.data.util.updater.EpisodeRemoteUpdater
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisodeDtos
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.network.datasource.ChapterRemoteDataSource
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class EpisodeRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<EpisodeLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<EpisodeRemoteDataSource>(relaxed = true)
    private val chapterRemoteDataSource = mockk<ChapterRemoteDataSource>(relaxed = true)
    private val remoteUpdater = mockk<EpisodeRemoteUpdater.Factory>(relaxed = true)

    private val repository: EpisodeRepository = EpisodeRepositoryImpl(
        localDataSource = localDataSource,
        remoteDataSource = remoteDataSource,
        chapterRemoteDataSource = chapterRemoteDataSource,
        remoteUpdater = remoteUpdater,
    )

    private val episodeDtos = episodeTestDataList.toEpisodeDtos("test_key")

    @After
    fun teardown() {
        confirmVerified(localDataSource, remoteDataSource, remoteUpdater, chapterRemoteDataSource)
    }

    @Test
    fun `Given person, When searchEpisodesByPerson, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val person = "John Doe"
            val expectedQuery = EpisodeQuery.Person(person)

            coEvery {
                remoteUpdater.create(expectedQuery)
            } returns mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            } returns flowOf(episodeDtos)

            // When
            repository.searchEpisodesByPerson(person, max = 10).test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            }
        }

    @Test
    fun `Given feedId, When getEpisodesByFeedId, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val feedId = 123L
            val expectedQuery = EpisodeQuery.FeedId(feedId)

            coEvery {
                remoteUpdater.create(expectedQuery)
            } returns mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            } returns flowOf(episodeDtos)

            // When
            repository.getEpisodesByFeedId(feedId, max = 10).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            }
        }

    @Test
    fun `Given feedUrl, When getEpisodesByFeedUrl, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val feedUrl = "https://example.com/feed.xml"
            val expectedQuery = EpisodeQuery.FeedUrl(feedUrl)

            coEvery {
                remoteUpdater.create(expectedQuery)
            } returns mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            } returns flowOf(episodeDtos)

            // When
            repository.getEpisodesByFeedUrl(feedUrl, max = 10).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            }
        }

    @Test
    fun `Given podcastGuid, When getEpisodesByPodcastGuid, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val guid = "test-podcast-guid"
            val expectedQuery = EpisodeQuery.PodcastGuid(guid)

            coEvery {
                remoteUpdater.create(expectedQuery)
            } returns mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            } returns flowOf(episodeDtos)

            // When
            repository.getEpisodesByPodcastGuid(guid, max = 10).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            }
        }

    @Test
    fun `Given episodeId, When getEpisodeById, Then calls remoteDataSource directly`() =
        runTest {
            // Given
            val episodeId = 1234L
            val query = EpisodeQuery.EpisodeId(episodeId)
            coEvery {
                remoteUpdater.create(query)
            } returns mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getEpisode(episodeId)
            } returns flowOf(episodeDtos.first())

            // When
            repository.getEpisodeById(episodeId).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestData.id, result?.id)
                awaitComplete()
            }
            coVerify {
                remoteUpdater.create(query)
                localDataSource.getEpisode(episodeId)
            }
        }

    @Test
    fun `When getLiveEpisodes, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val expectedQuery = EpisodeQuery.Live

            coEvery {
                remoteUpdater.create(expectedQuery)
            } returns mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            } returns flowOf(episodeDtos)

            // When
            repository.getLiveEpisodes(max = 10).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerify {
                remoteUpdater.create(expectedQuery)
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            }
        }

    @Test
    fun `Given parameters, When getRandomEpisodes, Then calls remoteDataSource directly`() =
        runTest {
            // Given
            val query = EpisodeQuery.Random

            coEvery {
                remoteUpdater.create(query)
            } returns mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getEpisodesByCacheKey(query.key)
            } returns flowOf(episodeDtos)

            // When
            repository.getRandomEpisodes().test {
                val result = awaitItem()

                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerify {
                remoteUpdater.create(query)
                localDataSource.getEpisodesByCacheKey(query.key)
            }
        }

    @Test
    fun `When getRecentEpisodes, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val expectedQuery = EpisodeQuery.Recent

            coEvery {
                remoteUpdater.create(expectedQuery)
            } returns mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            } returns flowOf(episodeDtos)

            // When
            repository.getRecentEpisodes(max = 10).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerify {
                remoteUpdater.create(expectedQuery)
                localDataSource.getEpisodesByCacheKey(expectedQuery.key)
            }
        }

    @Test
    fun `When getLikedEpisodes, Then calls localDataSource directly`() =
        runTest {
            // Given
            val dtos = episodeDtos.mapIndexed { index, dto ->
                dto.copy(likedAt = Instant.fromEpochSeconds(1757883600L + index))
            }
            coEvery { localDataSource.getLikedEpisodes() } returns flowOf(dtos)

            // When
            repository.getLikedEpisodes().test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(dtos[0].episode.id, result[0].id)
                assertEquals(dtos[1].episode.id, result[1].id)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getLikedEpisodes()
            }
        }

    @Test
    fun `When getPlayingEpisodes, Then calls localDataSource directly`() =
        runTest {
            // Given
            val dtos = episodeDtos.mapIndexed { index, dto ->
                dto.copy(
                    playedAt = Instant.fromEpochSeconds(1757883600L + index),
                    isCompleted = index < 5,
                )
            }
            coEvery { localDataSource.getPlayedEpisodes() } returns flowOf(dtos)

            // When
            repository.getPlayingEpisodes().test {
                val result = awaitItem()
                // Then
                assertEquals(5, result.size)
                assertEquals(dtos[5].episode.id, result[0].id)
                assertEquals(dtos[6].episode.id, result[1].id)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getPlayedEpisodes()
            }
        }

    @Test
    fun `When getPlayedEpisodes, Then calls localDataSource directly`() =
        runTest {
            // Given
            val dtos = episodeDtos.mapIndexed { index, dto ->
                dto.copy(
                    playedAt = Instant.fromEpochSeconds(1757883600L + index),
                    isCompleted = index < 5,
                )
            }
            coEvery { localDataSource.getPlayedEpisodes() } returns flowOf(dtos)

            // When
            repository.getPlayedEpisodes().test {
                val result = awaitItem()
                // Then
                assertEquals(5, result.size)
                assertEquals(dtos[0].episode.id, result[0].id)
                assertEquals(dtos[1].episode.id, result[1].id)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getPlayedEpisodes()
            }
        }

    @Test
    fun `When getAllPlayedEpisodes, Then calls localDataSource directly`() =
        runTest {
            // Given
            val dtos = episodeDtos.mapIndexed { index, dto ->
                dto.copy(
                    playedAt = Instant.fromEpochSeconds(1757883600L + index),
                    isCompleted = index < 5,
                )
            }
            coEvery { localDataSource.getPlayedEpisodes() } returns flowOf(dtos)

            // When
            repository.getAllPlayedEpisodes().test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(dtos[0].episode.id, result[0].id)
                assertEquals(dtos[1].episode.id, result[1].id)
                assertEquals(dtos[5].episode.id, result[5].id)
                assertEquals(dtos[6].episode.id, result[6].id)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getPlayedEpisodes()
            }
        }

    @Test
    fun `Given dependencies, When toggleLiked, Then calls localDataSource toggleLiked`() =
        runTest {
            // Given
            val episodeId = 123L
            coEvery { localDataSource.toggleLiked(episodeId) } returns true

            // When
            repository.toggleLiked(episodeId)

            // Then
            coVerifySequence {
                localDataSource.toggleLiked(episodeId)
            }
        }

    @Test
    fun `When updatePlayed, Then calls localDataSource upsertPlayed`() =
        runTest {
            // Given
            val episodeId = 123L
            val position = 30.seconds
            val isCompleted = false
            coEvery { localDataSource.upsertPlayed(any()) } returns Unit

            // When
            repository.updatePlayed(episodeId, position, isCompleted)

            // Then
            coVerify {
                localDataSource.upsertPlayed(
                    match {
                        it.id == episodeId && it.position == position && it.isCompleted == isCompleted
                    }
                )
            }
        }

    @Test
    fun `Given dependencies, When updateDurationOfEpisodes is called, Then calls localDataSource updateDurationOfEpisodes`() =
        runTest {
            // Given
            coEvery { localDataSource.updateDurationOfEpisodes(any(), any()) } just Runs

            // When
            repository.updateDurationOfEpisodes(123L, 30.seconds)

            // Then
            coVerifySequence {
                localDataSource.updateDurationOfEpisodes(123L, 30.seconds)
            }
        }

    @Test
    fun `Given dependencies, When fetchChapters is called, Then calls chapterRemoteDataSource fetchChapters`() =
        runTest {
            // Given
            val url = "https://example.com/chapters.json"
            coEvery { chapterRemoteDataSource.fetchChapters(any()) } returns emptyList()

            // When
            repository.fetchChapters(url)

            // Then
            coVerifySequence {
                chapterRemoteDataSource.fetchChapters(url)
            }
        }
}