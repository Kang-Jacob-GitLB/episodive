package io.jacob.episodive.core.datastore.datasource

import io.jacob.episodive.core.datastore.model.UserPreferences
import io.jacob.episodive.core.datastore.store.UserPreferencesStore
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.LastPlayState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPreferencesDataSourceImpl @Inject constructor(
    private val store: UserPreferencesStore
) : UserPreferencesDataSource {
    override suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        store.setFirstLaunch(isFirstLaunch)
    }

    override suspend fun addCategory(category: Category) {
        store.addCategory(category)
    }

    override suspend fun addCategories(categories: List<Category>) {
        store.addCategories(categories)
    }

    override suspend fun removeCategory(category: Category) {
        store.removeCategory(category)
    }

    override fun getCategories(): Flow<List<Category>> {
        return store.getCategories()
    }

    override suspend fun setSpeed(speed: Float) {
        store.setSpeed(speed)
    }

    override fun getUserPreferences(): Flow<UserPreferences> {
        return store.getUserPreferences()
    }

    override suspend fun saveLastPlayState(
        episodeId: Long,
        index: Int,
        positionMs: Long,
        shuffle: Boolean,
        repeat: Int,
    ) {
        store.saveLastPlayState(episodeId, index, positionMs, shuffle, repeat)
    }

    override suspend fun getLastPlayState(): LastPlayState? {
        return store.getLastPlayState()
    }

    override suspend fun clearLastPlayState() {
        store.clearLastPlayState()
    }
}