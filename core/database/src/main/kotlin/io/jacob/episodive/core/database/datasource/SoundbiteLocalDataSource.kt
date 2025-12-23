package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.model.SoundbiteEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface SoundbiteLocalDataSource {
    suspend fun upsertSoundbites(soundbites: List<SoundbiteEntity>)
    suspend fun deleteSoundbite(episodeId: Long)
    suspend fun deleteSoundbites()
    suspend fun replaceSoundbites(soundbites: List<SoundbiteEntity>)
    fun getSoundbites(limit: Int): Flow<List<SoundbiteEntity>>
    fun getSoundbitesPaging(): PagingSource<Int, SoundbiteEntity>
    suspend fun getSoundbitesOldestCachedAt(): Instant?
}