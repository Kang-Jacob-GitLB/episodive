package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingConfig
import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Medium
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.model.PodcastResponse
import io.jacob.episodive.core.network.model.RecentFeedResponse
import io.jacob.episodive.core.network.model.RecentNewFeedResponse
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PodcastRemoteUpdaterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastLocal = mockk<PodcastLocalDataSource>(relaxed = true)
    private val podcastRemote = mockk<PodcastRemoteDataSource>(relaxed = true)
    private val feedLocal = mockk<FeedLocalDataSource>(relaxed = true)
    private val feedRemote = mockk<FeedRemoteDataSource>(relaxed = true)

    @After
    fun teardown() {
        confirmVerified(
            podcastLocal,
            podcastRemote,
            feedLocal,
            feedRemote,
        )
    }

    @Test
    fun `Given dependencies, When search query, Then call dataSource's functions`() =
        runTest {
            // Given
            val search = "test podcast"
            val query = PodcastQuery.Search(search)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.searchPodcasts(any(), any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getPodcastsByGroupKey("search:test podcast", 10)
                podcastLocal.getOldestCreatedAtByGroupKey("search:test podcast")
                podcastRemote.searchPodcasts(search, 1000)
                podcastLocal.replacePodcasts(any(), "search:test podcast")
            }
        }

    @Test
    fun `Given dependencies, When search query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val search = "test podcast"
            val query = PodcastQuery.Search(search)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.searchPodcasts(any(), any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getOldestCreatedAtByGroupKey("search:test podcast")
                podcastRemote.searchPodcasts(search, 1000)
                podcastLocal.replacePodcasts(any(), "search:test podcast")
                podcastLocal.getPodcastsByGroupKeyPaging("search:test podcast")
            }
        }

    @Test
    fun `Given dependencies, When feedId query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedId = 123L
            val query = PodcastQuery.FeedId(feedId)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getPodcastsByGroupKey("feedId:123", 10)
                podcastLocal.getOldestCreatedAtByGroupKey("feedId:123")
                podcastRemote.getPodcastByFeedId(any())
                podcastLocal.replacePodcasts(any(), "feedId:123")
            }
        }

    @Test
    fun `Given dependencies, When feedId query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedId = 123L
            val query = PodcastQuery.FeedId(feedId)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getOldestCreatedAtByGroupKey("feedId:123")
                podcastRemote.getPodcastByFeedId(any())
                podcastLocal.replacePodcasts(any(), "feedId:123")
                podcastLocal.getPodcastsByGroupKeyPaging("feedId:123")
            }
        }

    @Test
    fun `Given dependencies, When feedUrl query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedUrl = "test-url"
            val query = PodcastQuery.FeedUrl(feedUrl)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastByFeedUrl(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getPodcastsByGroupKey("feedUrl:test-url", 10)
                podcastLocal.getOldestCreatedAtByGroupKey("feedUrl:test-url")
                podcastRemote.getPodcastByFeedUrl(any())
                podcastLocal.replacePodcasts(any(), "feedUrl:test-url")
            }
        }

    @Test
    fun `Given dependencies, When feedUrl query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedUrl = "test-url"
            val query = PodcastQuery.FeedUrl(feedUrl)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastByFeedUrl(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getOldestCreatedAtByGroupKey("feedUrl:test-url")
                podcastRemote.getPodcastByFeedUrl(any())
                podcastLocal.replacePodcasts(any(), "feedUrl:test-url")
                podcastLocal.getPodcastsByGroupKeyPaging("feedUrl:test-url")
            }
        }

    @Test
    fun `Given dependencies, When feedGuid query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedGuid = "test-guid"
            val query = PodcastQuery.FeedGuid(feedGuid)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastByGuid(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getPodcastsByGroupKey("feedGuid:test-guid", 10)
                podcastLocal.getOldestCreatedAtByGroupKey("feedGuid:test-guid")
                podcastRemote.getPodcastByGuid(any())
                podcastLocal.replacePodcasts(any(), "feedGuid:test-guid")
            }
        }

    @Test
    fun `Given dependencies, When feedGuid query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedGuid = "test-guid"
            val query = PodcastQuery.FeedGuid(feedGuid)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastByGuid(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getOldestCreatedAtByGroupKey("feedGuid:test-guid")
                podcastRemote.getPodcastByGuid(any())
                podcastLocal.replacePodcasts(any(), "feedGuid:test-guid")
                podcastLocal.getPodcastsByGroupKeyPaging("feedGuid:test-guid")
            }
        }

    @Test
    fun `Given dependencies, When medium query, Then call dataSource's functions`() =
        runTest {
            // Given
            val medium = Medium.PODCAST.value
            val query = PodcastQuery.Medium(medium)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastsByMedium(any(), any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getPodcastsByGroupKey("medium:podcast", 10)
                podcastLocal.getOldestCreatedAtByGroupKey("medium:podcast")
                podcastRemote.getPodcastsByMedium(any(), 1000)
                podcastLocal.replacePodcasts(any(), "medium:podcast")
            }
        }

    @Test
    fun `Given dependencies, When medium query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val medium = Medium.PODCAST.value
            val query = PodcastQuery.Medium(medium)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastsByMedium(any(), any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getOldestCreatedAtByGroupKey("medium:podcast")
                podcastRemote.getPodcastsByMedium(any(), 1000)
                podcastLocal.replacePodcasts(any(), "medium:podcast")
                podcastLocal.getPodcastsByGroupKeyPaging("medium:podcast")
            }
        }

    @Test
    fun `Given dependencies, When channel query, Then call dataSource's functions`() =
        runTest {
            // Given
            val channel = Channel(
                id = 1,
                title = "test",
                description = "test",
                image = "test",
                link = "test",
                count = 1,
                podcastGuids = listOf("test")
            )
            val query = PodcastQuery.ByChannel(channel)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastsByGuids(any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getPodcastsByGroupKey("channel:1", 10)
                podcastLocal.getOldestCreatedAtByGroupKey("channel:1")
                podcastRemote.getPodcastsByGuids(any())
                podcastLocal.replacePodcasts(any(), "channel:1")
            }
        }

    @Test
    fun `Given dependencies, When channel query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val channel = Channel(
                id = 1,
                title = "test",
                description = "test",
                image = "test",
                link = "test",
                count = 1,
                podcastGuids = listOf("test")
            )
            val query = PodcastQuery.ByChannel(channel)
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastRemote.getPodcastsByGuids(any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getOldestCreatedAtByGroupKey("channel:1")
                podcastRemote.getPodcastsByGuids(any())
                podcastLocal.replacePodcasts(any(), "channel:1")
                podcastLocal.getPodcastsByGroupKeyPaging("channel:1")
            }
        }

    @Test
    fun `Given dependencies, When trending query, Then call dataSources's functions`() =
        runTest {
            // Given
            val query = PodcastQuery.Trending(
                language = "ko",
                categories = listOf(Category.BUSINESS, Category.POLITICS),
            )
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs
            coEvery {
                feedRemote.getTrendingFeeds(any(), any(), any(), any(), any())
            } returns listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                podcastRemote.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)

            // When
            updater.getFlowList(count = 100).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getPodcastsByGroupKey("trending:ko:9,59", 100)
                podcastLocal.getOldestCreatedAtByGroupKey("trending:ko:9,59")
                feedRemote.getTrendingFeeds(
                    max = 100,
                    since = any(),
                    language = "ko",
                    includeCategories = "9,59",
                    excludeCategories = any(),
                )
                podcastRemote.getPodcastByFeedId(any())
                podcastLocal.replacePodcasts(any(), "trending:ko:9,59")
            }
        }

    @Test
    fun `Given dependencies, When trending query paging, Then call dataSources's functions`() =
        runTest {
            // Given
            val query = PodcastQuery.Trending(
                language = "ko",
                categories = listOf(Category.BUSINESS, Category.POLITICS),
            )
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs
            coEvery {
                feedRemote.getTrendingFeeds(any(), any(), any(), any(), any())
            } returns listOf(mockk<TrendingFeedResponse>(relaxed = true))
            coEvery {
                podcastRemote.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getOldestCreatedAtByGroupKey("trending:ko:9,59")
                feedRemote.getTrendingFeeds(
                    max = 100,
                    since = any(),
                    language = "ko",
                    includeCategories = "9,59",
                    excludeCategories = any(),
                )
                podcastRemote.getPodcastByFeedId(any())
                podcastLocal.replacePodcasts(any(), "trending:ko:9,59")
                podcastLocal.getPodcastsByGroupKeyPaging("trending:ko:9,59")
            }
        }

    @Test
    fun `Given dependencies, When recent query, Then call dataSources's functions`() =
        runTest {
            // Given
            val query = PodcastQuery.Recent(
                language = "ko",
                categories = listOf(Category.BUSINESS, Category.POLITICS),
            )
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs
            coEvery {
                feedRemote.getRecentFeeds(any(), any(), any(), any(), any())
            } returns listOf(mockk<RecentFeedResponse>(relaxed = true))
            coEvery {
                podcastRemote.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)

            // When
            updater.getFlowList(count = 100).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getPodcastsByGroupKey("recent:ko:9,59", 100)
                podcastLocal.getOldestCreatedAtByGroupKey("recent:ko:9,59")
                feedRemote.getRecentFeeds(
                    max = 100,
                    since = any(),
                    language = "ko",
                    includeCategories = "9,59",
                    excludeCategories = any(),
                )
                podcastRemote.getPodcastByFeedId(any())
                podcastLocal.replacePodcasts(any(), "recent:ko:9,59")
            }
        }

    @Test
    fun `Given dependencies, When recent query paging, Then call dataSources's functions`() =
        runTest {
            // Given
            val query = PodcastQuery.Recent(
                language = "ko",
                categories = listOf(Category.BUSINESS, Category.POLITICS),
            )
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs
            coEvery {
                feedRemote.getRecentFeeds(any(), any(), any(), any(), any())
            } returns listOf(mockk<RecentFeedResponse>(relaxed = true))
            coEvery {
                podcastRemote.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getOldestCreatedAtByGroupKey("recent:ko:9,59")
                feedRemote.getRecentFeeds(
                    max = 100,
                    since = any(),
                    language = "ko",
                    includeCategories = "9,59",
                    excludeCategories = any(),
                )
                podcastRemote.getPodcastByFeedId(any())
                podcastLocal.replacePodcasts(any(), "recent:ko:9,59")
                podcastLocal.getPodcastsByGroupKeyPaging("recent:ko:9,59")
            }
        }

    @Test
    fun `Given dependencies, When recent new query, Then call dataSources's functions`() =
        runTest {
            // Given
            val query = PodcastQuery.RecentNew
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs
            coEvery {
                feedRemote.getRecentNewFeeds(any(), any())
            } returns listOf(mockk<RecentNewFeedResponse>(relaxed = true))
            coEvery {
                podcastRemote.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)

            // When
            updater.getFlowList(count = 100).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getPodcastsByGroupKey("recent_new", 100)
                podcastLocal.getOldestCreatedAtByGroupKey("recent_new")
                feedRemote.getRecentNewFeeds(
                    max = 100,
                    since = any(),
                )
                podcastRemote.getPodcastByFeedId(any())
                podcastLocal.replacePodcasts(any(), "recent_new")
            }
        }

    @Test
    fun `Given dependencies, When recent new query paging, Then call dataSources's functions`() =
        runTest {
            // Given
            val query = PodcastQuery.RecentNew
            val updater = PodcastRemoteUpdater(
                podcastLocal = podcastLocal,
                podcastRemote = podcastRemote,
                feedLocal = feedLocal,
                feedRemote = feedRemote,
                query = query,
            )
            coEvery {
                podcastLocal.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                podcastLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                podcastLocal.replacePodcasts(any(), any())
            } just Runs
            coEvery {
                feedRemote.getRecentNewFeeds(any(), any())
            } returns listOf(mockk<RecentNewFeedResponse>(relaxed = true))
            coEvery {
                podcastRemote.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)

            // When
            updater.getPagingData(
                pagingConfig = PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    prefetchDistance = 5,
                )
            ).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                podcastLocal.getOldestCreatedAtByGroupKey("recent_new")
                feedRemote.getRecentNewFeeds(
                    max = 100,
                    since = any(),
                )
                podcastRemote.getPodcastByFeedId(any())
                podcastLocal.replacePodcasts(any(), "recent_new")
                podcastLocal.getPodcastsByGroupKeyPaging("recent_new")
            }
        }
}