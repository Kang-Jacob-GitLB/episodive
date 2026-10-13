package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStationsUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    operator fun invoke(kind: StationKind): Flow<List<Station>> {
        return stationRepository.getStations(kind)
    }
}
