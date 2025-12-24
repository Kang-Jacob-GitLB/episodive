package io.jacob.episodive.core.domain.repository

import androidx.paging.PagingData
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface EpisodeRepository {
    fun searchEpisodesByPerson(
        person: String,
        max: Int,
    ): Flow<List<Episode>>

    fun searchEpisodesByPersonPaging(
        person: String,
    ): Flow<PagingData<Episode>>

    fun getEpisodesByFeedId(
        feedId: Long,
        max: Int,
    ): Flow<List<Episode>>

    fun getEpisodesByFeedIdPaging(
        feedId: Long,
    ): Flow<PagingData<Episode>>

    fun getEpisodesByFeedUrl(
        feedUrl: String,
        max: Int,
    ): Flow<List<Episode>>

    fun getEpisodesByFeedUrlPaging(
        feedUrl: String,
    ): Flow<PagingData<Episode>>

    fun getEpisodesByPodcastGuid(
        guid: String,
        max: Int,
    ): Flow<List<Episode>>

    fun getEpisodesByPodcastGuidPaging(
        guid: String,
    ): Flow<PagingData<Episode>>

    fun getLiveEpisodes(max: Int): Flow<List<Episode>>

    fun getLiveEpisodesPaging(): Flow<PagingData<Episode>>

    fun getRandomEpisodes(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
        excludeCategories: List<Category> = emptyList(),
    ): Flow<List<Episode>>

    fun getRandomEpisodesPaging(
        language: String?,
        includeCategories: List<Category>,
        excludeCategories: List<Category>,
    ): Flow<PagingData<Episode>>

    fun getRecentEpisodes(
        max: Int,
        excludeString: String? = null,
    ): Flow<List<Episode>>

    fun getRecentEpisodesPaging(): Flow<PagingData<Episode>>

    fun getSoundbiteEpisodes(max: Int): Flow<List<Episode>>

    fun getSoundbiteEpisodesPaging(): Flow<PagingData<Episode>>

    fun getEpisodesByIds(ids: List<Long>): Flow<List<Episode>>

    fun getLikedEpisodes(query: String? = null, max: Int): Flow<List<Episode>>

    fun getLikedEpisodesPaging(query: String? = null): Flow<PagingData<Episode>>

    fun getPlayedEpisodes(
        isCompleted: Boolean? = null,
        query: String? = null,
        max: Int,
    ): Flow<List<Episode>>

    fun getPlayedEpisodesPaging(
        isCompleted: Boolean? = null,
        query: String? = null,
    ): Flow<PagingData<Episode>>

    fun isLiked(id: Long): Flow<Boolean>

    suspend fun toggleLiked(id: Long): Boolean

    suspend fun updatePlayed(id: Long, position: Duration, isCompleted: Boolean)

    suspend fun updateEpisodeDuration(id: Long, duration: Duration)

    suspend fun replaceEpisodes(episodes: List<Episode>, groupKey: String)

    suspend fun fetchChapters(url: String): List<Chapter>
}