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

    override fun getSoundbiteEpisodes(max: Int): Flow<List<Episode>> {
        val query = EpisodeQuery.Soundbite

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getSoundbiteEpisodesPaging(): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.Soundbite

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getLikedEpisodes(query: String?, max: Int): Flow<List<Episode>> {
        return localDataSource.getLikedEpisodes(
            query = query,
            limit = max,
        ).map { episodes ->
            episodes.toEpisodes()
        }
    }

    override fun getLikedEpisodesPaging(query: String?): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getLikedEpisodesPaging(query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun getPlayedEpisodes(
        isCompleted: Boolean?,
        query: String?,
        max: Int,
    ): Flow<List<Episode>> {
        return localDataSource.getPlayedEpisodes(
            isCompleted = isCompleted,
            query = query,
            limit = max,
        ).map { episodes ->
            episodes.toEpisodes()
        }
    }

    override fun getPlayedEpisodesPaging(
        isCompleted: Boolean?,
        query: String?,
    ): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                localDataSource.getPlayedEpisodesPaging(
                    isCompleted = isCompleted,
                    query = query,
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun isLiked(id: Long): Flow<Boolean> {
        return localDataSource.isLikedEpisode(id)
    }

    override suspend fun toggleLiked(id: Long): Boolean {
        return localDataSource.toggleLikedEpisode(id)
    }

    override suspend fun updatePlayed(
        id: Long,
        position: Duration,
        isCompleted: Boolean,
    ) {
        localDataSource.updatePlayedEpisode(
            PlayedEpisodeEntity(
                id = id,
                playedAt = Clock.System.now(),
                position = position,
                isCompleted = isCompleted,
            )
        )
    }

    override suspend fun updateEpisodeDuration(id: Long, duration: Duration) {
        localDataSource.updateEpisodeDuration(id, duration)
    }

    override suspend fun fetchChapters(url: String): List<Chapter> {
        return chapterRemoteDataSource.fetchChapters(url)
    }
}