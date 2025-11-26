package io.jacob.episodive.core.domain.usecase.user

import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class SetSpeedUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)

    private val useCase = SetSpeedUseCase(
        userRepository = userRepository,
    )

    @After
    fun teardown() {
        confirmVerified(userRepository)
    }

    @Test
    fun `Given dependencies, when invoke called, then repository called`() =
        runTest {
            // Given
            val speed = 1.5f
            coEvery { userRepository.setSpeed(any()) } just runs

            // When
            useCase(speed)

            // Then
            coVerifySequence {
                userRepository.setSpeed(speed)
            }
        }
}