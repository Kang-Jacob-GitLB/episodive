package io.jacob.episodive.core.domain.usecase.podcast

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GetUserRecommendedPodcastsPagingUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val podcastRepository = mockk<PodcastRepository>(relaxed = true)

    private val useCase = GetUserRecommendedPodcastsPagingUseCase(
        userRepository = userRepository,
        podcastRepository = podcastRepository,
    )

    @After
    fun teardown() {
        confirmVerified(
            userRepository,
            podcastRepository,
        )
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
                podcastRepository.getRecommendedPodcastsPaging(any(), any(), any())
            } returns mockk(relaxed = true)

            // When
            useCase(50).test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                userRepository.getUserData()
                podcastRepository.getRecommendedPodcastsPaging(
                    50,
                    "ko",
                    listOf(Category.AFTER_SHOWS, Category.BUSINESS)
                )
            }
        }
}