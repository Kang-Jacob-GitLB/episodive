package io.jacob.episodive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.PodcastEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

@Dao
interface PodcastDao {
    @Upsert
    suspend fun upsertPodcast(podcast: PodcastEntity)

    @Upsert
    suspend fun upsertPodcasts(podcasts: List<PodcastEntity>)

    @Query("DELETE FROM podcasts WHERE id = :id")
    suspend fun deletePodcast(id: Long)

    @Query("DELETE FROM podcasts")
    suspend fun deletePodcasts()

    @Query("DELETE FROM podcasts WHERE cacheKey = :cacheKey")
    suspend fun deletePodcastsByCacheKey(cacheKey: String)

    @Transaction
    suspend fun replacePodcasts(podcasts: List<PodcastEntity>) {
        podcasts.groupBy { it.cacheKey }.forEach { (cacheKey, podcastGroup) ->
            deletePodcastsByCacheKey(cacheKey)
            upsertPodcasts(podcastGroup)
        }
    }

    @Query("SELECT * FROM podcasts WHERE id = :id ORDER BY cachedAt DESC LIMIT 1")
    fun getPodcast(id: Long): Flow<PodcastEntity?>

    @Query("SELECT * FROM podcasts")
    fun getPodcasts(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE cacheKey = :cacheKey")
    fun getPodcastsByCacheKey(cacheKey: String): Flow<List<PodcastEntity>>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFollowed(followedPodcastEntity: FollowedPodcastEntity)

    @Query("DELETE FROM followed_podcasts WHERE id = :id")
    suspend fun removeFollowed(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM followed_podcasts WHERE id = :id)")
    fun isFollowed(id: Long): Boolean

    @Transaction
    suspend fun toggleFollowed(id: Long): Boolean {
        return if (isFollowed(id)) {
            removeFollowed(id)
            false
        } else {
            addFollowed(
                FollowedPodcastEntity(
                    id = id,
                    followedAt = Clock.System.now(),
                    isNotificationEnabled = false
                )
            )
            true
        }
    }

    @Query("SELECT * FROM followed_podcasts ORDER BY followedAt DESC")
    fun getFollowedPodcasts(): Flow<List<FollowedPodcastEntity>>
}