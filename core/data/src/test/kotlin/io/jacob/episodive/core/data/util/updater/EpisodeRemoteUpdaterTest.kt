package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingConfig
import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSource
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.SoundbiteRemoteDataSource
import io.jacob.episodive.core.network.model.EpisodeResponse
import io.jacob.episodive.core.network.model.SoundbiteResponse
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

    private val episodeLocal = mockk<EpisodeLocalDataSource>(relaxed = true)
    private val episodeRemote = mockk<EpisodeRemoteDataSource>(relaxed = true)
    private val soundbiteLocal = mockk<SoundbiteLocalDataSource>(relaxed = true)
    private val soundbiteRemote = mockk<SoundbiteRemoteDataSource>(relaxed = true)

    @After
    fun teardown() {
        confirmVerified(
            episodeLocal,
            episodeRemote,
            soundbiteLocal,
            soundbiteRemote,
        )
    }

    @Test
    fun `Given dependencies, When person query, Then call dataSource's functions`() =
        runTest {
            // Given
            val person = "John Doe"
            val query = EpisodeQuery.Person(person)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.searchEpisodesByPerson(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                episodeLocal.getEpisodesByGroupKey(any(), 10)
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.searchEpisodesByPerson(person, 1000)
                episodeLocal.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When person query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val person = "John Doe"
            val query = EpisodeQuery.Person(person)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.searchEpisodesByPerson(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
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
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.searchEpisodesByPerson(person, 1000)
                episodeLocal.replaceEpisodes(any(), any())
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When feedId query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedId = 123L
            val query = EpisodeQuery.FeedId(feedId)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getEpisodesByFeedId(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                episodeLocal.getEpisodesByGroupKey(any(), 10)
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getEpisodesByFeedId(feedId, 1000)
                episodeLocal.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When feedId query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedId = 123L
            val query = EpisodeQuery.FeedId(feedId)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getEpisodesByFeedId(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
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
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getEpisodesByFeedId(feedId, 1000)
                episodeLocal.replaceEpisodes(any(), any())
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When feedUrl query, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedUrl = "https://example.com/feed.xml"
            val query = EpisodeQuery.FeedUrl(feedUrl)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getEpisodesByFeedUrl(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                episodeLocal.getEpisodesByGroupKey(any(), 10)
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getEpisodesByFeedUrl(feedUrl, 1000)
                episodeLocal.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When feedUrl query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val feedUrl = "https://example.com/feed.xml"
            val query = EpisodeQuery.FeedUrl(feedUrl)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getEpisodesByFeedUrl(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
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
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getEpisodesByFeedUrl(feedUrl, 1000)
                episodeLocal.replaceEpisodes(any(), any())
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When podcastGuid query, Then call dataSource's functions`() =
        runTest {
            // Given
            val podcastGuid = "test-guid"
            val query = EpisodeQuery.PodcastGuid(podcastGuid)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getEpisodesByPodcastGuid(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                episodeLocal.getEpisodesByGroupKey(any(), 10)
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getEpisodesByPodcastGuid(podcastGuid, 1000)
                episodeLocal.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When podcastGuid query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val podcastGuid = "test-guid"
            val query = EpisodeQuery.PodcastGuid(podcastGuid)
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getEpisodesByPodcastGuid(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
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
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getEpisodesByPodcastGuid(podcastGuid, 1000)
                episodeLocal.replaceEpisodes(any(), any())
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When live query, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Live
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getLiveEpisodes(any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                episodeLocal.getEpisodesByGroupKey(any(), 10)
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getLiveEpisodes(max = 6)
                episodeLocal.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When live query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Live
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getLiveEpisodes(any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
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
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getLiveEpisodes(max = 6)
                episodeLocal.replaceEpisodes(any(), any())
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When random query, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Random()
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getRandomEpisodes(any(), any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                episodeLocal.getEpisodesByGroupKey(any(), 10)
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getRandomEpisodes(max = 6, any(), any())
                episodeLocal.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When random query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Random()
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getRandomEpisodes(any(), any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
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
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getRandomEpisodes(max = 6, any(), any())
                episodeLocal.replaceEpisodes(any(), any())
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When recent query, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getRecentEpisodes(any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
            } just Runs

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                episodeLocal.getEpisodesByGroupKey(any(), 10)
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getRecentEpisodes(max = 6)
                episodeLocal.replaceEpisodes(any(), any())
            }
        }

    @Test
    fun `Given dependencies, When recent query paging, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Recent
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getRecentEpisodes(any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
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
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                episodeRemote.getRecentEpisodes(max = 6)
                episodeLocal.replaceEpisodes(any(), any())
                episodeLocal.getEpisodesByGroupKeyPaging(any())
            }
        }

    @Test
    fun `Given dependencies, When soundbite query, Then call dataSource's functions`() =
        runTest {
            // Given
            val query = EpisodeQuery.Soundbite
            val updater = EpisodeRemoteUpdater(
                episodeLocal = episodeLocal,
                episodeRemote = episodeRemote,
                soundbiteLocal = soundbiteLocal,
                soundbiteRemote = soundbiteRemote,
                query = query,
            )
            coEvery {
                episodeLocal.getEpisodesByGroupKey(any(), any())
            } returns mockk(relaxed = true)
            coEvery {
                episodeLocal.getOldestCreatedAtByGroupKey(any())
            } returns null
            coEvery {
                episodeRemote.getEpisodeById(any())
            } returns mockk<EpisodeResponse>(relaxed = true)
            coEvery {
                episodeLocal.replaceEpisodes(any(), any())
            } just Runs
            coEvery {
                soundbiteLocal.replaceSoundbites(any())
            } just Runs
            coEvery {
                soundbiteRemote.getSoundbites(any())
            } returns listOf(mockk<SoundbiteResponse>(relaxed = true))

            // When
            updater.getFlowList(count = 10).test {
                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerifySequence {
                episodeLocal.getEpisodesByGroupKey(any(), 10)
                episodeLocal.getOldestCreatedAtByGroupKey(any())
                soundbiteRemote.getSoundbites(max = 100)
                soundbiteLocal.replaceSoundbites(any())
                episodeRemote.getEpisodeById(any())
                episodeLocal.replaceEpisodes(any(), any())
            }
        }
}