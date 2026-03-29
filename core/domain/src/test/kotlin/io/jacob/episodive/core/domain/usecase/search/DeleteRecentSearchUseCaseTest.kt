package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.model.RecentSearch
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
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
            val recentSearch = RecentSearch.Query(id = 1, query = "test", searchedAt = kotlin.time.Clock.System.now())
            coEvery { recentSearchRepository.deleteRecentSearch(any()) } just Runs

            // When
            useCase(recentSearch)

            // Then
            coVerifySequence {
                recentSearchRepository.deleteRecentSearch(recentSearch)
            }
        }
}