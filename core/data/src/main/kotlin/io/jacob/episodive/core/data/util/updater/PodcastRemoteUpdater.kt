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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
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
            is PodcastQuery.Search -> podcastRemote.searchPodcasts(query.query, fetchSize)
            is PodcastQuery.FeedId -> podcastRemote.getPodcastByFeedId(query.feedId)
                ?.let { listOf(it) } ?: emptyList()

            is PodcastQuery.FeedUrl -> podcastRemote.getPodcastByFeedUrl(query.feedUrl)
                ?.let { listOf(it) } ?: emptyList()

            is PodcastQuery.FeedGuid -> podcastRemote.getPodcastByGuid(query.feedGuid)
                ?.let { listOf(it) } ?: emptyList()

            is PodcastQuery.Medium -> podcastRemote.getPodcastsByMedium(query.medium, fetchSize)
            is PodcastQuery.ByChannel -> podcastRemote.getPodcastsByGuids(query.channel.podcastGuids)

            is PodcastQuery.Trending -> coroutineScope {
                val trends = feedRemote.getTrendingFeeds(
                    max = 100,
                    language = query.language,
                    includeCategories = query.categories.toCommaString(),
                )

                trends.chunked(20).flatMap { chunk ->
                    chunk.map { trend ->
                        async {
                            podcastRemote.getPodcastByFeedId(trend.id)
                        }
                    }.awaitAll().filterNotNull()
                }
            }

            is PodcastQuery.Recent -> coroutineScope {
                val recents = feedRemote.getRecentFeeds(
                    max = 100,
                    language = query.language,
                    includeCategories = query.categories.toCommaString(),
                )

                recents.chunked(20).flatMap { chunk ->
                    chunk.map { recent ->
                        async {
                            podcastRemote.getPodcastByFeedId(recent.id)
                        }
                    }.awaitAll().filterNotNull()
                }
            }

            is PodcastQuery.RecentNew -> coroutineScope {
                val recents = feedRemote.getRecentNewFeeds(max = 100)

                recents.chunked(20).flatMap { chunk ->
                    chunk.map { recent ->
                        async {
                            podcastRemote.getPodcastByFeedId(recent.id)
                        }
                    }.awaitAll().filterNotNull()
                }
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