package io.jacob.episodive.core.data.util.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toFeedEntities
import io.jacob.episodive.core.database.mapper.toPodcastEntity
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.GroupKey
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.model.mapper.toFeedsFromRecent
import io.jacob.episodive.core.model.mapper.toFeedsFromTrending
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcast
import io.jacob.episodive.core.network.mapper.toRecentFeeds
import io.jacob.episodive.core.network.mapper.toTrendingFeeds
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import timber.log.Timber
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class RecommendedPodcastPagingSource(
    private val podcastLocal: PodcastLocalDataSource,
    private val podcastRemote: PodcastRemoteDataSource,
    private val feedLocal: FeedLocalDataSource,
    private val feedRemote: FeedRemoteDataSource,
    private val maxFeeds: Int = 1000,
    private val language: String? = null,
    private val categories: List<Category> = emptyList(),
    private val timeToLive: Duration = 10.minutes,
) : PagingSource<Int, PodcastWithExtrasView>() {

    override fun getRefreshKey(state: PagingState<Int, PodcastWithExtrasView>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(state.config.pageSize)
                ?: anchorPage?.nextKey?.minus(state.config.pageSize)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PodcastWithExtrasView> {
        return try {
            ensureRecommendedPodcastsAreFresh()

            val offset = params.key ?: 0
            val limit = params.loadSize
            Timber.w("offset: $offset, limit: $limit")

            val feeds = feedLocal.getFeedsPagingList(
                groupKey = groupKeyRecommended,
                offset = offset,
                limit = limit
            )

            if (feeds.isEmpty()) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (offset > 0) offset - limit else null,
                    nextKey = null
                )
            }

            val podcastIds = feeds.map { it.id }

            fetchMissingPodcasts(podcastIds, concurrency = limit)

            val allPodcasts = podcastLocal.getPodcastsByIdsOnce(podcastIds)
            val orderedPodcasts = podcastIds.mapNotNull { id ->
                allPodcasts.find { it.podcast.id == id }
            }

            LoadResult.Page(
                data = orderedPodcasts,
                prevKey = if (offset > 0) offset - limit else null,
                nextKey = if (feeds.size == limit) offset + limit else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun ensureRecommendedPodcastsAreFresh() = coroutineScope {
        val oldestCreatedAt = feedLocal.getFeedsOldestCachedAt(groupKeyRecommended)
        val isExpired = oldestCreatedAt?.let {
            Clock.System.now() - it > timeToLive
        } ?: true

        if (isExpired) {
            val trendingResponses = async {
                feedRemote.getTrendingFeeds(
                    max = maxFeeds,
                    language = language,
                    includeCategories = categories.toCommaString(),
                ).toTrendingFeeds().toFeedsFromTrending()
            }
            val recentResponses = async {
                feedRemote.getRecentFeeds(
                    max = maxFeeds,
                    language = language,
                    includeCategories = categories.toCommaString(),
                ).toRecentFeeds().toFeedsFromRecent()
            }

            val feedEntities = (trendingResponses.await() + recentResponses.await())
                .distinctBy { it.id }
                .sortedByDescending { it.newestItemPublishTime }
                .toFeedEntities(groupKey = groupKeyRecommended)

            feedLocal.replaceFeedsByGroupKey(feedEntities, groupKeyRecommended)
            podcastLocal.replacePodcasts(emptyList(), groupKeyRecommended)
            Timber.w("replaceFeedsByGroupKey size: ${feedEntities.size}")
        }
    }

    private suspend fun fetchMissingPodcasts(podcastIds: List<Long>, concurrency: Int) {
        val cachedPodcasts = podcastLocal.getPodcastsByIdsOnce(podcastIds)
        val cachedPodcastIds = cachedPodcasts.map { it.podcast.id }.toSet()
        val missingPodcastIds = podcastIds.filterNot { it in cachedPodcastIds }
        Timber.w("cached size: ${cachedPodcasts.size}, missing size: ${missingPodcastIds.size}")

        if (missingPodcastIds.isEmpty()) return

        val podcastEntities = missingPodcastIds
            .withIndex()
            .asFlow()
            .flatMapMerge(concurrency = concurrency) { (index, podcastId) ->
                flow {
                    try {
                        val podcastResponse = podcastRemote.getPodcastByFeedId(podcastId)
                        podcastResponse?.let {
                            val podcast = it.toPodcast()
                            val podcastEntity = podcast.toPodcastEntity()
                            emit(index to podcastEntity)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            .toList()
            .sortedBy { it.first }
            .map { it.second }

        if (podcastEntities.isNotEmpty()) {
            Timber.w("add recommended podcasts size: ${podcastEntities.size}")
            podcastLocal.upsertPodcastsWithGroup(
                podcasts = podcastEntities,
                groupKey = groupKeyRecommended
            )
        }
    }

    private val groupKeyRecommended: String
        get() = "${GroupKey.RECOMMENDED}:${language ?: "all"}:${categories.toCommaString()}"
}
