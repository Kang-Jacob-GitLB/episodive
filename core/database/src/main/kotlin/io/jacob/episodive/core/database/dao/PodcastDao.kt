package io.jacob.episodive.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.PodcastDto
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

    @Query(
        """
        SELECT
            podcasts.*,
            followed_podcasts.followedAt,
            followed_podcasts.isNotificationEnabled
        FROM podcasts
        LEFT JOIN followed_podcasts ON podcasts.id = followed_podcasts.id
        WHERE podcasts.id = :id
        ORDER BY podcasts.cachedAt DESC
        LIMIT 1
    """
    )
    fun getPodcast(id: Long): Flow<PodcastDto?>

    @Query(
        """
        SELECT
            podcasts.*,
            followed_podcasts.followedAt,
            followed_podcasts.isNotificationEnabled
        FROM podcasts
        LEFT JOIN followed_podcasts ON podcasts.id = followed_podcasts.id
        LIMIT :limit
    """
    )
    fun getPodcasts(limit: Int): Flow<List<PodcastDto>>

    @Query(
        """
        SELECT
            podcasts.*,
            followed_podcasts.followedAt,
            followed_podcasts.isNotificationEnabled
        FROM podcasts
        LEFT JOIN followed_podcasts ON podcasts.id = followed_podcasts.id
    """
    )
    fun getPodcastsPaging(): PagingSource<Int, PodcastDto>

    @Query(
        """
        SELECT
            podcasts.*,
            followed_podcasts.followedAt,
            followed_podcasts.isNotificationEnabled
        FROM podcasts
        LEFT JOIN followed_podcasts ON podcasts.id = followed_podcasts.id
        WHERE podcasts.cacheKey = :cacheKey
        LIMIT :limit
    """
    )
    fun getPodcastsByCacheKey(cacheKey: String, limit: Int): Flow<List<PodcastDto>>

    @Query(
        """
        SELECT
            podcasts.*,
            followed_podcasts.followedAt,
            followed_podcasts.isNotificationEnabled
        FROM podcasts
        LEFT JOIN followed_podcasts ON podcasts.id = followed_podcasts.id
        WHERE podcasts.cacheKey = :cacheKey
    """
    )
    fun getPodcastsByCacheKeyPaging(cacheKey: String): PagingSource<Int, PodcastDto>


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

    @Query(
        """
        SELECT
            podcasts.*,
            followed_podcasts.followedAt,
            followed_podcasts.isNotificationEnabled
        FROM podcasts
        INNER JOIN followed_podcasts ON podcasts.id = followed_podcasts.id
        WHERE podcasts.cachedAt = (
            SELECT MAX(cachedAt)
            FROM podcasts p2
            WHERE p2.id = podcasts.id
        )
        ORDER BY followed_podcasts.followedAt DESC
        LIMIT :limit
    """
    )
    fun getFollowedPodcasts(limit: Int): Flow<List<PodcastDto>>

    @Query(
        """
        SELECT
            podcasts.*,
            followed_podcasts.followedAt,
            followed_podcasts.isNotificationEnabled
        FROM podcasts
        INNER JOIN followed_podcasts ON podcasts.id = followed_podcasts.id
        WHERE podcasts.cachedAt = (
            SELECT MAX(cachedAt)
            FROM podcasts p2
            WHERE p2.id = podcasts.id
        )
        ORDER BY followed_podcasts.followedAt DESC
    """
    )
    fun getFollowedPodcastsPaging(): PagingSource<Int, PodcastDto>
}