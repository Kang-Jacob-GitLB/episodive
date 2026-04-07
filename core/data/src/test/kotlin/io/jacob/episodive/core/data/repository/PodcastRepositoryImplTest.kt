package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.model.PodcastResponse
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PodcastRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastLocalDataSource = mockk<PodcastLocalDataSource>(relaxed = true)
    private val podcastRemoteDataSource = mockk<PodcastRemoteDataSource>(relaxed = true)
    private val feedLocalDataSource = mockk<FeedLocalDataSource>(relaxed = true)
    private val feedRemoteDataSource = mockk<FeedRemoteDataSource>(relaxed = true)
    private val remoteUpdater = mockk<PodcastRemoteUpdater.Factory>(relaxed = true)

    private val repository = PodcastRepositoryImpl(
        podcastLocalDataSource = podcastLocalDataSource,
        podcastRemoteDataSource = podcastRemoteDataSource,
        feedLocalDataSource = feedLocalDataSource,
        feedRemoteDataSource = feedRemoteDataSource,
        remoteUpdater = remoteUpdater,
    )

    @Test
    fun `When getFollowedPodcastIdsWithNotificationEnabled, Then delegates to localDataSource`() =
        runTest {
            // Given
            val expected = listOf(1L, 2L, 3L)
            coEvery { podcastLocalDataSource.getFollowedPodcastIdsWithNotificationEnabled() } returns expected

            // When
            val result = repository.getFollowedPodcastIdsWithNotificationEnabled()

            // Then
            assertEquals(expected, result)
            coVerify { podcastLocalDataSource.getFollowedPodcastIdsWithNotificationEnabled() }
        }

    @Test
    fun `When toggleFollowed, Then delegates to localDataSource toggleFollowedPodcast`() =
        runTest {
            // Given
            val id = 5778530L
            coEvery { podcastLocalDataSource.toggleFollowedPodcast(id) } returns true

            // When
            val result = repository.toggleFollowed(id)

            // Then
            assertEquals(true, result)
            coVerify { podcastLocalDataSource.toggleFollowedPodcast(id) }
        }

    @Test
    fun `Given feedId, When getPodcastByFeedId, Then creates remoteUpdater with FeedId query`() =
        runTest {
            // Given
            val feedId = 5778530L
            val mockUpdater = mockk<io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater>(relaxed = true)
            val entityView = mockk<PodcastWithExtrasView>(relaxed = true)
            coEvery { remoteUpdater.create(any()) } returns mockUpdater
            every { mockUpdater.getFlowList(any()) } returns flowOf(listOf(entityView))

            // When / Then
            repository.getPodcastByFeedId(feedId).test {
                val result = awaitItem()
                assertTrue(result is Podcast)
                awaitComplete()
            }
            coVerify { remoteUpdater.create(any()) }
        }

    @Test
    fun `Given query, When searchPodcasts, Then delegates to podcastRemoteDataSource`() = runTest {
        // Given
        val query = "kotlin"
        val max = 10
        coEvery {
            podcastRemoteDataSource.searchPodcasts(query = query, max = max)
        } returns listOf(mockk<PodcastResponse>(relaxed = true))

        // When
        var result: List<Podcast>? = null
        repository.searchPodcasts(query = query, max = max).collect { result = it }

        // Then
        assertTrue(result != null)
        coVerify { podcastRemoteDataSource.searchPodcasts(query = query, max = max) }
    }

    @Test
    fun `Given query, When getFollowedPodcasts, Then delegates to podcastLocalDataSource`() =
        runTest {
            // Given
            val query = "test"
            val max = 5
            val entityView = mockk<PodcastWithExtrasView>(relaxed = true)
            every {
                podcastLocalDataSource.getFollowedPodcasts(query, max)
            } returns flowOf(listOf(entityView))

            // When / Then
            repository.getFollowedPodcasts(query = query, max = max).test {
                val result = awaitItem()
                assertTrue(result.size == 1)
                awaitComplete()
            }
            coVerify { podcastLocalDataSource.getFollowedPodcasts(query, max) }
        }

    @Test
    fun `Given feedUrl, When getPodcastByFeedUrl, Then creates remoteUpdater with FeedUrl query`() =
        runTest {
            // Given
            val feedUrl = "https://example.com/feed.xml"
            val mockUpdater = mockk<io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater>(relaxed = true)
            val entityView = mockk<PodcastWithExtrasView>(relaxed = true)
            coEvery { remoteUpdater.create(any()) } returns mockUpdater
            every { mockUpdater.getFlowList(any()) } returns flowOf(listOf(entityView))

            // When / Then
            repository.getPodcastByFeedUrl(feedUrl).test {
                awaitItem() // result may be null or Podcast
                awaitComplete()
            }
            coVerify { remoteUpdater.create(any()) }
        }

    @Test
    fun `Given guid, When getPodcastByGuid, Then creates remoteUpdater with FeedGuid query`() =
        runTest {
            // Given
            val guid = "some-guid-1234"
            val mockUpdater = mockk<io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater>(relaxed = true)
            val entityView = mockk<PodcastWithExtrasView>(relaxed = true)
            coEvery { remoteUpdater.create(any()) } returns mockUpdater
            every { mockUpdater.getFlowList(any()) } returns flowOf(listOf(entityView))

            // When / Then
            repository.getPodcastByGuid(guid).test {
                awaitItem()
                awaitComplete()
            }
            coVerify { remoteUpdater.create(any()) }
        }

    @Test
    fun `Given trending params, When getTrendingPodcasts, Then creates remoteUpdater with Trending query`() =
        runTest {
            // Given
            val max = 10
            val language = "ko"
            val categories = emptyList<io.jacob.episodive.core.model.Category>()
            val mockUpdater = mockk<io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater>(relaxed = true)
            val entityView = mockk<PodcastWithExtrasView>(relaxed = true)
            coEvery { remoteUpdater.create(any()) } returns mockUpdater
            every { mockUpdater.getFlowList(max) } returns flowOf(listOf(entityView))

            // When / Then
            repository.getTrendingPodcasts(max, language, categories).test {
                val result = awaitItem()
                assertTrue(result.size == 1)
                awaitComplete()
            }
            coVerify { remoteUpdater.create(any()) }
        }
}
