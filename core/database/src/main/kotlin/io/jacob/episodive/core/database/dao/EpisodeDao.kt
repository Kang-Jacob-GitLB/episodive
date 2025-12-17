package io.jacob.episodive.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeGroupEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

@Dao
interface EpisodeDao {
    @Upsert
    suspend fun upsertEpisode(episode: EpisodeEntity)

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Upsert
    suspend fun upsertEpisodeGroup(episodeGroup: EpisodeGroupEntity)

    @Upsert
    suspend fun upsertEpisodeGroups(episodeGroups: List<EpisodeGroupEntity>)

    @Transaction
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>, groupKey: String) {
        upsertEpisodes(episodes)

        val createdAt = Clock.System.now()
        val groups = episodes.mapIndexed { order, episode ->
            EpisodeGroupEntity(
                groupKey = groupKey,
                id = episode.id,
                order = order,
                createdAt = createdAt,
            )
        }
        upsertEpisodeGroups(groups)
    }

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun deleteEpisode(id: Long)

    @Query("DELETE FROM episodes")
    suspend fun deleteEpisodes()

    @Query(
        """
        DELETE FROM episodes
        WHERE id IN (
            SELECT id FROM episode_group WHERE groupKey = :groupKey
        )
    """
    )
    suspend fun deleteEpisodesByGroupKey(groupKey: String)

    @Transaction
    suspend fun replaceEpisodes(episodes: List<EpisodeEntity>, groupKey: String) {
        deleteEpisodesByGroupKey(groupKey)
        upsertEpisodes(episodes, groupKey)
    }

    @Query("UPDATE episodes SET duration = :duration WHERE id = :id")
    suspend fun updateDurationOfEpisodes(id: Long, duration: Duration)

    @Query("SELECT * FROM episode_with_extras WHERE id = :id")
    fun getEpisodeById(id: Long): Flow<EpisodeWithExtrasView?>

    @Query("SELECT * FROM episode_with_extras WHERE id IN (:ids)")
    fun getEpisodesByIds(ids: List<Long>): Flow<List<EpisodeWithExtrasView>>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE (:query IS NULL OR id IN (
            SELECT id FROM episodes_fts WHERE episodes_fts MATCH :query
        ))
        ORDER BY datePublished DESC
        LIMIT :limit
    """
    )
    fun getEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE (:query IS NULL OR id IN (
            SELECT id FROM episodes_fts WHERE episodes_fts MATCH :query
        ))
        ORDER BY datePublished DESC
    """
    )
    fun getEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE id IN (SELECT id FROM episode_group WHERE groupKey = :groupKey)
        ORDER BY datePublished DESC
        LIMIT :limit
    """
    )
    fun getEpisodesByGroupKey(groupKey: String, limit: Int): Flow<List<EpisodeWithExtrasView>>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE id IN (SELECT id FROM episode_group WHERE groupKey = :groupKey)
        ORDER BY datePublished DESC
    """
    )
    fun getEpisodesByGroupKeyPaging(groupKey: String): PagingSource<Int, EpisodeWithExtrasView>

    @Query("SELECT MIN(createdAt) FROM episode_group WHERE groupKey = :groupKey")
    suspend fun getEpisodesOldestCreatedAtByGroupKey(groupKey: String): Instant?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLiked(likedEpisode: LikedEpisodeEntity)

    @Query("DELETE FROM liked_episodes WHERE id = :id")
    suspend fun removeLiked(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_episodes WHERE id = :id)")
    fun isLiked(id: Long): Flow<Boolean>

    @Transaction
    suspend fun toggleLiked(id: Long): Boolean {
        return if (isLiked(id).first()) {
            removeLiked(id)
            false
        } else {
            addLiked(
                LikedEpisodeEntity(
                    id = id,
                    likedAt = Clock.System.now(),
                )
            )
            true
        }
    }

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE likedAt IS NOT NULL
        AND (:query IS NULL OR id IN (
            SELECT id FROM episodes_fts WHERE episodes_fts MATCH :query
        ))
        ORDER BY likedAt DESC
        LIMIT :limit
    """
    )
    fun getLikedEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE likedAt IS NOT NULL
        AND (:query IS NULL OR id IN (
            SELECT id FROM episodes_fts WHERE episodes_fts MATCH :query
        ))
        ORDER BY likedAt DESC
    """
    )
    fun getLikedEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>

    @Upsert
    suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity)

    @Query("DELETE FROM played_episodes WHERE id = :id")
    suspend fun removePlayed(id: Long)

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE playedAt IS NOT NULL 
        AND (:isCompleted IS NULL OR isCompleted = :isCompleted)
        AND (:query IS NULL OR id IN (
            SELECT id FROM episodes_fts WHERE episodes_fts MATCH :query
        ))
        ORDER BY playedAt DESC
        LIMIT :limit
    """
    )
    fun getPlayedEpisodes(
        isCompleted: Boolean? = null,
        query: String? = null,
        limit: Int,
    ): Flow<List<EpisodeWithExtrasView>>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE playedAt IS NOT NULL 
        AND (:isCompleted IS NULL OR isCompleted = :isCompleted)
        AND (:query IS NULL OR id IN (
            SELECT id FROM episodes_fts WHERE episodes_fts MATCH :query
        ))
        ORDER BY playedAt DESC
    """
    )
    fun getPlayedEpisodesPaging(
        isCompleted: Boolean? = null,
        query: String? = null,
    ): PagingSource<Int, EpisodeWithExtrasView>
}