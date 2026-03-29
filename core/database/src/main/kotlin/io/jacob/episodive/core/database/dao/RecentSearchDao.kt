package io.jacob.episodive.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecentSearches(limit: Int): Flow<List<RecentSearchEntity>>

    @Upsert
    suspend fun upsertRecentSearch(recentSearch: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE id = :id")
    suspend fun deleteRecentSearch(id: Long)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}