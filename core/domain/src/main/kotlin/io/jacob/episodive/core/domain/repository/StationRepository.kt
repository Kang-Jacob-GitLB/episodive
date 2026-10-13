package io.jacob.episodive.core.domain.repository

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
