package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface FeedLocalDataSource {
    suspend fun upsertTrendingFeeds(feeds: List<TrendingFeedEntity>)
    suspend fun deleteTrendingFeed(id: Long)
    suspend fun deleteTrendingFeeds()
    suspend fun deleteTrendingFeedsByCacheKey(cacheKey: String)
    suspend fun replaceTrendingFeeds(feeds: List<TrendingFeedEntity>)
    fun getTrendingFeedsByCacheKey(cacheKey: String, limit: Int): Flow<List<TrendingFeedEntity>>
    fun getTrendingFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, TrendingFeedEntity>
    suspend fun getTrendingFeedsOldestCachedAtByCacheKey(cacheKey: String): Instant?


    suspend fun upsertRecentFeeds(feeds: List<RecentFeedEntity>)
    suspend fun deleteRecentFeed(id: Long)
    suspend fun deleteRecentFeeds()
    suspend fun deleteRecentFeedsByCacheKey(cacheKey: String)
    suspend fun replaceRecentFeeds(feeds: List<RecentFeedEntity>)
    fun getRecentFeedsByCacheKey(cacheKey: String, limit: Int): Flow<List<RecentFeedEntity>>
    fun getRecentFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, RecentFeedEntity>
    suspend fun getRecentFeedsOldestCachedAtByCacheKey(cacheKey: String): Instant?


    suspend fun upsertRecentNewFeeds(feeds: List<RecentNewFeedEntity>)
    suspend fun deleteRecentNewFeed(id: Long)
    suspend fun deleteRecentNewFeeds()
    suspend fun deleteRecentNewFeedsByCacheKey(cacheKey: String)
    suspend fun replaceRecentNewFeeds(feeds: List<RecentNewFeedEntity>)
    fun getRecentNewFeedsByCacheKey(cacheKey: String, limit: Int): Flow<List<RecentNewFeedEntity>>
    fun getRecentNewFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, RecentNewFeedEntity>
    suspend fun getRecentNewFeedsOldestCachedAtByCacheKey(cacheKey: String): Instant?


    suspend fun upsertSoundbites(soundbites: List<SoundbiteEntity>)
    suspend fun deleteSoundbite(episodeId: Long)
    suspend fun deleteSoundbites()
    suspend fun replaceSoundbites(soundbites: List<SoundbiteEntity>)
    fun getSoundbites(limit: Int): Flow<List<SoundbiteEntity>>
    fun getSoundbitesPaging(): PagingSource<Int, SoundbiteEntity>
    suspend fun getSoundbitesOldestCachedAt(): Instant?
}