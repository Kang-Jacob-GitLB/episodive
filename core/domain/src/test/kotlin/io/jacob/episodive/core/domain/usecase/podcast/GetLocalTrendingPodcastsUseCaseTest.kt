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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class GetLocalTrendingPodcastsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val getTrendingPodcastsUseCase = mockk<GetTrendingPodcastsUseCase>(relaxed = true)

    private val useCase = GetLocalTrendingPodcastsUseCase(
        userRepository = userRepository,
        getTrendingPodcastsUseCase = getTrendingPodcastsUseCase,
    )

    @After
    fun teardown() {
        confirmVerified(
            userRepository,
            getTrendingPodcastsUseCase,
        )
    }

    @Test
    fun `Given empty categories, When invoke called, Then return empty`() =
        runTest {
            // Given
            coEvery {
                userRepository.getUserData()
            } returns flowOf(UserData())

            // When
            useCase(10).test {
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
    fun `Given not empty categories, When invoke called, Then return trending podcasts`() =
        runTest {
            // Given
            coEvery {
                userRepository.getUserData()
            } returns flowOf(
                UserData(
                    categories = listOf(Category.AFTER_SHOWS, Category.BUSINESS)
                )
            )
            coEvery {
                getTrendingPodcastsUseCase(any(), any(), any())
            } returns mockk(relaxed = true)

            // When
            useCase(10).test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                userRepository.getUserData()
                val language = Locale.getDefault().language
                getTrendingPodcastsUseCase(
                    10,
                    language,
                    listOf(Category.AFTER_SHOWS, Category.BUSINESS)
                )
            }
        }
}