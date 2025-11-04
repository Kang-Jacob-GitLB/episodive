package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.data.util.Cacher
import io.jacob.episodive.core.data.util.query.PodcastQuery
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.mapper.toPodcast
import io.jacob.episodive.core.database.mapper.toPodcasts
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.mapper.toPodcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PodcastRepositoryImpl @Inject constructor(
    private val localDataSource: PodcastLocalDataSource,
    private val remoteDataSource: PodcastRemoteDataSource,
    private val remoteUpdater: PodcastRemoteUpdater.Factory,
) : PodcastRepository {
    override fun searchPodcasts(
        query: String,
        max: Int?,
    ): Flow<List<Podcast>> {
        val query = PodcastQuery.Search(query)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getPodcastsByCacheKey(query.key)
            }
        ).flow.map { it.toPodcasts() }
    }

    override fun getPodcastByFeedId(feedId: Long): Flow<Podcast?> {
        val query = PodcastQuery.FeedId(feedId)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getPodcast(feedId).map { entity ->
                    entity?.let { listOf(it) } ?: emptyList()
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
        max: Int?,
    ): Flow<List<Podcast>> {
        val query = PodcastQuery.Medium(medium)

        return Cacher(
            remoteUpdater = remoteUpdater.create(query),
            sourceFactory = {
                localDataSource.getPodcastsByCacheKey(query.key)
            }
        ).flow.map { it.toPodcasts() }
    }

    override fun getFollowedPodcasts(query: String?): Flow<List<Podcast>> {
        return combine(
            localDataSource.getPodcasts(),
            localDataSource.getFollowedPodcasts(),
        ) { podcasts, followed ->
            val podcastMap = podcasts.associateBy { it.id }
            followed.mapNotNull { followedPodcast ->
                podcastMap[followedPodcast.id]?.let { podcast ->
                    if (query == null || podcast.matchesQuery(query)) {
                        podcast.toPodcast().copy(
                            followedAt = followedPodcast.followedAt,
                            isNotificationEnabled = followedPodcast.isNotificationEnabled,
                        )
                    } else null
                }
            }
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