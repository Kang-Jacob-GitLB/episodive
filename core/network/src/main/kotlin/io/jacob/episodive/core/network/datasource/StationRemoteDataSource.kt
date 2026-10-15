package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.model.StationDetailsResponse

interface StationRemoteDataSource {
    /** 재생할 스트림 URL. 서버가 거부했거나 주소가 비어 있으면 null. */
    suspend fun resolveStreamUrl(stationUuid: String): String?

    /** uuid 에 해당하는 레코드들. 응답 순서는 보장되지 않고, 모르는 uuid 는 빠진다. */
    suspend fun getStationDetails(stationUuids: List<String>): List<StationDetailsResponse>

    /** 국가([countryCode], ISO alpha-2)의 인기 스테이션. 서버가 준 순서(인기순) 그대로이며 걸러내지 않은 원본이다. */
    suspend fun getStationsByCountry(countryCode: String): List<StationDetailsResponse>
}
