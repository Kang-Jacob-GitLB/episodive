package io.jacob.episodive.core.datastore.datasource

import io.jacob.episodive.core.datastore.model.UserPreferences
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.LastPlayState
import io.jacob.episodive.core.model.Repeat
import kotlinx.coroutines.flow.Flow

interface UserPreferencesDataSource {
    suspend fun setFirstLaunch(isFirstLaunch: Boolean)
    suspend fun addCategory(category: Category)
    suspend fun addCategories(categories: List<Category>)
    suspend fun removeCategory(category: Category)
    fun getCategories(): Flow<List<Category>>
    suspend fun setSpeed(speed: Float)
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun saveLastPlayState(episodeId: Long, index: Int, positionMs: Long, shuffle: Boolean, repeat: Repeat)
    suspend fun getLastPlayState(): LastPlayState?
    suspend fun clearLastPlayState()
}
