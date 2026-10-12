package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import javax.inject.Inject

class ToggleFavoriteStationUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    /** 바뀐 뒤의 상태(true = 즐겨찾기됨)를 돌려줘 UI 가 결과로 문구를 고를 수 있다. */
    suspend operator fun invoke(stationId: String): Boolean {
        return stationRepository.toggleFavorite(stationId)
    }
}
