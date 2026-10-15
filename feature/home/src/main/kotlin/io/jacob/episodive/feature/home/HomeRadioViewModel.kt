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
import io.jacob.episodive.core.model.StationListing
import io.jacob.episodive.core.model.StationListingSource
import io.jacob.episodive.core.model.asDataError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val HomeRadioMaxStations = 10
private const val HomeRadioMinStations = 8
private const val RetryBaseMillis = 1_000L
private const val RetryMaxMillis = 30_000L
private const val PendingGraceMillis = 200L

/** 목록을 기다리는 동안. 출처를 아직 모르므로 입구를 강제하지 않는다. */
private val PendingListing = StationListing("", StationListingSource.Curated, emptyList())

/** 해외 목록을 받지 못했을 때. 카탈로그는 실패하지 않으므로 해외다. */
private val FailedListing = StationListing("", StationListingSource.RadioBrowser, emptyList())

/** 1·2·4·8·16초, 이후 30초. */
private fun retryDelayMillis(attempt: Long): Long =
    (RetryBaseMillis shl attempt.coerceAtMost(5L).toInt()).coerceAtMost(RetryMaxMillis)

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
     * 채우기용 전체 목록. 해외 목록은 네트워크라 늦게 오거나 실패할 수 있어, 즐겨찾기·최근이 기다리지 않도록 구독하자마자
     * 빈 목록을 흘려 먼저 그리게 한다. 실패하면 빈 목록을 흘린 뒤 1·2·4…초(최대 30초) 간격으로 다시 구독해 복구한다 —
     * 유스케이스를 통째로 다시 구독하므로 그사이 바뀐 국가도 따라간다. 한국 카탈로그는 메모리라 실패하지 않는다.
     *
     * 출처가 해외인 채 비어 있으면([HomeRadioState.Success.isEntranceOnly]) 홈이 헤더와 "더 보기" 입구만 남긴다.
     * 입구를 닫으면 사용자가 라디오 화면에 들어갈 길이 없어진다.
     */
    private fun catalogListing(getStationsUseCase: GetStationsUseCase): Flow<StationListing> =
        getStationsUseCase(StationKind.RADIO)
            .retryWhen { cause, attempt ->
                if (cause is CancellationException) return@retryWhen false
                Timber.w(cause, "홈 라디오 전체 목록을 불러오지 못했다. attempt: $attempt")
                emit(FailedListing)
                delay(retryDelayMillis(attempt))
                true
            }
            .withPendingAfterGrace()

    /**
     * 첫 값이 [PendingGraceMillis] 안에 오지 않으면 [PendingListing] 을 먼저 흘려 즐겨찾기·최근이 기다리지 않게 한다.
     * 곧바로 흘리지 않는 것은 한국 카탈로그처럼 바로 오는 목록에서 "즐겨찾기만 → 채워짐" 두 번 그려 깜빡이는 것을
     * 피하기 위해서다.
     */
    private fun Flow<StationListing>.withPendingAfterGrace(): Flow<StationListing> {
        val upstream = this
        return channelFlow {
            val pending = launch {
                delay(PendingGraceMillis)
                send(PendingListing)
            }
            upstream.collect {
                pending.cancel()
                send(it)
            }
        }
    }

    val state: StateFlow<HomeRadioState> = combine(
        getFavoriteStationsUseCase(),
        getRecentStationsUseCase(HomeRadioMaxStations),
        catalogListing(getStationsUseCase),
        playerRepository.nowPlayingStation,
        playerRepository.isPlaying,
        playerRepository.isBuffering,
        stationPlaybackController.pendingStationId,
    ) { favorites, recents, listing, nowPlayingStation, isPlaying, isBuffering, pendingId ->
        val mine = (favorites + recents).distinctBy { it.id }.take(HomeRadioMaxStations)
        // 한두 번 들은 기록만으로는 섹션이 휑하다. 모자라면 카탈로그로 최소 개수까지 채운다.
        val taken = mine.mapTo(HashSet()) { it.id }
        val filler = listing.stations.filter { it.id !in taken }.take((HomeRadioMinStations - mine.size).coerceAtLeast(0))
        val stations = mine + filler
        HomeRadioState.Success(
            stations = stations,
            isEntranceOnly = stations.isEmpty() && listing.source == StationListingSource.RadioBrowser,
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
        /**
         * 보여 줄 스테이션이 없지만 해외 목록이라 섹션 헤더와 "더 보기" 입구는 남겨야 한다. 라디오 화면으로 가는 유일한
         * 길이다. 한국 카탈로그는 늘 채워져 있어 거짓이다.
         */
        val isEntranceOnly: Boolean = false,
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
