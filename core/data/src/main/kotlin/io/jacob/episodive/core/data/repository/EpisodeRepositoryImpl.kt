package io.jacob.episodive.core.data.repository

import androidx.room.Transaction
import io.jacob.episodive.core.data.util.Cacher
import io.jacob.episodive.core.data.util.CacherSingle
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.data.util.updater.EpisodeRemoteUpdater
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisode
import io.jacob.episodive.core.database.mapper.toEpisodes
import io.jacob.episodive.core.database.mapper.toLikedEpisodes
import io.jacob.episodive.core.database.mapper.toPlayedEpisodes
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.LikedEpisode
import io.jacob.episodive.core.model.PlayedEpisode
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        max: Int?
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
        since: Instant?
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.FeedId(
            feedId = feedId,
            max = max ?: 10
        )

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
        since: Instant?
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
        since: Instant?
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

        return CacherSingle(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisode(id)
            }
        ).flow.map { it?.toEpisode() }
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
        excludeCategories: List<Category>
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
        excludeString: String?
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Recent

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getEpisodesByCacheKey(query.key)
            }
        ).flow.map { it.toEpisodes() }
    }

    override fun getLikedEpisodes(query: String?): Flow<List<LikedEpisode>> {
        return localDataSource.getLikedEpisodes(query).map { it.toLikedEpisodes() }
    }

    override fun getPlayingEpisodes(query: String?): Flow<List<PlayedEpisode>> {
        return localDataSource.getPlayingEpisodes(query).map { it.toPlayedEpisodes() }
    }

    override fun getPlayedEpisodes(query: String?): Flow<List<PlayedEpisode>> {
        return localDataSource.getPlayedEpisodes(query).map { it.toPlayedEpisodes() }
    }

    @Transaction
    override suspend fun toggleLiked(id: Long): Boolean {
        return if (localDataSource.isLiked(id).first()) {
            localDataSource.removeLiked(id)
            false
        } else {
            localDataSource.addLiked(
                LikedEpisodeEntity(
                    id = id,
                    likedAt = Clock.System.now()
                )
            )
            true
        }
    }

    override suspend fun updatePlayed(
        id: Long,
        position: Duration,
        isCompleted: Boolean
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
}