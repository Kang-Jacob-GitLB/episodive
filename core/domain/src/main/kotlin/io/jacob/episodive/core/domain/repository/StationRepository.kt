package io.jacob.episodive.core.domain.repository

import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationStream
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    fun getStations(kind: StationKind): Flow<List<Station>>

    /** 재생 직전에 부른다. 알 수 없는 방송국이거나 주소를 못 얻으면 null. */
    suspend fun resolveStreamUrl(stationId: String): StationStream?
}
