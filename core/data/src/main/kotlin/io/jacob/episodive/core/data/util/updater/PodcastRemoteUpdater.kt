package io.jacob.episodive.core.data.util.updater

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.model.PodcastDto
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcasts
import io.jacob.episodive.core.network.model.PodcastResponse
import kotlin.time.Clock

class PodcastRemoteUpdater @AssistedInject constructor(
    private val localDataSource: PodcastLocalDataSource,
    private val remoteDataSource: PodcastRemoteDataSource,
    @Assisted("query") override val query: PodcastQuery,
) : RemoteUpdater<PodcastQuery, List<PodcastResponse>, List<PodcastEntity>, List<PodcastDto>>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: PodcastQuery): PodcastRemoteUpdater
    }

    override suspend fun fetchFromRemote(): List<PodcastResponse> {
        return when (query) {
            is PodcastQuery.Search -> remoteDataSource.searchPodcasts(query.query, query.max)
            is PodcastQuery.Medium -> remoteDataSource.getPodcastsByMedium(query.medium, query.max)
            is PodcastQuery.FeedId -> remoteDataSource.getPodcastByFeedId(query.feedId)
                ?.let { listOf(it) }
                ?: emptyList()

            is PodcastQuery.ByChannel -> remoteDataSource.getPodcastsByGuids(query.channel.podcastGuids)
        }
    }

    override suspend fun convertToEntity(response: List<PodcastResponse>): List<PodcastEntity> {
        return response.toPodcasts().toPodcastEntities(query.key)
    }

    override suspend fun saveToLocal(entity: List<PodcastEntity>) {
        localDataSource.replacePodcasts(entity)
    }

    override suspend fun isExpired(output: List<PodcastDto>): Boolean {
        if (output.isEmpty()) return true
        val oldestCache = output.minBy { it.podcast.cachedAt }.podcast.cachedAt
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }
}