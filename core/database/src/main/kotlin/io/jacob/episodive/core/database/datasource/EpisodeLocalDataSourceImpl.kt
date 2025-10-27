package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeDto
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeDto
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

    override suspend fun addLiked(likedEpisode: LikedEpisodeEntity) {
        episodeDao.addLiked(likedEpisode)
    }

    override suspend fun removeLiked(id: Long) {
        episodeDao.removeLiked(id)
    }

    override suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity) {
        episodeDao.upsertPlayed(playedEpisode)
    }

    override suspend fun removePlayed(id: Long) {
        episodeDao.removePlayed(id)
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

    override fun getLikedEpisodes(query: String?): Flow<List<LikedEpisodeDto>> {
        return episodeDao.getLikedEpisodes(query)
    }

    override fun getPlayingEpisodes(query: String?): Flow<List<PlayedEpisodeDto>> {
        return episodeDao.getPlayingEpisodes(query)
    }

    override fun getPlayedEpisodes(query: String?): Flow<List<PlayedEpisodeDto>> {
        return episodeDao.getPlayedEpisodes(query)
    }

    override fun getAllPlayedEpisodes(query: String?): Flow<List<PlayedEpisodeDto>> {
        return episodeDao.getAllPlayedEpisodes(query)
    }

    override fun isLiked(id: Long): Flow<Boolean> {
        return episodeDao.isLiked(id)
    }

    override fun getEpisodeCount(): Flow<Int> {
        return episodeDao.getEpisodeCount()
    }

    override fun getLikedEpisodeCount(): Flow<Int> {
        return episodeDao.getLikedEpisodeCount()
    }

    override fun getPlayingEpisodeCount(): Flow<Int> {
        return episodeDao.getPlayingEpisodeCount()
    }

    override fun getPlayedEpisodeCount(): Flow<Int> {
        return episodeDao.getPlayedEpisodeCount()
    }
}