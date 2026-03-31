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

class GetSavedEpisodesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = GetSavedEpisodesUseCase(
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(episodeRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            coEvery {
                episodeRepository.getSavedEpisodes(any(), any())
            } returns mockk(relaxed = true)

            useCase(null, 6).test {
                awaitComplete()
            }

            coVerifySequence {
                episodeRepository.getSavedEpisodes(any(), any())
            }
        }

    @Test
    fun `Given query, when invoke called with query, then repository called with query`() =
        runTest {
            coEvery {
                episodeRepository.getSavedEpisodes(any(), any())
            } returns mockk(relaxed = true)

            useCase("search", 10).test {
                awaitComplete()
            }

            coVerifySequence {
                episodeRepository.getSavedEpisodes("search", 10)
            }
        }

    @Test
    fun `Given no query, when invoke called with max only, then repository called with null query`() =
        runTest {
            coEvery {
                episodeRepository.getSavedEpisodes(any(), any())
            } returns mockk(relaxed = true)

            useCase(max = 6).test {
                awaitComplete()
            }

            coVerifySequence {
                episodeRepository.getSavedEpisodes(null, 6)
            }
        }
}
