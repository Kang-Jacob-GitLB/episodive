package io.jacob.episodive.core.network.api

import io.jacob.episodive.core.network.model.StationDetailsResponse
import io.jacob.episodive.core.network.model.StationUrlResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApi {
    /**
     * 방송국의 현재 스트림 URL. Radio Browser 의 클릭 집계도 겸하므로 목록을 그릴 때가 아니라
     * 실제로 재생할 때만 부른다.
     */
    @GET("json/url/{stationuuid}")
    suspend fun getStationUrl(
        @Path("stationuuid") stationUuid: String,
    ): StationUrlResponse

    /**
     * 스테이션 레코드를 uuid 로 조회한다. 클릭 집계는 하지 않으므로 화면을 그릴 때 불러도 된다.
     *
     * @param uuids 쉼표로 이어 붙인 uuid. 응답 순서는 요청 순서와 무관하다.
     */
    @GET("json/stations/byuuid")
    suspend fun getStationsByUuid(
        @Query("uuids") uuids: String,
    ): List<StationDetailsResponse>

    /**
     * 국가별 인기 스테이션. 깨진 것은 서버가 걸러 주고(`hidebroken`) 클릭 수 내림차순이다. 재생 가능 여부의
     * 세부 판정(https·코덱 등)은 클라이언트 몫이라 [limit] 은 필터 뒤에도 남을 만큼 넉넉히 받는다.
     * 클릭 집계는 하지 않는다.
     *
     * @param countryCode ISO 3166-1 alpha-2 (`US`)
     */
    @GET("json/stations/search?hidebroken=true&order=clickcount&reverse=true")
    suspend fun searchStations(
        @Query("countrycode") countryCode: String,
        @Query("limit") limit: Int,
    ): List<StationDetailsResponse>
}
