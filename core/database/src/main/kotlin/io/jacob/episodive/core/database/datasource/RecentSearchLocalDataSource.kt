package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.model.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

interface RecentSearchLocalDataSource {
    fun getRecentSearches(limit: Int): Flow<List<RecentSearchEntity>>
    suspend fun upsertRecentSearch(recentSearch: RecentSearchEntity)
    suspend fun deleteRecentSearch(id: Long)
    suspend fun clearRecentSearches()
}