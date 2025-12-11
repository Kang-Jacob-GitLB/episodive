package io.jacob.episodive.core.data.util.updater

import androidx.paging.PagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toSoundbiteEntities
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toSoundbites
import io.jacob.episodive.core.network.model.SoundbiteResponse
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class SoundbiteRemoteUpdater @AssistedInject constructor(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    @Assisted("query") override val query: FeedQuery,
) : RemoteUpdater<FeedQuery, SoundbiteResponse, SoundbiteEntity, SoundbiteEntity>(query) {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("query") query: FeedQuery): SoundbiteRemoteUpdater
    }

    override suspend fun fetchFromRemote(fetchSize: Int): List<SoundbiteResponse> {
        return when (query) {
            is FeedQuery.Soundbite -> remoteDataSource.getRecentSoundbites(max = fetchSize)
            else -> emptyList()
        }
    }

    override suspend fun convertToEntity(responses: List<SoundbiteResponse>): List<SoundbiteEntity> {
        return responses.toSoundbites().toSoundbiteEntities(query.key)
    }

    override suspend fun replaceToLocal(entities: List<SoundbiteEntity>) {
        localDataSource.replaceSoundbites(entities)
    }

    override suspend fun isExpired(): Boolean {
        val oldestCachedAt = localDataSource.getSoundbitesOldestCachedAtByCacheKey(query.key)
            ?: return true

        val now = Clock.System.now()
        return now - oldestCachedAt > query.timeToLive
    }

    override fun getPagingSource(): PagingSource<Int, SoundbiteEntity> {
        return localDataSource.getSoundbitesByCacheKeyPaging(query.key)
    }

    override fun getFlowSource(count: Int): Flow<List<SoundbiteEntity>> {
        return localDataSource.getSoundbitesByCacheKey(query.key, count)
    }
}