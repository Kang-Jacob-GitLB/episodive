package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.datastore.datasource.UserPreferencesDataSource
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UserRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userPreferencesDataSource = mockk<UserPreferencesDataSource>(relaxed = true)

    private val repository: UserRepository = UserRepositoryImpl(
        userPreferencesDataSource = userPreferencesDataSource
    )

    @After
    fun teardown() {
        confirmVerified(userPreferencesDataSource)
    }

    @Test
    fun `Given dependencies, When setFirstLaunch, Then call data source's method`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.setFirstLaunch(any()) } just Runs

            // When
            repository.setFirstLaunch(false)

            // Then
            coVerify { userPreferencesDataSource.setFirstLaunch(false) }
        }

    @Test
    fun `Given dependencies, When addCategory, Then call data source's method`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.addCategory(any()) } just Runs

            // When
            repository.addCategory(Category.ARTS)

            // Then
            coVerify { userPreferencesDataSource.addCategory(Category.ARTS) }
        }

    @Test
    fun `Given dependencies, When addCategories, Then call data source's method`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.addCategories(any()) } just Runs

            // When
            repository.addCategories(emptyList())

            // Then
            coVerify { userPreferencesDataSource.addCategories(emptyList()) }
        }

    @Test
    fun `Given dependencies, When removeCategory, Then call data source's method`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.removeCategory(any()) } just Runs

            // When
            repository.removeCategory(Category.ARTS)

            // Then
            coVerify { userPreferencesDataSource.removeCategory(Category.ARTS) }
        }

    @Test
    fun `Given dependencies and contain, When toggleCategory, Then call data source's methods`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.getCategories() } returns flowOf(listOf(Category.ARTS))
            coEvery { userPreferencesDataSource.removeCategory(any()) } just Runs

            // When
            val result = repository.toggleCategory(Category.ARTS)

            // Then
            assertFalse(result)
            coVerifySequence {
                userPreferencesDataSource.getCategories()
                userPreferencesDataSource.removeCategory(Category.ARTS)
            }
        }

    @Test
    fun `Given dependencies and not contain, When toggleCategory, Then call data source's methods`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.getCategories() } returns flowOf(emptyList())
            coEvery { userPreferencesDataSource.addCategory(any()) } just Runs

            // When
            val result = repository.toggleCategory(Category.ARTS)

            // Then
            assertTrue(result)
            coVerifySequence {
                userPreferencesDataSource.getCategories()
                userPreferencesDataSource.addCategory(Category.ARTS)
            }
        }

    @Test
    fun `Given dependencies, When getCategories, Then call data source's method`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.getCategories() } returns mockk()

            // When
            repository.getCategories()

            // Then
            coVerify { userPreferencesDataSource.getCategories() }
        }

    @Test
    fun `Given dependencies, When getUserData, Then call data source's method`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.getUserPreferences() } returns mockk()

            // When
            repository.getUserData()

            // Then
            coVerify { userPreferencesDataSource.getUserPreferences() }
        }

    @Test
    fun `Given dependencies, When saveLastPlayState, Then call data source's method`() =
        runTest {
            // Given
            coEvery {
                userPreferencesDataSource.saveLastPlayState(any(), any(), any(), any(), any())
            } just Runs

            // When
            repository.saveLastPlayState(123L, 2, 5000L, true, Repeat.ONE)

            // Then
            coVerify {
                userPreferencesDataSource.saveLastPlayState(123L, 2, 5000L, true, Repeat.ONE)
            }
        }

    @Test
    fun `Given dependencies, When getLastPlayState, Then call data source's method`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.getLastPlayState() } returns null

            // When
            repository.getLastPlayState()

            // Then
            coVerify { userPreferencesDataSource.getLastPlayState() }
        }

    @Test
    fun `Given dependencies, When clearLastPlayState, Then call data source's method`() =
        runTest {
            // Given
            coEvery { userPreferencesDataSource.clearLastPlayState() } just Runs

            // When
            repository.clearLastPlayState()

            // Then
            coVerify { userPreferencesDataSource.clearLastPlayState() }
        }
}
