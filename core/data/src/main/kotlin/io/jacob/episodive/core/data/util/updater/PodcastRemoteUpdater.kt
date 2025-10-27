package io.jacob.episodive.core.data.util.updater

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.mapper.toPodcastEntity
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcast
import io.jacob.episodive.core.network.mapper.toPodcasts
import io.jacob.episodive.core.network.model.PodcastResponse
import kotlin.time.Clock

class PodcastRemoteUpdater @AssistedInject constructor(
    private val localDataSource: PodcastLocalDataSource,
    private val remoteDataSource: PodcastRemoteDataSource,
    @Assisted("query") override val query: PodcastQuery,
) : BaseRemoteUpdater<PodcastEntity, PodcastQuery, PodcastResponse>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: PodcastQuery): PodcastRemoteUpdater
    }

    override suspend fun fetchFromRemote(query: PodcastQuery): List<PodcastResponse> {
        return when (query) {
            is PodcastQuery.Search -> remoteDataSource.searchPodcasts(query.query)
            is PodcastQuery.Medium -> remoteDataSource.getPodcastsByMedium(query.medium)
            else -> emptyList()
        }
    }

    override suspend fun fetchFromRemoteSingle(query: PodcastQuery): PodcastResponse? {
        return when (query) {
            is PodcastQuery.FeedId -> remoteDataSource.getPodcastByFeedId(query.feedId)
            else -> null
        }
    }

    override suspend fun mapToEntities(
        responses: List<PodcastResponse>,
        query: PodcastQuery,
    ): List<PodcastEntity> {
        return responses.toPodcasts().toPodcastEntities(query.key)
    }

    override suspend fun mapToEntity(
        response: PodcastResponse?,
        query: PodcastQuery,
    ): PodcastEntity? {
        return response?.toPodcast()?.toPodcastEntity(query.key)
    }

    override suspend fun saveToLocal(entities: List<PodcastEntity>) {
        localDataSource.upsertPodcasts(entities)
    }

    override suspend fun saveToLocal(entity: PodcastEntity?) {
        entity?.let { localDataSource.upsertPodcast(it) }
    }

    override suspend fun isExpired(cached: List<PodcastEntity>): Boolean {
        if (cached.isEmpty()) return true
        val oldestCache = cached.minByOrNull { it.cachedAt }?.cachedAt
            ?: return true
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }

    override suspend fun isExpired(cached: PodcastEntity?): Boolean {
        if (cached == null) return true
        val oldCached = cached.cachedAt
        val now = Clock.System.now()
        return now - oldCached > query.timeToLive
    }

    override suspend fun deleteLocal(query: PodcastQuery) {
        when (query) {
            is PodcastQuery.Search,
            is PodcastQuery.Medium -> localDataSource.deletePodcastsByCacheKey(query.key)

            is PodcastQuery.FeedId -> {}
        }
    }
}