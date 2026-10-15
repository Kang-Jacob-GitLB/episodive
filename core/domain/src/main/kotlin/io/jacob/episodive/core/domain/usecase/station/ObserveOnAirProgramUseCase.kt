package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.OnAirRefresh
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveOnAirProgramUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    operator fun invoke(
        stationId: String,
        includeSong: Boolean = true,
        refresh: OnAirRefresh = OnAirRefresh.Live,
    ): Flow<OnAirProgram?> {
        return stationRepository.observeOnAir(stationId, includeSong, refresh)
    }
}
