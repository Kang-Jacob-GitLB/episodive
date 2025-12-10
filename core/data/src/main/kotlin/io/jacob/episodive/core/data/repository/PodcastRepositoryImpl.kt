package io.jacob.episodive.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import io.jacob.episodive.core.data.util.Cacher
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcast
import io.jacob.episodive.core.database.mapper.toPodcasts
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcast
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

class PodcastRepositoryImpl @Inject constructor(
    private val localDataSource: PodcastLocalDataSource,
    private val remoteDataSource: PodcastRemoteDataSource,
    private val remoteUpdater: PodcastRemoteUpdater.Factory,
) : PodcastRepository {
    private val config = PagingConfig(
        pageSize = 20,
        enablePlaceholders = false,
        prefetchDistance = 5
    )

    override fun searchPodcasts(
        query: String,
        max: Int,
    ): Flow<List<Podcast>> {
        val query = PodcastQuery.Search(query, max)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getPodcastsByCacheKey(query.key, query.max)
            }
        ).flow.map { it.toPodcasts() }
    }

    override fun searchPodcastsPaging(query: String): Flow<PagingData<Podcast>> {
        val query = PodcastQuery.Search(query, 10000)
        val updater = remoteUpdater.create(query)

        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getPodcastsByCacheKeyPaging(query.key) }
        ).flow
            .onStart {
                coroutineScope {
                    launch {
                        updater.load(
                            localDataSource.getPodcastsByCacheKey(query.key, query.max).first()
                        )
                    }
                }
            }
            .map { pagingData ->
                pagingData.map { it.toPodcast() }
            }
    }

    override fun getPodcastByFeedId(feedId: Long): Flow<Podcast?> {
        val query = PodcastQuery.FeedId(feedId)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getPodcast(feedId).map { dto ->
                    dto?.let { listOf(it) } ?: emptyList()
                }
            }
        ).flow.map { it.ifEmpty { null }?.firstOrNull()?.toPodcast() }
    }

    override fun getPodcastByFeedUrl(feedUrl: String): Flow<Podcast?> = flow {
        val podcast = remoteDataSource.getPodcastByFeedUrl(
            feedUrl = feedUrl
        )?.toPodcast()

        emit(podcast)
    }

    override fun getPodcastByGuid(guid: String): Flow<Podcast?> = flow {
        val podcast = remoteDataSource.getPodcastByGuid(
            guid = guid
        )?.toPodcast()

        emit(podcast)
    }

    override fun getPodcastsByMedium(
        medium: String,
        max: Int,
    ): Flow<List<Podcast>> {
        val query = PodcastQuery.Medium(medium, max)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getPodcastsByCacheKey(query.key, query.max)
            }
        ).flow.map { it.toPodcasts() }
    }

    override fun getPodcastsByMediumPaging(medium: String): Flow<PagingData<Podcast>> {
        val query = PodcastQuery.Medium(medium, 10000)
        val updater = remoteUpdater.create(query)

        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getPodcastsByCacheKeyPaging(query.key) }
        ).flow
            .onStart {
                coroutineScope {
                    launch {
                        updater.load(
                            localDataSource.getPodcastsByCacheKey(query.key, query.max).first()
                        )
                    }
                }
            }
            .map { pagingData ->
                pagingData.map { it.toPodcast() }
            }
    }

    override fun getPodcastsByChannel(channel: Channel): Flow<List<Podcast>> {
        val query = PodcastQuery.ByChannel(channel, 10000)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getPodcastsByCacheKey(query.key, query.max)
            }
        ).flow.map { it.toPodcasts() }
    }

    override fun getPodcastsByChannelPaging(channel: Channel): Flow<PagingData<Podcast>> {
        val query = PodcastQuery.ByChannel(channel, 10000)
        val updater = remoteUpdater.create(query)

        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getPodcastsByCacheKeyPaging(query.key) }
        ).flow
            .onStart {
                coroutineScope {
                    launch {
                        updater.load(
                            localDataSource.getPodcastsByCacheKey(query.key, query.max).first()
                        )
                    }
                }
            }
            .map { pagingData ->
                pagingData.map { it.toPodcast() }
            }
    }

    override fun getFollowedPodcasts(query: String?, max: Int): Flow<List<Podcast>> {
        return localDataSource.getFollowedPodcasts(max).map { podcasts ->
            podcasts
                .filter { query == null || it.podcast.matchesQuery(query) }
                .toPodcasts()
        }
    }

    override fun getFollowedPodcastsPaging(query: String?): Flow<PagingData<Podcast>> {
        return Pager(
            config = config,
            pagingSourceFactory = { localDataSource.getFollowedPodcastsPaging() }
        ).flow
            .map { pagingData ->
                pagingData.map { it.toPodcast() }
            }
    }

    override suspend fun toggleFollowed(id: Long): Boolean {
        return localDataSource.toggleFollowed(id)
    }

    private fun PodcastEntity.matchesQuery(query: String): Boolean {
        return title.contains(query, ignoreCase = true) ||
                description.contains(query, ignoreCase = true) ||
                author.contains(query, ignoreCase = true) ||
                ownerName.contains(query, ignoreCase = true)
    }
}