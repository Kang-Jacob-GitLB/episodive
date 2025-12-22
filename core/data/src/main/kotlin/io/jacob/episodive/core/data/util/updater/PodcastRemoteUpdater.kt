package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcasts
import io.jacob.episodive.core.network.model.PodcastResponse
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class PodcastRemoteUpdater @AssistedInject constructor(
    private val localDataSource: PodcastLocalDataSource,
    private val remoteDataSource: PodcastRemoteDataSource,
    @Assisted("query") override val query: PodcastQuery,
) : RemoteUpdater<PodcastQuery, PodcastResponse, PodcastEntity, PodcastWithExtrasView>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: PodcastQuery): PodcastRemoteUpdater
    }

    override suspend fun fetchFromRemote(fetchSize: Int): List<PodcastResponse> {
        return when (query) {
            is PodcastQuery.Search -> remoteDataSource.searchPodcasts(query.query, fetchSize)
            is PodcastQuery.FeedId -> remoteDataSource.getPodcastByFeedId(query.feedId)
                ?.let { listOf(it) } ?: emptyList()

            is PodcastQuery.FeedUrl -> remoteDataSource.getPodcastByFeedUrl(query.feedUrl)
                ?.let { listOf(it) } ?: emptyList()

            is PodcastQuery.FeedGuid -> remoteDataSource.getPodcastByGuid(query.feedGuid)
                ?.let { listOf(it) } ?: emptyList()

            is PodcastQuery.Medium -> remoteDataSource.getPodcastsByMedium(query.medium, fetchSize)
            is PodcastQuery.ByChannel -> remoteDataSource.getPodcastsByGuids(query.channel.podcastGuids)
        }
    }

    override suspend fun convertToEntity(responses: List<PodcastResponse>): List<PodcastEntity> {
        return responses.toPodcasts().toPodcastEntities()
    }

    override suspend fun replaceToLocal(entities: List<PodcastEntity>) {
        localDataSource.replacePodcasts(entities, query.key)
    }

    override suspend fun isExpired(): Boolean {
        val oldestCreatedAt = localDataSource.getOldestCreatedAtByGroupKey(query.key)
            ?: return true

        val now = Clock.System.now()
        return now - oldestCreatedAt > query.timeToLive
    }

    override fun getPagingSource(): PagingSource<Int, PodcastWithExtrasView> {
        return localDataSource.getPodcastsByGroupKeyPaging(query.key)
    }

    override fun getFlowSource(count: Int): Flow<List<PodcastWithExtrasView>> {
        return localDataSource.getPodcastsByGroupKey(query.key, count)
    }
}