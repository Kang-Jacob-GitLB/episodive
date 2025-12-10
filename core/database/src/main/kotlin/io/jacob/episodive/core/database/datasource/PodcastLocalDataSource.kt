package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.PodcastDto
import io.jacob.episodive.core.database.model.PodcastEntity
import kotlinx.coroutines.flow.Flow

interface PodcastLocalDataSource {
    suspend fun upsertPodcast(podcast: PodcastEntity)
    suspend fun upsertPodcasts(podcasts: List<PodcastEntity>)
    suspend fun deletePodcast(id: Long)
    suspend fun deletePodcasts()
    suspend fun deletePodcastsByCacheKey(cacheKey: String)
    suspend fun replacePodcasts(podcasts: List<PodcastEntity>)
    fun getPodcast(id: Long): Flow<PodcastDto?>
    fun getPodcasts(limit: Int = -1): Flow<List<PodcastDto>>
    fun getPodcastsPaging(): PagingSource<Int, PodcastDto>
    fun getPodcastsByCacheKey(cacheKey: String, limit: Int = -1): Flow<List<PodcastDto>>
    fun getPodcastsByCacheKeyPaging(cacheKey: String): PagingSource<Int, PodcastDto>

    suspend fun addFollowed(followedPodcastEntity: FollowedPodcastEntity)
    suspend fun removeFollowed(id: Long)
    fun isFollowed(id: Long): Boolean
    suspend fun toggleFollowed(id: Long): Boolean
    fun getFollowedPodcasts(limit: Int = -1): Flow<List<PodcastDto>>
    fun getFollowedPodcastsPaging(): PagingSource<Int, PodcastDto>
}