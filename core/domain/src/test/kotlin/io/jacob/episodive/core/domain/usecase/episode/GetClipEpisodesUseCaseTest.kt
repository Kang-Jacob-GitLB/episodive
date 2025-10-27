package io.jacob.episodive.core.domain.usecase.episode

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
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
                // Use case emits in batches of 5 episodes
                // soundbiteTestDataList has 10 items
                // Emits: 5, 10 (2 emissions total)
                val firstBatch = awaitItem()
                assertEquals(5, firstBatch.size)

                val secondBatch = awaitItem()
                assertEquals(10, secondBatch.size)

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
