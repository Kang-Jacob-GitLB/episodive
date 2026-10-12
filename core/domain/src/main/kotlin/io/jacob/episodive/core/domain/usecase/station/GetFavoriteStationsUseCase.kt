package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.Station
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoriteStationsUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    operator fun invoke(): Flow<List<Station>> {
        return stationRepository.getFavoriteStations()
    }
}
