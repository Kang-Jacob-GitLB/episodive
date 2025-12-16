package io.jacob.episodive.core.domain.usecase.podcast

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GetFollowedPodcastsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)

    private val useCase = GetFollowedPodcastsUseCase(
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
                podcastRepository.getFollowedPodcasts(any(), 10)
            } returns mockk(relaxed = true)

            // When
            useCase(max = 10).test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                podcastRepository.getFollowedPodcasts(any(), 10)
            }
        }
}