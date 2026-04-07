package io.jacob.episodive.core.data.repository

import app.cash.turbine.test
import io.jacob.episodive.core.data.util.updater.EpisodeRemoteUpdater
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSource
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.network.datasource.ChapterRemoteDataSource
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.SoundbiteRemoteDataSource
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class EpisodeRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeLocalDataSource = mockk<EpisodeLocalDataSource>(relaxed = true)
    private val episodeRemoteDataSource = mockk<EpisodeRemoteDataSource>(relaxed = true)
    private val chapterRemoteDataSource = mockk<ChapterRemoteDataSource>(relaxed = true)
    private val soundbiteLocalDataSource = mockk<SoundbiteLocalDataSource>(relaxed = true)
    private val soundbiteRemoteDataSource = mockk<SoundbiteRemoteDataSource>(relaxed = true)
    private val remoteUpdater = mockk<EpisodeRemoteUpdater.Factory>(relaxed = true)

    private val repository = EpisodeRepositoryImpl(
        episodeLocalDataSource = episodeLocalDataSource,
        episodeRemoteDataSource = episodeRemoteDataSource,
        chapterRemoteDataSource = chapterRemoteDataSource,
        soundbiteLocalDataSource = soundbiteLocalDataSource,
        soundbiteRemoteDataSource = soundbiteRemoteDataSource,
        remoteUpdater = remoteUpdater,
    )

    @Test
    fun `Given feedId, When getLatestEpisodeDatePublished, Then delegates to localDataSource`() =
        runTest {
            // Given
            val feedId = 5778530L
            val expected = Instant.fromEpochSeconds(1000)
            coEvery { episodeLocalDataSource.getLatestEpisodeDatePublished(feedId) } returns expected

            // When
            val result = repository.getLatestEpisodeDatePublished(feedId)

            // Then
            assertEquals(expected, result)
            coVerify { episodeLocalDataSource.getLatestEpisodeDatePublished(feedId) }
        }

    @Test
    fun `Given feedId and since, When fetchAndSaveNewEpisodes, Then fetches from remote and saves locally`() =
        runTest {
            // Given
            val feedId = 5778530L
            val since = Instant.fromEpochSeconds(1000)
            coEvery {
                episodeRemoteDataSource.getEpisodesByFeedId(feedId = feedId, since = since.epochSeconds)
            } returns emptyList()

            // When
            val result = repository.fetchAndSaveNewEpisodes(feedId, since)

            // Then
            assertEquals(emptyList<Any>(), result)
            coVerify { episodeRemoteDataSource.getEpisodesByFeedId(feedId = feedId, since = since.epochSeconds) }
            coVerify { episodeLocalDataSource.upsertEpisodes(emptyList()) }
        }

    @Test
    fun `Given episode, When upsertEpisode, Then delegates to localDataSource`() = runTest {
        // Given
        val episode = episodeTestData

        // When
        repository.upsertEpisode(episode)

        // Then
        coVerify { episodeLocalDataSource.upsertEpisode(any()) }
    }

    @Test
    fun `Given episodeId, When getEpisodeById with no episode, Then returns null`() = runTest {
        // Given
        val id = 999L
        every { episodeLocalDataSource.getEpisodeById(id) } returns flowOf(null)

        // When / Then
        repository.getEpisodeById(id).test {
            assertNull(awaitItem())
            awaitComplete()
        }
        coVerify { episodeLocalDataSource.getEpisodeById(id) }
    }

    @Test
    fun `Given episodes and groupKey, When replaceEpisodes, Then delegates to localDataSource`() =
        runTest {
            // Given
            val episodes = emptyList<Episode>()
            val groupKey = "feedId:123"

            // When
            repository.replaceEpisodes(episodes, groupKey)

            // Then
            coVerify { episodeLocalDataSource.replaceEpisodes(emptyList(), groupKey) }
        }

    @Test
    fun `Given episode, When toggleLikedEpisode returns true, Then delegates to localDataSource`() =
        runTest {
            // Given
            val episode = episodeTestData
            coEvery { episodeLocalDataSource.toggleLikedEpisode(any()) } returns true

            // When
            val result = repository.toggleLikedEpisode(episode)

            // Then
            assertEquals(true, result)
            coVerify { episodeLocalDataSource.toggleLikedEpisode(any()) }
        }

    @Test
    fun `Given episode, When toggleLikedEpisode returns false, Then delegates to localDataSource`() =
        runTest {
            // Given
            val episode = episodeTestData
            coEvery { episodeLocalDataSource.toggleLikedEpisode(any()) } returns false

            // When
            val result = repository.toggleLikedEpisode(episode)

            // Then
            assertEquals(false, result)
            coVerify { episodeLocalDataSource.toggleLikedEpisode(any()) }
        }

    @Test
    fun `Given id and position, When updatePlayed, Then delegates to localDataSource`() = runTest {
        // Given
        val id = episodeTestData.id
        val position = 30.seconds
        val isCompleted = false

        // When
        repository.updatePlayed(id = id, position = position, isCompleted = isCompleted)

        // Then
        coVerify { episodeLocalDataSource.updatePlayedEpisode(any()) }
    }

    @Test
    fun `Given id and duration, When updateEpisodeDuration, Then delegates to localDataSource`() =
        runTest {
            // Given
            val id = 1L
            val duration = Duration.parse("1h")

            // When
            repository.updateEpisodeDuration(id, duration)

            // Then
            coVerify { episodeLocalDataSource.updateEpisodeDuration(id, duration) }
        }

    @Test
    fun `Given groupKey, When getEpisodesByGroupKey, Then delegates to localDataSource`() =
        runTest {
            // Given
            val groupKey = "feedId:123"
            every {
                episodeLocalDataSource.getEpisodesByGroupKey(groupKey, Int.MAX_VALUE)
            } returns kotlinx.coroutines.flow.flowOf(emptyList())

            // When
            val result = repository.getEpisodesByGroupKey(groupKey)

            // Then
            assertEquals(emptyList<Any>(), result)
            coVerify { episodeLocalDataSource.getEpisodesByGroupKey(groupKey, Int.MAX_VALUE) }
        }

    @Test
    fun `Given id, When removeSavedEpisode, Then delegates to localDataSource`() = runTest {
        // Given
        val id = 42L

        // When
        repository.removeSavedEpisode(id)

        // Then
        coVerify { episodeLocalDataSource.removeSavedEpisode(id) }
    }

    @Test
    fun `Given url, When fetchChapters, Then delegates to chapterRemoteDataSource`() = runTest {
        // Given
        val url = "https://example.com/chapters.json"
        coEvery { chapterRemoteDataSource.fetchChapters(url) } returns emptyList()

        // When
        val result = repository.fetchChapters(url)

        // Then
        assertEquals(emptyList<Any>(), result)
        coVerify { chapterRemoteDataSource.fetchChapters(url) }
    }

    @Test
    fun `Given ids, When getEpisodesByIds, Then delegates to localDataSource`() = runTest {
        val ids = listOf(1L, 2L, 3L)
        every { episodeLocalDataSource.getEpisodesByIds(ids) } returns flowOf(emptyList())

        repository.getEpisodesByIds(ids).test {
            assertEquals(emptyList<Any>(), awaitItem())
            awaitComplete()
        }
        coVerify { episodeLocalDataSource.getEpisodesByIds(ids) }
    }

    @Test
    fun `Given episode, When isLikedEpisode, Then delegates to localDataSource`() = runTest {
        val episode = episodeTestData
        every { episodeLocalDataSource.isLikedEpisode(any()) } returns flowOf(true)

        repository.isLikedEpisode(episode).test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Given id and progress, When updateSavedEpisodeProgress, Then delegates to localDataSource`() =
        runTest {
            val id = 1L
            val downloadedSize = 1024L
            val status = io.jacob.episodive.core.model.DownloadStatus.DOWNLOADING

            repository.updateSavedEpisodeProgress(id, downloadedSize, status)

            coVerify { episodeLocalDataSource.updateSavedEpisodeProgress(id, downloadedSize, status) }
        }

    @Test
    fun `Given feedId, When getEpisodesByFeedId, Then fetches from remote`() = runTest {
        val feedId = 5778530L
        coEvery { episodeRemoteDataSource.getEpisodesByFeedId(feedId, 10) } returns emptyList()

        repository.getEpisodesByFeedId(feedId, 10).test {
            assertEquals(emptyList<Any>(), awaitItem())
            awaitComplete()
        }
        coVerify { episodeRemoteDataSource.getEpisodesByFeedId(feedId, 10) }
    }
}
