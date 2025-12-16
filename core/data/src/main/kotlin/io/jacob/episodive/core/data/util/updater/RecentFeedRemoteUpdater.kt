package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toRecentFeedEntities
import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toRecentFeeds
import io.jacob.episodive.core.network.model.RecentFeedResponse
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class RecentFeedRemoteUpdater @AssistedInject constructor(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    @Assisted("query") override val query: FeedQuery,
) : RemoteUpdater<FeedQuery, RecentFeedResponse, RecentFeedEntity, RecentFeedEntity>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: FeedQuery): RecentFeedRemoteUpdater
    }

    override suspend fun fetchFromRemote(fetchSize: Int): List<RecentFeedResponse> {
        return when (query) {
            is FeedQuery.Recent ->
                remoteDataSource.getRecentFeeds(
                    max = fetchSize,
                    language = query.language,
                    includeCategories = query.categories.toCommaString(),
                )

            else -> emptyList()
        }
    }

    override suspend fun convertToEntity(responses: List<RecentFeedResponse>): List<RecentFeedEntity> {
        return responses.toRecentFeeds().toRecentFeedEntities(query.key)
    }

    override suspend fun replaceToLocal(entities: List<RecentFeedEntity>) {
        localDataSource.replaceRecentFeeds(entities)
    }

    override suspend fun isExpired(): Boolean {
        val oldestCachedAt = localDataSource.getRecentFeedsOldestCachedAtByCacheKey(query.key)
            ?: return true

        val now = Clock.System.now()
        return now - oldestCachedAt > query.timeToLive
    }

    override fun getPagingSource(): PagingSource<Int, RecentFeedEntity> {
        return localDataSource.getRecentFeedsByCacheKeyPaging(query.key)
    }

    override fun getFlowSource(count: Int): Flow<List<RecentFeedEntity>> {
        return localDataSource.getRecentFeedsByCacheKey(query.key, count)
    }
}