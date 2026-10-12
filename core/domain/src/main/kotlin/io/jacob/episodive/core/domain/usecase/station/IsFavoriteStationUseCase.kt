package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsFavoriteStationUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    operator fun invoke(stationId: String): Flow<Boolean> {
        return stationRepository.isFavorite(stationId)
    }
}
