package io.jacob.episodive.core.domain.repository

import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow

interface PodcastRepository {
    fun searchPodcasts(
        query: String,
        max: Int? = null,
    ): Flow<List<Podcast>>

    fun getPodcastByFeedId(feedId: Long): Flow<Podcast?>

    fun getPodcastByFeedUrl(feedUrl: String): Flow<Podcast?>

    fun getPodcastByGuid(guid: String): Flow<Podcast?>

    fun getPodcastsByMedium(
        medium: String,
        max: Int? = null,
    ): Flow<List<Podcast>>

    fun getFollowedPodcasts(query: String? = null): Flow<List<Podcast>>

    suspend fun toggleFollowed(id: Long): Boolean
}