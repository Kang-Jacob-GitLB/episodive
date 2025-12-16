package io.jacob.episodive.core.domain.usecase.podcast

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList
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

class GetTrendingPodcastsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private val getPodcastsByFeedIdsParallellyUseCase =
        mockk<GetPodcastsByFeedIdsParallellyUseCase>(relaxed = true)

    private val useCase = GetTrendingPodcastsUseCase(
        feedRepository = feedRepository,
        getPodcastsByFeedIdsParallellyUseCase = getPodcastsByFeedIdsParallellyUseCase,
    )

    @After
    fun teardown() {
        confirmVerified(feedRepository, getPodcastsByFeedIdsParallellyUseCase)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            coEvery {
                feedRepository.getTrendingFeeds(any(), any(), any(), any())
            } returns flowOf(trendingFeedTestDataList)
            coEvery {
                getPodcastsByFeedIdsParallellyUseCase(any())
            } returns podcastTestDataList

            // When
            useCase(10).test {
                val result = awaitItem()
                assertEquals(10, result.size)
                awaitComplete()
            }

            // Then
            coVerify {
                feedRepository.getTrendingFeeds(any(), any(), any(), any())
                getPodcastsByFeedIdsParallellyUseCase(any())
            }
        }
}