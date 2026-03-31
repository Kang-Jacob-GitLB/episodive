package io.jacob.episodive.core.datastore.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.testing.util.DisabledOnWindows
import io.jacob.episodive.core.testing.util.DisabledOnWindowsRule
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @get:Rule
    val disabledOnWindowsRule = DisabledOnWindowsRule()

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

    private fun createFreshDataStore(name: String): DataStore<Preferences> {
        val file = File(tmpFolder.root, "${name}.preferences_pb")
        return PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { file }
        )
    }

    private fun createFreshStore(name: String): UserPreferencesStore {
        return UserPreferencesStore(createFreshDataStore(name))
    }

    @Test
    fun setFirstLaunch_updatesFirstLaunchPreference() = runTest {
        userPreferencesStore.setFirstLaunch(false)

        val result = userPreferencesStore.getUserPreferences().first()
        assertFalse(result.isFirstLaunch)
    }

    @Test
    @DisabledOnWindows
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
    @DisabledOnWindows
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
    @DisabledOnWindows
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
    fun setSpeed_updatesSpeedPreference() = runTest {
        userPreferencesStore.setSpeed(1.5f)

        val result = userPreferencesStore.getUserPreferences().first()
        assertEquals(1.5f, result.speed)
    }


    @Test
    fun getUserPreferences_withDefaultValues_returnsDefaults() = runTest {
        val result = userPreferencesStore.getUserPreferences().first()

        assertTrue(result.isFirstLaunch)
        assertEquals(Locale.getDefault().language, result.language)
        assertTrue(result.categories.isEmpty())
    }

    @Test
    @DisabledOnWindows
    fun removeCategory_whenCategoryDoesNotExist_doesNotChangeCategories() = runTest {
        userPreferencesStore.addCategories(listOf(Category.ARTS, Category.SCIENCE))

        userPreferencesStore.removeCategory(Category.TECHNOLOGY)

        val result = userPreferencesStore.getUserPreferences().first()
        assertEquals(2, result.categories.size)
        assertTrue(result.categories.contains(Category.ARTS))
        assertTrue(result.categories.contains(Category.SCIENCE))
    }

    @Test
    @DisabledOnWindows
    fun setSpeed_withDifferentValues_updatesCorrectly() = runTest {
        userPreferencesStore.setSpeed(0.5f)
        var result = userPreferencesStore.getUserPreferences().first()
        assertEquals(0.5f, result.speed)

        userPreferencesStore.setSpeed(2.0f)
        result = userPreferencesStore.getUserPreferences().first()
        assertEquals(2.0f, result.speed)
    }

    @Test
    @DisabledOnWindows
    fun setFirstLaunch_toTrue_updatesCorrectly() = runTest {
        userPreferencesStore.setFirstLaunch(false)
        var result = userPreferencesStore.getUserPreferences().first()
        assertFalse(result.isFirstLaunch)

        userPreferencesStore.setFirstLaunch(true)
        result = userPreferencesStore.getUserPreferences().first()
        assertTrue(result.isFirstLaunch)
    }

    @Test
    @DisabledOnWindows
    fun addCategory_withDuplicateCategory_doesNotAddDuplicate() = runTest {
        userPreferencesStore.addCategory(Category.ARTS)
        userPreferencesStore.addCategory(Category.ARTS)

        val result = userPreferencesStore.getUserPreferences().first()
        assertEquals(1, result.categories.size)
        assertEquals(Category.ARTS, result.categories.first())
    }

    @Test
    fun saveLastPlayState_storesAllFields() = runTest {
        userPreferencesStore.saveLastPlayState(
            episodeId = 123L,
            index = 2,
            positionMs = 5000L,
            shuffle = true,
            repeat = Repeat.ONE,
        )

        val result = userPreferencesStore.getLastPlayState()
        assertNotNull(result)
        assertEquals(123L, result!!.episodeId)
        assertEquals(2, result.index)
        assertEquals(5000L, result.positionMs)
        assertTrue(result.shuffle)
        assertEquals(Repeat.ONE, result.repeat)
    }

    @Test
    fun getLastPlayState_withNoData_returnsNull() = runTest {
        val result = userPreferencesStore.getLastPlayState()
        assertNull(result)
    }

    @Test
    @DisabledOnWindows
    fun clearLastPlayState_removesAllFields() = runTest {
        userPreferencesStore.saveLastPlayState(
            episodeId = 123L,
            index = 2,
            positionMs = 5000L,
            shuffle = true,
            repeat = Repeat.ONE,
        )
        assertNotNull(userPreferencesStore.getLastPlayState())

        userPreferencesStore.clearLastPlayState()

        assertNull(userPreferencesStore.getLastPlayState())
    }

    @Test
    @DisabledOnWindows
    fun saveLastPlayState_overwritesPreviousState() = runTest {
        userPreferencesStore.saveLastPlayState(
            episodeId = 100L,
            index = 0,
            positionMs = 1000L,
            shuffle = false,
            repeat = Repeat.OFF,
        )

        userPreferencesStore.saveLastPlayState(
            episodeId = 200L,
            index = 3,
            positionMs = 9000L,
            shuffle = true,
            repeat = Repeat.ALL,
        )

        val result = userPreferencesStore.getLastPlayState()
        assertNotNull(result)
        assertEquals(200L, result!!.episodeId)
        assertEquals(3, result.index)
        assertEquals(9000L, result.positionMs)
        assertTrue(result.shuffle)
        assertEquals(Repeat.ALL, result.repeat)
    }

    @Test
    @DisabledOnWindows
    fun getLastPlayState_withOnlyEpisodeId_returnsDefaults() = runTest {
        dataStore.edit {
            it[UserPreferencesStore.UserPreferencesKeys.lastPlayingEpisodeId] = 999L
        }

        val result = userPreferencesStore.getLastPlayState()
        assertNotNull(result)
        assertEquals(999L, result!!.episodeId)
        assertEquals(0, result.index)
        assertEquals(0L, result.positionMs)
        assertFalse(result.shuffle)
        assertEquals(Repeat.OFF, result.repeat)
    }

    @Test
    fun getUserPreferences_withDefaultSpeed_returnsOnePointZero() = runTest {
        val result = userPreferencesStore.getUserPreferences().first()
        assertEquals(1f, result.speed)
    }

    @Test
    fun addCategory_singleCategory_storesInPreferences() = runTest {
        userPreferencesStore.addCategory(Category.SCIENCE)

        val result = userPreferencesStore.getCategories().first()
        assertEquals(1, result.size)
        assertEquals(Category.SCIENCE, result.first())
    }

    @Test
    fun addCategory_duplicateCategory_doesNotDuplicate() = runTest {
        val store = createFreshStore("add_dup_cat")
        store.addCategory(Category.HEALTH)
        store.addCategory(Category.HEALTH)

        val result = store.getCategories().first()
        assertEquals(1, result.size)
        assertEquals(Category.HEALTH, result.first())
    }

    @Test
    @DisabledOnWindows
    fun removeCategory_existingCategory_removesIt2() = runTest {
        userPreferencesStore.addCategories(listOf(Category.ARTS, Category.TECHNOLOGY))
        userPreferencesStore.removeCategory(Category.ARTS)

        val result = userPreferencesStore.getCategories().first()
        assertEquals(1, result.size)
        assertEquals(Category.TECHNOLOGY, result.first())
    }

    @Test
    fun removeCategory_fromEmptyCategories_noOp() = runTest {
        // removeCategory on empty categories (no prior write) covers the
        // mutableListOf() default path + contains(category)==false branch
        userPreferencesStore.removeCategory(Category.COMEDY)

        val result = userPreferencesStore.getCategories().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getLastPlayState_withOnlyEpisodeIdSet_returnsDefaultsForOtherFields() = runTest {
        dataStore.edit {
            it[UserPreferencesStore.UserPreferencesKeys.lastPlayingEpisodeId] = 42L
        }

        val result = userPreferencesStore.getLastPlayState()
        assertNotNull(result)
        assertEquals(42L, result!!.episodeId)
        assertEquals(0, result.index)
        assertEquals(0L, result.positionMs)
        assertFalse(result.shuffle)
        assertEquals(Repeat.OFF, result.repeat)
    }

    @Test
    fun clearLastPlayState_removesAllFieldsFromEmptyState() = runTest {
        // clearLastPlayState on empty state verifies it doesn't throw
        userPreferencesStore.clearLastPlayState()

        assertNull(userPreferencesStore.getLastPlayState())
    }

    @Test
    @DisabledOnWindows
    fun clearLastPlayState_afterSaving_returnsNull2() = runTest {
        userPreferencesStore.saveLastPlayState(
            episodeId = 555L,
            index = 1,
            positionMs = 3000L,
            shuffle = false,
            repeat = Repeat.ALL,
        )
        userPreferencesStore.clearLastPlayState()

        assertNull(userPreferencesStore.getLastPlayState())
    }

    @Test
    fun saveLastPlayState_thenGetReturnsDefaultsForUnsetFields() = runTest {
        // Given - save with known defaults for index, position, shuffle, repeat
        userPreferencesStore.saveLastPlayState(
            episodeId = 999L,
            index = 0,
            positionMs = 0L,
            shuffle = false,
            repeat = Repeat.OFF,
        )

        // When
        val result = userPreferencesStore.getLastPlayState()

        // Then - verify default-like values are returned correctly
        assertNotNull(result)
        assertEquals(999L, result!!.episodeId)
        assertEquals(0, result.index)
        assertEquals(0L, result.positionMs)
        assertFalse(result.shuffle)
        assertEquals(Repeat.OFF, result.repeat)
    }

    @Test
    fun setFirstLaunch_false_thenGetUserPreferences_returnsNotFirstLaunch() = runTest {
        // Given
        userPreferencesStore.setFirstLaunch(false)

        // When
        val result = userPreferencesStore.getUserPreferences().first()

        // Then
        assertFalse(result.isFirstLaunch)
        assertEquals(1f, result.speed)
        assertTrue(result.categories.isEmpty())
    }
}
