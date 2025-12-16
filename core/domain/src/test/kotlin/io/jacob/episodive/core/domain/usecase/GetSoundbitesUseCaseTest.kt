package io.jacob.episodive.core.domain.usecase

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GetSoundbitesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val feedRepository = mockk<FeedRepository>(relaxed = true)

    private val useCase = GetSoundbitesUseCase(
        feedRepository = feedRepository,
    )

    @After
    fun teardown() {
        confirmVerified(feedRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            coEvery { feedRepository.getRecentSoundbites(any()) } returns mockk(relaxed = true)

            // When
            useCase(10).test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                feedRepository.getRecentSoundbites(any())
            }
        }
}