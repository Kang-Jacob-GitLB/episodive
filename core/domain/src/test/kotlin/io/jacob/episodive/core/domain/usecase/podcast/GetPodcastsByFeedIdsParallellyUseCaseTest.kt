package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.testing.model.podcastTestData
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

class GetPodcastsByFeedIdsParallellyUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)

    private val useCase = GetPodcastsByFeedIdsParallellyUseCase(
        podcastRepository = podcastRepository,
    )

    @After
    fun teardown() {
        confirmVerified(podcastRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            coEvery {
                podcastRepository.getPodcastByFeedId(any())
            } answers {
                val feedId = arg<Long>(0)
                flowOf(podcastTestData.copy(id = feedId))
            }

            // When
            val ids = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)
            val result = useCase(ids)

            // Then
            assertEquals(10, result.size)
            result.forEachIndexed { index, podcast ->
                assertEquals(ids[index], podcast.id)
            }
            coVerifySequence {
                repeat(ids.size) {
                    podcastRepository.getPodcastByFeedId(any())
                }
            }
        }
}