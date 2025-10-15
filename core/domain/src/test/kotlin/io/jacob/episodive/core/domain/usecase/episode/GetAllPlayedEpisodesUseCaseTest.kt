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

class GetAllPlayedEpisodesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = GetAllPlayedEpisodesUseCase(
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(episodeRepository)
    }

    @Test
    fun `Given dependencies, When invoke called, Then repository called`() =
        runTest {
            // Given
            coEvery { episodeRepository.getAllPlayedEpisodes() } returns mockk(relaxed = true)

            // When
            useCase().test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                episodeRepository.getAllPlayedEpisodes()
            }
        }
}