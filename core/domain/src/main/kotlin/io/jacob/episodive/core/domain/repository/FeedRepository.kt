package io.jacob.episodive.core.domain.repository

import androidx.paging.PagingData
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.RecentFeed
import io.jacob.episodive.core.model.RecentNewFeed
import io.jacob.episodive.core.model.Soundbite
import io.jacob.episodive.core.model.TrendingFeed
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getTrendingFeeds(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
        excludeCategories: List<Category> = emptyList(),
    ): Flow<List<TrendingFeed>>

    fun getTrendingFeedsPaging(
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<PagingData<TrendingFeed>>

    fun getRecentFeeds(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
        excludeCategories: List<Category> = emptyList(),
    ): Flow<List<RecentFeed>>

    fun getRecentFeedsPaging(
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<PagingData<RecentFeed>>

    fun getRecentNewFeeds(
        max: Int,
    ): Flow<List<RecentNewFeed>>

    fun getRecentNewFeedsPaging(): Flow<PagingData<RecentNewFeed>>

    fun getRecentSoundbites(max: Int): Flow<List<Soundbite>>

    fun getRecentSoundbitesPaging(): Flow<PagingData<Soundbite>>
}