package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toRecentNewFeedEntities
import io.jacob.episodive.core.database.mapper.toRecentNewFeedEntity
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toRecentNewFeed
import io.jacob.episodive.core.network.mapper.toRecentNewFeeds
import io.jacob.episodive.core.network.model.RecentNewFeedResponse
import kotlin.time.Clock

class RecentNewFeedRemoteUpdater(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    override val query: FeedQuery,
) : BaseRemoteUpdater<RecentNewFeedEntity, FeedQuery, RecentNewFeedResponse>(query) {

    override suspend fun fetchFromRemote(query: FeedQuery): List<RecentNewFeedResponse> {
        return when (query) {
            is FeedQuery.RecentNew -> remoteDataSource.getRecentNewFeeds()
            else -> emptyList()
        }
    }

    override suspend fun fetchFromRemoteSingle(query: FeedQuery): RecentNewFeedResponse? {
        return null
    }

    override suspend fun mapToEntities(
        responses: List<RecentNewFeedResponse>,
        query: FeedQuery,
    ): List<RecentNewFeedEntity> {
        return responses.toRecentNewFeeds().toRecentNewFeedEntities(query.key)
    }

    override suspend fun mapToEntity(
        response: RecentNewFeedResponse?,
        query: FeedQuery,
    ): RecentNewFeedEntity? {
        return response?.toRecentNewFeed()?.toRecentNewFeedEntity(query.key)
    }

    override suspend fun saveToLocal(entities: List<RecentNewFeedEntity>) {
        localDataSource.upsertRecentNewFeeds(entities)
    }

    override suspend fun saveToLocal(entity: RecentNewFeedEntity?) {
        entity?.let { localDataSource.upsertRecentNewFeeds(listOf(it)) }
    }

    override suspend fun isExpired(cached: List<RecentNewFeedEntity>): Boolean {
        if (cached.isEmpty()) return true
        val oldestCache = cached.minByOrNull { it.cachedAt }?.cachedAt
            ?: return true
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }

    override suspend fun isExpired(cached: RecentNewFeedEntity?): Boolean {
        if (cached == null) return true
        val oldCached = cached.cachedAt
        val now = Clock.System.now()
        return now - oldCached > query.timeToLive
    }

    override suspend fun deleteLocal(query: FeedQuery) {
        localDataSource.deleteRecentNewFeedsByCacheKey(query.key)
    }
}