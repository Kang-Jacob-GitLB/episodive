package io.jacob.episodive.core.datastore.datasource

import io.jacob.episodive.core.datastore.store.UserPreferencesStore
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class UserPreferencesDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = mockk<UserPreferencesStore>(relaxed = true)

    private val dataSource: UserPreferencesDataSource = UserPreferencesDataSourceImpl(
        store = store,
    )

    @After
    fun teardown() {
        confirmVerified(store)
    }

    @Test
    fun `Given dependencies, When setFirstLaunch, Then call store's method`() =
        runTest {
            // Given
            coEvery { store.setFirstLaunch(any()) } just Runs

            // When
            dataSource.setFirstLaunch(false)

            // Then
            coVerify { store.setFirstLaunch(false) }
        }

    @Test
    fun `Given dependencies, When addCategory, Then call store's method`() =
        runTest {
            // Given
            coEvery { store.addCategory(any()) } just Runs

            // When
            dataSource.addCategory(Category.ARTS)

            // Then
            coVerify { store.addCategory(Category.ARTS) }
        }

    @Test
    fun `Given dependencies, When removeCategory, Then call store's method`() =
        runTest {
            // Given
            coEvery { store.removeCategory(any()) } just Runs

            // When
            dataSource.removeCategory(Category.ARTS)

            // Then
            coVerify { store.removeCategory(Category.ARTS) }
        }

    @Test
    fun `Given dependencies, When addCategories, Then call store's method`() =
        runTest {
            // Given
            coEvery { store.addCategories(any()) } just Runs

            // When
            dataSource.addCategories(listOf(Category.CAREERS, Category.SCIENCE))

            // Then
            coVerify { store.addCategories(listOf(Category.CAREERS, Category.SCIENCE)) }
        }

    @Test
    fun `Given dependencies, When getCategories, Then call store's method`() =
        runTest {
            // Given
            coEvery { store.getCategories() } returns mockk()

            // When
            dataSource.getCategories()

            // Then
            coVerify { store.getCategories() }
        }

    @Test
    fun `Given dependencies, When setSpeed, Then call store's method`() =
        runTest {
            // Given
            coEvery { store.setSpeed(any()) } just Runs

            // When
            dataSource.setSpeed(1f)

            // Then
            coVerify { store.setSpeed(1f) }
        }

    @Test
    fun `Given dependencies, When getUserPreferences, Then call store's method`() =
        runTest {
            // Given
            coEvery { store.getUserPreferences() } returns mockk()

            // When
            dataSource.getUserPreferences()

            // Then
            coVerify { store.getUserPreferences() }
        }
}