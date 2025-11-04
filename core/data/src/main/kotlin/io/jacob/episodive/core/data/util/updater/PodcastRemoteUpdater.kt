package io.jacob.episodive.core.data.util.updater

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcasts
import io.jacob.episodive.core.network.model.PodcastResponse
import kotlin.time.Clock

class PodcastRemoteUpdater @AssistedInject constructor(
    private val localDataSource: PodcastLocalDataSource,
    private val remoteDataSource: PodcastRemoteDataSource,
    @Assisted("query") override val query: PodcastQuery,
) : RemoteUpdater<PodcastQuery, List<PodcastResponse>, List<PodcastEntity>>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: PodcastQuery): PodcastRemoteUpdater
    }

    override suspend fun fetchFromRemote(): List<PodcastResponse> {
        return when (query) {
            is PodcastQuery.Search -> remoteDataSource.searchPodcasts(query.query)
            is PodcastQuery.Medium -> remoteDataSource.getPodcastsByMedium(query.medium)
            is PodcastQuery.FeedId -> remoteDataSource.getPodcastByFeedId(query.feedId)
                ?.let { listOf(it) }
                ?: emptyList()
        }
    }

    override suspend fun mapToEntities(response: List<PodcastResponse>): List<PodcastEntity> {
        return response.toPodcasts().toPodcastEntities(query.key)
    }

    override suspend fun saveToLocal(entity: List<PodcastEntity>) {
        localDataSource.upsertPodcasts(entity)
    }

    override suspend fun isExpired(cached: List<PodcastEntity>): Boolean {
        if (cached.isEmpty()) return true
        val oldestCache = cached.minByOrNull { it.cachedAt }?.cachedAt
            ?: return true
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }

    override suspend fun deleteLocal() {
        when (query) {
            is PodcastQuery.Search,
            is PodcastQuery.Medium -> localDataSource.deletePodcastsByCacheKey(query.key)

            is PodcastQuery.FeedId -> {}
        }
    }
}