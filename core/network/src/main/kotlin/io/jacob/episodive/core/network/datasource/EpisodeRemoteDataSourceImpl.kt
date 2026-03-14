package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.EpisodeApi
import io.jacob.episodive.core.network.model.EpisodeResponse
import timber.log.Timber
import javax.inject.Inject

class EpisodeRemoteDataSourceImpl @Inject constructor(
    private val episodeApi: EpisodeApi
) : EpisodeRemoteDataSource {
    override suspend fun searchEpisodesByPerson(
        person: String,
        max: Int?,
    ): List<EpisodeResponse> {
        Timber.i("searchEpisodesByPerson person: $person")
        return episodeApi.searchEpisodesByPerson(
            person = person,
            max = max,
        ).dataList
    }

    override suspend fun getEpisodesByFeedId(
        feedId: Long,
        max: Int?,
        since: Long?,
    ): List<EpisodeResponse> {
        Timber.i("getEpisodesByFeedId feedId: $feedId")
        val response = episodeApi.getEpisodesByFeedId(
            feedId = feedId,
            max = max,
            since = since,
        )
        return response.liveEpisodes?.plus(response.dataList) ?: response.dataList
    }

    override suspend fun getEpisodesByFeedUrl(
        feedUrl: String,
        max: Int?,
        since: Long?,
    ): List<EpisodeResponse> {
        Timber.i("getEpisodesByFeedUrl feedUrl: $feedUrl")
        return episodeApi.getEpisodesByFeedUrl(
            feedUrl = feedUrl,
            max = max,
            since = since,
        ).dataList
    }

    override suspend fun getEpisodesByPodcastGuid(
        guid: String,
        max: Int?,
        since: Long?,
    ): List<EpisodeResponse> {
        Timber.i("getEpisodesByPodcastGuid guid: $guid")
        return episodeApi.getEpisodesByPodcastGuid(
            guid = guid,
            max = max,
            since = since,
        ).dataList
    }

    override suspend fun getEpisodeById(id: Long): EpisodeResponse? {
        Timber.i("getEpisodeById: $id")
        return episodeApi.getEpisodeById(id = id).data
    }

    override suspend fun getLiveEpisodes(max: Int?): List<EpisodeResponse> {
        Timber.i("getLiveEpisodes max: $max")
        return episodeApi.getLiveEpisodes(max = max).dataList
    }

    override suspend fun getRandomEpisodes(
        max: Int?,
        language: String?,
        includeCategories: String?,
        excludeCategories: String?,
    ): List<EpisodeResponse> {
        Timber.i("getRandomEpisodes max: $max")
        return episodeApi.getRandomEpisodes(
            max = max,
            language = language,
            includeCategories = includeCategories,
            excludeCategories = excludeCategories,
        ).dataList
    }

    override suspend fun getRecentEpisodes(
        max: Int?,
        excludeString: String?,
    ): List<EpisodeResponse> {
        Timber.i("getRecentEpisodes max: $max")
        return episodeApi.getRecentEpisodes(
            max = max,
            excludeString = excludeString,
        ).dataList
    }
}