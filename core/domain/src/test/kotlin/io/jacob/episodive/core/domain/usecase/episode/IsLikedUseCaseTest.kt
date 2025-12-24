package io.jacob.episodive.core.domain.usecase.episode

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IsLikedUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = IsLikedEpisodeUseCase(
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(episodeRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            val episode = episodeTestData
            coEvery { episodeRepository.isLikedEpisode(any()) } returns flowOf(true)

            // When
            useCase(episode).test {
                val result = awaitItem()
                // Then
                assertTrue(result)
                awaitComplete()
            }

            coVerifySequence {
                episodeRepository.isLikedEpisode(episode)
            }
        }
}