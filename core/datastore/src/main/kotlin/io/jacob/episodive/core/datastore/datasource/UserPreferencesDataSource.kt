package io.jacob.episodive.core.datastore.datasource

import io.jacob.episodive.core.datastore.model.UserPreferences
import io.jacob.episodive.core.model.Category
import kotlinx.coroutines.flow.Flow

interface UserPreferencesDataSource {
    suspend fun setFirstLaunch(isFirstLaunch: Boolean)
    suspend fun addCategory(category: Category)
    suspend fun addCategories(categories: List<Category>)
    suspend fun removeCategory(category: Category)
    fun getCategories(): Flow<List<Category>>
    suspend fun setSpeed(speed: Float)
    fun getUserPreferences(): Flow<UserPreferences>
}