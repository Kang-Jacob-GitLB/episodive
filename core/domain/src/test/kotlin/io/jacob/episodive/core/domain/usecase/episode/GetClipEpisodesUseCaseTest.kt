package io.jacob.episodive.core.domain.usecase.episode

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.soundbiteTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GetClipEpisodesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = GetClipEpisodesUseCase(
        feedRepository = feedRepository,
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        // confirmVerified(feedRepository, episodeRepository)
    }

    @Test
    fun `Given dependencies, When invoke called, Then repository called`() =
        runTest {
            // Given
            coEvery { feedRepository.getRecentSoundbites() } returns flowOf(soundbiteTestDataList)
            coEvery { episodeRepository.getEpisodeById(any()) } returns flowOf(episodeTestData)

            // When
            useCase().test {
                // Use case emits progressively as each episode is collected
                // soundbiteTestDataList has 10 items
                // Each episode collection triggers an emission with accumulated results
                var lastEmission = emptyList<Episode>()
                var emissionCount = 0

                // Collect all emissions until we get the final list with 10 items
                while (emissionCount < 10) {
                    lastEmission = awaitItem()
                    emissionCount++
                    // Each emission should be non-empty and growing
                    assertTrue(lastEmission.isNotEmpty())
                }

                // Final emission should contain all 10 clip episodes
                assertEquals(10, lastEmission.size)

                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerify {
                feedRepository.getRecentSoundbites()
                // getEpisodeById is called 10 times (once per soundbite)
                episodeRepository.getEpisodeById(any())
            }
        }

    @Test
    fun `Given empty from recent soundbites, When invoke called, Then returns empty list`() =
        runTest {
            // Given
            coEvery { feedRepository.getRecentSoundbites() } returns flowOf(emptyList())

            // When
            useCase().test {
                val clipEpisodes = awaitItem()
                assertEquals(0, clipEpisodes.size)
                awaitComplete()
            }

            // Then
            coVerify {
                feedRepository.getRecentSoundbites()
            }
        }
}
