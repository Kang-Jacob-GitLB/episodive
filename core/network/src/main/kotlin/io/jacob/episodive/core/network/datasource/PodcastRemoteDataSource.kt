package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.model.PodcastResponse

interface PodcastRemoteDataSource {
    suspend fun searchPodcasts(
        query: String,
        max: Int? = null,
    ): List<PodcastResponse>

    suspend fun getPodcastByFeedId(feedId: Long): PodcastResponse?

    suspend fun getPodcastByFeedUrl(feedUrl: String): PodcastResponse?

    suspend fun getPodcastByGuid(guid: String): PodcastResponse?

    suspend fun getPodcastsByMedium(
        medium: String,
        max: Int? = null,
    ): List<PodcastResponse>

    suspend fun getPodcastsByGuids(guids: List<String>): List<PodcastResponse>
}