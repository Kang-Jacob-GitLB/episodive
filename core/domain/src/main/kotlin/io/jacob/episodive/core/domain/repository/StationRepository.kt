package io.jacob.episodive.core.domain.repository

import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationStream
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    fun getStations(kind: StationKind): Flow<List<Station>>

    /** 카탈로그에 있는 스테이션. 메모리에서 바로 답한다. 모르는 id 면 null. */
    fun getStation(stationId: String): Station?

    /**
     * 재생 직전에 부른다. 알 수 없는 방송국이거나 주소를 못 얻으면 null.
     *
     * @param excluding 이미 실패한 주소. 후보가 여기 있으면 건너뛰고 다음 후보로 간다(fallback 도 마찬가지).
     */
    suspend fun resolveStreamUrl(
        stationId: String,
        excluding: Set<String> = emptySet(),
    ): StationStream?

    /**
     * 홈페이지·음질·태그. 네트워크로 받아 24시간 메모리에 캐시한다. 모르는 스테이션이거나 받지 못했거나
     * 쓸 만한 값이 하나도 없으면 null — 부가 정보라 호출한 쪽은 그 섹션만 숨기면 된다.
     */
    suspend fun getStationDetails(stationId: String): StationDetails?

    /**
     * 지금 방송 중인 프로그램. 구독하는 동안 프로그램이 바뀌는 시각에 맞춰 새로 발행한다. 방송사 비공식 API 라
     * 편성 정보가 없는 스테이션·네트워크 실패·편성표에 지금 시각이 없으면 null 을 발행하고(에러로 올리지 않는다),
     * 호출한 쪽은 프로그램 정보 없이 그리면 된다.
     */
    fun observeOnAir(stationId: String): Flow<OnAirProgram?>

    /**
     * 오늘(Asia/Seoul) 방송분, 시작 시각순. [observeOnAir] 와 같은 편성표 캐시를 쓰므로 네트워크를 따로 부르지 않는다.
     * 자정을 넘겨 오늘로 이어지는 어제 시작 프로그램도 포함한다. 편성 정보가 없는 스테이션·실패면 빈 목록을 발행한다.
     */
    fun observeTodaySchedule(stationId: String): Flow<List<OnAirProgram>>

    /** 즐겨찾기한 순서의 역순(최신순). 카탈로그에서 빠진 스테이션은 제외된다. */
    fun getFavoriteStations(): Flow<List<Station>>

    fun isFavorite(stationId: String): Flow<Boolean>

    /** 바뀐 뒤의 상태를 돌려준다(true = 즐겨찾기됨). UI 가 결과로 문구를 고를 수 있게 한다. */
    suspend fun toggleFavorite(stationId: String): Boolean

    /**
     * 최근 들은 순. 카탈로그에서 빠진 스테이션은 제외된다. [limit] 은 최대 [MAX_RECENT_STATIONS] 까지
     * 의미가 있다 — 그보다 많이는 보관하지 않는다.
     */
    fun getRecentStations(limit: Int): Flow<List<Station>>

    /** 재생을 시작한 스테이션을 최근 청취에 기록한다. */
    suspend fun markPlayed(stationId: String)

    companion object {
        /** 최근 청취로 보관하는 최대 개수. */
        const val MAX_RECENT_STATIONS = 20
    }
}
