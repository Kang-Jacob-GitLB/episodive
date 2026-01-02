package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.database.util.asFtsWildcard
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Instant

class EpisodeLocalDataSourceImpl @Inject constructor(
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

    override suspend fun getEpisodesByIdsOneShot(ids: List<Long>): List<EpisodeWithExtrasView> {
        return episodeDao.getEpisodesByIdsOneShot(ids)
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
}