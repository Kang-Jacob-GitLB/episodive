package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.PodcastApi
import io.jacob.episodive.core.network.model.PodcastResponse
import javax.inject.Inject

class PodcastRemoteDataSourceImpl @Inject constructor(
    private val podcastApi: PodcastApi,
) : PodcastRemoteDataSource {
    override suspend fun searchPodcasts(
        query: String,
        max: Int?,
    ): List<PodcastResponse> {
        return podcastApi.searchPodcasts(
            query = query,
            max = max,
        ).dataList
    }

    override suspend fun getPodcastByFeedId(feedId: Long): PodcastResponse? {
        return podcastApi.getPodcastByFeedId(feedId = feedId).data
    }

    override suspend fun getPodcastByFeedUrl(feedUrl: String): PodcastResponse? {
        return podcastApi.getPodcastByFeedUrl(feedUrl = feedUrl).data
    }

    override suspend fun getPodcastByGuid(guid: String): PodcastResponse? {
        return podcastApi.getPodcastByGuid(guid = guid).data
    }

    override suspend fun getPodcastsByMedium(
        medium: String,
        max: Int?,
    ): List<PodcastResponse> {
        return podcastApi.getPodcastsByMedium(
            medium = medium,
            max = max,
        ).dataList
    }

    override suspend fun getPodcastsByGuids(guids: List<String>): List<PodcastResponse> {
        return podcastApi.getPodcastsByGuids(guids = guids).dataList
    }
}