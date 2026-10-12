package io.jacob.episodive.feature.radio

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
import io.jacob.episodive.core.domain.usecase.station.ToggleFavoriteStationUseCase
import io.jacob.episodive.core.model.DataError
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.asDataError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

private const val RecentStationLimit = 10

@HiltViewModel
class RadioViewModel @Inject constructor(
    getStationsUseCase: GetStationsUseCase,
    getFavoriteStationsUseCase: GetFavoriteStationsUseCase,
    getRecentStationsUseCase: GetRecentStationsUseCase,
    private val toggleFavoriteStationUseCase: ToggleFavoriteStationUseCase,
    private val stationPlaybackController: StationPlaybackController,
    @param:Player(EpisodivePlayers.Main) playerRepository: PlayerRepository,
) : ViewModel() {

    // 재시도는 소스 체인을 통째로 재구독해야 한다. 아래 체인은 전부 콜드 Flow 라 재구독해도 부작용이 없다.
    private val retryTrigger = MutableStateFlow(0)

    // 스테이션 여부는 nowPlayingStation 하나로만 가른다. nowPlaying(에피소드)과 섞지 않는다.
    val state: StateFlow<RadioState> = retryTrigger.flatMapLatest {
        combine(
            getStationsUseCase(StationKind.RADIO),
            getFavoriteStationsUseCase(),
            getRecentStationsUseCase(RecentStationLimit),
            playerRepository.nowPlayingStation,
            playerRepository.isPlaying,
            playerRepository.isBuffering,
            stationPlaybackController.pendingStationId,
        ) { stations, favorites, recents, nowPlayingStation, isPlaying, isBuffering, pendingId ->
            RadioState.Success(
                favorites = favorites,
                recents = recents,
                nationwide = stations.filter { it.region == null },
                regional = stations.filter { it.region != null },
                favoriteIds = favorites.mapTo(HashSet()) { it.id },
                nowPlayingStationId = nowPlayingStation?.id,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                pendingStationId = pendingId,
            )
        }.catch<RadioState> { e ->
            Timber.e(e, "라디오 목록을 불러오지 못했다")
            emit(RadioState.Error(e.asDataError()))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RadioState.Loading
    )

    private val _action = MutableSharedFlow<RadioAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<RadioEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
    }

    // collectLatest 로 받지 않는다. 즐겨찾기 토글 같은 무관한 액션이 오면 진행 중인 재생 요청이
    // 취소된다. 재생 요청의 취소는 clickStation 이 직접 한다.
    private fun handleActions() = viewModelScope.launch {
        _action.collect { action ->
            when (action) {
                is RadioAction.ClickBack -> clickBack()
                is RadioAction.ClickStation -> clickStation(action.station)
                is RadioAction.ToggleFavorite -> toggleFavorite(action.stationId)
                is RadioAction.Retry -> retry()
            }
        }
    }

    fun sendAction(action: RadioAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun clickBack() = viewModelScope.launch {
        _effect.emit(RadioEffect.NavigateBack)
    }

    private fun clickStation(station: Station) {
        // 연타 직렬화·같은 스테이션 토글·실패 안내는 전부 컨트롤러가 한다.
        stationPlaybackController.play(station.id)
    }

    /**
     * 토글과 이펙트 발행을 한 덩어리로 직렬화한다. 안 그러면 첫 토글이 느릴 때 두 번째의 이펙트가 먼저
     * 도착해, 실제 상태와 반대로 말하는 스낵바가 남는다(되돌리기가 엉뚱한 방향으로 토글한다).
     */
    private val favoriteMutex = Mutex()

    private fun toggleFavorite(stationId: String) = viewModelScope.launch {
        favoriteMutex.withLock {
            val isFavoriteNow = toggleFavoriteStationUseCase(stationId)
            _effect.emit(RadioEffect.ShowFavoriteSnackbar(stationId, isFavoriteNow))
        }
    }

    private fun retry() {
        retryTrigger.update { it + 1 }
    }
}

sealed interface RadioState {
    data object Loading : RadioState
    data class Success(
        val favorites: List<Station>,
        val recents: List<Station>,
        /** 방송사 전국 방송(지역 없음). */
        val nationwide: List<Station>,
        /** 지역·공동체 방송. */
        val regional: List<Station>,
        /** [favorites] 에서 파생한다. 행마다 즐겨찾기 여부를 따로 구독하지 않는다. */
        val favoriteIds: Set<String>,
        val nowPlayingStationId: String?,
        val isPlaying: Boolean,
        val isBuffering: Boolean,
        val pendingStationId: String?,
    ) : RadioState

    data class Error(val error: DataError) : RadioState
}

sealed interface RadioAction {
    data object ClickBack : RadioAction
    data class ClickStation(val station: Station) : RadioAction
    data class ToggleFavorite(val stationId: String) : RadioAction
    data object Retry : RadioAction
}

sealed interface RadioEffect {
    data object NavigateBack : RadioEffect
    data class ShowFavoriteSnackbar(val stationId: String, val isFavorite: Boolean) : RadioEffect
}
