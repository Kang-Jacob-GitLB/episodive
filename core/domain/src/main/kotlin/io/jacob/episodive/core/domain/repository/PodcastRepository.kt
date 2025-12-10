package io.jacob.episodive.core.domain.repository

import androidx.paging.PagingData
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow

interface PodcastRepository {
    fun searchPodcasts(
        query: String,
        max: Int,
    ): Flow<List<Podcast>>

    fun searchPodcastsPaging(query: String): Flow<PagingData<Podcast>>

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

    fun getFollowedPodcasts(
        query: String? = null,
        max: Int,
    ): Flow<List<Podcast>>

    fun getFollowedPodcastsPaging(query: String? = null): Flow<PagingData<Podcast>>

    suspend fun toggleFollowed(id: Long): Boolean
}