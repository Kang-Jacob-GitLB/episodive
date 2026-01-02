package io.jacob.episodive.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import io.jacob.episodive.core.data.util.paging.RecommendedPodcastPagingSource
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcast
import io.jacob.episodive.core.database.mapper.toPodcasts
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcasts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PodcastRepositoryImpl @Inject constructor(
    private val podcastLocalDataSource: PodcastLocalDataSource,
    private val podcastRemoteDataSource: PodcastRemoteDataSource,
    private val feedLocalDataSource: FeedLocalDataSource,
    private val feedRemoteDataSource: FeedRemoteDataSource,
    private val remoteUpdater: PodcastRemoteUpdater.Factory,
) : PodcastRepository {
    private val config = PagingConfig(
        pageSize = 20,
        prefetchDistance = 5,
        enablePlaceholders = false
    )

    override fun searchPodcasts(
        query: String,
        max: Int,
    ): Flow<List<Podcast>> = flow {
        podcastRemoteDataSource.searchPodcasts(
            query = query,
            max = max,
        ).toPodcasts()
            .let { emit(it) }
    }

    override fun getPodcastByFeedId(feedId: Long): Flow<Podcast?> {
        val query = PodcastQuery.FeedId(feedId)

        return remoteUpdater.create(query)
            .getFlowList(1)
            .map { it.firstOrNull()?.toPodcast() }
    }

    override fun getPodcastByFeedUrl(feedUrl: String): Flow<Podcast?> {
        val query = PodcastQuery.FeedUrl(feedUrl)

        return remoteUpdater.create(query)
            .getFlowList(1)
            .map { it.firstOrNull()?.toPodcast() }
    }

    override fun getPodcastByGuid(guid: String): Flow<Podcast?> {
        val query = PodcastQuery.FeedGuid(guid)

        return remoteUpdater.create(query)
            .getFlowList(1)
            .map { it.firstOrNull()?.toPodcast() }
    }

    override fun getPodcastsByMedium(
        medium: String,
        max: Int,
    ): Flow<List<Podcast>> {
        val query = PodcastQuery.Medium(medium)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toPodcasts() }
    }

    override fun getPodcastsByMediumPaging(medium: String): Flow<PagingData<Podcast>> {
        val query = PodcastQuery.Medium(medium)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toPodcast() }
            }
    }

    override fun getPodcastsByChannel(channel: Channel): Flow<List<Podcast>> {
        val query = PodcastQuery.ByChannel(channel)

        return remoteUpdater.create(query)
            .getFlowList(100)
            .map { it.toPodcasts() }
    }

    override fun getPodcastsByChannelPaging(channel: Channel): Flow<PagingData<Podcast>> {
        val query = PodcastQuery.ByChannel(channel)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toPodcast() }
            }
    }

    override fun getTrendingPodcasts(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
    ): Flow<List<Podcast>> {
        val query = PodcastQuery.Trending(max, language, includeCategories)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toPodcasts() }
    }

    override fun getRecentPodcasts(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
    ): Flow<List<Podcast>> {
        val query = PodcastQuery.Recent(max, language, includeCategories)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toPodcasts() }
    }

    override fun getRecentNewPodcasts(max: Int): Flow<List<Podcast>> {
        val query = PodcastQuery.RecentNew(max)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toPodcasts() }
    }

    override fun getRecommendedPodcasts(
        max: Int,
        language: String?,
        includeCategories: List<Category>
    ): Flow<List<Podcast>> {
        val query = PodcastQuery.Recommended(max, language, includeCategories)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toPodcasts() }
    }

    override fun getRecommendedPodcastsPaging(
        max: Int,
        language: String?,
        includeCategories: List<Category>
    ): Flow<PagingData<Podcast>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                prefetchDistance = 5,
                initialLoadSize = 10,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                RecommendedPodcastPagingSource(
                    podcastLocal = podcastLocalDataSource,
                    podcastRemote = podcastRemoteDataSource,
                    feedLocal = feedLocalDataSource,
                    feedRemote = feedRemoteDataSource,
                    maxFeeds = max,
                    language = language,
                    categories = includeCategories,
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toPodcast() }
        }
    }

    override fun getFollowedPodcasts(query: String?, max: Int): Flow<List<Podcast>> {
        return podcastLocalDataSource.getFollowedPodcasts(query, max)
            .map { podcasts ->
                podcasts.toPodcasts()
            }
    }

    override fun getFollowedPodcastsPaging(query: String?): Flow<PagingData<Podcast>> {
        return Pager(
            config = config,
            pagingSourceFactory = { podcastLocalDataSource.getFollowedPodcastsPaging(query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toPodcast() }
        }
    }

    override suspend fun toggleFollowed(id: Long): Boolean {
        return podcastLocalDataSource.toggleFollowedPodcast(id)
    }
}