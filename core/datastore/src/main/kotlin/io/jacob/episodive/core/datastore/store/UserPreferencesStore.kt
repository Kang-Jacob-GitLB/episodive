package io.jacob.episodive.core.datastore.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.jacob.episodive.core.datastore.model.UserPreferences
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.mapper.toCategories
import io.jacob.episodive.core.model.mapper.toCommaString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject

class UserPreferencesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    object UserPreferencesKeys {
        val isFirstLaunch = booleanPreferencesKey("is_first_launch")
        val categories = stringPreferencesKey("categories")
        val speed = floatPreferencesKey("speed")
    }

    suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        dataStore.edit { it[UserPreferencesKeys.isFirstLaunch] = isFirstLaunch }
    }

    suspend fun addCategory(category: Category) {
        dataStore.edit { preferences ->
            val currentCategories =
                preferences[UserPreferencesKeys.categories]?.toCategories()?.toMutableList()
                    ?: mutableListOf()
            if (!currentCategories.contains(category)) {
                currentCategories.add(category)
                preferences[UserPreferencesKeys.categories] = currentCategories.toCommaString()
            }
        }
    }

    suspend fun addCategories(categories: List<Category>) {
        dataStore.edit { preferences ->
            val currentCategories =
                preferences[UserPreferencesKeys.categories]?.toCategories() ?: emptyList()
            val mergedCategories = (currentCategories + categories).distinct()
            preferences[UserPreferencesKeys.categories] = mergedCategories.toCommaString()
        }
    }

    suspend fun removeCategory(category: Category) {
        dataStore.edit { preferences ->
            val currentCategories =
                preferences[UserPreferencesKeys.categories]?.toCategories()?.toMutableList()
                    ?: mutableListOf()
            if (currentCategories.remove(category)) {
                preferences[UserPreferencesKeys.categories] = currentCategories.toCommaString()
            }
        }
    }

    fun getCategories(): Flow<List<Category>> =
        dataStore.data.map { preferences ->
            preferences[UserPreferencesKeys.categories]?.toCategories() ?: emptyList()
        }

    suspend fun setSpeed(speed: Float) {
        dataStore.edit { it[UserPreferencesKeys.speed] = speed }
    }

    fun getUserPreferences(): Flow<UserPreferences> =
        dataStore.data.map { preferences ->
            UserPreferences(
                isFirstLaunch = preferences[UserPreferencesKeys.isFirstLaunch] ?: true,
                language = Locale.getDefault().language,
                categories = preferences[UserPreferencesKeys.categories]?.toCategories()
                    ?: emptyList(),
                speed = preferences[UserPreferencesKeys.speed] ?: 1f
            )
        }
}