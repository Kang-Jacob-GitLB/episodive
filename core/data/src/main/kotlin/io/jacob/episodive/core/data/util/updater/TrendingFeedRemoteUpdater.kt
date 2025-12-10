package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toTrendingFeedEntities
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toTrendingFeeds
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import kotlin.time.Clock

class TrendingFeedRemoteUpdater(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    override val query: FeedQuery,
) : RemoteUpdater<FeedQuery, List<TrendingFeedResponse>, List<TrendingFeedEntity>, List<TrendingFeedEntity>>(
    query
) {

    override suspend fun fetchFromRemote(): List<TrendingFeedResponse> {
        return when (query) {
            is FeedQuery.Trending ->
                remoteDataSource.getTrendingFeeds(
                    max = query.max,
                    language = query.language,
                    includeCategories = query.categories.toCommaString(),
                )

            else -> emptyList()
        }
    }

    override suspend fun convertToEntity(response: List<TrendingFeedResponse>): List<TrendingFeedEntity> {
        return response.toTrendingFeeds().toTrendingFeedEntities(query.key)
    }

    override suspend fun saveToLocal(entity: List<TrendingFeedEntity>) {
        localDataSource.replaceTrendingFeeds(entity)
    }

    override suspend fun isExpired(output: List<TrendingFeedEntity>): Boolean {
        if (output.isEmpty()) return true
        val oldestCache = output.minBy { it.cachedAt }.cachedAt
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }
}