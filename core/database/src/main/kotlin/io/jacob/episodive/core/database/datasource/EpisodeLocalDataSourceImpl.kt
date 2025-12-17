package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeGroupEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Instant

class EpisodeLocalDataSourceImpl @Inject constructor(
    private val episodeDao: EpisodeDao,
) : EpisodeLocalDataSource {
    override suspend fun upsertEpisode(episode: EpisodeEntity) {
        episodeDao.upsertEpisode(episode)
    }

    override suspend fun upsertEpisodes(episodes: List<EpisodeEntity>) {
        episodeDao.upsertEpisodes(episodes)
    }

    override suspend fun upsertEpisodeGroup(episodeGroup: EpisodeGroupEntity) {
        episodeDao.upsertEpisodeGroup(episodeGroup)
    }

    override suspend fun upsertEpisodeGroups(episodeGroups: List<EpisodeGroupEntity>) {
        episodeDao.upsertEpisodeGroups(episodeGroups)
    }

    override suspend fun upsertEpisodes(episodes: List<EpisodeEntity>, groupKey: String) {
        episodeDao.upsertEpisodes(
            episodes = episodes,
            groupKey = groupKey,
        )
    }

    override suspend fun deleteEpisode(id: Long) {
        episodeDao.deleteEpisode(id)
    }

    override suspend fun deleteEpisodes() {
        episodeDao.deleteEpisodes()
    }

    override suspend fun deleteEpisodesByGroupKey(groupKey: String) {
        episodeDao.deleteEpisodesByGroupKey(groupKey)
    }

    override suspend fun replaceEpisodes(episodes: List<EpisodeEntity>, groupKey: String) {
        episodeDao.replaceEpisodes(
            episodes = episodes,
            groupKey = groupKey,
        )
    }

    override suspend fun updateDurationOfEpisodes(id: Long, duration: Duration) {
        episodeDao.updateDurationOfEpisodes(
            id = id,
            duration = duration,
        )
    }

    override fun getEpisodeById(id: Long): Flow<EpisodeWithExtrasView?> {
        return episodeDao.getEpisodeById(id)
    }

    override fun getEpisodesByIds(ids: List<Long>): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getEpisodesByIds(ids)
    }

    override fun getEpisodes(query: String?, limit: Int): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getEpisodes(
            query = query?.ifBlank { null },
            limit = limit,
        )
    }

    override fun getEpisodesPaging(query: String?): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getEpisodesPaging(query?.ifBlank { null })
    }

    override fun getEpisodesByGroupKey(
        groupKey: String,
        limit: Int,
    ): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getEpisodesByGroupKey(
            groupKey = groupKey,
            limit = limit,
        )
    }

    override fun getEpisodesByGroupKeyPaging(groupKey: String): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getEpisodesByGroupKeyPaging(groupKey)
    }

    override suspend fun getEpisodesOldestCreatedAtByGroupKey(groupKey: String): Instant? {
        return episodeDao.getEpisodesOldestCreatedAtByGroupKey(groupKey)
    }

    override suspend fun addLiked(likedEpisode: LikedEpisodeEntity) {
        episodeDao.addLiked(likedEpisode)
    }

    override suspend fun removeLiked(id: Long) {
        episodeDao.removeLiked(id)
    }

    override fun isLiked(id: Long): Flow<Boolean> {
        return episodeDao.isLiked(id)
    }

    override suspend fun toggleLiked(id: Long): Boolean {
        return episodeDao.toggleLiked(id)
    }

    override fun getLikedEpisodes(query: String?, limit: Int): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getLikedEpisodes(
            query = query?.ifBlank { null },
            limit = limit,
        )
    }

    override fun getLikedEpisodesPaging(query: String?): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getLikedEpisodesPaging(query?.ifBlank { null })
    }

    override suspend fun upsertPlayed(playedEpisode: PlayedEpisodeEntity) {
        episodeDao.upsertPlayed(playedEpisode)
    }

    override suspend fun removePlayed(id: Long) {
        episodeDao.removePlayed(id)
    }

    override fun getPlayedEpisodes(
        isCompleted: Boolean?,
        query: String?,
        limit: Int,
    ): Flow<List<EpisodeWithExtrasView>> {
        return episodeDao.getPlayedEpisodes(
            isCompleted = isCompleted,
            query = query?.ifBlank { null },
            limit = limit,
        )
    }

    override fun getPlayedEpisodesPaging(
        isCompleted: Boolean?,
        query: String?,
    ): PagingSource<Int, EpisodeWithExtrasView> {
        return episodeDao.getPlayedEpisodesPaging(
            isCompleted = isCompleted,
            query = query?.ifBlank { null },
        )
    }
}