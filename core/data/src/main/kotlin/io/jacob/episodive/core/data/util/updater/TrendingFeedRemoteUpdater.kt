package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toTrendingFeedEntities
import io.jacob.episodive.core.database.mapper.toTrendingFeedEntity
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toTrendingFeed
import io.jacob.episodive.core.network.mapper.toTrendingFeeds
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import kotlin.time.Clock

class TrendingFeedRemoteUpdater(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    override val query: FeedQuery,
) : BaseRemoteUpdater<TrendingFeedEntity, FeedQuery, TrendingFeedResponse>(query) {

    override suspend fun fetchFromRemote(query: FeedQuery): List<TrendingFeedResponse> {
        return when (query) {
            is FeedQuery.Trending ->
                remoteDataSource.getTrendingFeeds(
                    language = query.language,
                    includeCategories = query.categories.toCommaString(),
                )

            else -> emptyList()
        }
    }

    override suspend fun fetchFromRemoteSingle(query: FeedQuery): TrendingFeedResponse? {
        return null
    }

    override suspend fun mapToEntities(
        responses: List<TrendingFeedResponse>,
        query: FeedQuery,
    ): List<TrendingFeedEntity> {
        return responses.toTrendingFeeds().toTrendingFeedEntities(query.key)
    }

    override suspend fun mapToEntity(
        response: TrendingFeedResponse?,
        query: FeedQuery,
    ): TrendingFeedEntity? {
        return response?.toTrendingFeed()?.toTrendingFeedEntity(query.key)
    }

    override suspend fun saveToLocal(entities: List<TrendingFeedEntity>) {
        localDataSource.upsertTrendingFeeds(entities)
    }

    override suspend fun saveToLocal(entity: TrendingFeedEntity?) {
        entity?.let { localDataSource.upsertTrendingFeeds(listOf(it)) }
    }

    override suspend fun isExpired(cached: List<TrendingFeedEntity>): Boolean {
        if (cached.isEmpty()) return true
        val oldestCache = cached.minByOrNull { it.cachedAt }?.cachedAt
            ?: return true
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }

    override suspend fun isExpired(cached: TrendingFeedEntity?): Boolean {
        if (cached == null) return true
        val oldCached = cached.cachedAt
        val now = Clock.System.now()
        return now - oldCached > query.timeToLive
    }

    override suspend fun deleteLocal(query: FeedQuery) {
        localDataSource.deleteTrendingFeedsByCacheKey(query.key)
    }
}