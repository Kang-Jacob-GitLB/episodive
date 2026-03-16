package io.jacob.episodive.core.data.util.paging

import androidx.paging.PagingSource
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.model.FeedEntity
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.GroupKey
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.model.RecentFeedResponse
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.jacob.episodive.core.testing.util.loadPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class RecommendedPodcastPagingSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invalidationTracker = mockk<InvalidationTracker>(relaxed = true)
    private val database = mockk<RoomDatabase>(relaxed = true) {
        every { invalidationTracker } returns this@RecommendedPodcastPagingSourceTest.invalidationTracker
    }
    private val podcastLocal = mockk<PodcastLocalDataSource>(relaxed = true)
    private val podcastRemote = mockk<PodcastRemoteDataSource>(relaxed = true)
    private val feedLocal = mockk<FeedLocalDataSource>(relaxed = true)
    private val feedRemote = mockk<FeedRemoteDataSource>(relaxed = true)

    private val defaultGroupKey = "${GroupKey.RECOMMENDED}:all:"

    private fun createPagingSource(
        language: String? = null,
        categories: List<Category> = emptyList(),
        timeToLive: Duration = 10.minutes,
    ) = RecommendedPodcastPagingSource(
        database = database,
        podcastLocal = podcastLocal,
        podcastRemote = podcastRemote,
        feedLocal = feedLocal,
        feedRemote = feedRemote,
        language = language,
        categories = categories,
        timeToLive = timeToLive,
    )

    private fun mockPodcastWithExtrasView(id: Long): PodcastWithExtrasView =
        mockk(relaxed = true) {
            every { podcast } returns mockk(relaxed = true) {
                every { this@mockk.id } returns id
            }
        }

    private fun mockFeedEntity(id: Long): FeedEntity =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
        }

    @Test
    fun `Given fresh cache with all podcasts cached, When load, Then returns podcasts in feed order`() =
        runTest {
            // Given
            val feeds = listOf(
                mockFeedEntity(100L),
                mockFeedEntity(200L),
                mockFeedEntity(300L),
            )
            val podcasts = listOf(
                mockPodcastWithExtrasView(100L),
                mockPodcastWithExtrasView(200L),
                mockPodcastWithExtrasView(300L),
            )
            coEvery { feedLocal.getFeedsOldestCachedAt(defaultGroupKey) } returns Clock.System.now()
            coEvery {
                feedLocal.getFeedsPagingList(defaultGroupKey, 0, 10)
            } returns feeds
            coEvery {
                podcastLocal.getPodcastsByIdsOnce(listOf(100L, 200L, 300L))
            } returns podcasts

            // When
            val page = createPagingSource().loadPage(loadSize = 10)

            // Then
            assertEquals(3, page.data.size)
            assertNull(page.prevKey)
            assertEquals(100L, page.data[0].podcast.id)
            assertEquals(200L, page.data[1].podcast.id)
            assertEquals(300L, page.data[2].podcast.id)
        }

    @Test
    fun `Given expired cache, When load, Then fetches trending and recent in parallel`() =
        runTest {
            // Given
            coEvery { feedLocal.getFeedsOldestCachedAt(defaultGroupKey) } returns null
            coEvery {
                feedRemote.getTrendingFeeds(any(), any(), any(), any(), any())
            } returns listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                feedRemote.getRecentFeeds(any(), any(), any(), any(), any())
            } returns listOf(mockk<RecentFeedResponse>(relaxed = true))
            coEvery {
                feedLocal.getFeedsPagingList(defaultGroupKey, any(), any())
            } returns emptyList()

            // When
            val page = createPagingSource().loadPage(loadSize = 10)

            // Then
            assertEquals(0, page.data.size)
            coVerify { feedRemote.getTrendingFeeds(any(), any(), any(), any(), any()) }
            coVerify { feedRemote.getRecentFeeds(any(), any(), any(), any(), any()) }
            coVerify { feedLocal.replaceFeedsByGroupKey(any(), defaultGroupKey) }
            coVerify { podcastLocal.replacePodcasts(emptyList(), defaultGroupKey) }
        }

    @Test
    fun `Given missing podcasts, When load, Then fetches from remote and upserts`() =
        runTest {
            // Given
            val feeds = listOf(
                mockFeedEntity(100L),
                mockFeedEntity(200L),
                mockFeedEntity(300L),
            )
            val cachedPodcast = mockPodcastWithExtrasView(100L)
            val allPodcasts = listOf(
                mockPodcastWithExtrasView(100L),
                mockPodcastWithExtrasView(200L),
                mockPodcastWithExtrasView(300L),
            )

            coEvery { feedLocal.getFeedsOldestCachedAt(defaultGroupKey) } returns Clock.System.now()
            coEvery {
                feedLocal.getFeedsPagingList(defaultGroupKey, 0, 10)
            } returns feeds
            // First call returns only 1 cached, second returns all
            coEvery {
                podcastLocal.getPodcastsByIdsOnce(listOf(100L, 200L, 300L))
            } returns listOf(cachedPodcast) andThen allPodcasts
            coEvery { podcastRemote.getPodcastByFeedId(any()) } returns mockk(relaxed = true)

            // When
            val page = createPagingSource().loadPage(loadSize = 10)

            // Then
            assertEquals(3, page.data.size)
            coVerify { podcastRemote.getPodcastByFeedId(200L) }
            coVerify { podcastRemote.getPodcastByFeedId(300L) }
            coVerify {
                podcastLocal.upsertPodcastsWithGroup(any(), defaultGroupKey)
            }
        }

    @Test
    fun `Given no feeds, When load, Then returns empty page`() =
        runTest {
            // Given
            coEvery { feedLocal.getFeedsOldestCachedAt(defaultGroupKey) } returns Clock.System.now()
            coEvery {
                feedLocal.getFeedsPagingList(defaultGroupKey, 0, 10)
            } returns emptyList()

            // When
            val page = createPagingSource().loadPage(loadSize = 10)

            // Then
            assertEquals(0, page.data.size)
            assertNull(page.prevKey)
            assertNull(page.nextKey)
        }

    @Test
    fun `Given first page full results, When load, Then nextKey is offset plus limit`() =
        runTest {
            // Given
            val feeds = (1L..5L).map { mockFeedEntity(it) }
            val podcasts = (1L..5L).map { mockPodcastWithExtrasView(it) }

            coEvery { feedLocal.getFeedsOldestCachedAt(defaultGroupKey) } returns Clock.System.now()
            coEvery {
                feedLocal.getFeedsPagingList(defaultGroupKey, 0, 5)
            } returns feeds
            coEvery { podcastLocal.getPodcastsByIdsOnce(any()) } returns podcasts

            // When
            val page = createPagingSource().loadPage(loadSize = 5)

            // Then
            assertEquals(5, page.data.size)
            assertNull(page.prevKey)
            assertEquals(5, page.nextKey)
        }

    @Test
    fun `Given last page partial, When load, Then nextKey null`() =
        runTest {
            // Given
            val feeds = (1L..2L).map { mockFeedEntity(it) }
            val podcasts = (1L..2L).map { mockPodcastWithExtrasView(it) }

            coEvery { feedLocal.getFeedsOldestCachedAt(defaultGroupKey) } returns Clock.System.now()
            coEvery {
                feedLocal.getFeedsPagingList(defaultGroupKey, 10, 5)
            } returns feeds
            coEvery { podcastLocal.getPodcastsByIdsOnce(any()) } returns podcasts

            // When
            val page = createPagingSource().loadPage(key = 10, loadSize = 5)

            // Then
            assertEquals(2, page.data.size)
            assertEquals(5, page.prevKey)
            assertNull(page.nextKey)
        }

    @Test
    fun `Given exception during load, When load, Then returns LoadResult Error`() =
        runTest {
            // Given
            coEvery {
                feedLocal.getFeedsOldestCachedAt(any())
            } throws RuntimeException("test error")

            // When
            val result = createPagingSource().load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )

            // Then
            assertTrue(result is PagingSource.LoadResult.Error)
            val error = result as PagingSource.LoadResult.Error
            assertEquals("test error", error.throwable.message)
        }

    @Test
    fun `Given language and categories, When load, Then uses correct groupKey`() =
        runTest {
            // Given
            val groupKey = "${GroupKey.RECOMMENDED}:en:9"
            coEvery { feedLocal.getFeedsOldestCachedAt(groupKey) } returns Clock.System.now()
            coEvery {
                feedLocal.getFeedsPagingList(groupKey, 0, 10)
            } returns emptyList()

            // When
            val page = createPagingSource(
                language = "en",
                categories = listOf(Category.BUSINESS),
            ).loadPage(loadSize = 10)

            // Then
            assertEquals(0, page.data.size)
            coVerify { feedLocal.getFeedsOldestCachedAt(groupKey) }
            coVerify { feedLocal.getFeedsPagingList(groupKey, 0, 10) }
        }
}
