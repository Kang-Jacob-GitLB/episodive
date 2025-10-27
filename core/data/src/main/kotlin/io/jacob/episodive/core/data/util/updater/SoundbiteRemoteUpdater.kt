package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toSoundbiteEntities
import io.jacob.episodive.core.database.mapper.toSoundbiteEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toSoundbite
import io.jacob.episodive.core.network.mapper.toSoundbites
import io.jacob.episodive.core.network.model.SoundbiteResponse
import kotlin.time.Clock

class SoundbiteRemoteUpdater(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    override val query: FeedQuery,
) : BaseRemoteUpdater<SoundbiteEntity, FeedQuery, SoundbiteResponse>(query) {

    override suspend fun fetchFromRemote(query: FeedQuery): List<SoundbiteResponse> {
        return when (query) {
            is FeedQuery.Soundbite -> remoteDataSource.getRecentSoundbites()
            else -> emptyList()
        }
    }

    override suspend fun fetchFromRemoteSingle(query: FeedQuery): SoundbiteResponse? {
        return null
    }

    override suspend fun mapToEntities(
        responses: List<SoundbiteResponse>,
        query: FeedQuery,
    ): List<SoundbiteEntity> {
        return responses.toSoundbites().toSoundbiteEntities(query.key)
    }

    override suspend fun mapToEntity(
        response: SoundbiteResponse?,
        query: FeedQuery,
    ): SoundbiteEntity? {
        return response?.toSoundbite()?.toSoundbiteEntity(query.key)
    }

    override suspend fun saveToLocal(entities: List<SoundbiteEntity>) {
        localDataSource.upsertSoundbites(entities)
    }

    override suspend fun saveToLocal(entity: SoundbiteEntity?) {
        entity?.let { localDataSource.upsertSoundbites(listOf(it)) }
    }

    override suspend fun isExpired(cached: List<SoundbiteEntity>): Boolean {
        if (cached.isEmpty()) return true
        val oldestCache = cached.minByOrNull { it.cachedAt }?.cachedAt
            ?: return true
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }

    override suspend fun isExpired(cached: SoundbiteEntity?): Boolean {
        if (cached == null) return true
        val oldCached = cached.cachedAt
        val now = Clock.System.now()
        return now - oldCached > query.timeToLive
    }

    override suspend fun deleteLocal(query: FeedQuery) {
        localDataSource.deleteSoundbitesByCacheKey(query.key)
    }
}