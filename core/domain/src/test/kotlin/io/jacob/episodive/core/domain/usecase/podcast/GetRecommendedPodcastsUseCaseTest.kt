package io.jacob.episodive.core.domain.usecase.podcast

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.testing.model.podcastTestDataList
import io.jacob.episodive.core.testing.model.recentFeedTestDataList
import io.jacob.episodive.core.testing.model.trendingFeedTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GetRecommendedPodcastsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private val getPodcastsByFeedIdsParallellyUseCase =
        mockk<GetPodcastsByFeedIdsParallellyUseCase>(relaxed = true)

    private val useCase = GetRecommendedPodcastsUseCase(
        userRepository = userRepository,
        feedRepository = feedRepository,
        getPodcastsByFeedIdsParallellyUseCase = getPodcastsByFeedIdsParallellyUseCase,
    )

    @After
    fun teardown() {
        confirmVerified(
            userRepository,
            feedRepository,
            getPodcastsByFeedIdsParallellyUseCase
        )
    }

    @Test
    fun `Given empty categories, when invoke called, then return empty list`() =
        runTest {
            // Given
            coEvery { userRepository.getUserData() } returns flowOf(UserData())

            // When
            useCase().test {
                val result = awaitItem()

                // Then
                assertTrue(result.isEmpty())
                awaitComplete()
            }

            coVerifySequence {
                userRepository.getUserData()
            }
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
                feedRepository.getTrendingFeeds(any(), any(), any(), any())
            } returns flowOf(trendingFeedTestDataList)
            coEvery {
                feedRepository.getRecentFeeds(any(), any(), any(), any())
            } returns flowOf(recentFeedTestDataList)
            coEvery {
                getPodcastsByFeedIdsParallellyUseCase(any())
            } returns podcastTestDataList

            // When
            useCase().test {
                val result = awaitItem()
                assertEquals(10, result.size)
                awaitComplete()
            }

            // Then
            coVerifySequence {
                userRepository.getUserData()
                feedRepository.getTrendingFeeds(any(), any(), any(), any())
                feedRepository.getRecentFeeds(any(), any(), any(), any())
                getPodcastsByFeedIdsParallellyUseCase(any())
            }
        }
}