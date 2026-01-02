package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.dao.FeedDao
import io.jacob.episodive.core.database.model.FeedEntity
import javax.inject.Inject
import kotlin.time.Instant

class FeedLocalDataSourceImpl @Inject constructor(
    private val feedDao: FeedDao,
) : FeedLocalDataSource {
    override suspend fun upsertFeeds(feeds: List<FeedEntity>) {
        feedDao.upsertFeeds(feeds)
    }

    override suspend fun deleteFeedsByGroupKey(groupKey: String) {
        feedDao.deleteFeedsByGroupKey(groupKey)
    }

    override suspend fun deleteFeeds() {
        feedDao.deleteFeeds()
    }

    override suspend fun replaceFeedsByGroupKey(
        feeds: List<FeedEntity>,
        groupKey: String
    ) {
        feedDao.replaceFeedsByGroupKey(feeds, groupKey)
    }

    override fun getFeeds(
        groupKey: String,
        limit: Int
    ): List<FeedEntity> {
        return feedDao.getFeeds(groupKey, limit)
    }

    override fun getFeedsPaging(groupKey: String): PagingSource<Int, FeedEntity> {
        return feedDao.getFeedsPaging(groupKey)
    }

    override suspend fun getFeedsPagingList(
        groupKey: String,
        offset: Int,
        limit: Int
    ): List<FeedEntity> {
        return feedDao.getFeedsPagingList(groupKey, offset, limit)
    }

    override suspend fun getFeedsOldestCachedAt(groupKey: String): Instant? {
        return feedDao.getFeedsOldestCachedAt(groupKey)
    }
}