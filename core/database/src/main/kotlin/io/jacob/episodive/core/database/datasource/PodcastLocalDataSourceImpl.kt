package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

class PodcastLocalDataSourceImpl(
    private val podcastDao: PodcastDao,
) : PodcastLocalDataSource {
    override fun getPodcastById(id: Long): Flow<PodcastWithExtrasView?> {
        return podcastDao.getPodcastById(id)
    }

    override fun getPodcastsByIds(ids: List<Long>): Flow<List<PodcastWithExtrasView>> {
        return podcastDao.getPodcastsByIds(ids)
    }

    override fun getPodcasts(query: String?, limit: Int): Flow<List<PodcastWithExtrasView>> {
        return podcastDao.getPodcasts(
            query = query?.ifBlank { null },
            limit = limit,
        )
    }

    override fun getPodcastsPaging(query: String?): PagingSource<Int, PodcastWithExtrasView> {
        return podcastDao.getPodcastsPaging(
            query = query?.ifBlank { null },
        )
    }

    override fun getPodcastsByGroupKey(
        groupKey: String,
        limit: Int,
    ): Flow<List<PodcastWithExtrasView>> {
        return podcastDao.getPodcastsByGroupKey(
            groupKey = groupKey,
            limit = limit,
        )
    }

    override fun getPodcastsByGroupKeyPaging(groupKey: String): PagingSource<Int, PodcastWithExtrasView> {
        return podcastDao.getPodcastsByGroupKeyPaging(groupKey)
    }

    override suspend fun getOldestCreatedAtByGroupKey(groupKey: String): Instant? {
        return podcastDao.getOldestCreatedAtByGroupKey(groupKey)
    }

    override suspend fun replacePodcasts(podcasts: List<PodcastEntity>, groupKey: String) {
        podcastDao.replacePodcasts(
            podcasts = podcasts,
            groupKey = groupKey,
        )
    }

    override fun isFollowedPodcast(id: Long): Flow<Boolean> {
        return podcastDao.isFollowedPodcast(id)
    }

    override suspend fun toggleFollowedPodcast(id: Long): Boolean {
        return podcastDao.toggleFollowedPodcast(id)
    }

    override fun getFollowedPodcasts(
        query: String?,
        limit: Int,
    ): Flow<List<PodcastWithExtrasView>> {
        return podcastDao.getFollowedPodcasts(
            query = query?.ifBlank { null },
            limit = limit,
        )
    }

    override fun getFollowedPodcastsPaging(query: String?): PagingSource<Int, PodcastWithExtrasView> {
        return podcastDao.getFollowedPodcastsPaging(
            query = query?.ifBlank { null }
        )
    }
}