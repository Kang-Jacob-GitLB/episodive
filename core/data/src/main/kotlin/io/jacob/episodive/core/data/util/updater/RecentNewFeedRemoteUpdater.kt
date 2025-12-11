package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toRecentNewFeedEntities
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toRecentNewFeeds
import io.jacob.episodive.core.network.model.RecentNewFeedResponse
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class RecentNewFeedRemoteUpdater @AssistedInject constructor(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    @Assisted("query") override val query: FeedQuery,
) : RemoteUpdater<FeedQuery, RecentNewFeedResponse, RecentNewFeedEntity, RecentNewFeedEntity>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: FeedQuery): RecentNewFeedRemoteUpdater
    }

    override suspend fun fetchFromRemote(fetchSize: Int): List<RecentNewFeedResponse> {
        return when (query) {
            is FeedQuery.RecentNew -> remoteDataSource.getRecentNewFeeds(max = fetchSize)
            else -> emptyList()
        }
    }

    override suspend fun convertToEntity(responses: List<RecentNewFeedResponse>): List<RecentNewFeedEntity> {
        return responses.toRecentNewFeeds().toRecentNewFeedEntities(query.key)
    }

    override suspend fun replaceToLocal(entities: List<RecentNewFeedEntity>) {
        localDataSource.replaceRecentNewFeeds(entities)
    }

    override suspend fun isExpired(): Boolean {
        val oldestCachedAt = localDataSource.getRecentNewFeedsOldestCachedAtByCacheKey(query.key)
            ?: return true

        val now = Clock.System.now()
        return now - oldestCachedAt > query.timeToLive
    }

    override fun getPagingSource(): PagingSource<Int, RecentNewFeedEntity> {
        return localDataSource.getRecentNewFeedsByCacheKeyPaging(query.key)
    }

    override fun getFlowSource(count: Int): Flow<List<RecentNewFeedEntity>> {
        return localDataSource.getRecentNewFeedsByCacheKey(query.key, count)
    }
}