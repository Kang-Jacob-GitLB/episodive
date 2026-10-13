package io.jacob.episodive.feature.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.common.combine
import io.jacob.episodive.core.domain.player.SleepTimer
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.station.GetFavoriteStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.ToggleFavoriteStationUseCase
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * 라디오 전용 플레이어 시트의 상태. 에피소드용 `PlayerViewModel` 은 스테이션이 올라 있는 동안 Error 라
 * 쓸 수 없다 — 에피소드 id·재생 위치 저장을 쥐고 있어 여기서 섞지 않는다.
 *
 * 스테이션 여부는 nowPlayingStation 하나로만 가른다. 에피소드 쪽 Flow 와 combine 하지 않는다.
 */
@HiltViewModel
class StationPlayerViewModel @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val stationPlaybackController: StationPlaybackController,
    private val getStationsUseCase: GetStationsUseCase,
    private val getFavoriteStationsUseCase: GetFavoriteStationsUseCase,
    private val toggleFavoriteStationUseCase: ToggleFavoriteStationUseCase,
    private val sleepTimer: SleepTimer,
) : ViewModel() {

    val state: StateFlow<StationPlayerState> = combine(
        playerRepository.nowPlayingStation,
        stationPlaybackController.pendingStationId,
        playerRepository.streamTitle,
        playerRepository.isPlaying,
        playerRepository.isBuffering,
        getFavoriteStationsUseCase(),
        getStationsUseCase(StationKind.RADIO),
        sleepTimer.remainingMs,
    ) { playing, pendingId, streamTitle, isPlaying, isBuffering, favorites, stations, sleepTimerRemainingMs ->
        // 받는 중인 스테이션을 먼저 보여 준다. 그동안 플레이어는 이전 스테이션을 그대로 들고 있다.
        val pending = pendingId?.let { id ->
            stations.firstOrNull { it.id == id } ?: favorites.firstOrNull { it.id == id }
        }
        val station = pending ?: playing
        when {
            pendingId == null && playing == null -> StationPlayerState.Hidden
            // 받는 중인 스테이션을 목록에서 아직 못 찾았고 들고 있는 것도 없다. 사라진 것이 아니라 준비 중이다.
            station == null -> StationPlayerState.Loading
            else -> {
                val ring = stationRing(station.id, favorites, stations)
                StationPlayerState.Active(
                    station = station,
                    isPending = pending != null,
                    // 받는 중에는 곡명이 이전 스테이션의 것이다.
                    streamTitle = streamTitle.takeIf { pending == null },
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    isFavorite = favorites.any { it.id == station.id },
                    previous = ring.neighbor(station.id, -1),
                    next = ring.neighbor(station.id, 1),
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StationPlayerState.Loading,
    )

    private val _action = MutableSharedFlow<StationPlayerAction>(extraBufferCapacity = 1)

    init {
        handleActions()
    }

    // 즐겨찾기 토글은 따로 띄운다. 여기서 기다리면 그동안 이전/다음 연타가 줄을 선다.
    private fun handleActions() = viewModelScope.launch {
        _action.collect { action ->
            when (action) {
                is StationPlayerAction.PlayOrPause -> playOrPause()
                is StationPlayerAction.Stop -> stationPlaybackController.stop()
                is StationPlayerAction.Previous -> step(-1)
                is StationPlayerAction.Next -> step(1)
                is StationPlayerAction.ToggleFavorite -> toggleFavorite()
                is StationPlayerAction.SetSleepTimer -> sleepTimer.start(action.durationMs)
                is StationPlayerAction.CancelSleepTimer -> sleepTimer.cancel()
            }
        }
    }

    fun sendAction(action: StationPlayerAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun playOrPause() {
        // 받는 중에는 누를 재생 상태가 없다. 지금 들고 있는 이전 스테이션을 멈추게 되면 안 된다.
        if (stationPlaybackController.pendingStationId.value != null) return
        playerRepository.playOrPause()
    }

    /**
     * 기준은 받는 중인 스테이션이 먼저다. 재생 중인 것을 기준으로 하면 컨트롤러가 진행 중인 요청을 무시하는 동안
     * "다음" 연타가 같은 곳으로 가 버린다. 상태가 아니라 그 순간의 값을 읽는다 — 연타가 구독 갱신을 앞선다.
     */
    private suspend fun step(delta: Int) {
        val anchorId = stationPlaybackController.pendingStationId.value
            ?: playerRepository.nowPlayingStation.first()?.id
            ?: return
        val ring = stationRing(
            anchorId = anchorId,
            favorites = getFavoriteStationsUseCase().first(),
            all = getStationsUseCase(StationKind.RADIO).first(),
        )
        ring.neighbor(anchorId, delta)?.let { stationPlaybackController.play(it.id) }
    }

    /** 토글을 직렬화한다. 이전 값을 읽고 쓰는 사이에 두 번째 토글이 끼면 한 번이 사라진다. */
    private val favoriteMutex = Mutex()

    private fun toggleFavorite() {
        val station = (state.value as? StationPlayerState.Active)?.station ?: return
        viewModelScope.launch {
            favoriteMutex.withLock { toggleFavoriteStationUseCase(station.id) }
        }
    }
}

sealed interface StationPlayerState {
    data object Loading : StationPlayerState

    /** 받는 중인 것도 재생 중인 것도 없다. 시트가 스스로 닫힌다. */
    data object Hidden : StationPlayerState

    data class Active(
        /** 받는 중이면 그 스테이션, 아니면 재생 중인 스테이션. */
        val station: Station,
        val isPending: Boolean,
        /** 받는 중에는 null. 이전 스테이션의 곡명을 새 스테이션 밑에 보이지 않는다. */
        val streamTitle: String?,
        val isPlaying: Boolean,
        val isBuffering: Boolean,
        val isFavorite: Boolean,
        val previous: Station?,
        val next: Station?,
        val sleepTimerRemainingMs: Long?,
    ) : StationPlayerState
}

sealed interface StationPlayerAction {
    data object PlayOrPause : StationPlayerAction

    /** 받는 중인 요청까지 취소하고 멈춘다. `stopStation` 이 아니라 컨트롤러의 stop 이다. */
    data object Stop : StationPlayerAction
    data object Previous : StationPlayerAction
    data object Next : StationPlayerAction
    data object ToggleFavorite : StationPlayerAction
    data class SetSleepTimer(val durationMs: Long) : StationPlayerAction
    data object CancelSleepTimer : StationPlayerAction
}
