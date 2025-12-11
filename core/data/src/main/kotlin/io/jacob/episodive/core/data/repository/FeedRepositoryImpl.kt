package io.jacob.episodive.core.data.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.data.util.updater.RecentFeedRemoteUpdater
import io.jacob.episodive.core.data.util.updater.RecentNewFeedRemoteUpdater
import io.jacob.episodive.core.data.util.updater.SoundbiteRemoteUpdater
import io.jacob.episodive.core.data.util.updater.TrendingFeedRemoteUpdater
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toRecentFeed
import io.jacob.episodive.core.database.mapper.toRecentFeeds
import io.jacob.episodive.core.database.mapper.toRecentNewFeed
import io.jacob.episodive.core.database.mapper.toRecentNewFeeds
import io.jacob.episodive.core.database.mapper.toSoundbite
import io.jacob.episodive.core.database.mapper.toSoundbites
import io.jacob.episodive.core.database.mapper.toTrendingFeed
import io.jacob.episodive.core.database.mapper.toTrendingFeeds
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.RecentFeed
import io.jacob.episodive.core.model.RecentNewFeed
import io.jacob.episodive.core.model.Soundbite
import io.jacob.episodive.core.model.TrendingFeed
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FeedRepositoryImpl @Inject constructor(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    private val trendingFeedRemoteUpdater: TrendingFeedRemoteUpdater.Factory,
    private val recentFeedRemoteUpdater: RecentFeedRemoteUpdater.Factory,
    private val recentNewFeedRemoteUpdater: RecentNewFeedRemoteUpdater.Factory,
    private val soundbiteRemoteUpdater: SoundbiteRemoteUpdater.Factory,
) : FeedRepository {
    private val config = PagingConfig(
        pageSize = 20,
        prefetchDistance = 5,
        enablePlaceholders = false
    )

    override fun getTrendingFeeds(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
        excludeCategories: List<Category>,
    ): Flow<List<TrendingFeed>> {
        val query = FeedQuery.Trending(
            language = language,
            categories = includeCategories
        )

        return trendingFeedRemoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toTrendingFeeds() }
    }

    override fun getTrendingFeedsPaging(
        language: String?,
        includeCategories: List<Category>,
    ): Flow<PagingData<TrendingFeed>> {
        val query = FeedQuery.Trending(
            language = language,
            categories = includeCategories
        )

        return trendingFeedRemoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toTrendingFeed() }
            }
    }

    override fun getRecentFeeds(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
        excludeCategories: List<Category>,
    ): Flow<List<RecentFeed>> {
        val query = FeedQuery.Recent(
            language = language,
            categories = includeCategories
        )

        return recentFeedRemoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toRecentFeeds() }
    }

    override fun getRecentFeedsPaging(
        language: String?,
        includeCategories: List<Category>,
    ): Flow<PagingData<RecentFeed>> {
        val query = FeedQuery.Recent(
            language = language,
            categories = includeCategories
        )

        return recentFeedRemoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toRecentFeed() }
            }
    }

    override fun getRecentNewFeeds(
        max: Int,
    ): Flow<List<RecentNewFeed>> {
        val query = FeedQuery.RecentNew

        return recentNewFeedRemoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toRecentNewFeeds() }
    }

    override fun getRecentNewFeedsPaging(): Flow<PagingData<RecentNewFeed>> {
        val query = FeedQuery.RecentNew

        return recentNewFeedRemoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toRecentNewFeed() }
            }
    }

    override fun getRecentSoundbites(max: Int): Flow<List<Soundbite>> {
        val query = FeedQuery.Soundbite

        return soundbiteRemoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toSoundbites() }
    }

    override fun getRecentSoundbitesPaging(): Flow<PagingData<Soundbite>> {
        val query = FeedQuery.Soundbite

        return soundbiteRemoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toSoundbite() }
            }
    }
}