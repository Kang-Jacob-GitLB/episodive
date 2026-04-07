package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant

interface EpisodeLocalDataSource {
    val database: RoomDatabase
    suspend fun upsertEpisode(episode: EpisodeEntity)
    suspend fun upsertEpisodesWithGroup(episodes: List<EpisodeEntity>, groupKey: String)
    suspend fun updateEpisodeDuration(id: Long, duration: Duration)
    fun getEpisodeById(id: Long): Flow<EpisodeWithExtrasView?>
    fun getEpisodesByIds(ids: List<Long>): Flow<List<EpisodeWithExtrasView>>
    suspend fun getEpisodesByIdsOnce(ids: List<Long>): List<EpisodeWithExtrasView>
    fun getEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>
    fun getEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>
    fun getEpisodesByGroupKey(groupKey: String, limit: Int): Flow<List<EpisodeWithExtrasView>>
    fun getEpisodesByGroupKeyPaging(groupKey: String): PagingSource<Int, EpisodeWithExtrasView>
    suspend fun getOldestCreatedAtByGroupKey(groupKey: String): Instant?
    suspend fun replaceEpisodes(episodes: List<EpisodeEntity>, groupKey: String)


    fun isLikedEpisode(episode: EpisodeEntity): Flow<Boolean>
    suspend fun toggleLikedEpisode(episode: EpisodeEntity): Boolean
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


    fun isSavedEpisode(episode: EpisodeEntity): Flow<Boolean>
    suspend fun toggleSavedEpisode(episode: EpisodeEntity, filePath: String): Boolean
    suspend fun updateSavedEpisodeProgress(id: Long, downloadedSize: Long, status: DownloadStatus)
    suspend fun updateSavedEpisodeStatus(id: Long, status: DownloadStatus)
    suspend fun removeSavedEpisode(id: Long)
    fun getSavedEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>
    fun getSavedEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>

    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)
    suspend fun getLatestEpisodeDatePublished(feedId: Long): Instant?
}