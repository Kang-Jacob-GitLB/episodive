package io.jacob.episodive.core.domain.usecase.podcast

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.testing.model.recentFeedTestDataList
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class GetRecommendedPodcastsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)

    private val useCase = GetRecommendedPodcastsUseCase(
        userRepository = userRepository,
        feedRepository = feedRepository,
        podcastRepository = podcastRepository,
    )

    @After
    fun teardown() {
//        confirmVerified(userRepository, feedRepository, podcastRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            coEvery {
                userRepository.getUserData()
            } returns flowOf(
                UserData(
                    language = "ko",
                    categories = listOf(Category.AFTER_SHOWS, Category.BUSINESS)
                )
            )
            coEvery {
                feedRepository.getTrendingFeeds(any(), any(), any(), any(), any())
            } returns flowOf(trendingFeedTestDataList)
            coEvery {
                feedRepository.getRecentFeeds(any(), any(), any(), any(), any())
            } returns flowOf(recentFeedTestDataList)
            coEvery {
                podcastRepository.getPodcastByFeedId(any())
            } returns flowOf(mockk(relaxed = true))

            // When
            useCase().test {
                // Use case emits in batches of 5 podcasts
                // Combined feeds: 10 trending + 10 recent = 20 unique feeds (after distinctBy)
                // Emits: 5, 10, 15, 20 (4 emissions total)
                val firstBatch = awaitItem()
                Assert.assertEquals(5, firstBatch.size)

                val secondBatch = awaitItem()
                Assert.assertEquals(10, secondBatch.size)

                val thirdBatch = awaitItem()
                Assert.assertEquals(15, thirdBatch.size)

                val fourthBatch = awaitItem()
                Assert.assertEquals(20, fourthBatch.size)

                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerify {
                userRepository.getUserData()
                feedRepository.getTrendingFeeds(any(), any(), any(), any(), any())
                feedRepository.getRecentFeeds(any(), any(), any(), any(), any())
                // getPodcastByFeedId is called 20 times (once per feed)
                podcastRepository.getPodcastByFeedId(any())
            }
        }
}