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
) : RemoteUpdater<FeedQuery, List<RecentNewFeedResponse>, List<RecentNewFeedEntity>, List<RecentNewFeedEntity>>(
    query
) {

    override suspend fun fetchFromRemote(): List<RecentNewFeedResponse> {
        return when (query) {
            is FeedQuery.RecentNew -> remoteDataSource.getRecentNewFeeds(max = query.max)
            else -> emptyList()
        }
    }

    override suspend fun convertToEntity(response: List<RecentNewFeedResponse>): List<RecentNewFeedEntity> {
        return response.toRecentNewFeeds().toRecentNewFeedEntities(query.key)
    }

    override suspend fun saveToLocal(entity: List<RecentNewFeedEntity>) {
        localDataSource.replaceRecentNewFeeds(entity)
    }

    override suspend fun isExpired(output: List<RecentNewFeedEntity>): Boolean {
        if (output.isEmpty()) return true
        val oldestCache = output.minBy { it.cachedAt }.cachedAt
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }
}