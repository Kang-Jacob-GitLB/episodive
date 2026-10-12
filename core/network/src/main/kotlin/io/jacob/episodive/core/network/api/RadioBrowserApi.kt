package io.jacob.episodive.core.network.api

import io.jacob.episodive.core.network.model.StationUrlResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface RadioBrowserApi {
    /**
     * 방송국의 현재 스트림 URL. Radio Browser 의 클릭 집계도 겸하므로 목록을 그릴 때가 아니라
     * 실제로 재생할 때만 부른다.
     */
    @GET("json/url/{stationuuid}")
    suspend fun getStationUrl(
        @Path("stationuuid") stationUuid: String,
    ): StationUrlResponse
}
