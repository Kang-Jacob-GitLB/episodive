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
import io.jacob.episodive.core.domain.usecase.station.ObserveStationProgramImagesUseCase
import io.jacob.episodive.core.model.DataError
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.asDataError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val HomeRadioMaxStations = 10
private const val HomeRadioMinStations = 8

/**
 * 홈의 라디오 섹션. [HomeViewModel] 의 state combine 에 넣지 않는다 — 한국 카탈로그는 늘 내용이 있어, 섞으면
 * 오프라인 오류 화면 판정이 영영 켜지지 않는다.
 *
 * 재생 요청의 직렬화는 [StationPlaybackController] 가 앱 전역에서 한다 — 여기서는 위임만 한다.
 */
@HiltViewModel
class HomeRadioViewModel @Inject constructor(
    getStationsUseCase: GetStationsUseCase,
    getFavoriteStationsUseCase: GetFavoriteStationsUseCase,
    getRecentStationsUseCase: GetRecentStationsUseCase,
    observeStationProgramImagesUseCase: ObserveStationProgramImagesUseCase,
    private val stationPlaybackController: StationPlaybackController,
    @param:Player(EpisodivePlayers.Main) playerRepository: PlayerRepository,
) : ViewModel() {

    /**
     * 채우기용 전체 목록. 해외 목록을 받지 못해도(오프라인 등) 섹션이 오류가 되지 않게 빈 목록으로 돌려, 즐겨찾기·최근만으로
     * 그린다. 한국 카탈로그는 메모리라 실패하지 않는다.
     */
    private fun catalogStations(getStationsUseCase: GetStationsUseCase): Flow<List<Station>> =
        getStationsUseCase(StationKind.RADIO)
            .map { it.stations }
            .catch { e ->
                Timber.w(e, "홈 라디오 전체 목록을 불러오지 못했다")
                emit(emptyList())
            }

    val state: StateFlow<HomeRadioState> = combine(
        getFavoriteStationsUseCase(),
        getRecentStationsUseCase(HomeRadioMaxStations),
        catalogStations(getStationsUseCase),
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

    /**
     * 섹션에 보이는 스테이션의 프로그램 이미지(id → 주소). `state` 와 따로 둔다 — 네트워크로 받는 값이라 섹션 자체가 늦게
     * 뜨면 안 된다. 섹션 구성이 바뀔 때만 구독 대상을 바꾼다.
     */
    val programImages: StateFlow<Map<String, String>> = state
        .map { (it as? HomeRadioState.Success)?.stations?.mapTo(LinkedHashSet()) { station -> station.id }.orEmpty() }
        // 순서·재생 상태가 바뀌어도 구성이 같으면 구독을 다시 걸지 않는다(집합으로 비교).
        .distinctUntilChanged()
        .flatMapLatest { ids -> observeStationProgramImagesUseCase(ids.toList()) }
        .catch { e ->
            Timber.w(e, "홈 라디오 프로그램 이미지를 불러오지 못했다")
            emit(emptyMap())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
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
