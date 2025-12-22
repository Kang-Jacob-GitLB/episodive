package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingConfig
import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Medium
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.model.PodcastResponse
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

    private val localDataSource = mockk<PodcastLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<PodcastRemoteDataSource>(relaxed = true)

    @After
    fun teardown() {
        confirmVerified(localDataSource, remoteDataSource)
    }

    @Test
    fun `Given dependencies, When search query, Then call dataSource's functions`() =
        runTest {
            // Given
            val search = "test podcast"
            val query = PodcastQuery.Search(search)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.searchPodcasts(any(), any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                localDataSource.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getPodcastsByGroupKey("search:test podcast", 10)
                localDataSource.getOldestCreatedAtByGroupKey("search:test podcast")
                remoteDataSource.searchPodcasts(search, 1000)
                localDataSource.replacePodcasts(any(), "search:test podcast")
            }
        }

    @Test
    fun `Given dependencies, When search query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val search = "test podcast"
            val query = PodcastQuery.Search(search)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.searchPodcasts(any(), any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                localDataSource.replacePodcasts(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey("search:test podcast")
                remoteDataSource.searchPodcasts(search, 1000)
                localDataSource.replacePodcasts(any(), "search:test podcast")
                localDataSource.getPodcastsByGroupKeyPaging("search:test podcast")
            }
        }

    @Test
    fun `Given dependencies, When feedId query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedId = 123L
            val query = PodcastQuery.FeedId(feedId)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                localDataSource.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getPodcastsByGroupKey("feedId:123", 10)
                localDataSource.getOldestCreatedAtByGroupKey("feedId:123")
                remoteDataSource.getPodcastByFeedId(any())
                localDataSource.replacePodcasts(any(), "feedId:123")
            }
        }

    @Test
    fun `Given dependencies, When feedId query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedId = 123L
            val query = PodcastQuery.FeedId(feedId)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastByFeedId(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                localDataSource.replacePodcasts(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey("feedId:123")
                remoteDataSource.getPodcastByFeedId(any())
                localDataSource.replacePodcasts(any(), "feedId:123")
                localDataSource.getPodcastsByGroupKeyPaging("feedId:123")
            }
        }

    @Test
    fun `Given dependencies, When feedUrl query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedUrl = "test-url"
            val query = PodcastQuery.FeedUrl(feedUrl)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastByFeedUrl(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                localDataSource.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getPodcastsByGroupKey("feedUrl:test-url", 10)
                localDataSource.getOldestCreatedAtByGroupKey("feedUrl:test-url")
                remoteDataSource.getPodcastByFeedUrl(any())
                localDataSource.replacePodcasts(any(), "feedUrl:test-url")
            }
        }

    @Test
    fun `Given dependencies, When feedUrl query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedUrl = "test-url"
            val query = PodcastQuery.FeedUrl(feedUrl)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastByFeedUrl(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                localDataSource.replacePodcasts(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey("feedUrl:test-url")
                remoteDataSource.getPodcastByFeedUrl(any())
                localDataSource.replacePodcasts(any(), "feedUrl:test-url")
                localDataSource.getPodcastsByGroupKeyPaging("feedUrl:test-url")
            }
        }

    @Test
    fun `Given dependencies, When feedGuid query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedGuid = "test-guid"
            val query = PodcastQuery.FeedGuid(feedGuid)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastByGuid(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                localDataSource.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getPodcastsByGroupKey("feedGuid:test-guid", 10)
                localDataSource.getOldestCreatedAtByGroupKey("feedGuid:test-guid")
                remoteDataSource.getPodcastByGuid(any())
                localDataSource.replacePodcasts(any(), "feedGuid:test-guid")
            }
        }

    @Test
    fun `Given dependencies, When feedGuid query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedGuid = "test-guid"
            val query = PodcastQuery.FeedGuid(feedGuid)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastByGuid(any())
            } returns mockk<PodcastResponse>(relaxed = true)
            coEvery {
                localDataSource.replacePodcasts(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey("feedGuid:test-guid")
                remoteDataSource.getPodcastByGuid(any())
                localDataSource.replacePodcasts(any(), "feedGuid:test-guid")
                localDataSource.getPodcastsByGroupKeyPaging("feedGuid:test-guid")
            }
        }

    @Test
    fun `Given dependencies, When medium query, Then call dataSource's functions`() =
        runTest {
            // Given
            val medium = Medium.PODCAST.value
            val query = PodcastQuery.Medium(medium)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastsByMedium(any(), any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                localDataSource.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getPodcastsByGroupKey("medium:podcast", 10)
                localDataSource.getOldestCreatedAtByGroupKey("medium:podcast")
                remoteDataSource.getPodcastsByMedium(any(), 1000)
                localDataSource.replacePodcasts(any(), "medium:podcast")
            }
        }

    @Test
    fun `Given dependencies, When medium query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val medium = Medium.PODCAST.value
            val query = PodcastQuery.Medium(medium)
            val updater = PodcastRemoteUpdater(
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastsByMedium(any(), any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                localDataSource.replacePodcasts(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey("medium:podcast")
                remoteDataSource.getPodcastsByMedium(any(), 1000)
                localDataSource.replacePodcasts(any(), "medium:podcast")
                localDataSource.getPodcastsByGroupKeyPaging("medium:podcast")
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
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastsByGuids(any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                localDataSource.replacePodcasts(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getPodcastsByGroupKey("channel:1", 10)
                localDataSource.getOldestCreatedAtByGroupKey("channel:1")
                remoteDataSource.getPodcastsByGuids(any())
                localDataSource.replacePodcasts(any(), "channel:1")
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
                podcastLocal = localDataSource,
                podcastRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getPodcastsByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getPodcastsByGuids(any())
            } returns listOf(mockk<PodcastResponse>(relaxed = true))
            coEvery {
                localDataSource.replacePodcasts(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey("channel:1")
                remoteDataSource.getPodcastsByGuids(any())
                localDataSource.replacePodcasts(any(), "channel:1")
                localDataSource.getPodcastsByGroupKeyPaging("channel:1")
            }
        }
}