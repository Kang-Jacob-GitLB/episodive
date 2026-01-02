package io.jacob.episodive.core.data.util.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisodeEntity
import io.jacob.episodive.core.database.mapper.toSoundbiteEntities
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.SoundbiteRemoteDataSource
import io.jacob.episodive.core.network.mapper.toEpisode
import io.jacob.episodive.core.network.mapper.toSoundbites
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class SoundbiteEpisodePagingSource(
    private val episodeLocal: EpisodeLocalDataSource,
    private val episodeRemote: EpisodeRemoteDataSource,
    private val soundbiteLocal: SoundbiteLocalDataSource,
    private val soundbiteRemote: SoundbiteRemoteDataSource,
    private val maxSoundbites: Int = 1000,
    private val timeToLive: Duration = 10.minutes,
) : PagingSource<Int, EpisodeWithExtrasView>() {

    override fun getRefreshKey(state: PagingState<Int, EpisodeWithExtrasView>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(state.config.pageSize)
                ?: anchorPage?.nextKey?.minus(state.config.pageSize)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EpisodeWithExtrasView> {
        return try {
            ensureSoundbitesAreFresh()

            val offset = params.key ?: 0
            val limit = params.loadSize

            val soundbites = soundbiteLocal.getSoundbitesPagingList(
                offset = offset,
                limit = limit
            )

            if (soundbites.isEmpty()) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (offset > 0) offset - limit else null,
                    nextKey = null
                )
            }

            val episodeIds = soundbites.map { it.episodeId }

            fetchMissingEpisodes(episodeIds)

            val allEpisodes = episodeLocal.getEpisodesByIdsOneShot(episodeIds)
            val orderedEpisodes = episodeIds.mapNotNull { id ->
                allEpisodes.find { it.episode.id == id }
            }

            LoadResult.Page(
                data = orderedEpisodes,
                prevKey = if (offset > 0) offset - limit else null,
                nextKey = if (soundbites.size == limit) offset + limit else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun ensureSoundbitesAreFresh() {
        val oldestCreatedAt = soundbiteLocal.getSoundbitesOldestCachedAt()
        val isExpired = oldestCreatedAt?.let {
            Clock.System.now() - it > timeToLive
        } ?: true

        if (isExpired) {
            val soundbiteResponses = soundbiteRemote.getSoundbites(max = maxSoundbites)
                .filterNot {
                    val regex = Regex("\\p{InCJK_UNIFIED_IDEOGRAPHS}")

                    it.title.contains(regex) ||
                            it.episodeTitle.contains(regex) ||
                            it.feedTitle.contains(regex)
                }

            val soundbiteEntities = soundbiteResponses
                .toSoundbites()
                .toSoundbiteEntities()

            soundbiteLocal.replaceSoundbites(soundbiteEntities)
        }
    }

    private suspend fun fetchMissingEpisodes(episodeIds: List<Long>) {
        val cachedEpisodes = episodeLocal.getEpisodesByIdsOneShot(episodeIds)
        val cachedEpisodeIds = cachedEpisodes.map { it.episode.id }.toSet()
        val missingEpisodeIds = episodeIds.filterNot { it in cachedEpisodeIds }

        if (missingEpisodeIds.isEmpty()) return

        val episodeEntities = missingEpisodeIds
            .withIndex()
            .asFlow()
            .flatMapMerge(concurrency = 5) { (index, episodeId) ->
                flow {
                    try {
                        val episodeResponse = episodeRemote.getEpisodeById(episodeId)
                        episodeResponse?.let {
                            val episode = it.toEpisode()
                            val episodeEntity = episode.toEpisodeEntity()
                            emit(index to episodeEntity)
                        }
                    } catch (e: Exception) {
                        // Continue with other episodes if one fails
                    }
                }
            }
            .toList()
            .sortedBy { it.first }
            .map { it.second }

        if (episodeEntities.isNotEmpty()) {
            episodeLocal.upsertEpisodesWithGroup(
                episodes = episodeEntities,
                groupKey = GROUP_KEY_SOUNDBITE
            )
        }
    }

    companion object {
        private const val GROUP_KEY_SOUNDBITE = "soundbite"
    }
}