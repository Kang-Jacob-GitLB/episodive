package io.jacob.episodive.core.domain.repository

import androidx.paging.PagingData
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface EpisodeRepository {
    suspend fun upsertEpisode(episode: Episode)

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

    fun getRandomEpisodes(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
        excludeCategories: List<Category> = emptyList(),
    ): Flow<List<Episode>>

    fun getRecentEpisodes(
        max: Int,
        excludeString: String? = null,
    ): Flow<List<Episode>>

    fun getSoundbiteEpisodes(max: Int): Flow<List<Episode>>

    fun getSoundbiteEpisodesPaging(max: Int): Flow<PagingData<Episode>>

    fun getEpisodeById(id: Long): Flow<Episode?>

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

    fun isLikedEpisode(episode: Episode): Flow<Boolean>

    suspend fun toggleLikedEpisode(episode: Episode): Boolean

    suspend fun updatePlayed(id: Long, position: Duration, isCompleted: Boolean)

    suspend fun updateEpisodeDuration(id: Long, duration: Duration)

    suspend fun replaceEpisodes(episodes: List<Episode>, groupKey: String)

    suspend fun fetchChapters(url: String): List<Chapter>
}