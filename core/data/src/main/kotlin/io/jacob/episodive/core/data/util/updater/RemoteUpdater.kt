package io.jacob.episodive.core.data.util.updater

import androidx.room.Transaction
import io.jacob.episodive.core.data.util.query.CacheableQuery

abstract class RemoteUpdater<Query : CacheableQuery, Response, Entity>(
    protected open val query: Query,
) {
    abstract suspend fun fetchFromRemote(): Response
    abstract suspend fun mapToEntities(response: Response): Entity
    abstract suspend fun saveToLocal(entity: Entity)
    abstract suspend fun isExpired(cached: Entity): Boolean
    abstract suspend fun deleteLocal()

    @Transaction
    suspend fun load(cached: Entity) {
        try {
            if (isExpired(cached)) {
                deleteLocal()
                val responses = fetchFromRemote()
                val entities = mapToEntities(responses)
                saveToLocal(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}