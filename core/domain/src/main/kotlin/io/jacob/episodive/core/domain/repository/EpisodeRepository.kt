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
        max: Int = 5,
    ): Flow<List<Episode>>

    fun searchEpisodesByPersonPaging(
        person: String,
    ): Flow<PagingData<Episode>>

    fun getEpisodesByFeedId(
        feedId: Long,
        max: Int = 5,
    ): Flow<List<Episode>>

    fun getEpisodesByFeedIdPaging(
        feedId: Long,
    ): Flow<PagingData<Episode>>

    fun getEpisodesByFeedUrl(
        feedUrl: String,
        max: Int = 5,
    ): Flow<List<Episode>>

    fun getEpisodesByFeedUrlPaging(
        feedUrl: String,
    ): Flow<PagingData<Episode>>

    fun getEpisodesByPodcastGuid(
        guid: String,
        max: Int = 5,
    ): Flow<List<Episode>>

    fun getEpisodesByPodcastGuidPaging(
        guid: String,
    ): Flow<PagingData<Episode>>

    fun getEpisodeById(id: Long): Flow<Episode?>

    fun getLiveEpisodes(max: Int = 6): Flow<List<Episode>>

    fun getLiveEpisodesPaging(): Flow<PagingData<Episode>>

    fun getRandomEpisodes(
        max: Int = 6,
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
        max: Int = 6,
        excludeString: String? = null,
    ): Flow<List<Episode>>

    fun getRecentEpisodesPaging(): Flow<PagingData<Episode>>

    fun getLikedEpisodes(query: String? = null, max: Int = -1): Flow<List<Episode>>

    fun getLikedEpisodesPaging(): Flow<PagingData<Episode>>

    fun getPlayingEpisodes(query: String? = null, max: Int = -1): Flow<List<Episode>>

    fun getPlayingEpisodesPaging(): Flow<PagingData<Episode>>

    fun getPlayedEpisodes(query: String? = null, max: Int = -1): Flow<List<Episode>>

    fun getPlayedEpisodesPaging(): Flow<PagingData<Episode>>

    fun getAllPlayedEpisodes(query: String? = null, max: Int = -1): Flow<List<Episode>>

    fun getAllPlayedEpisodesPaging(): Flow<PagingData<Episode>>

    fun isLiked(id: Long): Flow<Boolean>

    suspend fun toggleLiked(id: Long): Boolean

    suspend fun updatePlayed(id: Long, position: Duration, isCompleted: Boolean)

    suspend fun updateDurationOfEpisodes(id: Long, duration: Duration)

    suspend fun fetchChapters(url: String): List<Chapter>
}