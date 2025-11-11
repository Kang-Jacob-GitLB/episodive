package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.domain.repository.PodcastRepository
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
    private val remoteUpdater = mockk<PodcastRemoteUpdater.Factory>(relaxed = true)

    private val repository: PodcastRepository = PodcastRepositoryImpl(
        localDataSource = localDataSource,
        remoteDataSource = remoteDataSource,
        remoteUpdater = remoteUpdater,
    )

    private val podcastEntities = podcastTestDataList.toPodcastEntities("test_key")

    @After
    fun teardown() {
        confirmVerified(localDataSource, remoteDataSource, remoteUpdater)
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
            } returns flowOf(podcastEntities)

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
            } returns flowOf(podcastEntities.first())

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
            } returns flowOf(podcastEntities)

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
            coEvery { localDataSource.getPodcasts() } returns flowOf(podcastEntities)
            coEvery { localDataSource.getFollowedPodcasts() } returns flowOf(
                listOf(
                    FollowedPodcastEntity(
                        id = 5778530,
                        followedAt = Instant.fromEpochSeconds(1757568578),
                        isNotificationEnabled = true,
                    ),
                    FollowedPodcastEntity(
                        id = 391008,
                        followedAt = Instant.fromEpochSeconds(1698800122),
                        isNotificationEnabled = true,
                    ),
                )
            )

            // When
            repository.getFollowedPodcasts().test {
                val result = awaitItem()
                // Then
                assertEquals(2, result.size)
                assertEquals(podcastTestDataList[0].id, result[0].id)
                assertEquals(podcastTestDataList[1].id, result[1].id)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getPodcasts()
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
}