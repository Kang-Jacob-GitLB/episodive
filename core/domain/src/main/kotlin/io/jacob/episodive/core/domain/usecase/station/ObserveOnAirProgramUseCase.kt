package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.OnAirProgram
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveOnAirProgramUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    operator fun invoke(stationId: String): Flow<OnAirProgram?> {
        return stationRepository.observeOnAir(stationId)
    }
}
