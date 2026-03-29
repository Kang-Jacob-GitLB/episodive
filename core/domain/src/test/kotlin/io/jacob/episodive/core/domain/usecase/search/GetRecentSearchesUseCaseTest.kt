package io.jacob.episodive.core.domain.usecase.search

import app.cash.turbine.test
import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.model.RecentSearch
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
            val now = kotlin.time.Clock.System.now()
            val recentSearches = listOf(
                RecentSearch.Query(id = 1, query = "test1", searchedAt = now),
                RecentSearch.Query(id = 2, query = "test2", searchedAt = now),
                RecentSearch.Query(id = 3, query = "test3", searchedAt = now),
            )
            coEvery {
                recentSearchRepository.getRecentSearches(any())
            } returns flowOf(recentSearches)

            // When
            useCase(10).test {
                // Then
                val result = awaitItem()
                assertEquals(recentSearches, result)
                awaitComplete()
            }
            coVerifySequence {
                recentSearchRepository.getRecentSearches(10)
            }
        }
}