package io.jacob.episodive.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Upsert
    suspend fun upsertTrendingFeeds(feeds: List<TrendingFeedEntity>)

    @Upsert
    suspend fun upsertRecentFeeds(feeds: List<RecentFeedEntity>)

    @Upsert
    suspend fun upsertRecentNewFeeds(feeds: List<RecentNewFeedEntity>)

    @Upsert
    suspend fun upsertSoundbites(soundbites: List<SoundbiteEntity>)

    @Query("DELETE FROM trending_feeds WHERE id = :id")
    suspend fun deleteTrendingFeed(id: Long)

    @Query("DELETE FROM trending_feeds")
    suspend fun deleteTrendingFeeds()

    @Query("DELETE FROM trending_feeds WHERE cacheKey = :cacheKey")
    suspend fun deleteTrendingFeedsByCacheKey(cacheKey: String)

    @Query("DELETE FROM recent_feeds WHERE id = :id")
    suspend fun deleteRecentFeed(id: Long)

    @Query("DELETE FROM recent_feeds")
    suspend fun deleteRecentFeeds()

    @Query("DELETE FROM recent_feeds WHERE cacheKey = :cacheKey")
    suspend fun deleteRecentFeedsByCacheKey(cacheKey: String)

    @Query("DELETE FROM recent_new_feeds WHERE id = :id")
    suspend fun deleteRecentNewFeed(id: Long)

    @Query("DELETE FROM recent_new_feeds")
    suspend fun deleteRecentNewFeeds()

    @Query("DELETE FROM recent_new_feeds WHERE cacheKey = :cacheKey")
    suspend fun deleteRecentNewFeedsByCacheKey(cacheKey: String)

    @Query("DELETE FROM soundbites WHERE episodeId = :episodeId")
    suspend fun deleteSoundbite(episodeId: Long)

    @Query("DELETE FROM soundbites")
    suspend fun deleteSoundbites()

    @Query("DELETE FROM soundbites WHERE cacheKey = :cacheKey")
    suspend fun deleteSoundbitesByCacheKey(cacheKey: String)

    @Query("SELECT * FROM trending_feeds WHERE cacheKey = :cacheKey")
    fun getTrendingFeedsByCacheKey(cacheKey: String): Flow<List<TrendingFeedEntity>>

    @Query("SELECT * FROM recent_feeds WHERE cacheKey = :cacheKey")
    fun getRecentFeedsByCacheKey(cacheKey: String): Flow<List<RecentFeedEntity>>

    @Query("SELECT * FROM recent_new_feeds WHERE cacheKey = :cacheKey")
    fun getRecentNewFeedsByCacheKey(cacheKey: String): Flow<List<RecentNewFeedEntity>>

    @Query("SELECT * FROM soundbites WHERE cacheKey = :cacheKey")
    fun getSoundbitesByCacheKey(cacheKey: String): Flow<List<SoundbiteEntity>>
}