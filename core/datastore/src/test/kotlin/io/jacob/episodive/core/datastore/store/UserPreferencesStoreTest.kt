package io.jacob.episodive.core.datastore.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale

class UserPreferencesStoreTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var userPreferencesStore: UserPreferencesStore
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        val testFile = File(tmpFolder.root, "test_preferences.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        userPreferencesStore = UserPreferencesStore(dataStore)
    }

    @Test
    fun setFirstLaunch_updatesFirstLaunchPreference() = runTest {
        userPreferencesStore.setFirstLaunch(false)

        val result = userPreferencesStore.getUserPreferences().first()
        assertFalse(result.isFirstLaunch)
    }

    @Test
    fun addCategory_withSingleCategory_storesCorrectly() = runTest {
        userPreferencesStore.addCategory(Category.ARTS)

        val result1 = userPreferencesStore.getUserPreferences().first()
        assertEquals(1, result1.categories.size)
        assertEquals(Category.ARTS, result1.categories.first())


        userPreferencesStore.addCategory(Category.TECHNOLOGY)

        val result2 = userPreferencesStore.getUserPreferences().first()
        assertEquals(2, result2.categories.size)
        assertTrue(result2.categories.contains(Category.ARTS))
        assertTrue(result2.categories.contains(Category.TECHNOLOGY))
    }

    @Test
    fun removeCategory_removesCorrectly() = runTest {
        userPreferencesStore.addCategories(
            listOf(
                Category.ARTS,
                Category.TECHNOLOGY,
                Category.SCIENCE
            )
        )

        var result = userPreferencesStore.getUserPreferences().first()
        assertEquals(3, result.categories.size)

        userPreferencesStore.removeCategory(Category.TECHNOLOGY)

        result = userPreferencesStore.getUserPreferences().first()
        assertEquals(2, result.categories.size)
        assertTrue(result.categories.contains(Category.ARTS))
        assertTrue(result.categories.contains(Category.SCIENCE))
        assertFalse(result.categories.contains(Category.TECHNOLOGY))
    }

    @Test
    fun addCategories_withSingleCategory_storesCorrectly() = runTest {
        val categories = listOf(Category.TECHNOLOGY)
        userPreferencesStore.addCategories(categories)

        val result = userPreferencesStore.getUserPreferences().first()
        assertEquals(1, result.categories.size)
        assertEquals(Category.TECHNOLOGY, result.categories.first())
    }

    @Test
    fun addCategories_withMultipleCategories_storesCorrectly() = runTest {
        val categories = listOf(Category.TECHNOLOGY, Category.SCIENCE, Category.HEALTH)
        userPreferencesStore.addCategories(categories)

        val result = userPreferencesStore.getUserPreferences().first()
        assertEquals(3, result.categories.size)
        assertTrue(result.categories.contains(Category.TECHNOLOGY))
        assertTrue(result.categories.contains(Category.SCIENCE))
        assertTrue(result.categories.contains(Category.HEALTH))
    }

    @Test
    fun addCategories_withDuplicateCategories_storesDistinctly() = runTest {
        userPreferencesStore.addCategories(
            listOf(
                Category.TECHNOLOGY,
                Category.SCIENCE,
                Category.HEALTH
            )
        )
        userPreferencesStore.addCategories(listOf(Category.TECHNOLOGY, Category.ARTS))

        val result = userPreferencesStore.getUserPreferences().first()
        assertEquals(4, result.categories.size)
        assertTrue(result.categories.contains(Category.TECHNOLOGY))
        assertTrue(result.categories.contains(Category.SCIENCE))
        assertTrue(result.categories.contains(Category.HEALTH))
        assertTrue(result.categories.contains(Category.ARTS))
    }

    @Test
    fun getCategories_withNoCategories_returnsEmptyList() = runTest {
        val result = userPreferencesStore.getCategories().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getUserPreferences_withDefaultValues_returnsDefaults() = runTest {
        val result = userPreferencesStore.getUserPreferences().first()

        assertTrue(result.isFirstLaunch)
        assertEquals(Locale.getDefault().language, result.language)
        assertTrue(result.categories.isEmpty())
    }
}