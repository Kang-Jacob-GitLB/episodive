package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeGroupEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant

interface EpisodeLocalDataSource {
    suspend fun upsertEpisode(episode: EpisodeEntity)
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)
    suspend fun upsertEpisodeGroup(episodeGroup: EpisodeGroupEntity)
    suspend fun upsertEpisodeGroups(episodeGroups: List<EpisodeGroupEntity>)
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>, groupKey: String)
    suspend fun deleteEpisode(id: Long)
    suspend fun deleteEpisodes()
    suspend fun deleteEpisodesByGroupKey(groupKey: String)
    suspend fun replaceEpisodes(episodes: List<EpisodeEntity>, groupKey: String)
    suspend fun updateDurationOfEpisodes(id: Long, duration: Duration)
    fun getEpisodeById(id: Long): Flow<EpisodeWithExtrasView?>
    fun getEpisodesByIds(ids: List<Long>): Flow<List<EpisodeWithExtrasView>>
    fun getEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>
    fun getEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>
    fun getEpisodesByGroupKey(groupKey: String, limit: Int): Flow<List<EpisodeWithExtrasView>>
    fun getEpisodesByGroupKeyPaging(groupKey: String): PagingSource<Int, EpisodeWithExtrasView>
    suspend fun getEpisodesOldestCreatedAtByGroupKey(groupKey: String): Instant?


    suspend fun addLiked(likedEpisode: LikedEpisodeEntity)
    suspend fun removeLiked(id: Long)
    fun isLiked(id: Long): Flow<Boolean>
    suspend fun toggleLiked(id: Long): Boolean
    fun getLikedEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>
    fun getLikedEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>


    suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity)
    suspend fun removePlayed(id: Long)
    fun getPlayedEpisodes(
        isCompleted: Boolean? = null,
        query: String? = null,
        limit: Int,
    ): Flow<List<EpisodeWithExtrasView>>

    fun getPlayedEpisodesPaging(
        isCompleted: Boolean? = null,
        query: String? = null,
    ): PagingSource<Int, EpisodeWithExtrasView>
}