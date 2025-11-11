package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.model.EpisodeDto
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow

interface EpisodeLocalDataSource {
    suspend fun upsertEpisode(episode: EpisodeEntity)
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)
    suspend fun deleteEpisode(id: Long)
    suspend fun deleteEpisodes()
    suspend fun deleteEpisodesByCacheKey(cacheKey: String)
    suspend fun replaceEpisodes(episodes: List<EpisodeEntity>)
    fun getEpisode(id: Long): Flow<EpisodeDto?>
    fun getEpisodes(): Flow<List<EpisodeDto>>
    fun getEpisodesByCacheKey(cacheKey: String): Flow<List<EpisodeDto>>

    suspend fun addLiked(likedEpisode: LikedEpisodeEntity)
    suspend fun removeLiked(id: Long)
    fun isLiked(id: Long): Boolean
    suspend fun toggleLiked(id: Long): Boolean
    fun getLikedEpisodes(): Flow<List<EpisodeDto>>

    suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity)
    suspend fun removePlayed(id: Long)
    fun getPlayedEpisodes(): Flow<List<EpisodeDto>>
}