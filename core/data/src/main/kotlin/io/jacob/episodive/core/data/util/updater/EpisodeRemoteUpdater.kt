package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingSource
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
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class EpisodeRemoteUpdater @AssistedInject constructor(
    private val localDataSource: EpisodeLocalDataSource,
    private val remoteDataSource: EpisodeRemoteDataSource,
    @Assisted("query") override val query: EpisodeQuery,
) : RemoteUpdater<EpisodeQuery, EpisodeResponse, EpisodeEntity, EpisodeDto>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: EpisodeQuery): EpisodeRemoteUpdater
    }

    override suspend fun fetchFromRemote(fetchSize: Int): List<EpisodeResponse> {
        return when (query) {
            is EpisodeQuery.Person -> remoteDataSource.searchEpisodesByPerson(
                person = query.person,
                max = fetchSize,
            )

            is EpisodeQuery.FeedId -> remoteDataSource.getEpisodesByFeedId(
                feedId = query.feedId,
                max = fetchSize,
            )

            is EpisodeQuery.FeedUrl -> remoteDataSource.getEpisodesByFeedUrl(
                feedUrl = query.feedUrl,
                max = fetchSize,
            )

            is EpisodeQuery.PodcastGuid -> remoteDataSource.getEpisodesByPodcastGuid(
                guid = query.podcastGuid,
                max = fetchSize,
            )

            is EpisodeQuery.Live -> remoteDataSource.getLiveEpisodes(max = 6)
            is EpisodeQuery.Random -> remoteDataSource.getRandomEpisodes(
                max = 6,
                language = query.language,
                includeCategories = query.categories.toCommaString(),
            )

            is EpisodeQuery.Recent -> remoteDataSource.getRecentEpisodes(max = 6)
            is EpisodeQuery.EpisodeId -> remoteDataSource.getEpisodeById(query.episodeId)
                ?.let { listOf(it) } ?: emptyList()
        }
    }

    override suspend fun convertToEntity(responses: List<EpisodeResponse>): List<EpisodeEntity> {
        return responses.toEpisodes().toEpisodeEntities(query.key)
    }

    override suspend fun replaceToLocal(entities: List<EpisodeEntity>) {
        localDataSource.replaceEpisodes(entities)
    }

    override suspend fun isExpired(): Boolean {
        val oldestCachedAt = localDataSource.getEpisodesOldestCachedAtByCacheKey(query.key)
            ?: return true

        val now = Clock.System.now()
        return now - oldestCachedAt > query.timeToLive
    }

    override fun getPagingSource(): PagingSource<Int, EpisodeDto> {
        return localDataSource.getEpisodesByCacheKeyPaging(query.key)
    }

    override fun getFlowSource(count: Int): Flow<List<EpisodeDto>> {
        return localDataSource.getEpisodesByCacheKey(query.key, count)
    }
}