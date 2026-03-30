package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.database.util.asFtsWildcard
import io.jacob.episodive.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Instant

class EpisodeLocalDataSourceImpl @Inject constructor(
    override val database: RoomDatabase,
    private val episodeDao: EpisodeDao,
) : EpisodeLocalDataSource {
    override suspend fun upsertEpisode(episode: EpisodeEntity) {
        episodeDao.upsertEpisode(episode)
    }

    override suspend fun upsertEpisodesWithGroup(episodes: List<EpisodeEntity>, groupKey: String) {
        episodeDao.upsertEpisodesWithGroup(episodes, groupKey)
    }

    override suspend fun updateEpisodeDuration(id: Long, duration: Duration) {
        episodeDao.updateEpisodeDuration(
            id = id,
            duration = duration,
        )
    }

    override fun getEpisodeById(id: Long): Flow<EpisodeWithExtrasView?> {
        return episodeDao.getEpisodeById(id)
    }

    override fun getEpisodesByIds(ids: List<Long>): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getEpisodesByIds(ids)
    }

    override suspend fun getEpisodesByIdsOnce(ids: List<Long>): List<EpisodeWithExtrasView> {
        return episodeDao.getEpisodesByIdsOnce(ids)
    }

    override fun getEpisodes(query: String?, limit: Int): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getEpisodes(
            query = query?.asFtsWildcard(),
            limit = limit,
        )
    }

    override fun getEpisodesPaging(query: String?): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getEpisodesPaging(query?.asFtsWildcard())
    }

    override fun getEpisodesByGroupKey(
        groupKey: String,
        limit: Int,
    ): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getEpisodesByGroupKey(
            groupKey = groupKey,
            limit = limit,
        )
    }

    override fun getEpisodesByGroupKeyPaging(groupKey: String): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getEpisodesByGroupKeyPaging(groupKey)
    }

    override suspend fun getOldestCreatedAtByGroupKey(groupKey: String): Instant? {
        return episodeDao.getOldestCreatedAtByGroupKey(groupKey)
    }

    override suspend fun replaceEpisodes(episodes: List<EpisodeEntity>, groupKey: String) {
        episodeDao.replaceEpisodes(
            episodes = episodes,
            groupKey = groupKey,
        )
    }

    override fun isLikedEpisode(episode: EpisodeEntity): Flow<Boolean> {
        return episodeDao.isLikedEpisode(episode.id)
    }

    override suspend fun toggleLikedEpisode(episode: EpisodeEntity): Boolean {
        return episodeDao.toggleLikedEpisode(episode)
    }

    override fun getLikedEpisodes(query: String?, limit: Int): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getLikedEpisodes(
            query = query?.asFtsWildcard(),
            limit = limit,
        )
    }

    override fun getLikedEpisodesPaging(query: String?): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getLikedEpisodesPaging(query?.asFtsWildcard())
    }

    override suspend fun updatePlayedEpisode(playedEpisode: PlayedEpisodeEntity) {
        episodeDao.upsertPlayedEpisode(playedEpisode)
    }

    override suspend fun removePlayedEpisode(id: Long) {
        episodeDao.removePlayedEpisode(id)
    }

    override fun getPlayedEpisodes(
        isCompleted: Boolean?,
        query: String?,
        limit: Int,
    ): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getPlayedEpisodes(
            isCompleted = isCompleted,
            query = query?.asFtsWildcard(),
            limit = limit,
        )
    }

    override fun getPlayedEpisodesPaging(
        isCompleted: Boolean?,
        query: String?,
    ): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getPlayedEpisodesPaging(
            isCompleted = isCompleted,
            query = query?.asFtsWildcard(),
        )
    }

    override fun isSavedEpisode(episode: EpisodeEntity): Flow<Boolean> {
        return episodeDao.isSavedEpisode(episode.id)
    }

    override suspend fun toggleSavedEpisode(episode: EpisodeEntity, filePath: String): Boolean {
        return episodeDao.toggleSavedEpisode(episode, filePath)
    }

    override suspend fun updateSavedEpisodeProgress(
        id: Long,
        downloadedSize: Long,
        status: DownloadStatus,
    ) {
        episodeDao.updateSavedEpisodeProgress(id, downloadedSize, status)
    }

    override suspend fun updateSavedEpisodeStatus(id: Long, status: DownloadStatus) {
        episodeDao.updateSavedEpisodeStatus(id, status)
    }

    override suspend fun removeSavedEpisode(id: Long) {
        episodeDao.removeSavedEpisode(id)
    }

    override fun getSavedEpisodes(query: String?, limit: Int): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getSavedEpisodes(
            query = query?.asFtsWildcard(),
            limit = limit,
        )
    }

    override fun getSavedEpisodesPaging(query: String?): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getSavedEpisodesPaging(query?.asFtsWildcard())
    }
}