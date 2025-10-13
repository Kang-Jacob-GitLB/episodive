package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class DeleteRecentSearchUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recentSearchRepository = mockk<RecentSearchRepository>(relaxed = true)

    private val useCase = DeleteRecentSearchUseCase(
        recentSearchRepository = recentSearchRepository,
    )

    @After
    fun teardown() {
        confirmVerified(recentSearchRepository)
    }

    @Test
    fun `Given dependencies, When invoke called, Then repository called`() =
        runTest {
            // Given
            val query = "test"
            coEvery { recentSearchRepository.deleteRecentSearch(any()) } just Runs

            // When
            useCase(query)

            // Then
            coVerifySequence {
                recentSearchRepository.deleteRecentSearch(query)
            }
        }
}