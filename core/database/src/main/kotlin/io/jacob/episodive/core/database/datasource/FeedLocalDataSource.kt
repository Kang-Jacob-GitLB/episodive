package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import kotlinx.coroutines.flow.Flow

interface FeedLocalDataSource {
    suspend fun upsertTrendingFeeds(feeds: List<TrendingFeedEntity>)
    suspend fun upsertRecentFeeds(feeds: List<RecentFeedEntity>)
    suspend fun upsertRecentNewFeeds(feeds: List<RecentNewFeedEntity>)
    suspend fun upsertSoundbites(soundbites: List<SoundbiteEntity>)
    suspend fun deleteTrendingFeed(id: Long)
    suspend fun deleteTrendingFeeds()
    suspend fun deleteTrendingFeedsByCacheKey(cacheKey: String)
    suspend fun deleteRecentFeed(id: Long)
    suspend fun deleteRecentFeeds()
    suspend fun deleteRecentFeedsByCacheKey(cacheKey: String)
    suspend fun deleteRecentNewFeed(id: Long)
    suspend fun deleteRecentNewFeeds()
    suspend fun deleteRecentNewFeedsByCacheKey(cacheKey: String)
    suspend fun deleteSoundbite(episodeId: Long)
    suspend fun deleteSoundbites()
    suspend fun deleteSoundbitesByCacheKey(cacheKey: String)
    fun getTrendingFeedsByCacheKey(cacheKey: String): Flow<List<TrendingFeedEntity>>
    fun getRecentFeedsByCacheKey(cacheKey: String): Flow<List<RecentFeedEntity>>
    fun getRecentNewFeedsByCacheKey(cacheKey: String): Flow<List<RecentNewFeedEntity>>
    fun getSoundbitesByCacheKey(cacheKey: String): Flow<List<SoundbiteEntity>>
}