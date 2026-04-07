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
import io.jacob.episodive.core.database.model.GroupKeyWithCount
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.database.model.SavedEpisodeEntity
import io.jacob.episodive.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

@Dao
interface EpisodeDao {
    companion object {
        private const val FTS_SEARCH_CONDITION = """
            (:query IS NULL OR id IN (SELECT rowid FROM episodes_fts WHERE episodes_fts MATCH :query))
        """
    }


    /** EPISODES **/

    @Upsert
    suspend fun upsertEpisode(episode: EpisodeEntity)

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Upsert
    suspend fun upsertEpisodeGroup(episodeGroup: EpisodeGroupEntity)

    @Upsert
    suspend fun upsertEpisodeGroups(episodeGroups: List<EpisodeGroupEntity>)

    @Transaction
    @Query("SELECT COALESCE(MAX(`order`), -1) FROM episode_group WHERE groupKey = :groupKey")
    suspend fun getMaxOrderByGroupKey(groupKey: String): Int

    @Transaction
    suspend fun upsertEpisodesWithGroup(episodes: List<EpisodeEntity>, groupKey: String) {
        if (episodes.isEmpty()) return

        upsertEpisodes(episodes)

        val createdAt = Clock.System.now()
        val startOrder = getMaxOrderByGroupKey(groupKey) + 1
        val groups = episodes.mapIndexed { index, episode ->
            EpisodeGroupEntity(
                groupKey = groupKey,
                id = episode.id,
                order = startOrder + index,
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
        WHERE id IN (:ids)
          AND NOT EXISTS (SELECT 1 FROM liked_episodes WHERE liked_episodes.id = episodes.id)
          AND NOT EXISTS (SELECT 1 FROM played_episodes WHERE played_episodes.id = episodes.id)
          AND NOT EXISTS (SELECT 1 FROM episode_group WHERE episode_group.id = episodes.id)
          AND NOT EXISTS (SELECT 1 FROM saved_episodes WHERE saved_episodes.id = episodes.id)
    """
    )
    suspend fun deleteEpisodesIfOrphaned(ids: List<Long>)

    @Query("DELETE FROM episode_group WHERE groupKey = :groupKey")
    suspend fun deleteEpisodeGroupsByGroupKey(groupKey: String)

    @Query("UPDATE episodes SET duration = :duration WHERE id = :id")
    suspend fun updateEpisodeDuration(id: Long, duration: Duration)

    @Query("SELECT * FROM episode_with_extras WHERE id = :id")
    fun getEpisodeById(id: Long): Flow<EpisodeWithExtrasView?>

    @Query("SELECT * FROM episode_with_extras WHERE id IN (:ids)")
    fun getEpisodesByIds(ids: List<Long>): Flow<List<EpisodeWithExtrasView>>

    @Query("SELECT * FROM episode_with_extras WHERE id IN (:ids)")
    suspend fun getEpisodesByIdsOnce(ids: List<Long>): List<EpisodeWithExtrasView>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE $FTS_SEARCH_CONDITION
        ORDER BY datePublished DESC
        LIMIT :limit
    """
    )
    fun getEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE $FTS_SEARCH_CONDITION
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


    /** EPISODE GROUPS **/

    @Query("SELECT * FROM episode_group WHERE groupKey = :groupKey")
    suspend fun getEpisodeGroupsByGroupKey(groupKey: String): List<EpisodeGroupEntity>

    @Query("SELECT MIN(createdAt) FROM episode_group WHERE groupKey = :groupKey")
    suspend fun getOldestCreatedAtByGroupKey(groupKey: String): Instant?

    @Query(
        """
        SELECT COUNT(*)
        FROM episode_group
        WHERE :prefix IS NULL OR groupKey LIKE :prefix || '%'
    """
    )
    suspend fun getEpisodeGroupCount(prefix: String? = null): Int

    @Query(
        """
        SELECT groupKey, COUNT(*) as count
        FROM episode_group
        WHERE :prefix IS NULL OR groupKey LIKE :prefix || '%'
        GROUP BY groupKey
        ORDER BY MIN(createdAt) ASC
    """
    )
    suspend fun getGroupKeysWithCounts(prefix: String? = null): List<GroupKeyWithCount>

    @Query("SELECT id FROM episode_group WHERE groupKey IN (:groupKeys)")
    suspend fun getEpisodeIdsByGroupKeys(groupKeys: List<String>): List<Long>

    @Query("DELETE FROM episode_group WHERE groupKey IN (:groupKeys)")
    suspend fun deleteEpisodeGroupsByGroupKeys(groupKeys: List<String>)

    @Transaction
    suspend fun deleteOldestGroupsIfExceedsLimit(
        threshold: Int,
        targetCount: Int = threshold,
        prefix: String? = null,
    ) {
        val totalCount = getEpisodeGroupCount(prefix)
        if (totalCount <= threshold) return

        val groupKeysWithCounts = getGroupKeysWithCounts(prefix)

        var currentCount = totalCount
        val groupKeysToDelete = mutableListOf<String>()

        for ((groupKey, count) in groupKeysWithCounts) {
            if (currentCount <= targetCount) break
            groupKeysToDelete.add(groupKey)
            currentCount -= count
        }

        if (groupKeysToDelete.isEmpty()) return

        val ids = getEpisodeIdsByGroupKeys(groupKeysToDelete)
        deleteEpisodeGroupsByGroupKeys(groupKeysToDelete)
        deleteEpisodesIfOrphaned(ids)
    }

    @Transaction
    suspend fun replaceEpisodes(episodes: List<EpisodeEntity>, groupKey: String) {
        val oldEpisodeIds = getEpisodeGroupsByGroupKey(groupKey).map { it.id }
        deleteEpisodeGroupsByGroupKey(groupKey)
        upsertEpisodesWithGroup(episodes, groupKey)
        deleteEpisodesIfOrphaned(oldEpisodeIds)

        val prefix = groupKey.split(":").first()
        deleteOldestGroupsIfExceedsLimit(threshold = 30_000, targetCount = 20_000, prefix = prefix)
    }


    @Query("SELECT MAX(datePublished) FROM episodes WHERE feedId = :feedId")
    suspend fun getLatestEpisodeDatePublished(feedId: Long): Instant?


    /** LIKED EPISODES **/

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLikedEpisode(likedEpisode: LikedEpisodeEntity)

    @Query("DELETE FROM liked_episodes WHERE id = :id")
    suspend fun removeLikedEpisode(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_episodes WHERE id = :id)")
    fun isLikedEpisode(id: Long): Flow<Boolean>

    @Transaction
    suspend fun toggleLikedEpisode(episode: EpisodeEntity): Boolean {
        return if (isLikedEpisode(episode.id).first()) {
            removeLikedEpisode(episode.id)
            deleteEpisodesIfOrphaned(listOf(episode.id))
            false
        } else {
            addLikedEpisode(
                LikedEpisodeEntity(
                    id = episode.id,
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
        AND $FTS_SEARCH_CONDITION
        ORDER BY likedAt DESC
        LIMIT :limit
    """
    )
    fun getLikedEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE likedAt IS NOT NULL
        AND $FTS_SEARCH_CONDITION
        ORDER BY likedAt DESC
    """
    )
    fun getLikedEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>


    /** PLAYED EPISODES **/

    @Upsert
    suspend fun upsertPlayedEpisode(playedEpisode: PlayedEpisodeEntity)

    @Query("DELETE FROM played_episodes WHERE id = :id")
    suspend fun deletePlayedEpisode(id: Long)

    @Transaction
    suspend fun removePlayedEpisode(id: Long) {
        deletePlayedEpisode(id)
        deleteEpisodesIfOrphaned(listOf(id))
    }

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE playedAt IS NOT NULL
        AND (:isCompleted IS NULL OR isCompleted = :isCompleted)
        AND $FTS_SEARCH_CONDITION
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
        AND $FTS_SEARCH_CONDITION
        ORDER BY playedAt DESC
    """
    )
    fun getPlayedEpisodesPaging(
        isCompleted: Boolean? = null,
        query: String? = null,
    ): PagingSource<Int, EpisodeWithExtrasView>


    /** SAVED EPISODES **/

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSavedEpisode(savedEpisode: SavedEpisodeEntity)

    @Query("DELETE FROM saved_episodes WHERE id = :id")
    suspend fun removeSavedEpisode(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_episodes WHERE id = :id)")
    fun isSavedEpisode(id: Long): Flow<Boolean>

    @Query(
        """
        UPDATE saved_episodes
        SET downloadedSize = :downloadedSize, downloadStatus = :status
        WHERE id = :id
    """
    )
    suspend fun updateSavedEpisodeProgress(id: Long, downloadedSize: Long, status: DownloadStatus)

    @Query("UPDATE saved_episodes SET downloadStatus = :status WHERE id = :id")
    suspend fun updateSavedEpisodeStatus(id: Long, status: DownloadStatus)

    @Transaction
    suspend fun toggleSavedEpisode(episode: EpisodeEntity, filePath: String): Boolean {
        return if (isSavedEpisode(episode.id).first()) {
            removeSavedEpisode(episode.id)
            deleteEpisodesIfOrphaned(listOf(episode.id))
            false
        } else {
            addSavedEpisode(
                SavedEpisodeEntity(
                    id = episode.id,
                    podcastId = episode.feedId,
                    savedAt = Clock.System.now(),
                    filePath = filePath,
                    totalSize = episode.enclosureLength,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )
            true
        }
    }

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE savedAt IS NOT NULL
        AND $FTS_SEARCH_CONDITION
        ORDER BY savedAt DESC
        LIMIT :limit
    """
    )
    fun getSavedEpisodes(query: String? = null, limit: Int): Flow<List<EpisodeWithExtrasView>>

    @Query(
        """
        SELECT * FROM episode_with_extras
        WHERE savedAt IS NOT NULL
        AND $FTS_SEARCH_CONDITION
        ORDER BY savedAt DESC
    """
    )
    fun getSavedEpisodesPaging(query: String? = null): PagingSource<Int, EpisodeWithExtrasView>
}
