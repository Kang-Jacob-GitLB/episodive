package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.mapper.toSoundbiteEntities
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.SoundbiteRemoteDataSource
import io.jacob.episodive.core.network.mapper.toEpisodes
import io.jacob.episodive.core.network.mapper.toSoundbites
import io.jacob.episodive.core.network.model.EpisodeResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlin.time.Clock

class EpisodeRemoteUpdater @AssistedInject constructor(
    private val episodeLocal: EpisodeLocalDataSource,
    private val episodeRemote: EpisodeRemoteDataSource,
    private val soundbiteLocal: SoundbiteLocalDataSource,
    private val soundbiteRemote: SoundbiteRemoteDataSource,
    @Assisted("query") override val query: EpisodeQuery,
) : RemoteUpdater<EpisodeQuery, EpisodeResponse, EpisodeEntity, EpisodeWithExtrasView>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: EpisodeQuery): EpisodeRemoteUpdater
    }

    override suspend fun fetchFromRemote(fetchSize: Int): List<EpisodeResponse> {
        return when (query) {
            is EpisodeQuery.Person -> episodeRemote.searchEpisodesByPerson(
                person = query.person,
                max = fetchSize,
            )

            is EpisodeQuery.FeedId -> episodeRemote.getEpisodesByFeedId(
                feedId = query.feedId,
                max = fetchSize,
            )

            is EpisodeQuery.FeedUrl -> episodeRemote.getEpisodesByFeedUrl(
                feedUrl = query.feedUrl,
                max = fetchSize,
            )

            is EpisodeQuery.PodcastGuid -> episodeRemote.getEpisodesByPodcastGuid(
                guid = query.podcastGuid,
                max = fetchSize,
            )

            is EpisodeQuery.Live -> episodeRemote.getLiveEpisodes(max = query.max)
            is EpisodeQuery.Random -> episodeRemote.getRandomEpisodes(
                max = query.max,
                language = query.language,
                includeCategories = query.categories.toCommaString(),
            )

            is EpisodeQuery.Recent -> episodeRemote.getRecentEpisodes(max = query.max)
            is EpisodeQuery.Soundbite -> {
                val soundbites = soundbiteRemote.getSoundbites(max = query.max)
                    .filterNot {
                        val regex = Regex("\\p{InCJK_UNIFIED_IDEOGRAPHS}")

                        it.title.contains(regex) ||
                                it.episodeTitle.contains(regex) ||
                                it.feedTitle.contains(regex)
                    }
                soundbiteLocal.replaceSoundbites(soundbites.toSoundbites().toSoundbiteEntities())

                soundbites.asFlow()
                    .flatMapMerge(concurrency = 10) { soundbite ->
                        flow { emit(episodeRemote.getEpisodeById(soundbite.episodeId)) }
                    }
                    .filterNotNull()
                    .toList()
            }
        }
    }

    override suspend fun convertToEntity(responses: List<EpisodeResponse>): List<EpisodeEntity> {
        return responses.toEpisodes().toEpisodeEntities()
    }

    override suspend fun replaceToLocal(entities: List<EpisodeEntity>) {
        episodeLocal.replaceEpisodes(entities, query.key)
    }

    override suspend fun isExpired(): Boolean {
        val oldestCreatedAt = episodeLocal.getOldestCreatedAtByGroupKey(query.key)
            ?: return true

        val now = Clock.System.now()
        return now - oldestCreatedAt > query.timeToLive
    }

    override fun getPagingSource(): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeLocal.getEpisodesByGroupKeyPaging(query.key)
    }

    override fun getFlowSource(count: Int): Flow<List<EpisodeWithExtrasView>> {
        return episodeLocal.getEpisodesByGroupKey(query.key, count)
    }
}