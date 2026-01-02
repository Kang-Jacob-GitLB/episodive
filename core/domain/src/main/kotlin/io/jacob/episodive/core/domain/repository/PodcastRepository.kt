package io.jacob.episodive.core.domain.repository

import androidx.paging.PagingData
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow

interface PodcastRepository {
    fun searchPodcasts(
        query: String,
        max: Int,
    ): Flow<List<Podcast>>

    fun getPodcastByFeedId(feedId: Long): Flow<Podcast?>

    fun getPodcastByFeedUrl(feedUrl: String): Flow<Podcast?>

    fun getPodcastByGuid(guid: String): Flow<Podcast?>

    fun getPodcastsByMedium(
        medium: String,
        max: Int,
    ): Flow<List<Podcast>>

    fun getPodcastsByMediumPaging(medium: String): Flow<PagingData<Podcast>>

    fun getPodcastsByChannel(channel: Channel): Flow<List<Podcast>>

    fun getPodcastsByChannelPaging(channel: Channel): Flow<PagingData<Podcast>>

    fun getTrendingPodcasts(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<List<Podcast>>

    fun getRecentPodcasts(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<List<Podcast>>

    fun getRecentNewPodcasts(
        max: Int,
    ): Flow<List<Podcast>>

    fun getRecommendedPodcasts(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<List<Podcast>>

    fun getRecommendedPodcastsPaging(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<PagingData<Podcast>>

    fun getFollowedPodcasts(
        query: String? = null,
        max: Int,
    ): Flow<List<Podcast>>

    fun getFollowedPodcastsPaging(query: String? = null): Flow<PagingData<Podcast>>

    suspend fun toggleFollowed(id: Long): Boolean
}