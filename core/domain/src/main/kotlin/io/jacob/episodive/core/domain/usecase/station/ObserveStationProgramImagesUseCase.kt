package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.OnAirRefresh
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * 목록에 보이는 스테이션들의 "지금 방송 중" 프로그램 이미지. 값은 스테이션 id → https 이미지 주소이고, 프로그램을 모르거나
 * 이미지가 없는 스테이션은 빠진다(호출한 쪽은 이니셜 아트로 그린다).
 *
 * 곡은 필요 없으므로 곡 폴링을 켜지 않고, 프로그램 경계에만 다시 확인한다([OnAirRefresh.Boundary]) — 캐시에 지금 프로그램이
 * 있으면 TTL 이 지나도 그대로 쓴다. 스테이션마다 편성 Flow 를 구독하지만 편성표는 스테이션별로 캐시한다. 방송사 응답을
 * 공유하는 것은 MBC 3채널뿐이고 나머지(KBS·SBS·CBS)는 스테이션마다 요청한다. 그래서 진입 때 요청이 한꺼번에 몰리는 것은
 * 허용하되 그 뒤로는 경계마다 한 번으로 줄인다. 첫 응답을 기다리지 않도록 스테이션마다 빈 값으로 시작한다.
 */
class ObserveStationProgramImagesUseCase @Inject constructor(
    private val stationRepository: StationRepository,
) {
    operator fun invoke(stationIds: List<String>): Flow<Map<String, String>> {
        val ids = stationIds.distinct()
        if (ids.isEmpty()) return flowOf(emptyMap())
        return combine(
            ids.map { id ->
                stationRepository.observeOnAir(id, includeSong = false, refresh = OnAirRefresh.Boundary)
                    .map { program -> program?.imageUrl?.takeIf { it.isNotBlank() } }
                    .onStart { emit(null) }
            }
        ) { images ->
            buildMap { images.forEachIndexed { i, url -> if (url != null) put(ids[i], url) } }
        }
    }
}
