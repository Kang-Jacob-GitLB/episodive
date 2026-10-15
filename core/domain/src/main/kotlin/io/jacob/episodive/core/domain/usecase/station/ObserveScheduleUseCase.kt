package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.StationSchedule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveScheduleUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    operator fun invoke(stationId: String): Flow<StationSchedule> {
        return stationRepository.observeSchedule(stationId)
    }
}
