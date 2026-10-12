package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.station.PlayStationResult
import io.jacob.episodive.core.domain.usecase.station.PlayStationUseCase
import io.jacob.episodive.core.model.Station
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 모든 판단은 [mainDispatcher] 에서 한다. `PlayStationUseCase` 와 플레이어가 메인 전용이고, [playJob] 과
 * [pendingStationId] 도 그 한 스레드에서만 만지므로 따로 동기화하지 않는다.
 */
class StationPlaybackControllerImpl(
    private val playStationUseCase: PlayStationUseCase,
    private val playerRepository: PlayerRepository,
    private val stationRepository: StationRepository,
    private val scope: CoroutineScope,
    private val mainDispatcher: CoroutineDispatcher,
) : StationPlaybackController {

    private val _pendingStationId = MutableStateFlow<String?>(null)
    override val pendingStationId: StateFlow<String?> = _pendingStationId.asStateFlow()

    // replay 를 두지 않는다. 지난 실패가 다음 구독자에게 재생되면 이미 지나간 일을 알린다.
    private val _unavailable = MutableSharedFlow<Station>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val unavailable: Flow<Station> = _unavailable

    private var playJob: Job? = null

    override fun play(stationId: String) {
        // UNDISPATCHED — 메인에서 부르면 판정과 pending 설정이 호출 안에서 끝나, 이어지는 호출이 그 결과를 본다.
        scope.launch(mainDispatcher, start = CoroutineStart.UNDISPATCHED) {
            // 플레이어의 현재 값(StateFlow)이라 중단 없이 돌아온다.
            val nowPlayingId = playerRepository.nowPlayingStation.first()?.id
            val pendingId = _pendingStationId.value

            if (nowPlayingId == stationId) {
                if (pendingId == null) {
                    playerRepository.playOrPause()
                } else {
                    // 다른 스테이션을 받는 중에 원래 듣던 것을 다시 골랐다. 받는 중인 쪽만 버리고 재생 상태는 그대로 둔다.
                    cancelPending()
                }
                return@launch
            }
            if (stationId == pendingId) return@launch

            playJob?.cancel()
            _pendingStationId.value = stationId
            val job = scope.launch(mainDispatcher) {
                if (playStationUseCase(stationId) == PlayStationResult.Unavailable) {
                    stationRepository.getStation(stationId)?.let { _unavailable.emit(it) }
                }
            }
            playJob = job
            // 취소된 앞 요청의 정리가 뒤 요청의 pending 을 지우지 않게, 자기 차례일 때만 비운다.
            job.invokeOnCompletion { if (job === playJob) _pendingStationId.value = null }
        }
    }

    override fun stop() {
        cancelPending()
        playerRepository.stop()
    }

    private fun cancelPending() {
        playJob?.cancel()
        playJob = null
        _pendingStationId.value = null
    }
}
