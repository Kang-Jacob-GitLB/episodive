package io.jacob.episodive.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import io.jacob.episodive.core.data.util.paging.FeedWindowPodcastPagingSource
import io.jacob.episodive.core.data.util.paging.PagingDefaults
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.data.util.query.QueryScope
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcast
import io.jacob.episodive.core.database.mapper.toPodcasts
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Feed
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.model.mapper.toFeedsFromRecent
import io.jacob.episodive.core.model.mapper.toFeedsFromTrending
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcasts
import io.jacob.episodive.core.network.mapper.toRecentFeeds
import io.jacob.episodive.core.network.mapper.toTrendingFeeds
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PodcastRepositoryImpl @Inject constructor(
    private val podcastLocalDataSource: PodcastLocalDataSource,
    private val podcastRemoteDataSource: PodcastRemoteDataSource,
    private val feedLocalDataSource: FeedLocalDataSource,
    private val feedRemoteDataSource: FeedRemoteDataSource,
    private val remoteUpdater: PodcastRemoteUpdater.Factory,
) : PodcastRepository {
    private val config = PagingDefaults.DEFAULT_CONFIG

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

    override suspend fun refreshPodcastByFeedId(feedId: Long) {
        remoteUpdater.create(PodcastQuery.FeedId(feedId)).forceRefresh()
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

    override fun getTrendingPodcastsPaging(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
    ): Flow<PagingData<Podcast>> {
        val query = PodcastQuery.Trending(max, language, includeCategories, QueryScope.FULL)

        return feedWindowPagingData(query) {
            feedRemoteDataSource.getTrendingFeeds(
                max = max,
                language = language,
                includeCategories = includeCategories.toCommaString(),
            ).toTrendingFeeds().toFeedsFromTrending()
        }
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

    override fun getRecentPodcastsPaging(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
    ): Flow<PagingData<Podcast>> {
        val query = PodcastQuery.Recent(max, language, includeCategories, QueryScope.FULL)

        return feedWindowPagingData(query) {
            feedRemoteDataSource.getRecentFeeds(
                max = max,
                language = language,
                includeCategories = includeCategories.toCommaString(),
            ).toRecentFeeds().toFeedsFromRecent()
        }
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
        val query = PodcastQuery.Recommended(max, language, includeCategories)

        // 추천만 피드 목록 수명이 10분이다(다른 목록은 1시간). 온보딩에서 한 번 훑고 마는
        // 화면이라 짧게 잡아 둔 값을 그대로 유지한다.
        return feedWindowPagingData(query, timeToLive = 10.minutes) {
            coroutineScope {
                val trending = async {
                    feedRemoteDataSource.getTrendingFeeds(
                        max = max,
                        language = language,
                        includeCategories = includeCategories.toCommaString(),
                    ).toTrendingFeeds().toFeedsFromTrending()
                }
                val recent = async {
                    feedRemoteDataSource.getRecentFeeds(
                        max = max,
                        language = language,
                        includeCategories = includeCategories.toCommaString(),
                    ).toRecentFeeds().toFeedsFromRecent()
                }

                (trending.await() + recent.await())
                    .distinctBy { it.id }
                    .sortedByDescending { it.newestItemPublishTime }
            }
        }
    }

    /**
     * 피드 목록을 먼저 받고 페이지 단위로만 상세를 채우는 팟캐스트 페이징.
     *
     * [query] 는 캐시 키와 기본 수명을 함께 준다 — 키 생성 규칙을 [PodcastQuery] 한 곳에만
     * 두기 위해서다. [fetchFeeds] 가 돌려주는 순서가 그대로 화면 순서가 된다.
     */
    private fun feedWindowPagingData(
        query: PodcastQuery,
        timeToLive: Duration = query.timeToLive,
        fetchFeeds: suspend () -> List<Feed>,
    ): Flow<PagingData<Podcast>> {
        return Pager(
            config = PagingDefaults.FEED_WINDOW_CONFIG,
            pagingSourceFactory = {
                FeedWindowPodcastPagingSource(
                    podcastLocal = podcastLocalDataSource,
                    podcastRemote = podcastRemoteDataSource,
                    feedLocal = feedLocalDataSource,
                    groupKey = query.key,
                    timeToLive = timeToLive,
                    fetchFeeds = fetchFeeds,
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

    override suspend fun getFollowedPodcastsToSync(): Map<Long, Instant> {
        return podcastLocalDataSource.getFollowedPodcastsToSync()
    }

    override suspend fun getFollowedPodcastsOnce(): List<Podcast> {
        return podcastLocalDataSource.getFollowedPodcastsOnce().toPodcasts()
    }

    override suspend fun followPodcast(id: Long): Boolean {
        return podcastLocalDataSource.followPodcast(id)
    }
}