package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.model.EpisodeDto
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.time.Duration

class EpisodeLocalDataSourceImpl @Inject constructor(
    private val episodeDao: EpisodeDao,
) : EpisodeLocalDataSource {
    override suspend fun upsertEpisode(episode: EpisodeEntity) {
        episodeDao.upsertEpisode(episode)
    }

    override suspend fun upsertEpisodes(episodes: List<EpisodeEntity>) {
        episodeDao.upsertEpisodes(episodes)
    }

    override suspend fun deleteEpisode(id: Long) {
        episodeDao.deleteEpisode(id)
    }

    override suspend fun deleteEpisodes() {
        episodeDao.deleteEpisodes()
    }

    override suspend fun deleteEpisodesByCacheKey(cacheKey: String) {
        episodeDao.deleteEpisodesByCacheKey(cacheKey)
    }

    override suspend fun replaceEpisodes(episodes: List<EpisodeEntity>) {
        episodeDao.replaceEpisodes(episodes)
    }

    override suspend fun updateDurationOfEpisodes(id: Long, duration: Duration) {
        episodeDao.updateDurationOfEpisodes(id, duration)
    }

    override fun getEpisode(id: Long): Flow<EpisodeDto?> {
        return episodeDao.getEpisode(id)
    }

    override fun getEpisodes(limit: Int): Flow<List<EpisodeDto>> {
        return episodeDao.getEpisodes(limit)
    }

    override fun getEpisodesPaging(): PagingSource<Int, EpisodeDto> {
        return episodeDao.getEpisodesPaging()
    }

    override fun getEpisodesByCacheKey(cacheKey: String, limit: Int): Flow<List<EpisodeDto>> {
        return episodeDao.getEpisodesByCacheKey(cacheKey, limit)
    }

    override fun getEpisodesByCacheKeyPaging(cacheKey: String): PagingSource<Int, EpisodeDto> {
        return episodeDao.getEpisodesByCacheKeyPaging(cacheKey)
    }

    override suspend fun addLiked(likedEpisode: LikedEpisodeEntity) {
        episodeDao.addLiked(likedEpisode)
    }

    override suspend fun removeLiked(id: Long) {
        episodeDao.removeLiked(id)
    }

    override fun isLiked(id: Long): Flow<Boolean> {
        return episodeDao.isLiked(id)
    }

    override suspend fun toggleLiked(id: Long): Boolean {
        return episodeDao.toggleLiked(id)
    }

    override fun getLikedEpisodes(limit: Int): Flow<List<EpisodeDto>> {
        return episodeDao.getLikedEpisodes(limit)
    }

    override fun getLikedEpisodesPaging(): PagingSource<Int, EpisodeDto> {
        return episodeDao.getLikedEpisodesPaging()
    }

    override suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity) {
        episodeDao.upsertPlayed(playedEpisode)
    }

    override suspend fun removePlayed(id: Long) {
        episodeDao.removePlayed(id)
    }

    override fun getPlayedEpisodes(limit: Int): Flow<List<EpisodeDto>> {
        return episodeDao.getPlayedEpisodes(limit)
    }

    override fun getPlayedEpisodesPaging(): PagingSource<Int, EpisodeDto> {
        return episodeDao.getPlayedEpisodesPaging()
    }
}