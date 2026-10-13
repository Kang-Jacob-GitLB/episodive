package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.StationDetails
import javax.inject.Inject

class GetStationDetailsUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(stationId: String): StationDetails? {
        return stationRepository.getStationDetails(stationId)
    }
}
