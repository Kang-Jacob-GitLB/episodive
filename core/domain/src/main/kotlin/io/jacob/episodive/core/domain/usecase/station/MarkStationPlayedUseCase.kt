package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import javax.inject.Inject

class MarkStationPlayedUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(stationId: String) {
        stationRepository.markPlayed(stationId)
    }
}
