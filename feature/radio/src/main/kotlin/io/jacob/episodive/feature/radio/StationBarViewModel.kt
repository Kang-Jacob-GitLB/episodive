package io.jacob.episodive.feature.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.player.SleepTimer
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.station.LiveStreamSupervisor
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.station.ObserveOnAirProgramUseCase
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.Station
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 앱 전역 라디오 미니바의 상태. 앱에 하나뿐이다 — 재생 불가 안내(`unplayable`)를 여기서만 받아야
 * 라디오 화면이 열려 있어도 스낵바가 한 번만 뜨고, 다른 탭에서도 알릴 수 있다.
 */
@HiltViewModel
class StationBarViewModel @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    supervisor: LiveStreamSupervisor,
    private val stationPlaybackController: StationPlaybackController,
    sleepTimer: SleepTimer,
    observeOnAirProgramUseCase: ObserveOnAirProgramUseCase,
) : ViewModel() {

    // 스테이션 여부는 nowPlayingStation 하나로만 가른다. nowPlaying(에피소드)과 combine 하면 전환 순간
    // 서로 다른 시점의 쌍을 본다 — 나머지 값은 이 스테이션이 정해진 뒤에 붙인다.
    val state: StateFlow<StationBarState> = playerRepository.nowPlayingStation
        .flatMapLatest { station ->
            if (station == null) {
                flowOf(StationBarState.Hidden)
            } else {
                combine(
                    playerRepository.streamTitle,
                    playerRepository.isPlaying,
                    playerRepository.isBuffering,
                    // 네트워크로 받는 값이라 늦게 온다. 첫 값을 기다리느라 바가 뜨지 못하지 않게 null 로 시작한다.
                    // 이 스테이션이 바뀌면 flatMapLatest 가 구독을 끊으므로 다른 스테이션의 프로그램이 섞이지 않는다.
                    observeOnAirProgramUseCase(station.id).onStart { emit(null) },
                ) { streamTitle, isPlaying, isBuffering, program ->
                    StationBarState.Active(
                        station = station,
                        streamTitle = streamTitle,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        program = program,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StationBarState.Hidden,
        )

    private val _effect = MutableSharedFlow<StationBarEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        // 감독자가 복구를 포기하면 항목을 비운 뒤에 알린다. 바가 사라지는 순간 도착하므로 화면 쪽 수집은
        // 바의 표시 여부와 무관해야 한다.
        viewModelScope.launch {
            supervisor.unplayable.collect { _effect.emit(StationBarEffect.ShowUnplayable(it)) }
        }
        // 주소를 얻지 못한 안내도 여기서만 받는다. 화면(홈·라디오)이 컴포즈되지 않은 사이에도, 어느 탭에서든
        // 보여야 하기 때문이다.
        viewModelScope.launch {
            stationPlaybackController.unavailable.collect { _effect.emit(StationBarEffect.ShowUnavailable(it)) }
        }
        // 타이머는 앱 수명이라 에피소드 쪽 안내(PlayerBar)가 스테이션 재생 중에는 입을 닫는다. 대신 여기서 알린다.
        // 스테이션이 올라 있을 때만 — 에피소드 재생 중 만료는 PlayerBar 가 이미 알린다.
        viewModelScope.launch {
            sleepTimer.expired.collect {
                if (playerRepository.nowPlayingStation.first() != null) {
                    _effect.emit(StationBarEffect.ShowSleepTimerExpired)
                }
            }
        }
    }

    /**
     * 지금 스테이션이 올라 있는가. [state] 는 구독이 걸리기 전까지 초기값(Hidden)이라, 새 Activity 의 첫
     * 프레임에 읽으면 오판한다. 플레이어의 현재 값을 직접 읽는다.
     */
    suspend fun isStationActive(): Boolean = playerRepository.nowPlayingStation.first() != null

    fun sendAction(action: StationBarAction) {
        when (action) {
            is StationBarAction.PlayOrPause -> playerRepository.playOrPause()
            // 받는 중인 요청을 취소하고 stop() 을 부르는 것까지 컨트롤러가 한다. stopStation 이 아니라
            // stop() 이어야 playRequestCount 가 올라 진행 중인 PlayStationUseCase 가 Superseded 로 걸러진다.
            is StationBarAction.Stop -> stationPlaybackController.stop()
        }
    }
}

sealed interface StationBarState {
    data object Hidden : StationBarState
    data class Active(
        val station: Station,
        val streamTitle: String?,
        val isPlaying: Boolean,
        val isBuffering: Boolean,
        /** 지금 방송 중인 프로그램. 모르면 null. */
        val program: OnAirProgram? = null,
    ) : StationBarState
}

sealed interface StationBarAction {
    data object PlayOrPause : StationBarAction
    data object Stop : StationBarAction
}

sealed interface StationBarEffect {
    data class ShowUnplayable(val station: Station) : StationBarEffect
    data class ShowUnavailable(val station: Station) : StationBarEffect
    data object ShowSleepTimerExpired : StationBarEffect
}
