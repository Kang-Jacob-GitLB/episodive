package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class SyncNewEpisodesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)
    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = SyncNewEpisodesUseCase(
        podcastRepository = podcastRepository,
        episodeRepository = episodeRepository,
    )

    @Test
    fun `Given no followed podcasts, when invoke, then returns empty list`() = runTest {
        coEvery { podcastRepository.getFollowedPodcastIdsWithNotificationEnabled() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Given followed podcasts with new episodes, when invoke, then returns results`() = runTest {
        val feedId = 5778530L
        val since = Instant.fromEpochSeconds(1757797200)
        val newEpisodes = episodeTestDataList.take(2)

        coEvery { podcastRepository.getFollowedPodcastIdsWithNotificationEnabled() } returns listOf(feedId)
        coEvery { episodeRepository.getLatestEpisodeDatePublished(feedId) } returns since
        coEvery { episodeRepository.fetchAndSaveNewEpisodes(feedId, since) } returns newEpisodes

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals(feedId, result[0].feedId)
        assertEquals(2, result[0].episodes.size)
    }

    @Test
    fun `Given followed podcast with no cached episodes, when invoke, then skips it`() = runTest {
        val feedId = 5778530L

        coEvery { podcastRepository.getFollowedPodcastIdsWithNotificationEnabled() } returns listOf(feedId)
        coEvery { episodeRepository.getLatestEpisodeDatePublished(feedId) } returns null

        val result = useCase()

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { episodeRepository.fetchAndSaveNewEpisodes(any(), any()) }
    }

    @Test
    fun `Given followed podcast with no new episodes, when invoke, then returns empty`() = runTest {
        val feedId = 5778530L
        val since = Instant.fromEpochSeconds(1757797200)

        coEvery { podcastRepository.getFollowedPodcastIdsWithNotificationEnabled() } returns listOf(feedId)
        coEvery { episodeRepository.getLatestEpisodeDatePublished(feedId) } returns since
        coEvery { episodeRepository.fetchAndSaveNewEpisodes(feedId, since) } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Given network error for one feed, when invoke, then continues with others`() = runTest {
        val feedId1 = 5778530L
        val feedId2 = 9999999L
        val since1 = Instant.fromEpochSeconds(1757797200)
        val since2 = Instant.fromEpochSeconds(1757883600)
        val newEpisodes = episodeTestDataList.take(1)

        coEvery { podcastRepository.getFollowedPodcastIdsWithNotificationEnabled() } returns listOf(feedId1, feedId2)
        coEvery { episodeRepository.getLatestEpisodeDatePublished(feedId1) } returns since1
        coEvery { episodeRepository.fetchAndSaveNewEpisodes(feedId1, since1) } throws RuntimeException("Network error")
        coEvery { episodeRepository.getLatestEpisodeDatePublished(feedId2) } returns since2
        coEvery { episodeRepository.fetchAndSaveNewEpisodes(feedId2, since2) } returns newEpisodes

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals(feedId2, result[0].feedId)
    }

    @Test
    fun `Given multiple followed podcasts, when invoke, then syncs only notification-enabled ones`() = runTest {
        val feedId1 = 5778530L
        val feedId2 = 9999999L
        val since = Instant.fromEpochSeconds(1757797200)
        val newEpisodes = episodeTestDataList.take(1)

        coEvery { podcastRepository.getFollowedPodcastIdsWithNotificationEnabled() } returns listOf(feedId1, feedId2)
        coEvery { episodeRepository.getLatestEpisodeDatePublished(any()) } returns since
        coEvery { episodeRepository.fetchAndSaveNewEpisodes(feedId1, since) } returns newEpisodes
        coEvery { episodeRepository.fetchAndSaveNewEpisodes(feedId2, since) } returns newEpisodes

        val result = useCase()

        assertEquals(2, result.size)
        coVerify(exactly = 1) { podcastRepository.getFollowedPodcastIdsWithNotificationEnabled() }
    }
}
