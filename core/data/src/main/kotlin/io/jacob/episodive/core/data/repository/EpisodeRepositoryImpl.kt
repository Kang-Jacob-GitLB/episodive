package io.jacob.episodive.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import io.jacob.episodive.core.data.util.paging.PagingDefaults
import io.jacob.episodive.core.data.util.paging.SoundbiteEpisodePagingSource
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.data.util.updater.EpisodeRemoteUpdater
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisode
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.mapper.toEpisodeEntity
import io.jacob.episodive.core.database.mapper.toEpisodes
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.DownloadStatus
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.network.datasource.ChapterRemoteDataSource
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.SoundbiteRemoteDataSource
import io.jacob.episodive.core.network.mapper.toEpisodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class EpisodeRepositoryImpl @Inject constructor(
    private val episodeLocalDataSource: EpisodeLocalDataSource,
    private val episodeRemoteDataSource: EpisodeRemoteDataSource,
    private val chapterRemoteDataSource: ChapterRemoteDataSource,
    private val soundbiteLocalDataSource: SoundbiteLocalDataSource,
    private val soundbiteRemoteDataSource: SoundbiteRemoteDataSource,
    private val remoteUpdater: EpisodeRemoteUpdater.Factory,
) : EpisodeRepository {
    private val config = PagingDefaults.DEFAULT_CONFIG

    override suspend fun upsertEpisode(episode: Episode) {
        episodeLocalDataSource.upsertEpisode(episode.toEpisodeEntity())
    }

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
    ): Flow<List<Episode>> = flow {
        episodeRemoteDataSource.getEpisodesByFeedId(
            feedId = feedId,
            max = max,
        ).toEpisodes()
            .let { emit(it) }
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

    override fun getLiveEpisodes(max: Int): Flow<List<Episode>> {
        val query = EpisodeQuery.Live(max = max)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getRandomEpisodes(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
        excludeCategories: List<Category>,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Random(max, language, includeCategories)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getRecentEpisodes(
        max: Int,
        excludeString: String?,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Recent(max)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getSoundbiteEpisodesPaging(max: Int): Flow<PagingData<Episode>> {
        return Pager(
            config = PagingConfig(
                pageSize = 5,
                prefetchDistance = 5,
                initialLoadSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                SoundbiteEpisodePagingSource(
                    database = episodeLocalDataSource.database,
                    episodeLocal = episodeLocalDataSource,
                    episodeRemote = episodeRemoteDataSource,
                    soundbiteLocal = soundbiteLocalDataSource,
                    soundbiteRemote = soundbiteRemoteDataSource,
                    maxSoundbites = max
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun getEpisodeById(id: Long): Flow<Episode?> {
        return episodeLocalDataSource.getEpisodeById(id)
            .map { it?.toEpisode() }
    }

    override fun getEpisodesByIds(ids: List<Long>): Flow<List<Episode>> {
        return episodeLocalDataSource.getEpisodesByIds(ids)
            .map { it.toEpisodes() }
    }

    override fun getLikedEpisodes(query: String?, max: Int): Flow<List<Episode>> {
        return episodeLocalDataSource.getLikedEpisodes(
            query = query,
            limit = max,
        ).map { it.toEpisodes() }
    }

    override fun getLikedEpisodesPaging(query: String?): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = { episodeLocalDataSource.getLikedEpisodesPaging(query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun getPlayedEpisodes(
        isCompleted: Boolean?,
        query: String?,
        max: Int,
    ): Flow<List<Episode>> {
        return episodeLocalDataSource.getPlayedEpisodes(
            isCompleted = isCompleted,
            query = query,
            limit = max,
        ).map { it.toEpisodes() }
    }

    override fun getPlayedEpisodesPaging(
        isCompleted: Boolean?,
        query: String?,
    ): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                episodeLocalDataSource.getPlayedEpisodesPaging(
                    isCompleted = isCompleted,
                    query = query,
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun isLikedEpisode(episode: Episode): Flow<Boolean> {
        return episodeLocalDataSource.isLikedEpisode(episode.toEpisodeEntity())
    }

    override suspend fun toggleLikedEpisode(episode: Episode): Boolean {
        return episodeLocalDataSource.toggleLikedEpisode(episode.toEpisodeEntity())
    }

    override suspend fun updatePlayed(
        id: Long,
        position: Duration,
        isCompleted: Boolean,
    ) {
        episodeLocalDataSource.updatePlayedEpisode(
            PlayedEpisodeEntity(
                id = id,
                playedAt = Clock.System.now(),
                position = position,
                isCompleted = isCompleted,
            )
        )
    }

    override suspend fun updateEpisodeDuration(id: Long, duration: Duration) {
        episodeLocalDataSource.updateEpisodeDuration(id, duration)
    }

    override suspend fun replaceEpisodes(episodes: List<Episode>, groupKey: String) {
        episodeLocalDataSource.replaceEpisodes(episodes.toEpisodeEntities(), groupKey)
    }

    override suspend fun fetchChapters(url: String): List<Chapter> {
        return chapterRemoteDataSource.fetchChapters(url)
    }

    override suspend fun getEpisodesByGroupKey(groupKey: String): List<Episode> {
        return episodeLocalDataSource.getEpisodesByGroupKey(groupKey, Int.MAX_VALUE)
            .first()
            .toEpisodes()
    }

    override fun getSavedEpisodes(query: String?, max: Int): Flow<List<Episode>> {
        return episodeLocalDataSource.getSavedEpisodes(
            query = query,
            limit = max,
        ).map { it.toEpisodes() }
    }

    override fun getSavedEpisodesPaging(query: String?): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = { episodeLocalDataSource.getSavedEpisodesPaging(query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override suspend fun toggleSavedEpisode(episode: Episode): Boolean {
        val filePath = "${episode.feedId}/${episode.id}.${episode.enclosureType.substringAfterLast("/", "mp3")}"
        return episodeLocalDataSource.toggleSavedEpisode(episode.toEpisodeEntity(), filePath)
    }

    override suspend fun updateSavedEpisodeProgress(
        id: Long,
        downloadedSize: Long,
        status: DownloadStatus,
    ) {
        episodeLocalDataSource.updateSavedEpisodeProgress(id, downloadedSize, status)
    }

    override suspend fun removeSavedEpisode(id: Long) {
        episodeLocalDataSource.removeSavedEpisode(id)
    }

    override suspend fun getLatestEpisodeDatePublished(feedId: Long): Instant? {
        return episodeLocalDataSource.getLatestEpisodeDatePublished(feedId)
    }

    override suspend fun fetchAndSaveNewEpisodes(feedId: Long, since: Instant): List<Episode> {
        val responses = episodeRemoteDataSource.getEpisodesByFeedId(
            feedId = feedId,
            since = since.epochSeconds,
        )
        val episodes = responses.toEpisodes()
        val entities = episodes.toEpisodeEntities()
        episodeLocalDataSource.upsertEpisodes(entities)
        return episodes
    }
}
