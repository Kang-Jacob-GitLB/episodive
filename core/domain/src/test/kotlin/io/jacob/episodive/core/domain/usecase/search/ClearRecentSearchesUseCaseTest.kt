package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ClearRecentSearchesUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recentSearchRepository = mockk<RecentSearchRepository>(relaxed = true)

    private val useCase = ClearRecentSearchesUseCase(
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
            coEvery { recentSearchRepository.clearRecentSearches() } just Runs

            // When
            useCase()

            // Then
            coVerifySequence {
                recentSearchRepository.clearRecentSearches()
            }
        }
}