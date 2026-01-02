package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastWithExtrasViews
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.model.PodcastResponse
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
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
    private val remoteUpdater = mockk<PodcastRemoteUpdater.Factory>(relaxed = true)

    private val repository: PodcastRepository = PodcastRepositoryImpl(
        podcastLocalDataSource = localDataSource,
        podcastRemoteDataSource = remoteDataSource,
        remoteUpdater = remoteUpdater,
    )

    private val podcastDtos = podcastTestDataList.toPodcastWithExtrasViews()

    @After
    fun teardown() {
        confirmVerified(
            localDataSource,
            remoteDataSource,
            remoteUpdater,
        )
    }

    @Test
    fun `Given search, When searchPodcasts is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val max = 10
            val search = "test"

            coEvery {
                remoteDataSource.searchPodcasts(any(), any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))

            // When
            repository.searchPodcasts(search, max = max).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                remoteDataSource.searchPodcasts(search, max)
            }
        }

    @Test
    fun `Given feedId, When getPodcastByFeedId is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val feedId = 12345L
            val query = PodcastQuery.FeedId(feedId)

            val updater = mockk<PodcastRemoteUpdater>(relaxed = true)
            coEvery { updater.getFlowList(1) } returns flowOf(listOf(podcastDtos.first()))
            coEvery { remoteUpdater.create(query) } returns updater

            // When
            repository.getPodcastByFeedId(feedId).test {
                val result = awaitItem()
                // Then
                assertEquals(podcastTestData.id, result?.id)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(query)
                updater.getFlowList(1)
            }
        }

    @Test
    fun `Given feedUrl, When getPodcastByFeedUrl is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val feedUrl = "test"
            val query = PodcastQuery.FeedUrl(feedUrl)

            val updater = mockk<PodcastRemoteUpdater>(relaxed = true)
            coEvery { updater.getFlowList(1) } returns flowOf(listOf(podcastDtos.first()))
            coEvery { remoteUpdater.create(query) } returns updater

            // When
            repository.getPodcastByFeedUrl(feedUrl).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                remoteUpdater.create(query)
                updater.getFlowList(1)
            }
        }

    @Test
    fun `Given guid, When getPodcastByGuid is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val guid = "test"
            val query = PodcastQuery.FeedGuid(guid)

            val updater = mockk<PodcastRemoteUpdater>(relaxed = true)
            coEvery { updater.getFlowList(1) } returns flowOf(listOf(podcastDtos.first()))
            coEvery { remoteUpdater.create(query) } returns updater

            // When
            repository.getPodcastByGuid(guid).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                remoteUpdater.create(query)
                updater.getFlowList(1)
            }
        }

    @Test
    fun `Given medium, When getPodcastsByMedium is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val max = 10
            val medium = "test"
            val query = PodcastQuery.Medium(medium)

            val updater = mockk<PodcastRemoteUpdater>(relaxed = true)
            coEvery { updater.getFlowList(max) } returns flowOf(podcastDtos)
            coEvery { remoteUpdater.create(query) } returns updater

            // When
            repository.getPodcastsByMedium(medium, max = max).test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(podcastTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(query)
                updater.getFlowList(max)
            }
        }

    @Test
    fun `Given dependencies, When getFollowedPodcasts is called, Then calls methods of dataSources`() =
        runTest {
            // Given
            val max = 10
            val dtos = podcastDtos.mapIndexed { index, dto ->
                dto.copy(
                    followedAt = Instant.fromEpochSeconds(1757568578L + index),
                    isNotificationEnabled = true,
                )
            }
            coEvery { localDataSource.getFollowedPodcasts(limit = max) } returns flowOf(dtos)

            // When
            repository.getFollowedPodcasts(max = max).test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(podcastTestDataList[0].id, result[0].id)
                assertEquals(podcastTestDataList[1].id, result[1].id)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getFollowedPodcasts(limit = max)
            }
        }

    @Test
    fun `Given dependencies, When toggleFollowed is called, Then call methods of dataSources`() =
        runTest {
            // Given
            val podcastId = 12345L
            coEvery { localDataSource.toggleFollowedPodcast(podcastId) } returns true

            // When
            repository.toggleFollowed(podcastId)

            // Then
            coVerifySequence {
                localDataSource.toggleFollowedPodcast(podcastId)
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

            val updater = mockk<PodcastRemoteUpdater>(relaxed = true)
            coEvery { updater.getFlowList(100) } returns flowOf(podcastDtos)
            coEvery { remoteUpdater.create(query) } returns updater

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
                updater.getFlowList(100)
            }
        }
}