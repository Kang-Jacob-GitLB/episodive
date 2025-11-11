package io.jacob.episodive.core.data.util

import io.jacob.episodive.core.data.util.query.CacheableQuery
import io.jacob.episodive.core.data.util.updater.RemoteUpdater
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class Cacher<Query : CacheableQuery, Response, Entity>(
    private val remoteUpdater: RemoteUpdater<Query, Response, Entity>,
    private val sourceFactory: () -> Flow<Entity>,
) {
    val flow: Flow<Entity> = flow {
        val source = sourceFactory()

        // 백그라운드에서 만료 체크 및 갱신
        coroutineScope {
            launch {
                remoteUpdater.load(source.first())
            }
        }

        // 로컬 데이터 스트림을 그대로 전달
        source.collect { data ->
            emit(data)
        }
    }
}