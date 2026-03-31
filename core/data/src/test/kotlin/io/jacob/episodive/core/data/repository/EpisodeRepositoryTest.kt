package io.jacob.episodive.core.data.repository

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.data.util.updater.EpisodeRemoteUpdater
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisodeEntity
import io.jacob.episodive.core.database.mapper.toEpisodeWithExtrasViews
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.DownloadStatus
import io.jacob.episodive.core.network.datasource.ChapterRemoteDataSource
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.SoundbiteRemoteDataSource
import io.jacob.episodive.core.network.model.EpisodeResponse
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class EpisodeRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val localDataSource = mockk<EpisodeLocalDataSource>(relaxed = true)
    private val remoteDataSource = mockk<EpisodeRemoteDataSource>(relaxed = true)
    private val chapterRemoteDataSource = mockk<ChapterRemoteDataSource>(relaxed = true)
    private val soundbiteLocalDataSource = mockk<SoundbiteLocalDataSource>(relaxed = true)
    private val soundbiteRemoteDataSource = mockk<SoundbiteRemoteDataSource>(relaxed = true)
    private val remoteUpdater = mockk<EpisodeRemoteUpdater.Factory>(relaxed = true)

    private val repository: EpisodeRepository = EpisodeRepositoryImpl(
        episodeLocalDataSource = localDataSource,
        episodeRemoteDataSource = remoteDataSource,
        chapterRemoteDataSource = chapterRemoteDataSource,
        soundbiteLocalDataSource = soundbiteLocalDataSource,
        soundbiteRemoteDataSource = soundbiteRemoteDataSource,
        remoteUpdater = remoteUpdater,
    )

    private val episodeDtos = episodeTestDataList.toEpisodeWithExtrasViews()

    @After
    fun teardown() {
        confirmVerified(
            localDataSource,
            remoteDataSource,
            remoteUpdater,
            chapterRemoteDataSource,
            soundbiteLocalDataSource,
            soundbiteRemoteDataSource
        )
    }

    @Test
    fun `Given person, When searchEpisodesByPerson, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val max = 10
            val person = "John Doe"
            val expectedQuery = EpisodeQuery.Person(person)

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery { mockUpdater.getFlowList(any()) } returns flowOf(episodeDtos)
            coEvery { remoteUpdater.create(expectedQuery) } returns mockUpdater

            // When
            repository.searchEpisodesByPerson(person, max = max).test {
                val result = awaitItem()
                // Then
                assertEquals(max, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                mockUpdater.getFlowList(any())
            }
        }

    @Test
    fun `Given feedId, When getEpisodesByFeedId, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val max = 10
            val feedId = 123L

            coEvery {
                remoteDataSource.getEpisodesByFeedId(any(), any())
            } returns listOf(mockk<EpisodeResponse>(relaxed = true))

            // When
            repository.getEpisodesByFeedId(feedId, max = max).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                remoteDataSource.getEpisodesByFeedId(feedId, max)
            }
        }

    @Test
    fun `Given feedId, When getEpisodesByFeedIdPaging, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val max = 10
            val feedId = 123L
            val expectedQuery = EpisodeQuery.FeedId(feedId)

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                mockUpdater.getPagingData(any())
            } returns flowOf(PagingData.from(episodeDtos))
            coEvery { remoteUpdater.create(expectedQuery) } returns mockUpdater

            // When
            val result = repository.getEpisodesByFeedIdPaging(feedId).asSnapshot()

            // Then
            assertEquals(episodeTestDataList.size, result.size)
            assertEquals(episodeTestDataList, result)

            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                mockUpdater.getPagingData(any())
            }
        }

    @Test
    fun `Given feedUrl, When getEpisodesByFeedUrl, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val max = 10
            val feedUrl = "https://example.com/feed.xml"
            val expectedQuery = EpisodeQuery.FeedUrl(feedUrl)

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery { mockUpdater.getFlowList(any()) } returns flowOf(episodeDtos)
            coEvery { remoteUpdater.create(expectedQuery) } returns mockUpdater

            // When
            repository.getEpisodesByFeedUrl(feedUrl, max = max).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                mockUpdater.getFlowList(any())
            }
        }

    @Test
    fun `Given podcastGuid, When getEpisodesByPodcastGuid, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val max = 10
            val guid = "test-podcast-guid"
            val expectedQuery = EpisodeQuery.PodcastGuid(guid)

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery { mockUpdater.getFlowList(any()) } returns flowOf(episodeDtos)
            coEvery { remoteUpdater.create(expectedQuery) } returns mockUpdater

            // When
            repository.getEpisodesByPodcastGuid(guid, max = max).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                mockUpdater.getFlowList(any())
            }
        }

    @Test
    fun `When getLiveEpisodes, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val max = 10
            val expectedQuery = EpisodeQuery.Live(max)

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery { mockUpdater.getFlowList(any()) } returns flowOf(episodeDtos)
            coEvery { remoteUpdater.create(expectedQuery) } returns mockUpdater

            // When
            repository.getLiveEpisodes(max = max).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerify {
                remoteUpdater.create(expectedQuery)
                mockUpdater.getFlowList(any())
            }
        }

    @Test
    fun `Given parameters, When getRandomEpisodes, Then calls remoteDataSource directly`() =
        runTest {
            // Given
            val max = 10
            val query = EpisodeQuery.Random(
                max = max,
                language = null,
                categories = emptyList(),
            )

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery { mockUpdater.getFlowList(any()) } returns flowOf(episodeDtos)
            coEvery { remoteUpdater.create(query) } returns mockUpdater

            // When
            repository.getRandomEpisodes(max).test {
                val result = awaitItem()

                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerify {
                remoteUpdater.create(query)
                mockUpdater.getFlowList(any())
            }
        }

    @Test
    fun `When getRecentEpisodes, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val max = 10
            val expectedQuery = EpisodeQuery.Recent(max = max)

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery { mockUpdater.getFlowList(any()) } returns flowOf(episodeDtos)
            coEvery { remoteUpdater.create(expectedQuery) } returns mockUpdater

            // When
            repository.getRecentEpisodes(max = max).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList.size, result.size)
                assertEquals(episodeTestDataList, result)
                awaitComplete()
            }
            coVerify {
                remoteUpdater.create(expectedQuery)
                mockUpdater.getFlowList(any())
            }
        }

    @Test
    fun `When getEpisodesByIds, Then calls localDataSource directly`() =
        runTest {
            // Given
            val ids = episodeTestDataList.map { it.id }
            coEvery {
                localDataSource.getEpisodesByIds(any())
            } returns flowOf(episodeDtos)

            // When
            repository.getEpisodesByIds(ids).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodesByIds(ids)
            }
        }

    @Test
    fun `When getLikedEpisodes, Then calls localDataSource directly`() =
        runTest {
            // Given
            val max = 10
            val dtos = episodeDtos.mapIndexed { index, dto ->
                dto.copy(likedAt = Instant.fromEpochSeconds(1757883600L + index))
            }
            coEvery { localDataSource.getLikedEpisodes(limit = max) } returns flowOf(dtos)

            // When
            repository.getLikedEpisodes(max = max).test {
                val result = awaitItem()
                // Then
                assertEquals(10, result.size)
                assertEquals(dtos[0].episode.id, result[0].id)
                assertEquals(dtos[1].episode.id, result[1].id)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getLikedEpisodes(limit = max)
            }
        }

    @Test
    fun `When getPlayedEpisodes, Then calls localDataSource directly`() =
        runTest {
            // Given
            coEvery {
                localDataSource.getPlayedEpisodes(
                    any(),
                    any(),
                    any()
                )
            } returns flowOf(mockk<List<EpisodeWithExtrasView>>(relaxed = true))


            // When
            repository.getPlayedEpisodes(max = 10).test {
                val result = awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getPlayedEpisodes(limit = 10)
            }
        }

    @Test
    fun `Given dependencies, When toggleLiked, Then calls localDataSource toggleLiked`() =
        runTest {
            // Given
            val episode = episodeTestData.toEpisodeEntity()
            coEvery { localDataSource.toggleLikedEpisode(episode) } returns true

            // When
            repository.toggleLikedEpisode(episodeTestData)

            // Then
            coVerifySequence {
                localDataSource.toggleLikedEpisode(episode)
            }
        }

    @Test
    fun `When updatePlayed, Then calls localDataSource upsertPlayed`() =
        runTest {
            // Given
            val episodeId = 123L
            val position = 30.seconds
            val isCompleted = false
            coEvery { localDataSource.updatePlayedEpisode(any()) } returns Unit

            // When
            repository.updatePlayed(episodeId, position, isCompleted)

            // Then
            coVerify {
                localDataSource.updatePlayedEpisode(
                    match {
                        it.id == episodeId && it.position == position && it.isCompleted == isCompleted
                    }
                )
            }
        }

    @Test
    fun `Given dependencies, When updateDurationOfEpisodes is called, Then calls localDataSource updateDurationOfEpisodes`() =
        runTest {
            // Given
            coEvery { localDataSource.updateEpisodeDuration(any(), any()) } just Runs

            // When
            repository.updateEpisodeDuration(123L, 30.seconds)

            // Then
            coVerifySequence {
                localDataSource.updateEpisodeDuration(123L, 30.seconds)
            }
        }

    @Test
    fun `Given dependencies, When fetchChapters is called, Then calls chapterRemoteDataSource fetchChapters`() =
        runTest {
            // Given
            val url = "https://example.com/chapters.json"
            coEvery { chapterRemoteDataSource.fetchChapters(any()) } returns emptyList()

            // When
            repository.fetchChapters(url)

            // Then
            coVerifySequence {
                chapterRemoteDataSource.fetchChapters(url)
            }
        }

    @Test
    fun `When upsertEpisode, Then calls localDataSource upsertEpisode`() =
        runTest {
            // Given
            val episode = episodeTestData
            coEvery { localDataSource.upsertEpisode(any()) } returns Unit

            // When
            repository.upsertEpisode(episode)

            // Then
            coVerifySequence {
                localDataSource.upsertEpisode(episode.toEpisodeEntity())
            }
        }

    @Test
    fun `When getEpisodeById, Then calls localDataSource getEpisodeById`() =
        runTest {
            // Given
            val episodeId = episodeTestData.id
            coEvery { localDataSource.getEpisodeById(episodeId) } returns flowOf(episodeDtos.first())

            // When
            repository.getEpisodeById(episodeId).test {
                val result = awaitItem()
                // Then
                assertEquals(episodeTestDataList[0].id, result?.id)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getEpisodeById(episodeId)
            }
        }

    @Test
    fun `When isLikedEpisode, Then calls localDataSource isLikedEpisode`() =
        runTest {
            // Given
            val episode = episodeTestData
            coEvery { localDataSource.isLikedEpisode(any()) } returns flowOf(true)

            // When
            repository.isLikedEpisode(episode).test {
                val result = awaitItem()
                // Then
                assertEquals(true, result)
                awaitComplete()
            }

            coVerifySequence {
                localDataSource.isLikedEpisode(episode.toEpisodeEntity())
            }
        }

    @Test
    fun `When getEpisodesByGroupKey, Then calls localDataSource with groupKey and MAX_VALUE`() =
        runTest {
            // Given
            val groupKey = "playlist"
            coEvery {
                localDataSource.getEpisodesByGroupKey(any(), any())
            } returns flowOf(episodeDtos)

            // When
            val result = repository.getEpisodesByGroupKey(groupKey)

            // Then
            assertEquals(episodeTestDataList.size, result.size)
            assertEquals(episodeTestDataList, result)
            coVerifySequence {
                localDataSource.getEpisodesByGroupKey(groupKey, Int.MAX_VALUE)
            }
        }

    @Test
    fun `Given person, When searchEpisodesByPersonPaging, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val person = "Jane Doe"
            val expectedQuery = EpisodeQuery.Person(person)

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                mockUpdater.getPagingData(any())
            } returns flowOf(PagingData.from(episodeDtos))
            coEvery { remoteUpdater.create(expectedQuery) } returns mockUpdater

            // When
            val result = repository.searchEpisodesByPersonPaging(person).asSnapshot()

            // Then
            assertEquals(episodeTestDataList.size, result.size)
            assertEquals(episodeTestDataList, result)

            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                mockUpdater.getPagingData(any())
            }
        }

    @Test
    fun `Given feedUrl, When getEpisodesByFeedUrlPaging, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val feedUrl = "https://example.com/feed.xml"
            val expectedQuery = EpisodeQuery.FeedUrl(feedUrl)

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                mockUpdater.getPagingData(any())
            } returns flowOf(PagingData.from(episodeDtos))
            coEvery { remoteUpdater.create(expectedQuery) } returns mockUpdater

            // When
            val result = repository.getEpisodesByFeedUrlPaging(feedUrl).asSnapshot()

            // Then
            assertEquals(episodeTestDataList.size, result.size)
            assertEquals(episodeTestDataList, result)

            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                mockUpdater.getPagingData(any())
            }
        }

    @Test
    fun `Given guid, When getEpisodesByPodcastGuidPaging, Then creates correct query and calls sourceFactory`() =
        runTest {
            // Given
            val guid = "test-podcast-guid"
            val expectedQuery = EpisodeQuery.PodcastGuid(guid)

            val mockUpdater = mockk<EpisodeRemoteUpdater>(relaxed = true)
            coEvery {
                mockUpdater.getPagingData(any())
            } returns flowOf(PagingData.from(episodeDtos))
            coEvery { remoteUpdater.create(expectedQuery) } returns mockUpdater

            // When
            val result = repository.getEpisodesByPodcastGuidPaging(guid).asSnapshot()

            // Then
            assertEquals(episodeTestDataList.size, result.size)
            assertEquals(episodeTestDataList, result)

            coVerifySequence {
                remoteUpdater.create(expectedQuery)
                mockUpdater.getPagingData(any())
            }
        }

    @Test
    fun `When getSavedEpisodes, Then calls localDataSource getSavedEpisodes`() =
        runTest {
            // Given
            coEvery { localDataSource.getSavedEpisodes(any(), any()) } returns flowOf(episodeDtos)

            // When
            repository.getSavedEpisodes(max = 10).test {
                awaitItem()
                awaitComplete()
            }

            // Then
            coVerifySequence {
                localDataSource.getSavedEpisodes(query = null, limit = 10)
            }
        }

    @Test
    fun `When toggleSavedEpisode, Then calls localDataSource with correct filePath`() =
        runTest {
            // Given
            val episode = episodeTestData
            coEvery { localDataSource.toggleSavedEpisode(any(), any()) } returns true

            // When
            val result = repository.toggleSavedEpisode(episode)

            // Then
            assertEquals(true, result)
            coVerify {
                localDataSource.toggleSavedEpisode(any(), any())
            }
        }

    @Test
    fun `When updateSavedEpisodeProgress, Then calls localDataSource`() =
        runTest {
            // Given
            coEvery { localDataSource.updateSavedEpisodeProgress(any(), any(), any()) } just Runs

            // When
            repository.updateSavedEpisodeProgress(123L, 5000L, DownloadStatus.COMPLETED)

            // Then
            coVerifySequence {
                localDataSource.updateSavedEpisodeProgress(123L, 5000L, DownloadStatus.COMPLETED)
            }
        }

    @Test
    fun `When removeSavedEpisode, Then calls localDataSource`() =
        runTest {
            // Given
            coEvery { localDataSource.removeSavedEpisode(any()) } just Runs

            // When
            repository.removeSavedEpisode(123L)

            // Then
            coVerifySequence {
                localDataSource.removeSavedEpisode(123L)
            }
        }

    @Test
    fun `When replaceEpisodes, Then calls localDataSource`() =
        runTest {
            // Given
            coEvery { localDataSource.replaceEpisodes(any(), any()) } just Runs

            // When
            repository.replaceEpisodes(episodeTestDataList, "groupKey")

            // Then
            coVerifySequence {
                localDataSource.replaceEpisodes(any(), "groupKey")
            }
        }

    @Test
    fun `When getLikedEpisodesPaging, Then returns flow of paging data`() =
        runTest {
            // Given
            coEvery { localDataSource.getLikedEpisodesPaging(any()) } returns mockk(relaxed = true)

            // When
            val flow = repository.getLikedEpisodesPaging()

            // Then
            assertNotNull(flow)
        }

    @Test
    fun `When getPlayedEpisodesPaging, Then returns flow of paging data`() =
        runTest {
            // Given
            coEvery {
                localDataSource.getPlayedEpisodesPaging(any(), any())
            } returns mockk(relaxed = true)

            // When
            val flow = repository.getPlayedEpisodesPaging()

            // Then
            assertNotNull(flow)
        }

    @Test
    fun `When getSavedEpisodesPaging, Then returns flow of paging data`() =
        runTest {
            // Given
            coEvery { localDataSource.getSavedEpisodesPaging(any()) } returns mockk(relaxed = true)

            // When
            val flow = repository.getSavedEpisodesPaging()

            // Then
            assertNotNull(flow)
        }

    @Test
    fun `When getSoundbiteEpisodesPaging, Then returns flow of paging data`() =
        runTest {
            // When
            val flow = repository.getSoundbiteEpisodesPaging(max = 10)

            // Then
            assertNotNull(flow)
        }
}
