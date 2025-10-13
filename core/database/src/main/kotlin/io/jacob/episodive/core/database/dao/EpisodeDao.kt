package io.jacob.episodive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeDto
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeDto
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLiked(likedEpisode: LikedEpisodeEntity)

    @Query("DELETE FROM liked_episodes WHERE id = :id")
    suspend fun removeLiked(id: Long)

    @Upsert
    suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity)

    @Query("DELETE FROM played_episodes WHERE id = :id")
    suspend fun removePlayed(id: Long)

    @Query(
        """
        SELECT *
        FROM episodes
        WHERE id = :id
        ORDER BY cachedAt DESC
        LIMIT 1
    """
    )
    fun getEpisode(id: Long): Flow<EpisodeEntity?>

    @Query("SELECT * FROM episodes")
    fun getEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE cacheKey = :cacheKey ORDER BY datePublished DESC")
    fun getEpisodesByCacheKey(cacheKey: String): Flow<List<EpisodeEntity>>

    @Query(
        """
        SELECT
            e.*,
            le.likedAt
        FROM liked_episodes le
        LEFT JOIN episodes e ON le.id = e.id
        WHERE e.cachedAt = (
            SELECT MAX(cachedAt) FROM episodes WHERE id = le.id
        )
        AND (:query IS NULL OR :query = '' OR 
            e.title LIKE '%' || :query || '%' COLLATE NOCASE OR 
            (e.description IS NOT NULL AND e.description LIKE '%' || :query || '%' COLLATE NOCASE) OR
            (e.feedAuthor IS NOT NULL AND e.feedAuthor LIKE '%' || :query || '%' COLLATE NOCASE))
        ORDER BY le.likedAt DESC
    """
    )
    fun getLikedEpisodes(query: String? = null): Flow<List<LikedEpisodeDto>>

    @Query(
        """
        SELECT
            e.*,
            pe.playedAt,
            pe.position,
            pe.isCompleted
        FROM played_episodes pe
        LEFT JOIN episodes e ON pe.id = e.id
        WHERE pe.isCompleted = 0
            AND e.cachedAt = (
                SELECT MAX(cachedAt) FROM episodes WHERE id = pe.id
            )
        AND (:query IS NULL OR :query = '' OR 
            e.title LIKE '%' || :query || '%' COLLATE NOCASE OR 
            (e.description IS NOT NULL AND e.description LIKE '%' || :query || '%' COLLATE NOCASE) OR
            (e.feedAuthor IS NOT NULL AND e.feedAuthor LIKE '%' || :query || '%' COLLATE NOCASE))
        ORDER BY pe.playedAt DESC
    """
    )
    fun getPlayingEpisodes(query: String? = null): Flow<List<PlayedEpisodeDto>>

    @Query(
        """
        SELECT
            e.*,
            pe.playedAt,
            pe.position,
            pe.isCompleted
        FROM played_episodes pe
        LEFT JOIN episodes e ON pe.id = e.id
        WHERE pe.isCompleted = 1
            AND e.cachedAt = (
                SELECT MAX(cachedAt) FROM episodes WHERE id = pe.id
            )
        AND (:query IS NULL OR :query = '' OR 
            e.title LIKE '%' || :query || '%' COLLATE NOCASE OR 
            (e.description IS NOT NULL AND e.description LIKE '%' || :query || '%' COLLATE NOCASE) OR
            (e.feedAuthor IS NOT NULL AND e.feedAuthor LIKE '%' || :query || '%' COLLATE NOCASE))
        ORDER BY pe.playedAt DESC
    """
    )
    fun getPlayedEpisodes(query: String? = null): Flow<List<PlayedEpisodeDto>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_episodes WHERE id = :id)")
    fun isLiked(id: Long): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM episodes")
    fun getEpisodeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM liked_episodes")
    fun getLikedEpisodeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM played_episodes WHERE isCompleted = 0")
    fun getPlayingEpisodeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM played_episodes WHERE isCompleted = 1")
    fun getPlayedEpisodeCount(): Flow<Int>
}