package io.jacob.episodive.core.domain.usecase.podcast

import app.cash.turbine.test
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

class GetMyTrendingPodcastsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val getTrendingPodcastsUseCase = mockk<GetTrendingPodcastsUseCase>(relaxed = true)

    private val useCase = GetMyTrendingPodcastsUseCase(
        userRepository = userRepository,
        getTrendingPodcastsUseCase = getTrendingPodcastsUseCase,
    )

    @After
    fun teardown() {
        confirmVerified(userRepository, getTrendingPodcastsUseCase)
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
                getTrendingPodcastsUseCase(any(), any())
            } returns mockk(relaxed = true)

            // When
            useCase().test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                userRepository.getUserData()
                getTrendingPodcastsUseCase(any(), any())
            }
        }
}