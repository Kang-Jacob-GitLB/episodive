package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.database.datasource.RecentSearchLocalDataSource
import io.jacob.episodive.core.database.model.RecentSearchEntity
import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock

class RecentSearchRepositoryImpl @Inject constructor(
    private val recentSearchLocalDataSource: RecentSearchLocalDataSource,
) : RecentSearchRepository {
    override fun getRecentSearches(limit: Int): Flow<List<String>> {
        return recentSearchLocalDataSource.getRecentSearches(limit).map { recentSearches ->
            recentSearches.map { it.query }
        }
    }

    override suspend fun upsertRecentSearch(query: String) {
        recentSearchLocalDataSource.upsertRecentSearch(
            RecentSearchEntity(
                query = query,
                searchedAt = Clock.System.now(),
            )
        )
    }

    override suspend fun deleteRecentSearch(query: String) {
        recentSearchLocalDataSource.deleteRecentSearch(query)
    }

    override suspend fun clearRecentSearches() {
        recentSearchLocalDataSource.clearRecentSearches()
    }
}