package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingConfig
import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.model.EpisodeResponse
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
    fun `Given dependencies, When person query, Then call dataSource's functions`() =
        runTest {
            // Given
            val person = "John Doe"
            val query = EpisodeQuery.Person(person)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.searchEpisodesByPerson(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodesByGroupKey(any(), 10)
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.searchEpisodesByPerson(person, 1000)
                localDataSource.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When person query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val person = "John Doe"
            val query = EpisodeQuery.Person(person)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.searchEpisodesByPerson(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.searchEpisodesByPerson(person, 1000)
                localDataSource.replaceEpisodes(any(), any())
                localDataSource.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When feedId query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedId = 123L
            val query = EpisodeQuery.FeedId(feedId)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getEpisodesByFeedId(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodesByGroupKey(any(), 10)
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getEpisodesByFeedId(feedId, 1000)
                localDataSource.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When feedId query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedId = 123L
            val query = EpisodeQuery.FeedId(feedId)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getEpisodesByFeedId(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getEpisodesByFeedId(feedId, 1000)
                localDataSource.replaceEpisodes(any(), any())
                localDataSource.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When feedUrl query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedUrl = "https://example.com/feed.xml"
            val query = EpisodeQuery.FeedUrl(feedUrl)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getEpisodesByFeedUrl(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodesByGroupKey(any(), 10)
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getEpisodesByFeedUrl(feedUrl, 1000)
                localDataSource.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When feedUrl query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedUrl = "https://example.com/feed.xml"
            val query = EpisodeQuery.FeedUrl(feedUrl)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getEpisodesByFeedUrl(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getEpisodesByFeedUrl(feedUrl, 1000)
                localDataSource.replaceEpisodes(any(), any())
                localDataSource.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When podcastGuid query, Then call dataSource's functions`() =
        runTest {
            // Given
            val podcastGuid = "test-guid"
            val query = EpisodeQuery.PodcastGuid(podcastGuid)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getEpisodesByPodcastGuid(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodesByGroupKey(any(), 10)
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getEpisodesByPodcastGuid(podcastGuid, 1000)
                localDataSource.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When podcastGuid query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val podcastGuid = "test-guid"
            val query = EpisodeQuery.PodcastGuid(podcastGuid)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getEpisodesByPodcastGuid(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getEpisodesByPodcastGuid(podcastGuid, 1000)
                localDataSource.replaceEpisodes(any(), any())
                localDataSource.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When live query, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Live
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getLiveEpisodes(any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodesByGroupKey(any(), 10)
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getLiveEpisodes(max = 6)
                localDataSource.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When live query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Live
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getLiveEpisodes(any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getLiveEpisodes(max = 6)
                localDataSource.replaceEpisodes(any(), any())
                localDataSource.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When random query, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Random()
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRandomEpisodes(any(), any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodesByGroupKey(any(), 10)
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getRandomEpisodes(max = 6, any(), any())
                localDataSource.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When random query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Random()
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRandomEpisodes(any(), any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getRandomEpisodes(max = 6, any(), any())
                localDataSource.replaceEpisodes(any(), any())
                localDataSource.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When recent query, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRecentEpisodes(any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodesByGroupKey(any(), 10)
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getRecentEpisodes(max = 6)
                localDataSource.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When recent query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getRecentEpisodes(any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getRecentEpisodes(max = 6)
                localDataSource.replaceEpisodes(any(), any())
                localDataSource.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When episodeId query, Then call dataSource's functions`() =
        runTest {
            // Given
            val episodeId = 123L
            val query = EpisodeQuery.EpisodeId(episodeId)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getEpisodeById(any())
            } returns mockk<EpisodeResponse>(relaxed = true)
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
            } just Runs


            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodesByGroupKey(any(), 10)
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getEpisodeById(episodeId)
                localDataSource.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When episodeId query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val episodeId = 123L
            val query = EpisodeQuery.EpisodeId(episodeId)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = localDataSource,
                episodeRemote = remoteDataSource,
                query = query,
            )
            coEvery {
                localDataSource.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                localDataSource.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                remoteDataSource.getEpisodeById(any())
            } returns mockk<EpisodeResponse>(relaxed = true)
            coEvery {
                localDataSource.replaceEpisodes(any(), any())
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
                localDataSource.getOldestCreatedAtByGroupKey(any())
                remoteDataSource.getEpisodeById(episodeId)
                localDataSource.replaceEpisodes(any(), any())
                localDataSource.getEpisodesByGroupKeyPaging(any())
            }
        }
}