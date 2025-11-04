package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.RecentSearchDao
import io.jacob.episodive.core.database.model.RecentSearchEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecentSearchLocalDataSourceImpl @Inject constructor(
    private val recentSearchDao: RecentSearchDao,
) : RecentSearchLocalDataSource {
    override fun getRecentSearches(limit: Int): Flow<List<RecentSearchEntity>> {
        return recentSearchDao.getRecentSearches(limit)
    }

    override suspend fun upsertRecentSearch(recentSearch: RecentSearchEntity) {
        recentSearchDao.upsertRecentSearch(recentSearch)
    }

    override suspend fun deleteRecentSearch(query: String) {
        recentSearchDao.deleteRecentSearch(query)
    }

    override suspend fun clearRecentSearches() {
        recentSearchDao.clearRecentSearches()
    }
}