package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.StationStream
import javax.inject.Inject

class ResolveStationStreamUrlUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(stationId: String): StationStream? {
        return stationRepository.resolveStreamUrl(stationId)
    }
}
