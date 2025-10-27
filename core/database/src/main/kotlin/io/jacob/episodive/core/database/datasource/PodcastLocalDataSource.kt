package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.model.FollowedPodcastDto
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.PodcastEntity
import kotlinx.coroutines.flow.Flow

interface PodcastLocalDataSource {
    suspend fun upsertPodcast(podcast: PodcastEntity)
    suspend fun upsertPodcasts(podcasts: List<PodcastEntity>)
    suspend fun deletePodcast(id: Long)
    suspend fun deletePodcasts()
    suspend fun deletePodcastsByCacheKey(cacheKey: String)
    suspend fun addFollowed(followedPodcastEntity: FollowedPodcastEntity)
    suspend fun addFolloweds(followedPodcastEntities: List<FollowedPodcastEntity>)
    suspend fun removeFollowed(id: Long)
    fun getPodcast(id: Long): Flow<PodcastEntity?>
    fun getPodcasts(): Flow<List<PodcastEntity>>
    fun getPodcastsByCacheKey(cacheKey: String): Flow<List<PodcastEntity>>
    fun getFollowedPodcasts(query: String? = null): Flow<List<FollowedPodcastDto>>
    fun isFollowed(id: Long): Flow<Boolean>
    fun getPodcastCount(): Flow<Int>
    fun getFollowedPodcastCount(): Flow<Int>
}