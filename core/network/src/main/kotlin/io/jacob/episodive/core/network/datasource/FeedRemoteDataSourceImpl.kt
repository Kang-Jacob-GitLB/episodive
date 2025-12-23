package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.FeedApi
import io.jacob.episodive.core.network.model.RecentFeedResponse
import io.jacob.episodive.core.network.model.RecentNewFeedResponse
import io.jacob.episodive.core.network.model.RecentNewValueFeedResponse
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import javax.inject.Inject

class FeedRemoteDataSourceImpl @Inject constructor(
    val feedApi: FeedApi,
) : FeedRemoteDataSource {
    override suspend fun getTrendingFeeds(
        max: Int?,
        since: Long?,
        language: String?,
        includeCategories: String?,
        excludeCategories: String?,
    ): List<TrendingFeedResponse> {
        return feedApi.getTrendingFeeds(
            max = max,
            since = since,
            language = language,
            includeCategories = includeCategories,
            excludeCategories = excludeCategories,
        ).dataList
    }

    override suspend fun getRecentFeeds(
        max: Int?,
        since: Long?,
        language: String?,
        includeCategories: String?,
        excludeCategories: String?,
    ): List<RecentFeedResponse> {
        return feedApi.getRecentFeeds(
            max = max,
            since = since,
            language = language,
            includeCategories = includeCategories,
            excludeCategories = excludeCategories,
        ).dataList
    }

    override suspend fun getRecentNewFeeds(
        max: Int?,
        since: Long?,
    ): List<RecentNewFeedResponse> {
        return feedApi.getRecentNewFeeds(
            max = max,
            since = since,
        ).dataList
    }

    override suspend fun getRecentNewValueFeeds(
        max: Int?,
        since: Long?,
    ): List<RecentNewValueFeedResponse> {
        return feedApi.getRecentNewValueFeeds(
            max = max,
            since = since,
        ).dataList
    }
}