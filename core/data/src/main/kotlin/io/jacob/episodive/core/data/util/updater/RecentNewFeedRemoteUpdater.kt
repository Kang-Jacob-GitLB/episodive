package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toRecentNewFeedEntities
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toRecentNewFeeds
import io.jacob.episodive.core.network.model.RecentNewFeedResponse
import kotlin.time.Clock

class RecentNewFeedRemoteUpdater(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    override val query: FeedQuery,
) : RemoteUpdater<FeedQuery, List<RecentNewFeedResponse>, List<RecentNewFeedEntity>>(query) {

    override suspend fun fetchFromRemote(): List<RecentNewFeedResponse> {
        return when (query) {
            is FeedQuery.RecentNew -> remoteDataSource.getRecentNewFeeds()
            else -> emptyList()
        }
    }

    override suspend fun mapToEntities(response: List<RecentNewFeedResponse>): List<RecentNewFeedEntity> {
        return response.toRecentNewFeeds().toRecentNewFeedEntities(query.key)
    }

    override suspend fun saveToLocal(entity: List<RecentNewFeedEntity>) {
        localDataSource.upsertRecentNewFeeds(entity)
    }

    override suspend fun isExpired(cached: List<RecentNewFeedEntity>): Boolean {
        if (cached.isEmpty()) return true
        val oldestCache = cached.minByOrNull { it.cachedAt }?.cachedAt
            ?: return true
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }

    override suspend fun deleteLocal() {
        localDataSource.deleteRecentNewFeedsByCacheKey(query.key)
    }
}