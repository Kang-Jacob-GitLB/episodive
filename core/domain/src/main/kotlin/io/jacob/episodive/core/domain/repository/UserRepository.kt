package io.jacob.episodive.core.domain.repository

import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.LastPlayState
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.model.UserData
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun setFirstLaunch(isFirstLaunch: Boolean)
    suspend fun addCategory(category: Category)
    suspend fun addCategories(categories: List<Category>)
    suspend fun removeCategory(category: Category)
    suspend fun toggleCategory(category: Category): Boolean
    fun getCategories(): Flow<List<Category>>
    suspend fun setSpeed(speed: Float)
    fun getUserData(): Flow<UserData>
    suspend fun saveLastPlayState(episodeId: Long, index: Int, positionMs: Long, shuffle: Boolean, repeat: Repeat)
    suspend fun getLastPlayState(): LastPlayState?
    suspend fun clearLastPlayState()
}
