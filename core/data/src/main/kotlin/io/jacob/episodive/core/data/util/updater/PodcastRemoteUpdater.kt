package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import io.jacob.episodive.core.model.mapper.toCommaString
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcasts
import io.jacob.episodive.core.network.model.PodcastResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlin.time.Clock

class PodcastRemoteUpdater @AssistedInject constructor(
    private val podcastLocal: PodcastLocalDataSource,
    private val podcastRemote: PodcastRemoteDataSource,
    private val feedRemote: FeedRemoteDataSource,
    @Assisted("query") override val query: PodcastQuery,
) : RemoteUpdater<PodcastQuery, PodcastResponse, PodcastEntity, PodcastWithExtrasView>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: PodcastQuery): PodcastRemoteUpdater
    }

    override suspend fun fetchFromRemote(fetchSize: Int): List<PodcastResponse> {
        return when (query) {
            is PodcastQuery.FeedId -> podcastRemote.getPodcastByFeedId(query.feedId)
                ?.let { listOf(it) } ?: emptyList()

            is PodcastQuery.FeedUrl -> podcastRemote.getPodcastByFeedUrl(query.feedUrl)
                ?.let { listOf(it) } ?: emptyList()

            is PodcastQuery.FeedGuid -> podcastRemote.getPodcastByGuid(query.feedGuid)
                ?.let { listOf(it) } ?: emptyList()

            is PodcastQuery.Medium -> podcastRemote.getPodcastsByMedium(query.medium, fetchSize)
            is PodcastQuery.ByChannel -> podcastRemote.getPodcastsByGuids(query.channel.podcastGuids)

            is PodcastQuery.Trending -> {
                feedRemote.getTrendingFeeds(
                    max = query.max,
                    language = query.language,
                    includeCategories = query.categories.toCommaString(),
                ).asFlow()
                    .flatMapMerge(concurrency = 10) { trend ->
                        flow { emit(podcastRemote.getPodcastByFeedId(trend.id)) }
                    }
                    .filterNotNull()
                    .toList()
            }

            is PodcastQuery.Recent -> {
                feedRemote.getRecentFeeds(
                    max = query.max,
                    language = query.language,
                    includeCategories = query.categories.toCommaString(),
                ).asFlow()
                    .flatMapMerge(concurrency = 10) { recent ->
                        flow { emit(podcastRemote.getPodcastByFeedId(recent.id)) }
                    }
                    .filterNotNull()
                    .toList()
            }

            is PodcastQuery.RecentNew -> {
                feedRemote.getRecentNewFeeds(max = query.max)
                    .asFlow()
                    .flatMapMerge(concurrency = 10) { recentNew ->
                        flow { emit(podcastRemote.getPodcastByFeedId(recentNew.id)) }
                    }
                    .filterNotNull()
                    .toList()
            }

            is PodcastQuery.Recommended -> coroutineScope {
                val trending = async {
                    feedRemote.getTrendingFeeds(
                        max = query.max / 2,
                        language = query.language,
                        includeCategories = query.categories.toCommaString(),
                    ).map { it.id to it.newestItemPublishTime }
                }
                val recent = async {
                    feedRemote.getRecentFeeds(
                        max = query.max / 2,
                        language = query.language,
                        includeCategories = query.categories.toCommaString(),
                    ).map { it.id to it.newestItemPublishTime }
                }

                val recommend = (trending.await() + recent.await())
                    .distinctBy { it.first }
                    .sortedByDescending { it.second }

                recommend.map { it.first }
                    .asFlow()
                    .withIndex()
                    .flatMapMerge(concurrency = 5) { (index, id) ->
                        flow {
                            podcastRemote.getPodcastByFeedId(id)?.let { podcast ->
                                emit(index to podcast)
                            }
                        }.catch { e ->
                            e.printStackTrace()
                        }
                    }
                    .toList()
                    .sortedBy { it.first }
                    .map { it.second }
            }
        }
    }

    override suspend fun convertToEntity(responses: List<PodcastResponse>): List<PodcastEntity> {
        return responses.toPodcasts().toPodcastEntities()
    }

    override suspend fun replaceToLocal(entities: List<PodcastEntity>) {
        podcastLocal.replacePodcasts(entities, query.key)
    }

    override suspend fun isExpired(): Boolean {
        val oldestCreatedAt = podcastLocal.getOldestCreatedAtByGroupKey(query.key)
            ?: return true

        val now = Clock.System.now()
        return now - oldestCreatedAt > query.timeToLive
    }

    override fun getPagingSource(): PagingSource<Int, PodcastWithExtrasView> {
        return podcastLocal.getPodcastsByGroupKeyPaging(query.key)
    }

    override fun getFlowSource(count: Int): Flow<List<PodcastWithExtrasView>> {
        return podcastLocal.getPodcastsByGroupKey(query.key, count)
    }
}