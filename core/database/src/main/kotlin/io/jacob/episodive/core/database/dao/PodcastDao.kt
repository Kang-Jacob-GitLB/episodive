package io.jacob.episodive.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.database.model.PodcastGroupEntity
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Instant

@Dao
interface PodcastDao {
    companion object {
        private const val FTS_SEARCH_CONDITION = """
            (:query IS NULL OR id IN (SELECT rowid FROM podcasts_fts WHERE podcasts_fts MATCH :query))
        """
    }


    /** PODCASTS **/

    @Upsert
    suspend fun upsertPodcast(podcast: PodcastEntity)

    @Upsert
    suspend fun upsertPodcasts(podcasts: List<PodcastEntity>)

    @Upsert
    suspend fun upsertPodcastGroup(podcastGroup: PodcastGroupEntity)

    @Upsert
    suspend fun upsertPodcastGroups(podcastGroups: List<PodcastGroupEntity>)

    @Transaction
    suspend fun upsertPodcastsWithGroup(podcasts: List<PodcastEntity>, groupKey: String) {
        upsertPodcasts(podcasts)

        val createdAt = Clock.System.now()
        val groups = podcasts.mapIndexed { order, podcast ->
            PodcastGroupEntity(
                groupKey = groupKey,
                id = podcast.id,
                order = order,
                createdAt = createdAt,
            )
        }
        upsertPodcastGroups(groups)
    }

    @Query("DELETE FROM podcasts WHERE id = :id")
    suspend fun deletePodcast(id: Long)

    @Query("DELETE FROM podcasts")
    suspend fun deletePodcasts()

    @Query(
        """
        DELETE FROM podcasts
        WHERE id IN (:ids)
          AND NOT EXISTS (SELECT 1 FROM followed_podcasts WHERE followed_podcasts.id = podcasts.id)
          AND NOT EXISTS (SELECT 1 FROM podcast_group WHERE podcast_group.id = podcasts.id)
    """
    )
    suspend fun deletePodcastsIfOrphaned(ids: List<Long>)

    @Query("DELETE FROM podcast_group WHERE groupKey = :groupKey")
    suspend fun deletePodcastGroupsByGroupKey(groupKey: String)

    @Query("SELECT * FROM podcast_with_extras WHERE id = :id")
    fun getPodcastById(id: Long): Flow<PodcastWithExtrasView?>

    @Query("SELECT * FROM podcast_with_extras WHERE id IN (:ids)")
    fun getPodcastsByIds(ids: List<Long>): Flow<List<PodcastWithExtrasView>>

    @Query(
        """
        SELECT * FROM podcast_with_extras
        WHERE $FTS_SEARCH_CONDITION
        ORDER BY lastUpdateTime DESC
        LIMIT :limit
    """
    )
    fun getPodcasts(query: String? = null, limit: Int): Flow<List<PodcastWithExtrasView>>

    @Query(
        """
        SELECT * FROM podcast_with_extras
        WHERE $FTS_SEARCH_CONDITION
        ORDER BY lastUpdateTime DESC
    """
    )
    fun getPodcastsPaging(query: String? = null): PagingSource<Int, PodcastWithExtrasView>

    @Query(
        """
        SELECT * FROM podcast_with_extras
        WHERE id IN (SELECT id FROM podcast_group WHERE groupKey = :groupKey)
        ORDER BY lastUpdateTime DESC
        LIMIT :limit
    """
    )
    fun getPodcastsByGroupKey(groupKey: String, limit: Int): Flow<List<PodcastWithExtrasView>>

    @Query(
        """
        SELECT * FROM podcast_with_extras
        WHERE id IN (SELECT id FROM podcast_group WHERE groupKey = :groupKey)
        ORDER BY lastUpdateTime DESC
    """
    )
    fun getPodcastsByGroupKeyPaging(groupKey: String): PagingSource<Int, PodcastWithExtrasView>

    @Query("SELECT * FROM podcast_group WHERE groupKey = :groupKey")
    suspend fun getPodcastGroupsByGroupKey(groupKey: String): List<PodcastGroupEntity>

    @Query("SELECT MIN(createdAt) FROM podcast_group WHERE groupKey = :groupKey")
    suspend fun getOldestCreatedAtByGroupKey(groupKey: String): Instant?

    @Transaction
    suspend fun replacePodcasts(podcasts: List<PodcastEntity>, groupKey: String) {
        val oldPodcastIds = getPodcastGroupsByGroupKey(groupKey).map { it.id }
        deletePodcastGroupsByGroupKey(groupKey)
        upsertPodcastsWithGroup(podcasts, groupKey)
        deletePodcastsIfOrphaned(oldPodcastIds)
    }


    /** FOLLOWED PODCASTS **/

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFollowedPodcast(followedPodcast: FollowedPodcastEntity)

    @Query("DELETE FROM followed_podcasts WHERE id = :id")
    suspend fun removeFollowedPodcast(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM followed_podcasts WHERE id = :id)")
    fun isFollowedPodcast(id: Long): Flow<Boolean>

    @Transaction
    suspend fun toggleFollowedPodcast(id: Long): Boolean {
        return if (isFollowedPodcast(id).first()) {
            removeFollowedPodcast(id)
            deletePodcastsIfOrphaned(listOf(id))
            false
        } else {
            addFollowedPodcast(
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
        SELECT * FROM podcast_with_extras
        WHERE followedAt IS NOT NULL
        AND $FTS_SEARCH_CONDITION
        ORDER BY followedAt DESC
        LIMIT :limit
    """
    )
    fun getFollowedPodcasts(query: String? = null, limit: Int): Flow<List<PodcastWithExtrasView>>

    @Query(
        """
        SELECT * FROM podcast_with_extras
        WHERE followedAt IS NOT NULL
        AND $FTS_SEARCH_CONDITION
        ORDER BY followedAt DESC
    """
    )
    fun getFollowedPodcastsPaging(query: String? = null): PagingSource<Int, PodcastWithExtrasView>
}