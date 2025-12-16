package io.jacob.episodive.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.data.util.updater.EpisodeRemoteUpdater
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisode
import io.jacob.episodive.core.database.mapper.toEpisodes
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.network.datasource.ChapterRemoteDataSource
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration

class EpisodeRepositoryImpl @Inject constructor(
    private val localDataSource: EpisodeLocalDataSource,
    private val remoteDataSource: EpisodeRemoteDataSource,
    private val chapterRemoteDataSource: ChapterRemoteDataSource,
    private val remoteUpdater: EpisodeRemoteUpdater.Factory,
) : EpisodeRepository {
    private val config = PagingConfig(
        pageSize = 20,
        prefetchDistance = 5,
        enablePlaceholders = false
    )

    override fun searchEpisodesByPerson(
        person: String,
        max: Int,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Person(person)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun searchEpisodesByPersonPaging(
        person: String,
    ): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.Person(person)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getEpisodesByFeedId(
        feedId: Long,
        max: Int,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.FeedId(feedId)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getEpisodesByFeedIdPaging(feedId: Long): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.FeedId(feedId)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getEpisodesByFeedUrl(
        feedUrl: String,
        max: Int,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.FeedUrl(feedUrl)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getEpisodesByFeedUrlPaging(feedUrl: String): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.FeedUrl(feedUrl)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getEpisodesByPodcastGuid(
        guid: String,
        max: Int,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.PodcastGuid(guid)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getEpisodesByPodcastGuidPaging(guid: String): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.PodcastGuid(guid)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getEpisodeById(id: Long): Flow<Episode?> {
        val query = EpisodeQuery.EpisodeId(id)

        return remoteUpdater.create(query)
            .getFlowList(1)
            .map { it.firstOrNull()?.toEpisode() }
    }

    override fun getLiveEpisodes(max: Int): Flow<List<Episode>> {
        val query = EpisodeQuery.Live

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getLiveEpisodesPaging(): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.Live

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getRandomEpisodes(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
        excludeCategories: List<Category>,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Random(language, includeCategories)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getRandomEpisodesPaging(
        language: String?,
        includeCategories: List<Category>,
        excludeCategories: List<Category>,
    ): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.Random(language, includeCategories)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getRecentEpisodes(
        max: Int,
        excludeString: String?,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Recent

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getRecentEpisodesPaging(): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.Recent

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getLikedEpisodes(query: String?, max: Int): Flow<List<Episode>> {
        return localDataSource.getLikedEpisodes(max).map { episodes ->
            episodes
                .filter { query == null || it.episode.matchesQuery(query) }
                .toEpisodes()
        }
    }

    override fun getLikedEpisodesPaging(): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getLikedEpisodesPaging() }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun getPlayingEpisodes(query: String?, max: Int): Flow<List<Episode>> {
        return localDataSource.getPlayedEpisodes(max).map { episodes ->
            episodes
                .filter { it.isCompleted == false }
                .filter { query == null || it.episode.matchesQuery(query) }
                .toEpisodes()
        }
    }

    override fun getPlayingEpisodesPaging(): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getPlayedEpisodesPaging() }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun getPlayedEpisodes(query: String?, max: Int): Flow<List<Episode>> {
        return localDataSource.getPlayedEpisodes(max).map { episodes ->
            episodes
                .filter { it.isCompleted == true }
                .filter { query == null || it.episode.matchesQuery(query) }
                .toEpisodes()
        }
    }

    override fun getPlayedEpisodesPaging(): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getPlayedEpisodesPaging() }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun getAllPlayedEpisodes(query: String?, max: Int): Flow<List<Episode>> {
        return localDataSource.getPlayedEpisodes(max).map { episodes ->
            episodes
                .filter { query == null || it.episode.matchesQuery(query) }
                .toEpisodes()
        }
    }

    override fun getAllPlayedEpisodesPaging(): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getPlayedEpisodesPaging() }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun isLiked(id: Long): Flow<Boolean> {
        return localDataSource.isLiked(id)
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

    override suspend fun fetchChapters(url: String): List<Chapter> {
        return chapterRemoteDataSource.fetchChapters(url)
    }

    private fun EpisodeEntity.matchesQuery(query: String): Boolean {
        return title.contains(query, ignoreCase = true) ||
                description?.contains(query, ignoreCase = true) == true ||
                feedAuthor?.contains(query, ignoreCase = true) == true ||
                feedTitle?.contains(query, ignoreCase = true) == true
    }
}