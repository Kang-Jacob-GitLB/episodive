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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `Given dependencies, When invoke called, Then emits progressively in chunks of 5`() =
        runTest {
            // Given
            coEvery { feedRepository.getRecentSoundbites() } returns flowOf(soundbiteTestDataList)
            coEvery { episodeRepository.getEpisodeById(any()) } returns flowOf(episodeTestData)

            // When
            useCase().test {
                // soundbiteTestDataList has 10 items, chunked by 5
                // First chunk: 5 episodes
                val firstEmission = awaitItem()
                assertEquals(5, firstEmission.size)
                firstEmission.forEach { episode ->
                    assertNotNull(episode.clipStartTime)
                    assertNotNull(episode.clipDuration)
                }

                // Second chunk (last): 10 episodes total
                val secondEmission = awaitItem()
                assertEquals(10, secondEmission.size)
                secondEmission.forEach { episode ->
                    assertNotNull(episode.clipStartTime)
                    assertNotNull(episode.clipDuration)
                }

                // After last chunk, the flow continues observing for changes
                // Cancel here since we don't need to test real-time updates in this test
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
    fun `Given episode with likedAt changes, When observing, Then emits updated episode`() =
        runTest {
            // Given
            // Create a map to store flows for each episode ID
            val episodeFlows = mutableMapOf<Long, MutableStateFlow<Episode>>()

            // Setup mock to return different flow for each episode ID
            coEvery { feedRepository.getRecentSoundbites() } returns flowOf(soundbiteTestDataList)
            coEvery { episodeRepository.getEpisodeById(any()) } answers {
                val episodeId = firstArg<Long>()
                episodeFlows.getOrPut(episodeId) {
                    MutableStateFlow(episodeTestData.copy(id = episodeId, likedAt = null))
                }
            }

            // When
            useCase().test {
                // First chunk: 5 episodes without likedAt
                val firstEmission = awaitItem()
                assertEquals(5, firstEmission.size)
                firstEmission.forEach { episode ->
                    assertNull(episode.likedAt)
                }

                // Second chunk: 10 episodes without likedAt
                val secondEmission = awaitItem()
                assertEquals(10, secondEmission.size)
                secondEmission.forEach { episode ->
                    assertNull(episode.likedAt)
                }

                // Simulate liking the first episode
                val likedAt = Instant.fromEpochMilliseconds(1234567890)
                val firstEpisodeId = soundbiteTestDataList.first().episodeId
                episodeFlows[firstEpisodeId]?.value =
                    episodeFlows[firstEpisodeId]!!.value.copy(likedAt = likedAt)

                // Should emit updated list with first episode liked
                val updatedEmission = awaitItem()
                assertEquals(10, updatedEmission.size)
                assertEquals(likedAt, updatedEmission.first().likedAt)
                // Other episodes should still have null likedAt
                updatedEmission.drop(1).forEach { episode ->
                    assertNull(episode.likedAt)
                }

                cancelAndIgnoreRemainingEvents()
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
