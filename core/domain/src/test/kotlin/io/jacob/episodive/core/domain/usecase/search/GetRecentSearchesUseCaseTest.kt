package io.jacob.episodive.core.domain.usecase.search

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GetRecentSearchesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recentSearchRepository = mockk<RecentSearchRepository>(relaxed = true)

    private val useCase = GetRecentSearchesUseCase(
        recentSearchRepository = recentSearchRepository
    )

    @After
    fun teardown() {
        confirmVerified(recentSearchRepository)
    }

    @Test
    fun `Given dependencies, When invoked, Then repository is called`() =
        runTest {
            // Given
            coEvery {
                recentSearchRepository.getRecentSearches(any())
            } returns flowOf(listOf("test1", "test2", "test3"))

            // When
            useCase().test {
                // Then
                val result = awaitItem()
                assertEquals(listOf("test1", "test2", "test3"), result)
                awaitComplete()
            }
            coVerifySequence {
                recentSearchRepository.getRecentSearches(5)
            }
        }
}