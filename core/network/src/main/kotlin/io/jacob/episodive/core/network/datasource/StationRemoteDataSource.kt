package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.model.StationDetailsResponse

interface StationRemoteDataSource {
    /** 재생할 스트림 URL. 서버가 거부했거나 주소가 비어 있으면 null. */
    suspend fun resolveStreamUrl(stationUuid: String): String?

    /** uuid 에 해당하는 레코드들. 응답 순서는 보장되지 않고, 모르는 uuid 는 빠진다. */
    suspend fun getStationDetails(stationUuids: List<String>): List<StationDetailsResponse>
}
