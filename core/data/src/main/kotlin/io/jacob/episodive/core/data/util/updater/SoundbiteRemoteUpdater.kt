package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.FeedQuery
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.mapper.toSoundbiteEntities
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.mapper.toSoundbites
import io.jacob.episodive.core.network.model.SoundbiteResponse
import kotlin.time.Clock

class SoundbiteRemoteUpdater(
    private val localDataSource: FeedLocalDataSource,
    private val remoteDataSource: FeedRemoteDataSource,
    override val query: FeedQuery,
) : RemoteUpdater<FeedQuery, List<SoundbiteResponse>, List<SoundbiteEntity>, List<SoundbiteEntity>>(
    query
) {

    override suspend fun fetchFromRemote(): List<SoundbiteResponse> {
        return when (query) {
            is FeedQuery.Soundbite -> remoteDataSource.getRecentSoundbites(max = query.max)
            else -> emptyList()
        }
    }

    override suspend fun convertToEntity(response: List<SoundbiteResponse>): List<SoundbiteEntity> {
        return response.toSoundbites().toSoundbiteEntities(query.key)
    }

    override suspend fun saveToLocal(entity: List<SoundbiteEntity>) {
        localDataSource.replaceSoundbites(entity)
    }

    override suspend fun isExpired(output: List<SoundbiteEntity>): Boolean {
        if (output.isEmpty()) return true
        val oldestCache = output.minBy { it.cachedAt }.cachedAt
        val now = Clock.System.now()
        return now - oldestCache > query.timeToLive
    }
}