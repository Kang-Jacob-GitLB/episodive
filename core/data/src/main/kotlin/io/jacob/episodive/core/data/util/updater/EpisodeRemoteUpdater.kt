package io.jacob.episodive.core.data.util.updater

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.model.EpisodeDto
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.mapper.toEpisodes
import io.jacob.episodive.core.network.model.EpisodeResponse
import kotlin.time.Clock

class EpisodeRemoteUpdater @AssistedInject constructor(
    private val localDataSource: EpisodeLocalDataSource,
    private val remoteDataSource: EpisodeRemoteDataSource,
    @Assisted("query") override val query: EpisodeQuery,
) : RemoteUpdater<EpisodeQuery, List<EpisodeResponse>, List<EpisodeEntity>, List<EpisodeDto>>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: EpisodeQuery): EpisodeRemoteUpdater
    }

    override suspend fun fetchFromRemote(): List<EpisodeResponse> {
        return when (query) {
            is EpisodeQuery.Person -> remoteDataSource.searchEpisodesByPerson(
                person = query.person,
                max = query.max,
            )

            is EpisodeQuery.FeedId -> remoteDataSource.getEpisodesByFeedId(
                feedId = query.feedId,
                max = query.max,
            )

            is EpisodeQuery.FeedUrl -> remoteDataSource.getEpisodesByFeedUrl(
                feedUrl = query.feedUrl,
                max = query.max,
            )

            is EpisodeQuery.PodcastGuid -> remoteDataSource.getEpisodesByPodcastGuid(
                guid = query.podcastGuid,
                max = query.max,
            )

            is EpisodeQuery.Live -> remoteDataSource.getLiveEpisodes(max = query.max)
            is EpisodeQuery.Random -> remoteDataSource.getRandomEpisodes(
                max = query.max,
                language = query.language,
                includeCategories = query.categories.toCommaString(),
            )

            is EpisodeQuery.Recent -> remoteDataSource.getRecentEpisodes(max = query.max)
            is EpisodeQuery.EpisodeId -> remoteDataSource.getEpisodeById(query.episodeId)
                ?.let { listOf(it) } ?: emptyList()
        }
    }

    override suspend fun convertToEntity(response: List<EpisodeResponse>): List<EpisodeEntity> {
        return response.toEpisodes().toEpisodeEntities(query.key)
    }

    override suspend fun saveToLocal(entity: List<EpisodeEntity>) {
        localDataSource.replaceEpisodes(entity)
    }

    override suspend fun isExpired(output: List<EpisodeDto>): Boolean {
        if (output.isEmpty()) return true
        val oldestCache = output.minBy { it.episode.cachedAt }.episode.cachedAt
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }
}