package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.DataError
import io.jacob.episodive.core.model.DataErrorException
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class RefreshPodcastUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)
    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = RefreshPodcastUseCase(
        podcastRepository = podcastRepository,
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(podcastRepository, episodeRepository)
    }

    @Test
    fun `Given podcast id, when invoke called, then both podcast and episodes are refreshed`() =
        runTest {
            useCase(1L)

            coVerify(exactly = 1) { podcastRepository.refreshPodcastByFeedId(1L) }
            coVerify(exactly = 1) { episodeRepository.refreshEpisodesByFeedId(1L) }
        }

    @Test
    fun `Given episodes refresh fails, when invoke called, then the failure is thrown`() =
        runTest {
            coEvery { episodeRepository.refreshEpisodesByFeedId(1L) } throws
                DataErrorException(DataError.Offline, RuntimeException("offline"))

            try {
                useCase(1L)
                fail("실패가 올라와야 한다")
            } catch (e: DataErrorException) {
                assertEquals(DataError.Offline, e.error)
            }

            coVerify(exactly = 1) { podcastRepository.refreshPodcastByFeedId(1L) }
            coVerify(exactly = 1) { episodeRepository.refreshEpisodesByFeedId(1L) }
        }
}
