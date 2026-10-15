package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetStationsUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    // 국가별 목록은 아직 화면에 연결하지 않았다. 한국 카탈로그로 고정한다.
    operator fun invoke(kind: StationKind): Flow<List<Station>> {
        return stationRepository.getStationListing(kind, countryCode = "KR").map { it.stations }
    }
}
