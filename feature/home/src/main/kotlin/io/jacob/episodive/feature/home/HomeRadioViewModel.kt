package io.jacob.episodive.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.common.combine
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.station.StationPlaybackController
import io.jacob.episodive.core.domain.usecase.station.GetFavoriteStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetRecentStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationsUseCase
import io.jacob.episodive.core.model.DataError
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.asDataError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val HomeRadioMaxStations = 10
private const val HomeRadioMinStations = 8

/**
 * 홈의 라디오 섹션. [HomeViewModel] 의 state combine 에 넣지 않는다 — 카탈로그라 늘 내용이 있어, 섞으면
 * 오프라인 오류 화면 판정이 영영 켜지지 않는다.
 *
 * 재생 요청의 직렬화는 [StationPlaybackController] 가 앱 전역에서 한다 — 여기서는 위임만 한다.
 */
@HiltViewModel
class HomeRadioViewModel @Inject constructor(
    getStationsUseCase: GetStationsUseCase,
    getFavoriteStationsUseCase: GetFavoriteStationsUseCase,
    getRecentStationsUseCase: GetRecentStationsUseCase,
    private val stationPlaybackController: StationPlaybackController,
    @param:Player(EpisodivePlayers.Main) playerRepository: PlayerRepository,
) : ViewModel() {

    val state: StateFlow<HomeRadioState> = combine(
        getFavoriteStationsUseCase(),
        getRecentStationsUseCase(HomeRadioMaxStations),
        getStationsUseCase(StationKind.RADIO),
        playerRepository.nowPlayingStation,
        playerRepository.isPlaying,
        playerRepository.isBuffering,
        stationPlaybackController.pendingStationId,
    ) { favorites, recents, catalog, nowPlayingStation, isPlaying, isBuffering, pendingId ->
        val mine = (favorites + recents).distinctBy { it.id }.take(HomeRadioMaxStations)
        // 한두 번 들은 기록만으로는 섹션이 휑하다. 모자라면 카탈로그로 최소 개수까지 채운다.
        val taken = mine.mapTo(HashSet()) { it.id }
        val filler = catalog.filter { it.id !in taken }.take((HomeRadioMinStations - mine.size).coerceAtLeast(0))
        HomeRadioState.Success(
            stations = mine + filler,
            nowPlayingStationId = nowPlayingStation?.id,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            pendingStationId = pendingId,
        )
    }.catch<HomeRadioState> { e ->
        Timber.e(e, "홈 라디오 섹션을 불러오지 못했다")
        emit(HomeRadioState.Error(e.asDataError()))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeRadioState.Loading,
    )

    private val _action = MutableSharedFlow<HomeRadioAction>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            _action.collect { action ->
                when (action) {
                    is HomeRadioAction.ClickStation -> stationPlaybackController.play(action.station.id)
                }
            }
        }
    }

    fun sendAction(action: HomeRadioAction) = viewModelScope.launch {
        _action.emit(action)
    }
}

sealed interface HomeRadioState {
    data object Loading : HomeRadioState
    data class Success(
        val stations: List<Station>,
        val nowPlayingStationId: String?,
        val isPlaying: Boolean,
        val isBuffering: Boolean,
        val pendingStationId: String?,
    ) : HomeRadioState

    data class Error(val error: DataError) : HomeRadioState
}

sealed interface HomeRadioAction {
    data class ClickStation(val station: Station) : HomeRadioAction
}
