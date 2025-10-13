package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.database.datasource.RecentSearchLocalDataSource
import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecentSearchRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recentSearchLocalDataSource = mockk<RecentSearchLocalDataSource>(relaxed = true)

    private val repository: RecentSearchRepository = RecentSearchRepositoryImpl(
        recentSearchLocalDataSource = recentSearchLocalDataSource
    )

    @After
    fun teardown() {
        confirmVerified(recentSearchLocalDataSource)
    }

    @Test
    fun `Given dependencies, When getRecentSearches called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.getRecentSearches(any())
            } returns mockk()

            // When
            repository.getRecentSearches(3)

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearches(3)
            }
        }

    @Test
    fun `Given dependencies, When upsertRecentSearch called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.upsertRecentSearch(any())
            } just Runs

            // When
            repository.upsertRecentSearch("query")

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.upsertRecentSearch(any())
            }
        }

    @Test
    fun `Given dependencies, When deleteRecentSearch called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.deleteRecentSearch(any())
            } just Runs

            // When
            repository.deleteRecentSearch("query")

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.deleteRecentSearch(any())
            }
        }

    @Test
    fun `Given dependencies, When clearRecentSearches called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.clearRecentSearches()
            } just Runs

            // When
            repository.clearRecentSearches()

            // Then
            coVerifySequence {
                recentSearchLocalDataSource.clearRecentSearches()
            }
        }

    @Test
    fun `Given dependencies, When getRecentSearchesCount called, Then call dataSource method`() =
        runTest {
            // Given
            coEvery {
                recentSearchLocalDataSource.getRecentSearchesCount()
            } returns 5

            // When
            val result = repository.getRecentSearchesCount()

            // Then
            assertEquals(5, result)
            coVerifySequence {
                recentSearchLocalDataSource.getRecentSearchesCount()
            }
        }
}