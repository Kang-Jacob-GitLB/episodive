package io.jacob.episodive.core.data.util.updater

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.mapper.toEpisodes
import io.jacob.episodive.core.network.model.EpisodeResponse
import kotlin.time.Clock

class EpisodeRemoteUpdater @AssistedInject constructor(
    private val localDataSource: EpisodeLocalDataSource,
    private val remoteDataSource: EpisodeRemoteDataSource,
    @Assisted("query") override val query: EpisodeQuery,
) : RemoteUpdater<EpisodeQuery, List<EpisodeResponse>, List<EpisodeEntity>>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: EpisodeQuery): EpisodeRemoteUpdater
    }

    override suspend fun fetchFromRemote(): List<EpisodeResponse> {
        return when (query) {
            is EpisodeQuery.Person -> remoteDataSource.searchEpisodesByPerson(
                person = query.person,
                max = 10000,
            )

            is EpisodeQuery.FeedId -> remoteDataSource.getEpisodesByFeedId(
                feedId = query.feedId,
                max = 10000,
            )

            is EpisodeQuery.FeedUrl -> remoteDataSource.getEpisodesByFeedUrl(
                feedUrl = query.feedUrl,
                max = 10000,
            )

            is EpisodeQuery.PodcastGuid -> remoteDataSource.getEpisodesByPodcastGuid(
                guid = query.podcastGuid,
                max = 10000,
            )

            is EpisodeQuery.Live -> remoteDataSource.getLiveEpisodes(max = 6)
            is EpisodeQuery.Random -> remoteDataSource.getRandomEpisodes(max = 6)
            is EpisodeQuery.Recent -> remoteDataSource.getRecentEpisodes(max = 6)
            is EpisodeQuery.EpisodeId -> remoteDataSource.getEpisodeById(query.episodeId)
                ?.let { listOf(it) }
                ?: emptyList()
        }
    }

    override suspend fun mapToEntities(response: List<EpisodeResponse>): List<EpisodeEntity> {
        return response.toEpisodes().toEpisodeEntities(query.key)
    }

    override suspend fun saveToLocal(entity: List<EpisodeEntity>) {
        localDataSource.upsertEpisodes(entity)
    }

    override suspend fun isExpired(cached: List<EpisodeEntity>): Boolean {
        if (cached.isEmpty()) return true
        val oldestCache = cached.minByOrNull { it.cachedAt }?.cachedAt
            ?: return true
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }

    override suspend fun deleteLocal() {
        when (query) {
            is EpisodeQuery.Person,
            is EpisodeQuery.FeedId,
            is EpisodeQuery.FeedUrl,
            is EpisodeQuery.PodcastGuid,
            is EpisodeQuery.Live,
            is EpisodeQuery.Random,
            is EpisodeQuery.Recent -> localDataSource.deleteEpisodesByCacheKey(query.key)

            is EpisodeQuery.EpisodeId -> {}
        }
    }
}