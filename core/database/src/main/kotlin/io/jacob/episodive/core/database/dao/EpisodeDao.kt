package io.jacob.episodive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

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

    @Query("SELECT * FROM episodes WHERE id = :id ORDER BY cachedAt DESC LIMIT 1")
    fun getEpisode(id: Long): Flow<EpisodeEntity?>

    @Query("SELECT * FROM episodes")
    fun getEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE cacheKey = :cacheKey ORDER BY datePublished DESC")
    fun getEpisodesByCacheKey(cacheKey: String): Flow<List<EpisodeEntity>>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLiked(likedEpisode: LikedEpisodeEntity)

    @Query("DELETE FROM liked_episodes WHERE id = :id")
    suspend fun removeLiked(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_episodes WHERE id = :id)")
    fun isLiked(id: Long): Boolean

    @Transaction
    suspend fun toggleLiked(id: Long): Boolean {
        return if (isLiked(id)) {
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

    @Query("SELECT * FROM liked_episodes ORDER BY likedAt DESC")
    fun getLikedEpisodes(): Flow<List<LikedEpisodeEntity>>


    @Upsert
    suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity)

    @Query("DELETE FROM played_episodes WHERE id = :id")
    suspend fun removePlayed(id: Long)

    @Query("SELECT * FROM played_episodes ORDER BY playedAt DESC")
    fun getPlayedEpisodes(): Flow<List<PlayedEpisodeEntity>>
}