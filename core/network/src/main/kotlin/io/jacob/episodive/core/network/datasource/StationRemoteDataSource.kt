package io.jacob.episodive.core.network.datasource

interface StationRemoteDataSource {
    /** 재생할 스트림 URL. 서버가 거부했거나 주소가 비어 있으면 null. */
    suspend fun resolveStreamUrl(stationUuid: String): String?
}
