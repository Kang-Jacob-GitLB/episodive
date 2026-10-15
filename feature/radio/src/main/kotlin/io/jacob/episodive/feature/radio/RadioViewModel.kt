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
import io.jacob.episodive.core.domain.usecase.station.ObserveStationProgramImagesUseCase
import io.jacob.episodive.core.domain.usecase.station.ToggleFavoriteStationUseCase
import io.jacob.episodive.core.model.DataError
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationListingSource
import io.jacob.episodive.core.model.asDataError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    observeStationProgramImagesUseCase: ObserveStationProgramImagesUseCase,
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
        ) { listing, favorites, recents, nowPlayingStation, isPlaying, isBuffering, pendingId ->
            // 큐레이션(한국)은 전국/지역으로 가르고, 해외는 인기 목록 하나다. 해외 스테이션은 region 이 주(州)라
            // 전국/지역 구분의 근거가 아니다.
            val curated = listing.source == StationListingSource.Curated
            RadioState.Success(
                favorites = favorites,
                recents = recents,
                nationwide = if (curated) listing.stations.filter { it.region == null } else emptyList(),
                regional = if (curated) listing.stations.filter { it.region != null } else emptyList(),
                popular = if (curated) emptyList() else listing.stations,
                listingSource = listing.source,
                countryCode = listing.countryCode,
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

    /**
     * 스테이션 id → 지금 방송 중인 프로그램 이미지. `state` 와 따로 둔다 — 네트워크로 받고 프로그램 경계마다 바뀌는 값이라
     * 섞으면 재생 상태·목록과 얽혀 매번 목록 전체가 다시 만들어진다. 화면이 구독하는 동안(WhileSubscribed)만 받는다.
     * 구독 대상은 전체 목록의 라디오다(해외 목록은 편성 정보가 없어 곧바로 null 이다). 즐겨찾기·최근은 그 부분집합이다.
     */
    val programImages: StateFlow<Map<String, String>> = getStationsUseCase(StationKind.RADIO)
        .map { listing -> listing.stations.mapTo(LinkedHashSet()) { it.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids -> observeStationProgramImagesUseCase(ids.toList()) }
        .catch { e ->
            // 부가 정보라 실패해도 목록은 이니셜 아트로 그대로 보인다.
            Timber.w(e, "프로그램 이미지를 불러오지 못했다")
            emit(emptyMap())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
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
        /** 해외 국가의 인기 방송. 큐레이션(한국)이면 비어 있고 [nationwide]·[regional] 이 채워진다. */
        val popular: List<Station> = emptyList(),
        val listingSource: StationListingSource = StationListingSource.Curated,
        /** 전체 목록의 기준 국가(ISO alpha-2). */
        val countryCode: String = "KR",
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
