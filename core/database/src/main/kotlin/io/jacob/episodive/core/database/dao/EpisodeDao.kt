package io.jacob.episodive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.EpisodeDto
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration

@Dao
interface EpisodeDao {
    @Upsert
    suspend fun upsertEpisode(episode: EpisodeEntity)

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun deleteEpisode(id: Long)

    @Query("DELETE FROM episodes")
    suspend fun deleteEpisodes()

    @Query("DELETE FROM episodes WHERE cacheKey = :cacheKey")
    suspend fun deleteEpisodesByCacheKey(cacheKey: String)

    @Transaction
    suspend fun replaceEpisodes(episodes: List<EpisodeEntity>) {
        episodes.groupBy { it.cacheKey }.forEach { (cacheKey, episodeGroup) ->
            deleteEpisodesByCacheKey(cacheKey)
            upsertEpisodes(episodeGroup)
        }
    }

    @Query("UPDATE episodes SET duration = :duration WHERE id = :id")
    suspend fun updateDurationOfEpisodes(id: Long, duration: Duration)

    @Query(
        """
        SELECT
            episodes.*,
            liked_episodes.likedAt,
            played_episodes.playedAt,
            played_episodes.position,
            played_episodes.isCompleted
        FROM episodes
        LEFT JOIN liked_episodes ON episodes.id = liked_episodes.id
        LEFT JOIN played_episodes ON episodes.id = played_episodes.id
        WHERE episodes.id = :id
        ORDER BY episodes.cachedAt DESC
        LIMIT 1
    """
    )
    fun getEpisode(id: Long): Flow<EpisodeDto?>

    @Query(
        """
        SELECT
            episodes.*,
            liked_episodes.likedAt,
            played_episodes.playedAt,
            played_episodes.position,
            played_episodes.isCompleted
        FROM episodes
        LEFT JOIN liked_episodes ON episodes.id = liked_episodes.id
        LEFT JOIN played_episodes ON episodes.id = played_episodes.id
        WHERE episodes.cachedAt = (
            SELECT MAX(cachedAt)
            FROM episodes e2
            WHERE e2.id = episodes.id
        )
    """
    )
    fun getEpisodes(): Flow<List<EpisodeDto>>

    @Query(
        """
        SELECT
            episodes.*,
            liked_episodes.likedAt,
            played_episodes.playedAt,
            played_episodes.position,
            played_episodes.isCompleted
        FROM episodes
        LEFT JOIN liked_episodes ON episodes.id = liked_episodes.id
        LEFT JOIN played_episodes ON episodes.id = played_episodes.id
        WHERE episodes.cacheKey = :cacheKey
        ORDER BY episodes.datePublished DESC
    """
    )
    fun getEpisodesByCacheKey(cacheKey: String): Flow<List<EpisodeDto>>


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
        SELECT
            episodes.*,
            liked_episodes.likedAt,
            played_episodes.playedAt,
            played_episodes.position,
            played_episodes.isCompleted
        FROM episodes
        INNER JOIN liked_episodes ON episodes.id = liked_episodes.id
        LEFT JOIN played_episodes ON episodes.id = played_episodes.id
        WHERE episodes.cachedAt = (
            SELECT MAX(cachedAt)
            FROM episodes e2
            WHERE e2.id = episodes.id
        )
        ORDER BY liked_episodes.likedAt DESC
    """
    )
    fun getLikedEpisodes(): Flow<List<EpisodeDto>>


    @Upsert
    suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity)

    @Query("DELETE FROM played_episodes WHERE id = :id")
    suspend fun removePlayed(id: Long)

    @Query(
        """
        SELECT
            episodes.*,
            liked_episodes.likedAt,
            played_episodes.playedAt,
            played_episodes.position,
            played_episodes.isCompleted
        FROM episodes
        INNER JOIN played_episodes ON episodes.id = played_episodes.id
        LEFT JOIN liked_episodes ON episodes.id = liked_episodes.id
        WHERE episodes.cachedAt = (
            SELECT MAX(cachedAt)
            FROM episodes e2
            WHERE e2.id = episodes.id
        )
        ORDER BY played_episodes.playedAt DESC
    """
    )
    fun getPlayedEpisodes(): Flow<List<EpisodeDto>>
}