package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.model.RecentFeedResponse
import io.jacob.episodive.core.network.model.RecentNewFeedResponse
import io.jacob.episodive.core.network.model.RecentNewValueFeedResponse
import io.jacob.episodive.core.network.model.TrendingFeedResponse

interface FeedRemoteDataSource {
    suspend fun getTrendingFeeds(
        max: Int? = null,
        since: Long? = null,
        language: String? = null,
        includeCategories: String? = null,
        excludeCategories: String? = null,
    ): List<TrendingFeedResponse>

    suspend fun getRecentFeeds(
        max: Int? = null,
        since: Long? = null,
        language: String? = null,
        includeCategories: String? = null,
        excludeCategories: String? = null,
    ): List<RecentFeedResponse>

    suspend fun getRecentNewFeeds(
        max: Int? = null,
        since: Long? = null,
    ): List<RecentNewFeedResponse>

    suspend fun getRecentNewValueFeeds(
        max: Int? = null,
        since: Long? = null,
    ): List<RecentNewValueFeedResponse>
}