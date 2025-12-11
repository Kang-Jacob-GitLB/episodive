package io.jacob.episodive.core.data.util.updater

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import io.jacob.episodive.core.data.util.query.CacheableQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

abstract class RemoteUpdater<Query : CacheableQuery, Response, Entity, Output : Any>(
    protected open val query: Query,
) {
    companion object {
        private const val FETCH_SIZE = 1000
    }

    protected abstract suspend fun fetchFromRemote(fetchSize: Int = FETCH_SIZE): List<Response>
    protected abstract suspend fun convertToEntity(responses: List<Response>): List<Entity>
    protected abstract suspend fun replaceToLocal(entities: List<Entity>)
    protected abstract suspend fun isExpired(): Boolean
    protected abstract fun getPagingSource(): PagingSource<Int, Output>
    protected abstract fun getFlowSource(count: Int): Flow<List<Output>>

    fun getPagingData(pagingConfig: PagingConfig): Flow<PagingData<Output>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { getPagingSource() }
        ).flow
            .onStart { refreshIfNeeded() }
    }

    fun getFlowList(count: Int): Flow<List<Output>> {
        return getFlowSource(count)
            .onStart { refreshIfNeeded() }
    }

    private suspend fun refreshIfNeeded() {
        if (isExpired()) {
            refresh()
        }
    }

    suspend fun refresh() {
        val remote = fetchFromRemote()
        val entity = convertToEntity(remote)
        replaceToLocal(entity)
    }
}