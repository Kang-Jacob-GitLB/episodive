package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.data.util.Cacher
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.data.util.updater.EpisodeRemoteUpdater
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisode
import io.jacob.episodive.core.database.mapper.toEpisodes
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class EpisodeRepositoryImpl @Inject constructor(
    private val localDataSource: EpisodeLocalDataSource,
    private val remoteDataSource: EpisodeRemoteDataSource,
    private val remoteUpdater: EpisodeRemoteUpdater.Factory,
) : EpisodeRepository {

    override fun searchEpisodesByPerson(
        person: String,
        max: Int?,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Person(person)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisodesByCacheKey(query.key)
            }
        ).flow.map { it.toEpisodes() }
    }

    override fun getEpisodesByFeedId(
        feedId: Long,
        max: Int?,
        since: Instant?,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.FeedId(feedId)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisodesByCacheKey(query.key)
            }
        ).flow.map { it.toEpisodes() }
    }

    override fun getEpisodesByFeedUrl(
        feedUrl: String,
        max: Int?,
        since: Instant?,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.FeedUrl(feedUrl)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisodesByCacheKey(query.key)
            }
        ).flow.map { it.toEpisodes() }
    }

    override fun getEpisodesByPodcastGuid(
        guid: String,
        max: Int?,
        since: Instant?,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.PodcastGuid(guid)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisodesByCacheKey(query.key)
            }
        ).flow.map { it.toEpisodes() }
    }

    override fun getEpisodeById(id: Long): Flow<Episode?> {
        val query = EpisodeQuery.EpisodeId(id)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisode(id).map { dto ->
                    dto?.let { listOf(it) } ?: emptyList()
                }
            }
        ).flow.map { it.firstOrNull()?.toEpisode() }
    }

    override fun getLiveEpisodes(max: Int?): Flow<List<Episode>> {
        val query = EpisodeQuery.Live

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisodesByCacheKey(query.key)
            }
        ).flow.map { it.toEpisodes() }
    }

    override fun getRandomEpisodes(
        max: Int?,
        language: String?,
        includeCategories: List<Category>,
        excludeCategories: List<Category>,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Random

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisodesByCacheKey(query.key)
            }
        ).flow.map { it.toEpisodes() }
    }

    override fun getRecentEpisodes(
        max: Int?,
        excludeString: String?,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Recent

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisodesByCacheKey(query.key)
            }
        ).flow.map { it.toEpisodes() }
    }

    override fun getLikedEpisodes(query: String?): Flow<List<Episode>> {
        return localDataSource.getLikedEpisodes().map { episodes ->
            episodes
                .filter { query == null || it.episode.matchesQuery(query) }
                .toEpisodes()
        }
    }

    override fun getPlayingEpisodes(query: String?): Flow<List<Episode>> {
        return localDataSource.getPlayedEpisodes().map { episodes ->
            episodes
                .filter { it.isCompleted == false }
                .filter { query == null || it.episode.matchesQuery(query) }
                .toEpisodes()
        }
    }

    override fun getPlayedEpisodes(query: String?): Flow<List<Episode>> {
        return localDataSource.getPlayedEpisodes().map { episodes ->
            episodes
                .filter { it.isCompleted == true }
                .filter { query == null || it.episode.matchesQuery(query) }
                .toEpisodes()
        }
    }

    override fun getAllPlayedEpisodes(query: String?): Flow<List<Episode>> {
        return localDataSource.getPlayedEpisodes().map { episodes ->
            episodes
                .filter { query == null || it.episode.matchesQuery(query) }
                .toEpisodes()
        }
    }

    override suspend fun toggleLiked(id: Long): Boolean {
        return localDataSource.toggleLiked(id)
    }

    override suspend fun updatePlayed(
        id: Long,
        position: Duration,
        isCompleted: Boolean,
    ) {
        localDataSource.upsertPlayed(
            PlayedEpisodeEntity(
                id = id,
                playedAt = Clock.System.now(),
                position = position,
                isCompleted = isCompleted,
            )
        )
    }

    override suspend fun updateDurationOfEpisodes(id: Long, duration: Duration) {
        localDataSource.updateDurationOfEpisodes(id, duration)
    }

    private fun EpisodeEntity.matchesQuery(query: String): Boolean {
        return title.contains(query, ignoreCase = true) ||
                description?.contains(query, ignoreCase = true) == true ||
                feedAuthor?.contains(query, ignoreCase = true) == true ||
                feedTitle?.contains(query, ignoreCase = true) == true
    }
}