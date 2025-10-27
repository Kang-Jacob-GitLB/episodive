package io.jacob.episodive.core.data.util.updater

import androidx.room.Transaction
import io.jacob.episodive.core.data.util.query.CacheableQuery

abstract class BaseRemoteUpdater<Entity, Query : CacheableQuery, Response>(
    protected open val query: Query,
) : RemoteUpdater<Entity> {

    abstract suspend fun fetchFromRemote(query: Query): List<Response>
    abstract suspend fun fetchFromRemoteSingle(query: Query): Response?
    abstract suspend fun mapToEntities(responses: List<Response>, query: Query): List<Entity>
    abstract suspend fun mapToEntity(response: Response?, query: Query): Entity?
    abstract suspend fun saveToLocal(entities: List<Entity>)
    abstract suspend fun saveToLocal(entity: Entity?)
    abstract suspend fun isExpired(cached: List<Entity>): Boolean
    abstract suspend fun isExpired(cached: Entity?): Boolean
    abstract suspend fun deleteLocal(query: Query)

    @Transaction
    override suspend fun load(cached: List<Entity>) {
        try {
            if (isExpired(cached)) {
                deleteLocal(query)
                val responses = fetchFromRemote(query)
                val entities = mapToEntities(responses, query)
                saveToLocal(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Transaction
    override suspend fun load(cached: Entity?) {
        try {
            if (isExpired(cached)) {
                deleteLocal(query)
                val responses = fetchFromRemoteSingle(query)
                val entities = mapToEntity(responses, query)
                saveToLocal(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}