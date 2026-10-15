package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.Station
import javax.inject.Inject

/**
 * id 로 스테이션 하나를 찾는다. 카탈로그 → 해외 목록(메모리) → 즐겨찾기·최근에 남긴 메타(Room) 순이라 현재 국가의
 * 목록에 없는 스테이션(다른 나라의 최근·즐겨찾기, 순위 변동으로 목록에서 빠진 것)도 찾는다. 모르면 null.
 */
class GetStationUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(stationId: String): Station? = stationRepository.getStation(stationId)
}
