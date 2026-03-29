package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.RecentSearchDao
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

class RecentSearchLocalDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recentSearchDao = mockk<RecentSearchDao>(relaxed = true)

    private val dataSource: RecentSearchLocalDataSource = RecentSearchLocalDataSourceImpl(
        recentSearchDao = recentSearchDao
    )

    @After
    fun teardown() {
        confirmVerified(recentSearchDao)
    }

    @Test
    fun `Given dependencies, When getRecentSearches called, Then called dao method`() =
        runTest {
            // Given
            coEvery {
                recentSearchDao.getRecentSearches(any())
            } returns mockk()

            // When
            dataSource.getRecentSearches(3)

            // Then
            coVerifySequence {
                recentSearchDao.getRecentSearches(3)
            }
        }

    @Test
    fun `Given dependencies, When upsertRecentSearch called, Then called dao method`() =
        runTest {
            // Given
            coEvery {
                recentSearchDao.upsertRecentSearch(any())
            } just Runs

            // When
            dataSource.upsertRecentSearch(mockk())

            // Then
            coVerifySequence {
                recentSearchDao.upsertRecentSearch(any())
            }
        }

    @Test
    fun `Given dependencies, When deleteRecentSearch called, Then called dao method`() =
        runTest {
            // Given
            coEvery {
                recentSearchDao.deleteRecentSearch(any())
            } just Runs

            // When
            dataSource.deleteRecentSearch(1L)

            // Then
            coVerifySequence {
                recentSearchDao.deleteRecentSearch(any())
            }
        }

    @Test
    fun `Given dependencies, When clearRecentSearches called, Then called dao method`() =
        runTest {
            // Given
            coEvery {
                recentSearchDao.clearRecentSearches()
            } just Runs

            // When
            dataSource.clearRecentSearches()

            // Then
            coVerifySequence {
                recentSearchDao.clearRecentSearches()
            }
        }
}