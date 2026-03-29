package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.RecentSearchRepository
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

class UpsertRecentSearchUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recentSearchRepository = mockk<RecentSearchRepository>(relaxed = true)

    private val useCase = UpsertRecentSearchUseCase(
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
            val query = "test"
            coEvery { recentSearchRepository.upsertRecentSearch(any<String>()) } just Runs

            // When
            useCase(query)

            // Then
            coVerifySequence {
                recentSearchRepository.upsertRecentSearch(query)
            }
        }
}