package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toTrendingFeedEntities
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toTrendingFeeds
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class TrendingFeedRemoteUpdater @AssistedInject constructor(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    @Assisted("query") override val query: FeedQuery,
) : RemoteUpdater<FeedQuery, TrendingFeedResponse, TrendingFeedEntity, TrendingFeedEntity>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: FeedQuery): TrendingFeedRemoteUpdater
    }

    override suspend fun fetchFromRemote(fetchSize: Int): List<TrendingFeedResponse> {
        return when (query) {
            is FeedQuery.Trending ->
                remoteDataSource.getTrendingFeeds(
                    max = fetchSize,
                    language = query.language,
                    includeCategories = query.categories.toCommaString(),
                )

            else -> emptyList()
        }
    }

    override suspend fun convertToEntity(responses: List<TrendingFeedResponse>): List<TrendingFeedEntity> {
        return responses.toTrendingFeeds().toTrendingFeedEntities(query.key)
    }

    override suspend fun replaceToLocal(entities: List<TrendingFeedEntity>) {
        localDataSource.replaceTrendingFeeds(entities)
    }

    override suspend fun isExpired(): Boolean {
        val oldestCachedAt = localDataSource.getTrendingFeedsOldestCachedAtByCacheKey(query.key)
            ?: return true

        val now = Clock.System.now()
        return now - oldestCachedAt > query.timeToLive
    }

    override fun getPagingSource(): PagingSource<Int, TrendingFeedEntity> {
        return localDataSource.getTrendingFeedsByCacheKeyPaging(query.key)
    }

    override fun getFlowSource(count: Int): Flow<List<TrendingFeedEntity>> {
        return localDataSource.getTrendingFeedsByCacheKey(query.key, count)
    }
}