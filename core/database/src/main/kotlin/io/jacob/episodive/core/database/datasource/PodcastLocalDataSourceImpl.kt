package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.PodcastDto
import io.jacob.episodive.core.database.model.PodcastEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

class PodcastLocalDataSourceImpl(
    private val podcastDao: PodcastDao,
) : PodcastLocalDataSource {
    override suspend fun upsertPodcast(podcast: PodcastEntity) {
        podcastDao.upsertPodcast(podcast)
    }

    override suspend fun upsertPodcasts(podcasts: List<PodcastEntity>) {
        podcastDao.upsertPodcasts(podcasts)
    }

    override suspend fun deletePodcast(id: Long) {
        podcastDao.deletePodcast(id)
    }

    override suspend fun deletePodcasts() {
        podcastDao.deletePodcasts()
    }

    override suspend fun deletePodcastsByCacheKey(cacheKey: String) {
        podcastDao.deletePodcastsByCacheKey(cacheKey)
    }

    override suspend fun replacePodcasts(podcasts: List<PodcastEntity>) {
        podcastDao.replacePodcasts(podcasts)
    }

    override fun getPodcast(id: Long): Flow<PodcastDto?> {
        return podcastDao.getPodcast(id)
    }

    override fun getPodcasts(limit: Int): Flow<List<PodcastDto>> {
        return podcastDao.getPodcasts(limit)
    }

    override fun getPodcastsPaging(): PagingSource<Int, PodcastDto> {
        return podcastDao.getPodcastsPaging()
    }

    override fun getPodcastsByCacheKey(cacheKey: String, limit: Int): Flow<List<PodcastDto>> {
        return podcastDao.getPodcastsByCacheKey(cacheKey, limit)
    }

    override fun getPodcastsByCacheKeyPaging(cacheKey: String): PagingSource<Int, PodcastDto> {
        return podcastDao.getPodcastsByCacheKeyPaging(cacheKey)
    }

    override suspend fun getPodcastsOldestCachedAtByCacheKey(cacheKey: String): Instant? {
        return podcastDao.getPodcastsOldestCachedAtByCacheKey(cacheKey)
    }

    override suspend fun addFollowed(followedPodcastEntity: FollowedPodcastEntity) {
        podcastDao.addFollowed(followedPodcastEntity)
    }

    override suspend fun removeFollowed(id: Long) {
        podcastDao.removeFollowed(id)
    }

    override fun isFollowed(id: Long): Boolean {
        return podcastDao.isFollowed(id)
    }

    override suspend fun toggleFollowed(id: Long): Boolean {
        return podcastDao.toggleFollowed(id)
    }

    override fun getFollowedPodcasts(limit: Int): Flow<List<PodcastDto>> {
        return podcastDao.getFollowedPodcasts(limit)
    }

    override fun getFollowedPodcastsPaging(): PagingSource<Int, PodcastDto> {
        return podcastDao.getFollowedPodcastsPaging()
    }
}