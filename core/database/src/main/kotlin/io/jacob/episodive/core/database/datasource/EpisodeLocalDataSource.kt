package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeDto
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeDto
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow

interface EpisodeLocalDataSource {
    suspend fun upsertEpisode(episode: EpisodeEntity)
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)
    suspend fun deleteEpisode(id: Long)
    suspend fun deleteEpisodes()
    suspend fun deleteEpisodesByCacheKey(cacheKey: String)
    suspend fun addLiked(likedEpisode: LikedEpisodeEntity)
    suspend fun removeLiked(id: Long)
    suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity)
    suspend fun removePlayed(id: Long)
    fun getEpisode(id: Long): Flow<EpisodeEntity?>
    fun getEpisodes(): Flow<List<EpisodeEntity>>
    fun getEpisodesByCacheKey(cacheKey: String): Flow<List<EpisodeEntity>>
    fun getLikedEpisodes(query: String? = null): Flow<List<LikedEpisodeDto>>
    fun getPlayingEpisodes(query: String? = null): Flow<List<PlayedEpisodeDto>>
    fun getPlayedEpisodes(query: String? = null): Flow<List<PlayedEpisodeDto>>
    fun getAllPlayedEpisodes(query: String? = null): Flow<List<PlayedEpisodeDto>>
    fun isLiked(id: Long): Flow<Boolean>
    fun getEpisodeCount(): Flow<Int>
    fun getLikedEpisodeCount(): Flow<Int>
    fun getPlayingEpisodeCount(): Flow<Int>
    fun getPlayedEpisodeCount(): Flow<Int>
}