package io.jacob.episodive.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.FeedEntity
import kotlin.time.Instant

@Dao
interface FeedDao {
    @Upsert
    suspend fun upsertFeeds(feeds: List<FeedEntity>)

    @Query("DELETE FROM feeds WHERE groupKey = :groupKey")
    suspend fun deleteFeedsByGroupKey(groupKey: String)

    @Query("DELETE FROM feeds")
    suspend fun deleteFeeds()

    @Transaction
    suspend fun replaceFeedsByGroupKey(feeds: List<FeedEntity>, groupKey: String) {
        deleteFeedsByGroupKey(groupKey)
        upsertFeeds(feeds)
    }

    @Query("SELECT * FROM feeds WHERE groupKey = :groupKey LIMIT :limit")
    fun getFeeds(groupKey: String, limit: Int): List<FeedEntity>

    @Query("SELECT * FROM feeds WHERE groupKey = :groupKey")
    fun getFeedsPaging(groupKey: String): PagingSource<Int, FeedEntity>

    @Query("SELECT * FROM feeds WHERE groupKey = :groupKey LIMIT :limit OFFSET :offset")
    suspend fun getFeedsPagingList(groupKey: String, offset: Int, limit: Int): List<FeedEntity>

    @Query("SELECT MIN(cachedAt) FROM feeds WHERE groupKey = :groupKey")
    suspend fun getFeedsOldestCachedAt(groupKey: String): Instant?
}