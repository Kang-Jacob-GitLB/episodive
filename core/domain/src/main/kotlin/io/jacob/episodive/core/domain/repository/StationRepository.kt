package io.jacob.episodive.core.domain.repository

import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.OnAirRefresh
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationListing
import io.jacob.episodive.core.model.StationSchedule
import io.jacob.episodive.core.model.StationStream
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    /**
     * 국가별 전체 목록. [countryCode] 가 null 이거나 `KR`(또는 두 글자 알파벳이 아닌 값)이면 앱이 고른
     * 카탈로그([io.jacob.episodive.core.model.StationListingSource.Curated])이고, 그 밖의 국가는 Radio Browser 의
     * 인기 목록이다. 국가는 이 목록에만 영향을 준다 — [getStation]·즐겨찾기·최근·재생 주소는 국가와 무관하다.
     *
     * 해외 목록은 메모리에 캐시하며(국가별 6시간) 받지 못하면 이전 값을 주고, 이전 값도 없으면 Flow 가 예외로
     * 끝난다. 호출한 쪽이 그 경우를 빈 목록으로 그리면 된다. [kind] 가 라디오가 아니면 국가와 무관하게 카탈로그다.
     */
    fun getStationListing(kind: StationKind, countryCode: String?): Flow<StationListing>

    /**
     * 스테이션 하나. 카탈로그 → 지금까지 받은 해외 목록(메모리) → 즐겨찾기·최근에 남긴 해외 메타(Room) 순으로
     * 찾는다. 모르는 id 면 null.
     */
    suspend fun getStation(stationId: String): Station?

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
     *
     * @param includeSong 곡을 주는 방송사(MBC)의 곡을 얹을지. 곡은 45초마다 폴링해야 해서, 제목만 필요한 호출(위젯·알림
     *   갱신 트리거)은 false 로 불러 백그라운드에서 폴링이 돌지 않게 한다.
     * @param refresh 재확인 주기. 목록 썸네일처럼 여러 스테이션을 한꺼번에 볼 때는 [OnAirRefresh.Boundary] 로 요청을 줄인다.
     */
    fun observeOnAir(
        stationId: String,
        includeSong: Boolean = true,
        refresh: OnAirRefresh = OnAirRefresh.Live,
    ): Flow<OnAirProgram?>

    /**
     * 지금 기준 편성표: 오늘(Asia/Seoul) 전체와 내일 새벽(05:00)까지, 시작 시각순. 자정을 넘겨 오늘로 이어지는 어제 시작
     * 프로그램도 포함한다. [observeOnAir] 와 같은 편성표 캐시를 쓰므로 네트워크를 따로 부르지 않는다. 편성 정보가 없는
     * 스테이션·실패면 빈 목록을 발행한다.
     */
    fun observeSchedule(stationId: String): Flow<StationSchedule>

    /** 즐겨찾기한 순서의 역순(최신순). 카탈로그에도 해외 메타에도 없는 스테이션은 제외된다. */
    fun getFavoriteStations(): Flow<List<Station>>

    fun isFavorite(stationId: String): Flow<Boolean>

    /** 바뀐 뒤의 상태를 돌려준다(true = 즐겨찾기됨). UI 가 결과로 문구를 고를 수 있게 한다. */
    suspend fun toggleFavorite(stationId: String): Boolean

    /**
     * 최근 들은 순. 카탈로그에도 해외 메타에도 없는 스테이션은 제외된다. [limit] 은 최대 [MAX_RECENT_STATIONS] 까지
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
