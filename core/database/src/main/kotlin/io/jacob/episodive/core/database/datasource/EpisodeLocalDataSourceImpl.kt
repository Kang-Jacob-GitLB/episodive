package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Instant

class EpisodeLocalDataSourceImpl @Inject constructor(
    private val episodeDao: EpisodeDao,
) : EpisodeLocalDataSource {
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

    override fun getEpisodes(query: String?, limit: Int): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getEpisodes(
            query = query?.ifBlank { null },
            limit = limit,
        )
    }

    override fun getEpisodesPaging(query: String?): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getEpisodesPaging(query?.ifBlank { null })
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

    override fun isLikedEpisode(id: Long): Flow<Boolean> {
        return episodeDao.isLikedEpisode(id)
    }

    override suspend fun toggleLikedEpisode(id: Long): Boolean {
        return episodeDao.toggleLikedEpisode(id)
    }

    override fun getLikedEpisodes(query: String?, limit: Int): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getLikedEpisodes(
            query = query?.ifBlank { null },
            limit = limit,
        )
    }

    override fun getLikedEpisodesPaging(query: String?): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getLikedEpisodesPaging(query?.ifBlank { null })
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
            query = query?.ifBlank { null },
            limit = limit,
        )
    }

    override fun getPlayedEpisodesPaging(
        isCompleted: Boolean?,
        query: String?,
    ): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getPlayedEpisodesPaging(
            isCompleted = isCompleted,
            query = query?.ifBlank { null },
        )
    }
}