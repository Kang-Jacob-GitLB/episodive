package io.jacob.episodive.core.data.util.updater

import io.jacob.episodive.core.data.util.query.CacheableQuery

abstract class RemoteUpdater<Query : CacheableQuery, Response, Entity, Output>(
    protected open val query: Query,
) {
    abstract suspend fun fetchFromRemote(): Response
    abstract suspend fun convertToEntity(response: Response): Entity
    abstract suspend fun saveToLocal(entity: Entity)
    abstract suspend fun isExpired(output: Output): Boolean

    suspend fun load(output: Output) {
        try {
            if (isExpired(output)) {
                val responses = fetchFromRemote()
                val entities = convertToEntity(responses)
                saveToLocal(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}