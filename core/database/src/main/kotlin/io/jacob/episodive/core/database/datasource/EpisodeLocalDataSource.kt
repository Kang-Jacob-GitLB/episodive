package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant

interface EpisodeLocalDataSource {
    suspend fun updateEpisodeDuration(id: Long, duration: Duration)
    fun getEpisodeById(id: Long): Flow<EpisodeWithExtrasView?>
    fun getEpisodesByIds(ids: List<Long>): Flow<List<EpisodeWithExtrasView>>
    fun getEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>
    fun getEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>
    fun getEpisodesByGroupKey(groupKey: String, limit: Int): Flow<List<EpisodeWithExtrasView>>
    fun getEpisodesByGroupKeyPaging(groupKey: String): PagingSource<Int, EpisodeWithExtrasView>
    suspend fun getOldestCreatedAtByGroupKey(groupKey: String): Instant?
    suspend fun replaceEpisodes(episodes: List<EpisodeEntity>, groupKey: String)


    fun isLikedEpisode(id: Long): Flow<Boolean>
    suspend fun toggleLikedEpisode(id: Long): Boolean
    fun getLikedEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>
    fun getLikedEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>


    suspend fun updatePlayedEpisode(playedEpisode: PlayedEpisodeEntity)
    suspend fun removePlayedEpisode(id: Long)
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