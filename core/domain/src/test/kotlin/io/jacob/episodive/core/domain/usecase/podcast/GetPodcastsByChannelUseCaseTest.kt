package io.jacob.episodive.core.domain.usecase.podcast

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GetPodcastsByChannelUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)

    private val useCase = GetPodcastsByChannelUseCase(
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
            val channel = Channel(
                id = 1,
                title = "name",
                description = "description",
                image = "imageUrl",
                link = "url",
                count = 1,
                podcastGuids = listOf("guid"),
            )
            coEvery {
                podcastRepository.getPodcastsByChannel(any())
            } returns mockk(relaxed = true)

            // When
            useCase(channel).test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                podcastRepository.getPodcastsByChannel(any())
            }
        }
}