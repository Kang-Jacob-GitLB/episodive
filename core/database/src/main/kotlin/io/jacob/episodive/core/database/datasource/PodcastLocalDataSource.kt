package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import androidx.room.RoomDatabase
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface PodcastLocalDataSource {
    val database: RoomDatabase
    suspend fun upsertPodcastsWithGroup(podcasts: List<PodcastEntity>, groupKey: String)
    fun getPodcastById(id: Long): Flow<PodcastWithExtrasView?>
    fun getPodcastsByIds(ids: List<Long>): Flow<List<PodcastWithExtrasView>>
    suspend fun getPodcastsByIdsOnce(ids: List<Long>): List<PodcastWithExtrasView>
    fun getPodcasts(query: String? = null, limit: Int): Flow<List<PodcastWithExtrasView>>
    fun getPodcastsPaging(query: String? = null): PagingSource<Int, PodcastWithExtrasView>
    fun getPodcastsByGroupKey(groupKey: String, limit: Int): Flow<List<PodcastWithExtrasView>>
    fun getPodcastsByGroupKeyPaging(groupKey: String): PagingSource<Int, PodcastWithExtrasView>
    suspend fun getOldestCreatedAtByGroupKey(groupKey: String): Instant?
    suspend fun replacePodcasts(podcasts: List<PodcastEntity>, groupKey: String)

    fun isFollowedPodcast(id: Long): Flow<Boolean>
    suspend fun toggleFollowedPodcast(id: Long): Boolean
    fun getFollowedPodcasts(query: String? = null, limit: Int): Flow<List<PodcastWithExtrasView>>
    fun getFollowedPodcastsPaging(query: String? = null): PagingSource<Int, PodcastWithExtrasView>
}