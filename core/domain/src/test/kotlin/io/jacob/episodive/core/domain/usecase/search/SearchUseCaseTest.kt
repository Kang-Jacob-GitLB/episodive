package io.jacob.episodive.core.domain.usecase.search

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)
    private val episodeRepository = mockk<EpisodeRepository>(relaxed = true)

    private val useCase = SearchUseCase(
        podcastRepository = podcastRepository,
        episodeRepository = episodeRepository,
    )

    @After
    fun teardown() {
        confirmVerified(podcastRepository, episodeRepository)
    }

    @Test
    fun `Given dependencies, When invoke called, Then repositories called`() =
        runTest {
            // Given
            coEvery {
                podcastRepository.searchPodcasts(any(), 10)
            } returns flowOf(podcastTestDataList.take(2))
            coEvery {
                episodeRepository.getEpisodesByFeedId(podcastTestDataList[0].id, any())
            } returns flowOf(episodeTestDataList.take(5))
            coEvery {
                episodeRepository.getEpisodesByFeedId(podcastTestDataList[1].id, any())
            } returns flowOf(episodeTestDataList.drop(5).take(5))

            // When
            useCase("query", 10).test {
                val result = awaitItem()

                // Then
                assertEquals(2, result.podcasts.size)
                result.podcasts.forEachIndexed { index, podcast ->
                    assertEquals(podcastTestDataList[index], podcast)
                }
                assertEquals(10, result.episodes.size)
                result.episodes.forEachIndexed { index, episode ->
                    assertEquals(episodeTestDataList[index], episode)
                }

                awaitComplete()
            }
            coVerifySequence {
                podcastRepository.searchPodcasts(any(), 10)
                episodeRepository.getEpisodesByFeedId(podcastTestDataList[0].id, any())
                episodeRepository.getEpisodesByFeedId(podcastTestDataList[1].id, any())
            }
        }
}