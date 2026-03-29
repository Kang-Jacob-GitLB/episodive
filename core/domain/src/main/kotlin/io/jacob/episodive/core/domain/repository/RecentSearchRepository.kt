package io.jacob.episodive.core.domain.repository

import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.RecentSearch
import kotlinx.coroutines.flow.Flow

interface RecentSearchRepository {
    fun getRecentSearches(limit: Int): Flow<List<RecentSearch>>
    suspend fun upsertRecentSearch(query: String)
    suspend fun upsertRecentSearch(podcast: Podcast)
    suspend fun upsertRecentSearch(episode: Episode)
    suspend fun deleteRecentSearch(recentSearch: RecentSearch)
    suspend fun clearRecentSearches()
}