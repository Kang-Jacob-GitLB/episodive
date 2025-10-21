package io.jacob.episodive.core.domain.usecase.user

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GetSelectableCategoriesUesCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)

    private val useCase = GetSelectableCategoriesUseCase(
        userRepository = userRepository,
    )

    @After
    fun teardown() {
        confirmVerified(userRepository)
    }

    @Test
    fun `Given dependencies, When invoke called, Then repository called`() =
        runTest {
            // Given
            every { userRepository.getCategories() } returns mockk(relaxed = true)

            // When
            useCase().test {
                awaitComplete()
            }

            // Then
            coVerifySequence {
                userRepository.getCategories()
            }
        }
}