package io.jacob.episodive.core.domain.usecase.episode

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GetPlayingEpisodesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = GetPlayingEpisodesUseCase(
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(episodeRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository not called`() =
        runTest {
            // Given
            coEvery { episodeRepository.getPlayedEpisodes(any(), any(), any()) } returns mockk(
                relaxed = true
            )

            // When
            useCase(max = 10).test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                episodeRepository.getPlayedEpisodes(isCompleted = false, query = null, max = 10)
            }
        }
}