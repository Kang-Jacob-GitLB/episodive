package io.jacob.episodive.core.domain.usecase.podcast

import app.cash.turbine.test
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GetRecommendedPodcastsUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMyTrendingPodcastsUseCase = mockk<GetMyTrendingPodcastsUseCase>(relaxed = true)
    private val getMyRecentPodcastsUseCase = mockk<GetMyRecentPodcastsUseCase>(relaxed = true)

    private val useCase = GetRecommendedPodcastsUseCase(
        getMyTrendingPodcastsUseCase = getMyTrendingPodcastsUseCase,
        getMyRecentPodcastsUseCase = getMyRecentPodcastsUseCase,
    )

    @After
    fun teardown() {
        confirmVerified(
            getMyTrendingPodcastsUseCase,
            getMyRecentPodcastsUseCase,
        )
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given & When
            useCase().test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                getMyTrendingPodcastsUseCase(100)
                getMyRecentPodcastsUseCase(100)
            }
        }
}