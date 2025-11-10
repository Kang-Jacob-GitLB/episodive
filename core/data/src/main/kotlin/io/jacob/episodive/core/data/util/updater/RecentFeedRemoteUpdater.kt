package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toRecentFeedEntities
import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toRecentFeeds
import io.jacob.episodive.core.network.model.RecentFeedResponse
import kotlin.time.Clock

class RecentFeedRemoteUpdater(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    override val query: FeedQuery,
) : RemoteUpdater<FeedQuery, List<RecentFeedResponse>, List<RecentFeedEntity>>(query) {

    override suspend fun fetchFromRemote(): List<RecentFeedResponse> {
        return when (query) {
            is FeedQuery.Recent ->
                remoteDataSource.getRecentFeeds(
                    language = query.language,
                    includeCategories = query.categories.toCommaString(),
                )

            else -> emptyList()
        }
    }

    override suspend fun mapToEntities(response: List<RecentFeedResponse>): List<RecentFeedEntity> {
        return response.toRecentFeeds().toRecentFeedEntities(query.key)
    }

    override suspend fun saveToLocal(entity: List<RecentFeedEntity>) {
        localDataSource.replaceRecentFeeds(entity)
    }

    override suspend fun isExpired(cached: List<RecentFeedEntity>): Boolean {
        if (cached.isEmpty()) return true
        val oldestCache = cached.minByOrNull { it.cachedAt }?.cachedAt
            ?: return true
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }
}