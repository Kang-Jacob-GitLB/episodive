package io.jacob.episodive.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import io.jacob.episodive.core.data.util.Cacher
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

class FeedRepositoryImpl @Inject constructor(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
) : FeedRepository {
    private val config = PagingConfig(
        pageSize = 20,
        enablePlaceholders = false,
        prefetchDistance = 5
    )

    override fun getTrendingFeeds(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
        excludeCategories: List<Category>,
    ): Flow<List<TrendingFeed>> {
        val query = FeedQuery.Trending(
            max = max,
            language = language,
            categories = includeCategories
        )

        return Cacher(
            remoteUpdater = TrendingFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query
            ),
            sourceFactory = {
                localDataSource.getTrendingFeedsByCacheKey(query.key, query.max)
            }
        ).flow.map { it.toTrendingFeeds() }
    }

    override fun getTrendingFeedsPaging(
        language: String?,
        includeCategories: List<Category>,
    ): Flow<PagingData<TrendingFeed>> {
        val query = FeedQuery.Trending(
            max = 10,
            language = language,
            categories = includeCategories
        )
        val updater = TrendingFeedRemoteUpdater(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            query = query
        )

        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getTrendingFeedsByCacheKeyPaging(query.key) }
        ).flow
            .onStart {
                coroutineScope {
                    launch {
                        updater.load(
                            localDataSource.getTrendingFeedsByCacheKey(
                                query.key,
                                query.max
                            ).first()
                        )
                    }
                }
            }
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
            max = max,
            language = language,
            categories = includeCategories
        )

        return Cacher(
            remoteUpdater = RecentFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query
            ),
            sourceFactory = {
                localDataSource.getRecentFeedsByCacheKey(query.key, query.max)
            }
        ).flow.map { it.toRecentFeeds() }
    }

    override fun getRecentFeedsPaging(
        language: String?,
        includeCategories: List<Category>,
    ): Flow<PagingData<RecentFeed>> {
        val query = FeedQuery.Recent(
            max = 10,
            language = language,
            categories = includeCategories
        )
        val updater = RecentFeedRemoteUpdater(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            query = query
        )

        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getRecentFeedsByCacheKeyPaging(query.key) }
        ).flow
            .onStart {
                coroutineScope {
                    launch {
                        updater.load(
                            localDataSource.getRecentFeedsByCacheKey(query.key, query.max).first()
                        )
                    }
                }
            }
            .map { pagingData ->
                pagingData.map { it.toRecentFeed() }
            }
    }

    override fun getRecentNewFeeds(
        max: Int,
    ): Flow<List<RecentNewFeed>> {
        val query = FeedQuery.RecentNew(max)

        return Cacher(
            remoteUpdater = RecentNewFeedRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query
            ),
            sourceFactory = {
                localDataSource.getRecentNewFeedsByCacheKey(query.key, query.max)
            }
        ).flow.map { it.toRecentNewFeeds() }
    }

    override fun getRecentNewFeedsPaging(): Flow<PagingData<RecentNewFeed>> {
        val query = FeedQuery.RecentNew(10)
        val updater = RecentNewFeedRemoteUpdater(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            query = query
        )

        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getRecentNewFeedsByCacheKeyPaging(query.key) }
        ).flow
            .onStart {
                coroutineScope {
                    launch {
                        updater.load(
                            localDataSource.getRecentNewFeedsByCacheKey(
                                query.key,
                                query.max
                            ).first()
                        )
                    }
                }
            }
            .map { pagingData ->
                pagingData.map { it.toRecentNewFeed() }
            }
    }

    override fun getRecentSoundbites(max: Int): Flow<List<Soundbite>> {
        val query = FeedQuery.Soundbite(max)

        return Cacher(
            remoteUpdater = SoundbiteRemoteUpdater(
                localDataSource = localDataSource,
                remoteDataSource = remoteDataSource,
                query = query
            ),
            sourceFactory = {
                localDataSource.getSoundbitesByCacheKey(query.key, query.max)
            }
        ).flow.map { it.toSoundbites() }
    }

    override fun getRecentSoundbitesPaging(): Flow<PagingData<Soundbite>> {
        val query = FeedQuery.Soundbite(10)
        val updater = SoundbiteRemoteUpdater(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            query = query
        )

        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getSoundbitesByCacheKeyPaging(query.key) }
        ).flow
            .onStart {
                coroutineScope {
                    launch {
                        updater.load(
                            localDataSource.getSoundbitesByCacheKey(query.key, query.max).first()
                        )
                    }
                }
            }
            .map { pagingData ->
                pagingData.map { it.toSoundbite() }
            }
    }
}