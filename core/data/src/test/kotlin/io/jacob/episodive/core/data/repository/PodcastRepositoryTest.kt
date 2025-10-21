package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastEntities
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
            coEvery {
                localDataSource.getFollowedPodcasts()
            } returns flowOf(mockk(relaxed = true))

            // When
            repository.getFollowedPodcasts().test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerify { localDataSource.getFollowedPodcasts() }
        }

    @Test
    fun `Given podcast is followed, When toggleFollowed is called, Then call removeFollowed`() =
        runTest {
            // Given
            val podcastId = 12345L
            coEvery { localDataSource.isFollowed(any()) } returns flowOf(true)
            coEvery { localDataSource.removeFollowed(any()) } returns Unit

            // When
            val result = repository.toggleFollowed(podcastId)

            // Then
            assertFalse(result)
            coVerifySequence {
                localDataSource.isFollowed(podcastId)
                localDataSource.removeFollowed(podcastId)
            }
        }

    @Test
    fun `Given podcast is not followed, When toggleFollowed is called, Then call addFollowed`() =
        runTest {
            // Given
            val podcastId = 12345L
            coEvery { localDataSource.isFollowed(any()) } returns flowOf(false)
            coEvery { localDataSource.addFollowed(any()) } returns Unit

            // When
            val result = repository.toggleFollowed(podcastId)

            // Then
            assertTrue(result)
            coVerifySequence {
                localDataSource.isFollowed(podcastId)
                localDataSource.addFollowed(match { it.id == podcastId })
            }
        }

    @Test
    fun `Given ids, When addFolloweds is called, Then call addFolloweds of localDataSource`() =
        runTest {
            // Given
            val ids = listOf(1L, 2L, 3L)
            coEvery { localDataSource.addFolloweds(any()) } returns Unit

            // When
            val result = repository.addFolloweds(ids)

            // Then
            assertTrue(result)
            coVerify { localDataSource.addFolloweds(match { it.size == ids.size }) }
        }
}