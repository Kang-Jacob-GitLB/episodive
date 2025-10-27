package io.jacob.episodive.core.domain.usecase.podcast

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.testing.model.recentFeedTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetRecentPodcastsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)

    private val useCase = GetRecentPodcastsUseCase(
        feedRepository = feedRepository,
        podcastRepository = podcastRepository,
    )

    @After
    fun teardown() {
        confirmVerified(feedRepository, podcastRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            coEvery {
                feedRepository.getRecentFeeds(any(), any(), any(), any(), any())
            } returns flowOf(recentFeedTestDataList)
            coEvery {
                podcastRepository.getPodcastByFeedId(any())
            } returns flowOf(mockk(relaxed = true))

            // When
            useCase().test {
                val firstBatch = awaitItem()
                assertEquals(5, firstBatch.size)

                val secondBatch = awaitItem()
                assertEquals(10, secondBatch.size)

                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerify {
                feedRepository.getRecentFeeds(any(), any(), any(), any(), any())
            }
            coVerify(exactly = 10) { podcastRepository.getPodcastByFeedId(any()) }
        }
}