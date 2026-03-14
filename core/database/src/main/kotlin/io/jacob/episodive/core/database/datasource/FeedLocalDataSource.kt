package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.model.FeedEntity
import kotlin.time.Instant

interface FeedLocalDataSource {
    suspend fun upsertFeeds(feeds: List<FeedEntity>)
    suspend fun deleteFeedsByGroupKey(groupKey: String)
    suspend fun deleteFeeds()
    suspend fun replaceFeedsByGroupKey(feeds: List<FeedEntity>, groupKey: String)
    fun getFeeds(groupKey: String, limit: Int): List<FeedEntity>
    fun getFeedsPaging(groupKey: String): PagingSource<Int, FeedEntity>
    suspend fun getFeedsPagingList(groupKey: String, offset: Int, limit: Int): List<FeedEntity>
    suspend fun getFeedsOldestCachedAt(groupKey: String): Instant?
}