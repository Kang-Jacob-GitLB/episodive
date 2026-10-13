package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.Station
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentStationsUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    /** [limit] 은 최대 [StationRepository.MAX_RECENT_STATIONS] 까지 의미가 있다. */
    operator fun invoke(limit: Int = 10): Flow<List<Station>> {
        return stationRepository.getRecentStations(limit)
    }
}
