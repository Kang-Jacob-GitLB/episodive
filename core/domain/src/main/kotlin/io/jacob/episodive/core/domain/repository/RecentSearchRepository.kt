package io.jacob.episodive.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface RecentSearchRepository {
    fun getRecentSearches(limit: Int): Flow<List<String>>
    suspend fun upsertRecentSearch(query: String)
    suspend fun deleteRecentSearch(query: String)
    suspend fun clearRecentSearches()
    suspend fun getRecentSearchesCount(): Int
}