package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastDtos
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.network.datasource.ChannelRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class PodcastRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<PodcastLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<PodcastRemoteDataSource>(relaxed = true)
    private val channelRemoteDataSource = mockk<ChannelRemoteDataSource>(relaxed = true)
    private val remoteUpdater = mockk<PodcastRemoteUpdater.Factory>(relaxed = true)

    private val repository: PodcastRepository = PodcastRepositoryImpl(
        localDataSource = localDataSource,
        remoteDataSource = remoteDataSource,
        channelRemoteDataSource = channelRemoteDataSource,
        remoteUpdater = remoteUpdater,
    )

    private val podcastDtos = podcastTestDataList.toPodcastDtos("test_key")

    @After
    fun teardown() {
        confirmVerified(
            localDataSource,
            remoteDataSource,
            channelRemoteDataSource,
            remoteUpdater,
        )
    }

    @Test
    fun `Given suspend dataSource, When getChannels is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            coEvery { channelRemoteDataSource.getChannels() } returns mockk(relaxed = true)

            // When
            repository.getChannels().test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                channelRemoteDataSource.getChannels()
            }
        }

    @Test
    fun `Given search, When searchPodcasts is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val search = "test"
            val query = PodcastQuery.Search(search)
            coEvery {
                remoteUpdater.create(query)
            } returns mockk<PodcastRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getPodcastsByCacheKey(query.key)
            } returns flowOf(podcastDtos)

            // When
            repository.searchPodcasts(search).test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(podcastTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(query)
                localDataSource.getPodcastsByCacheKey(query.key)
            }
        }

    @Test
    fun `Given feedId, When getPodcastByFeedId is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val feedId = 12345L
            val query = PodcastQuery.FeedId(feedId)
            coEvery {
                remoteUpdater.create(query)
            } returns mockk<PodcastRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getPodcast(feedId)
            } returns flowOf(podcastDtos.first())

            // When
            repository.getPodcastByFeedId(feedId).test {
                val result = awaitItem()
                // Then
                assertEquals(podcastTestData.id, result?.id)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(query)
                localDataSource.getPodcast(feedId)
            }
        }

    @Test
    fun `Given feedUrl, When getPodcastByFeedUrl is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            coEvery {
                remoteDataSource.getPodcastByFeedUrl(any())
            } returns mockk(relaxed = true)

            // When
            repository.getPodcastByFeedUrl("test").test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerify { remoteDataSource.getPodcastByFeedUrl(any()) }
        }

    @Test
    fun `Given guid, When getPodcastByGuid is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            coEvery {
                remoteDataSource.getPodcastByGuid(any())
            } returns mockk(relaxed = true)

            // When
            repository.getPodcastByGuid("test").test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerify { remoteDataSource.getPodcastByGuid(any()) }
        }

    @Test
    fun `Given medium, When getPodcastsByMedium is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val medium = "test"
            val query = PodcastQuery.Medium(medium)
            coEvery {
                remoteUpdater.create(query)
            } returns mockk<PodcastRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getPodcastsByCacheKey(query.key)
            } returns flowOf(podcastDtos)

            // When
            repository.getPodcastsByMedium(medium).test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(podcastTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(query)
                localDataSource.getPodcastsByCacheKey(query.key)
            }
        }

    @Test
    fun `Given dependencies, When getFollowedPodcasts is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val dtos = podcastDtos.mapIndexed { index, dto ->
                dto.copy(
                    followedAt = Instant.fromEpochSeconds(1757568578L + index),
                    isNotificationEnabled = true,
                )
            }
            coEvery { localDataSource.getFollowedPodcasts() } returns flowOf(dtos)

            // When
            repository.getFollowedPodcasts().test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(podcastTestDataList[0].id, result[0].id)
                assertEquals(podcastTestDataList[1].id, result[1].id)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getFollowedPodcasts()
            }
        }

    @Test
    fun `Given dependencies, When toggleFollowed is called, Then call methods of dataSources`() =
        runTest {
            // Given
            val podcastId = 12345L
            coEvery { localDataSource.toggleFollowed(podcastId) } returns true

            // When
            repository.toggleFollowed(podcastId)

            // Then
            coVerifySequence {
                localDataSource.toggleFollowed(podcastId)
            }
        }

    @Test
    fun `Given channel, When getPodcastsByChannel is called, Then calls methods of dataSources`() =
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
            coEvery {
                remoteUpdater.create(query)
            } returns mockk<PodcastRemoteUpdater>(relaxed = true)
            coEvery {
                localDataSource.getPodcastsByCacheKey(query.key)
            } returns flowOf(podcastDtos)

            // When
            repository.getPodcastsByChannel(channel).test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(podcastTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(query)
                localDataSource.getPodcastsByCacheKey(query.key)
            }
        }
}