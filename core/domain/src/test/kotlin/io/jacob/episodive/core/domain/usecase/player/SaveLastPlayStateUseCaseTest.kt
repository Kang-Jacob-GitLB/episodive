package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class SaveLastPlayStateUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)

    private val useCase = SaveLastPlayStateUseCase(
        userRepository = userRepository,
    )

    @After
    fun teardown() {
        confirmVerified(userRepository)
    }

    @Test
    fun `Given parameters, When invoke, Then delegates to userRepository`() =
        runTest {
            // Given
            coEvery {
                userRepository.saveLastPlayState(any(), any(), any(), any(), any())
            } just Runs

            // When
            useCase(
                episodeId = 123L,
                index = 2,
                positionMs = 5000L,
                shuffle = true,
                repeat = Repeat.ONE,
            )

            // Then
            coVerify {
                userRepository.saveLastPlayState(123L, 2, 5000L, true, Repeat.ONE)
            }
        }
}
