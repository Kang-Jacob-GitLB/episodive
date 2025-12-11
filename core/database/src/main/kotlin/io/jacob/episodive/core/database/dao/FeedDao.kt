package io.jacob.episodive.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

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

    @Transaction
    suspend fun replaceTrendingFeeds(feeds: List<TrendingFeedEntity>) {
        feeds.groupBy { it.cacheKey }.forEach { (cacheKey, feedGroup) ->
            deleteTrendingFeedsByCacheKey(cacheKey)
            upsertTrendingFeeds(feedGroup)
        }
    }

    @Query("DELETE FROM recent_feeds WHERE id = :id")
    suspend fun deleteRecentFeed(id: Long)

    @Query("DELETE FROM recent_feeds")
    suspend fun deleteRecentFeeds()

    @Query("DELETE FROM recent_feeds WHERE cacheKey = :cacheKey")
    suspend fun deleteRecentFeedsByCacheKey(cacheKey: String)

    @Transaction
    suspend fun replaceRecentFeeds(feeds: List<RecentFeedEntity>) {
        feeds.groupBy { it.cacheKey }.forEach { (cacheKey, feedGroup) ->
            deleteRecentFeedsByCacheKey(cacheKey)
            upsertRecentFeeds(feedGroup)
        }
    }

    @Query("DELETE FROM recent_new_feeds WHERE id = :id")
    suspend fun deleteRecentNewFeed(id: Long)

    @Query("DELETE FROM recent_new_feeds")
    suspend fun deleteRecentNewFeeds()

    @Query("DELETE FROM recent_new_feeds WHERE cacheKey = :cacheKey")
    suspend fun deleteRecentNewFeedsByCacheKey(cacheKey: String)

    @Transaction
    suspend fun replaceRecentNewFeeds(feeds: List<RecentNewFeedEntity>) {
        feeds.groupBy { it.cacheKey }.forEach { (cacheKey, feedGroup) ->
            deleteRecentNewFeedsByCacheKey(cacheKey)
            upsertRecentNewFeeds(feedGroup)
        }
    }

    @Query("DELETE FROM soundbites WHERE episodeId = :episodeId")
    suspend fun deleteSoundbite(episodeId: Long)

    @Query("DELETE FROM soundbites")
    suspend fun deleteSoundbites()

    @Query("DELETE FROM soundbites WHERE cacheKey = :cacheKey")
    suspend fun deleteSoundbitesByCacheKey(cacheKey: String)

    @Transaction
    suspend fun replaceSoundbites(soundbites: List<SoundbiteEntity>) {
        soundbites.groupBy { it.cacheKey }.forEach { (cacheKey, soundbiteGroup) ->
            deleteSoundbitesByCacheKey(cacheKey)
            upsertSoundbites(soundbiteGroup)
        }
    }

    @Query("SELECT * FROM trending_feeds WHERE cacheKey = :cacheKey LIMIT :limit")
    fun getTrendingFeedsByCacheKey(cacheKey: String, limit: Int): Flow<List<TrendingFeedEntity>>

    @Query("SELECT * FROM trending_feeds WHERE cacheKey = :cacheKey")
    fun getTrendingFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, TrendingFeedEntity>

    @Query("SELECT MIN(cachedAt) FROM trending_feeds WHERE cacheKey = :cacheKey")
    suspend fun getTrendingFeedsOldestCachedAtByCacheKey(cacheKey: String): Instant?

    @Query("SELECT * FROM recent_feeds WHERE cacheKey = :cacheKey LIMIT :limit")
    fun getRecentFeedsByCacheKey(cacheKey: String, limit: Int): Flow<List<RecentFeedEntity>>

    @Query("SELECT * FROM recent_feeds WHERE cacheKey = :cacheKey")
    fun getRecentFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, RecentFeedEntity>

    @Query("SELECT MIN(cachedAt) FROM recent_feeds WHERE cacheKey = :cacheKey")
    suspend fun getRecentFeedsOldestCachedAtByCacheKey(cacheKey: String): Instant?

    @Query("SELECT * FROM recent_new_feeds WHERE cacheKey = :cacheKey LIMIT :limit")
    fun getRecentNewFeedsByCacheKey(cacheKey: String, limit: Int): Flow<List<RecentNewFeedEntity>>

    @Query("SELECT * FROM recent_new_feeds WHERE cacheKey = :cacheKey")
    fun getRecentNewFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, RecentNewFeedEntity>

    @Query("SELECT MIN(cachedAt) FROM recent_new_feeds WHERE cacheKey = :cacheKey")
    suspend fun getRecentNewFeedsOldestCachedAtByCacheKey(cacheKey: String): Instant?

    @Query("SELECT * FROM soundbites WHERE cacheKey = :cacheKey LIMIT :limit")
    fun getSoundbitesByCacheKey(cacheKey: String, limit: Int): Flow<List<SoundbiteEntity>>

    @Query("SELECT * FROM soundbites WHERE cacheKey = :cacheKey")
    fun getSoundbitesByCacheKeyPaging(cacheKey: String): PagingSource<Int, SoundbiteEntity>

    @Query("SELECT MIN(cachedAt) FROM soundbites WHERE cacheKey = :cacheKey")
    suspend fun getSoundbitesOldestCachedAtByCacheKey(cacheKey: String): Instant?
}