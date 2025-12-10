package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
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
    suspend fun replaceTrendingFeeds(feeds: List<TrendingFeedEntity>)
    suspend fun replaceRecentFeeds(feeds: List<RecentFeedEntity>)
    suspend fun replaceRecentNewFeeds(feeds: List<RecentNewFeedEntity>)
    suspend fun replaceSoundbites(soundbites: List<SoundbiteEntity>)
    fun getTrendingFeedsByCacheKey(
        cacheKey: String,
        limit: Int = -1,
    ): Flow<List<TrendingFeedEntity>>

    fun getTrendingFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, TrendingFeedEntity>
    fun getRecentFeedsByCacheKey(cacheKey: String, limit: Int = -1): Flow<List<RecentFeedEntity>>
    fun getRecentFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, RecentFeedEntity>
    fun getRecentNewFeedsByCacheKey(
        cacheKey: String,
        limit: Int = -1,
    ): Flow<List<RecentNewFeedEntity>>

    fun getRecentNewFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, RecentNewFeedEntity>
    fun getSoundbitesByCacheKey(cacheKey: String, limit: Int = -1): Flow<List<SoundbiteEntity>>
    fun getSoundbitesByCacheKeyPaging(cacheKey: String): PagingSource<Int, SoundbiteEntity>
}