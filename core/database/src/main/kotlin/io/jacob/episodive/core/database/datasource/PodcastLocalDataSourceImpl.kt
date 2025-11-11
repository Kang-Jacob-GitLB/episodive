package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.PodcastEntity
import kotlinx.coroutines.flow.Flow

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

    override fun getPodcast(id: Long): Flow<PodcastEntity?> {
        return podcastDao.getPodcast(id)
    }

    override fun getPodcasts(): Flow<List<PodcastEntity>> {
        return podcastDao.getPodcasts()
    }

    override fun getPodcastsByCacheKey(cacheKey: String): Flow<List<PodcastEntity>> {
        return podcastDao.getPodcastsByCacheKey(cacheKey)
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

    override fun getFollowedPodcasts(): Flow<List<FollowedPodcastEntity>> {
        return podcastDao.getFollowedPodcasts()
    }
}