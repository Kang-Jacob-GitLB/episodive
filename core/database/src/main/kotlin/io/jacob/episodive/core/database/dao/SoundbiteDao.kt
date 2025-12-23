package io.jacob.episodive.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.SoundbiteEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
interface SoundbiteDao {
    @Upsert
    suspend fun upsertSoundbites(soundbites: List<SoundbiteEntity>)

    @Query("DELETE FROM soundbites WHERE episodeId = :episodeId")
    suspend fun deleteSoundbite(episodeId: Long)

    @Query("DELETE FROM soundbites")
    suspend fun deleteSoundbites()

    @Transaction
    suspend fun replaceSoundbites(soundbites: List<SoundbiteEntity>) {
        deleteSoundbites()
        upsertSoundbites(soundbites)
    }

    @Query("SELECT * FROM soundbites LIMIT :limit")
    fun getSoundbites(limit: Int): Flow<List<SoundbiteEntity>>

    @Query("SELECT * FROM soundbites")
    fun getSoundbitesPaging(): PagingSource<Int, SoundbiteEntity>

    @Query("SELECT MIN(cachedAt) FROM soundbites")
    suspend fun getSoundbitesOldestCachedAt(): Instant?
}