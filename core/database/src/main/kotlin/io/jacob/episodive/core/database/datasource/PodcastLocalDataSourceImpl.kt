package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.model.FollowedPodcastDto
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

    override suspend fun deletePodcastsByCacheKey(cacheKey: String) {
        podcastDao.deletePodcastsByCacheKey(cacheKey)
    }

    override suspend fun deletePodcasts() {
        podcastDao.deletePodcasts()
    }

    override suspend fun addFollowed(followedPodcastEntity: FollowedPodcastEntity) {
        podcastDao.addFollowed(followedPodcastEntity)
    }

    override suspend fun addFolloweds(followedPodcastEntities: List<FollowedPodcastEntity>) {
        podcastDao.addFolloweds(followedPodcastEntities)
    }

    override suspend fun removeFollowed(id: Long) {
        podcastDao.removeFollowed(id)
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

    override fun getFollowedPodcasts(query: String?): Flow<List<FollowedPodcastDto>> {
        return podcastDao.getFollowedPodcasts(query)
    }

    override fun isFollowed(id: Long): Flow<Boolean> {
        return podcastDao.isFollowed(id)
    }

    override fun getPodcastCount(): Flow<Int> {
        return podcastDao.getPodcastCount()
    }

    override fun getFollowedPodcastCount(): Flow<Int> {
        return podcastDao.getFollowedPodcastCount()
    }
}