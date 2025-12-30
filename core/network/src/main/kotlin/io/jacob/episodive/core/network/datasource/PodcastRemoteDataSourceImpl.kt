package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.PodcastApi
import io.jacob.episodive.core.network.model.PodcastResponse
import timber.log.Timber
import javax.inject.Inject

class PodcastRemoteDataSourceImpl @Inject constructor(
    private val podcastApi: PodcastApi,
) : PodcastRemoteDataSource {
    override suspend fun searchPodcasts(
        query: String,
        max: Int?,
    ): List<PodcastResponse> {
        Timber.i("searchPodcasts query: $query")
        return podcastApi.searchPodcasts(
            query = query,
            max = max,
        ).dataList
    }

    override suspend fun getPodcastByFeedId(feedId: Long): PodcastResponse? {
        Timber.i("getPodcastByFeedId feedId: $feedId")
        return podcastApi.getPodcastByFeedId(feedId = feedId).data
    }

    override suspend fun getPodcastByFeedUrl(feedUrl: String): PodcastResponse? {
        Timber.i("getPodcastByFeedUrl feedUrl: $feedUrl")
        return podcastApi.getPodcastByFeedUrl(feedUrl = feedUrl).data
    }

    override suspend fun getPodcastByGuid(guid: String): PodcastResponse? {
        Timber.i("getPodcastByGuid guid: $guid")
        return podcastApi.getPodcastByGuid(guid = guid).data
    }

    override suspend fun getPodcastsByMedium(
        medium: String,
        max: Int?,
    ): List<PodcastResponse> {
        Timber.i("getPodcastsByMedium medium: $medium")
        return podcastApi.getPodcastsByMedium(
            medium = medium,
            max = max,
        ).dataList
    }

    override suspend fun getPodcastsByGuids(guids: List<String>): List<PodcastResponse> {
        Timber.i("getPodcastsByGuids guids: $guids")
        return podcastApi.getPodcastsByGuids(guids = guids).dataList
    }
}