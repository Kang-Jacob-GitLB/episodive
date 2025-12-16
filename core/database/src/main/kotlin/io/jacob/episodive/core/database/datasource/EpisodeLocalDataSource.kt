package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.model.EpisodeDto
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant

interface EpisodeLocalDataSource {
    suspend fun upsertEpisode(episode: EpisodeEntity)
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)
    suspend fun deleteEpisode(id: Long)
    suspend fun deleteEpisodes()
    suspend fun deleteEpisodesByCacheKey(cacheKey: String)
    suspend fun replaceEpisodes(episodes: List<EpisodeEntity>)
    suspend fun updateDurationOfEpisodes(id: Long, duration: Duration)
    fun getEpisodeById(id: Long): Flow<EpisodeDto?>
    fun getEpisodesByIds(ids: List<Long>): Flow<List<EpisodeDto>>
    fun getEpisodes(limit: Int): Flow<List<EpisodeDto>>
    fun getEpisodesPaging(): PagingSource<Int, EpisodeDto>
    fun getEpisodesByCacheKey(cacheKey: String, limit: Int): Flow<List<EpisodeDto>>
    fun getEpisodesByCacheKeyPaging(cacheKey: String): PagingSource<Int, EpisodeDto>
    suspend fun getEpisodesOldestCachedAtByCacheKey(cacheKey: String): Instant?

    suspend fun addLiked(likedEpisode: LikedEpisodeEntity)
    suspend fun removeLiked(id: Long)
    fun isLiked(id: Long): Flow<Boolean>
    suspend fun toggleLiked(id: Long): Boolean
    fun getLikedEpisodes(limit: Int): Flow<List<EpisodeDto>>
    fun getLikedEpisodesPaging(): PagingSource<Int, EpisodeDto>

    suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity)
    suspend fun removePlayed(id: Long)
    fun getPlayedEpisodes(limit: Int): Flow<List<EpisodeDto>>
    fun getPlayedEpisodesPaging(): PagingSource<Int, EpisodeDto>
}