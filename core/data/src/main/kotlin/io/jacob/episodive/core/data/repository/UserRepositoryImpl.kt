package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.datastore.datasource.UserPreferencesDataSource
import io.jacob.episodive.core.datastore.mapper.toUserData
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.LastPlayState
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.model.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : UserRepository {
    override suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        userPreferencesDataSource.setFirstLaunch(isFirstLaunch)
    }

    override suspend fun addCategory(category: Category) {
        userPreferencesDataSource.addCategory(category)
    }

    override suspend fun addCategories(categories: List<Category>) {
        userPreferencesDataSource.addCategories(categories)
    }

    override suspend fun removeCategory(category: Category) {
        userPreferencesDataSource.removeCategory(category)
    }

    override suspend fun toggleCategory(category: Category): Boolean {
        val currentCategories = userPreferencesDataSource.getCategories().first()
        return if (currentCategories.contains(category)) {
            userPreferencesDataSource.removeCategory(category)
            false
        } else {
            userPreferencesDataSource.addCategory(category)
            true
        }
    }

    override fun getCategories(): Flow<List<Category>> {
        return userPreferencesDataSource.getCategories()
    }

    override suspend fun setSpeed(speed: Float) {
        userPreferencesDataSource.setSpeed(speed)
    }

    override fun getUserData(): Flow<UserData> {
        return userPreferencesDataSource.getUserPreferences()
            .map { it.toUserData() }
    }

    override suspend fun saveLastPlayState(
        episodeId: Long,
        index: Int,
        positionMs: Long,
        shuffle: Boolean,
        repeat: Repeat,
    ) {
        userPreferencesDataSource.saveLastPlayState(episodeId, index, positionMs, shuffle, repeat)
    }

    override suspend fun getLastPlayState(): LastPlayState? {
        return userPreferencesDataSource.getLastPlayState()
    }

    override suspend fun clearLastPlayState() {
        userPreferencesDataSource.clearLastPlayState()
    }
}
