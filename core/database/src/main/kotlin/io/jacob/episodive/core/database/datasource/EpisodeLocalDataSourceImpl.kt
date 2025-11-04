package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

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

    override fun getEpisode(id: Long): Flow<EpisodeEntity?> {
        return episodeDao.getEpisode(id)
    }

    override fun getEpisodes(): Flow<List<EpisodeEntity>> {
        return episodeDao.getEpisodes()
    }

    override fun getEpisodesByCacheKey(cacheKey: String): Flow<List<EpisodeEntity>> {
        return episodeDao.getEpisodesByCacheKey(cacheKey)
    }

    override suspend fun addLiked(likedEpisode: LikedEpisodeEntity) {
        episodeDao.addLiked(likedEpisode)
    }

    override suspend fun removeLiked(id: Long) {
        episodeDao.removeLiked(id)
    }

    override fun isLiked(id: Long): Boolean {
        return episodeDao.isLiked(id)
    }

    override suspend fun toggleLiked(id: Long): Boolean {
        return episodeDao.toggleLiked(id)
    }

    override fun getLikedEpisodes(): Flow<List<LikedEpisodeEntity>> {
        return episodeDao.getLikedEpisodes()
    }

    override suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity) {
        episodeDao.upsertPlayed(playedEpisode)
    }

    override suspend fun removePlayed(id: Long) {
        episodeDao.removePlayed(id)
    }

    override fun getPlayedEpisodes(): Flow<List<PlayedEpisodeEntity>> {
        return episodeDao.getPlayedEpisodes()
    }
}