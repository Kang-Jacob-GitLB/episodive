package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.dao.SoundbiteDao
import io.jacob.episodive.core.database.model.SoundbiteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.time.Instant

class SoundbiteLocalDataSourceImpl @Inject constructor(
    private val soundbiteDao: SoundbiteDao,
) : SoundbiteLocalDataSource {
    override suspend fun upsertSoundbites(soundbites: List<SoundbiteEntity>) {
        soundbiteDao.upsertSoundbites(soundbites)
    }

    override suspend fun deleteSoundbite(episodeId: Long) {
        soundbiteDao.deleteSoundbite(episodeId)
    }

    override suspend fun deleteSoundbites() {
        soundbiteDao.deleteSoundbites()
    }

    override suspend fun replaceSoundbites(soundbites: List<SoundbiteEntity>) {
        soundbiteDao.replaceSoundbites(soundbites)
    }

    override fun getSoundbites(limit: Int): Flow<List<SoundbiteEntity>> {
        return soundbiteDao.getSoundbites(limit)
    }

    override fun getSoundbitesPaging(): PagingSource<Int, SoundbiteEntity> {
        return soundbiteDao.getSoundbitesPaging()
    }

    override suspend fun getSoundbitesPagingList(offset: Int, limit: Int): List<SoundbiteEntity> {
        return soundbiteDao.getSoundbitesPagingList(offset, limit)
    }

    override suspend fun getSoundbitesOldestCachedAt(): Instant? {
        return soundbiteDao.getSoundbitesOldestCachedAt()
    }
}