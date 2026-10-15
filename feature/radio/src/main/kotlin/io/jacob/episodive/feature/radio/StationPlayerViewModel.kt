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
import io.jacob.episodive.core.domain.usecase.caption.CaptionToggleResult
import io.jacob.episodive.core.domain.usecase.caption.ObserveCaptionDownloadFailuresUseCase
import io.jacob.episodive.core.domain.usecase.caption.ObserveStationCaptionUseCase
import io.jacob.episodive.core.domain.usecase.caption.StationCaption
import io.jacob.episodive.core.domain.usecase.caption.ToggleCaptionUseCase
import io.jacob.episodive.core.domain.usecase.station.GetFavoriteStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetRecentStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationDetailsUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationUseCase
import io.jacob.episodive.core.domain.usecase.station.GetStationsUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveOnAirProgramUseCase
import io.jacob.episodive.core.domain.usecase.station.ObserveScheduleUseCase
import io.jacob.episodive.core.domain.usecase.station.ToggleFavoriteStationUseCase
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationSchedule
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.LiveCaptionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import timber.log.Timber

private const val RecentStationLimit = 10

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
    private val getStationUseCase: GetStationUseCase,
    private val getRecentStationsUseCase: GetRecentStationsUseCase,
    private val toggleFavoriteStationUseCase: ToggleFavoriteStationUseCase,
    private val getStationDetailsUseCase: GetStationDetailsUseCase,
    private val observeOnAirProgramUseCase: ObserveOnAirProgramUseCase,
    private val observeScheduleUseCase: ObserveScheduleUseCase,
    private val sleepTimer: SleepTimer,
    observeStationCaptionUseCase: ObserveStationCaptionUseCase,
    private val toggleCaptionUseCase: ToggleCaptionUseCase,
    observeCaptionDownloadFailuresUseCase: ObserveCaptionDownloadFailuresUseCase,
) : ViewModel() {

    /**
     * 현재 국가의 전체 목록. 해외 목록은 네트워크라 늦게 오거나 실패할 수 있다 — 구독하자마자 빈 목록을 흘려
     * 즐겨찾기·재생 상태로 시트가 먼저 그려지게 하고, 실패는 빈 목록으로 둔다(지금 듣는 스테이션은 플레이어가
     * 들고 있고 즐겨찾기·최근으로도 찾는다). 공유해 두어 [step] 이 새로 구독해(해외면 요청을 다시 돌려) 기다리지
     * 않고 지금 값을 읽는다.
     */
    private val allStations: StateFlow<List<Station>> = getStationsUseCase(StationKind.RADIO)
        .map { it.stations }
        .catch { e ->
            Timber.w(e, "스테이션 전체 목록을 불러오지 못했다")
            emit(emptyList())
        }
        .onStart { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    /**
     * 받는 중인 스테이션을 저장소에서 찾은 결과. 목록 사본으로는 못 찾는 경우(다른 나라의 최근, 순위 변동으로 빠진 것)를
     * 위한 것이라 목록에서 바로 찾을 수 있으면 쓰이지 않는다. 구독하자마자 null 을 흘려 조회를 기다리지 않고 시작한다.
     * 어느 id 의 결과인지 함께 싣는다 — 조회가 끝나기 전에 받는 스테이션이 바뀌면 낡은 결과를 쓰지 않게 한다.
     */
    private val pendingLookup: Flow<PendingLookup?> = stationPlaybackController.pendingStationId
        .mapLatest { id -> id?.let { PendingLookup(it, getStationUseCase(it)) } }
        .onStart { emit(null) }

    val state: StateFlow<StationPlayerState> = combine(
        playerRepository.nowPlayingStation,
        stationPlaybackController.pendingStationId,
        pendingLookup,
        playerRepository.streamTitle,
        playerRepository.isPlaying,
        playerRepository.isBuffering,
        getFavoriteStationsUseCase(),
        allStations,
        getRecentStationsUseCase(RecentStationLimit),
        sleepTimer.remainingMs,
    ) { playing, pendingId, lookup, streamTitle, isPlaying, isBuffering, favorites, stations, recents, sleepTimerRemainingMs ->
        // 받는 중인 스테이션을 먼저 보여 준다. 그동안 플레이어는 이전 스테이션을 그대로 들고 있다.
        // 목록에서 바로 찾고, 없으면 저장소 조회 결과를 쓴다.
        val pending = pendingId?.let { id ->
            stations.firstOrNull { it.id == id }
                ?: favorites.firstOrNull { it.id == id }
                ?: recents.firstOrNull { it.id == id }
                ?: lookup?.takeIf { it.id == id }?.station
        }
        val station = pending ?: playing
        when {
            pendingId == null && playing == null -> StationPlayerState.Hidden
            // 받는 중인 스테이션을 목록에서 아직 못 찾았고 들고 있는 것도 없다. 사라진 것이 아니라 준비 중이다.
            station == null -> StationPlayerState.Loading
            else -> {
                val ring = stationRing(station.id, favorites, stations, recents)
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
        // replay 를 바로 비운다. 구독이 끊긴 사이 낡은 Hidden 이 남아 있으면 시트를 다시 열자마자 그 값을 읽고
        // 닫아 버린다 — 새 값은 Room 왕복 뒤에야 온다. 끊긴 동안은 Loading 으로 돌아가 새 값을 기다린다.
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000, replayExpirationMillis = 0),
        initialValue = StationPlayerState.Loading,
    )

    /**
     * 표시 중인 스테이션의 부가 정보. `state` 와 따로 둔다 — 네트워크로 받는 값이라 `Active` 에 넣으면 재생 상태가
     * 바뀔 때마다 얽히고, 늦게 와도 시트를 막지 않아야 한다.
     *
     * 값에 스테이션 id 를 싣는다. 스테이션이 바뀌어도 값을 비우지 않고(비우면 시트 안 정보 영역이 사라졌다
     * 돌아와 스크롤이 튄다) 화면이 id 로 자기 차례인지 가른다 — 늦게 오는 이전 스테이션 상세는 그려지지 않는다.
     * transformLatest 가 이전 요청도 취소한다.
     */
    val details: StateFlow<StationDetailsOf?> = state
        .map { (it as? StationPlayerState.Active)?.station?.id }
        .distinctUntilChanged()
        .transformLatest { stationId ->
            if (stationId != null) emit(StationDetailsOf(stationId, getStationDetailsUseCase(stationId)))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    /**
     * 표시 중인 스테이션이 지금 방송 중인 프로그램. `state` 와 따로 둔다 — 네트워크로 받고 프로그램 경계마다
     * 바뀌는 값이라 `Active` 에 넣으면 재생 상태와 얽힌다. [details] 와 같은 이유로 값에 스테이션 id 를 싣고
     * 화면이 id 로 자기 차례인지 가른다. flatMapLatest 가 이전 스테이션의 구독을 끊는다.
     */
    val onAir: StateFlow<OnAirOf?> = state
        .map { (it as? StationPlayerState.Active)?.station?.id }
        .distinctUntilChanged()
        .flatMapLatest { stationId ->
            if (stationId == null) {
                emptyFlow()
            } else {
                observeOnAirProgramUseCase(stationId).map { OnAirOf(stationId, it) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    /**
     * 표시 중인 스테이션의 지금 기준 편성표(오늘 전체 + 내일 새벽까지). [onAir] 와 같은 이유로 `state` 와 따로 두고 id 를 싣는다. 편성표는 [onAir] 와
     * 같은 캐시에서 나오므로 지금 방송 행은 화면이 [onAir] 의 현재 항목과 맞춰 가린다 — 경계 시각에 둘이 어긋나지 않는다.
     */
    val schedule: StateFlow<ScheduleOf?> = state
        .map { (it as? StationPlayerState.Active)?.station?.id }
        .distinctUntilChanged()
        .flatMapLatest { stationId ->
            if (stationId == null) {
                emptyFlow()
            } else {
                observeScheduleUseCase(stationId).map { ScheduleOf(stationId, it) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    /**
     * 자막 상태(스테이션 id 가 붙는다). `state` 에 끼우지 않는다 — 시트가 이 StateFlow 를 수집하는 동안만 인식이 돈다. 시트가 닫히면
     * `WhileSubscribed(5s)` 뒤 세션이 끝난다. 토글은 에피소드와 공유하는 `isCaptionEnabled` 하나다.
     */
    val caption: StateFlow<StationCaption> = observeStationCaptionUseCase()
        // catch 로 끝내면 토글이 죽는다(저장값은 켜짐인데 화면은 꺼짐으로 굳는다). 짧은 백오프로 재구독하며,
        // stateIn 이 재시도 대기 중에도 마지막 값을 들고 있다.
        .retryWhen { cause, attempt ->
            Timber.w(cause, "스테이션 자막 스트림이 끊겼다 - 재구독 시도 ${attempt + 1}")
            val backoffMs = (1_000L shl attempt.toInt().coerceIn(0, 5)).coerceAtMost(30_000L)
            delay(backoffMs)
            true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StationCaption(stationId = null, state = LiveCaptionState.Initial),
        )

    private val _action = MutableSharedFlow<StationPlayerAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<StationPlayerEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
        // 모델 다운로드·로드 실패는 caption 에 담기지 않는 일회성 이벤트다.
        viewModelScope.launch {
            observeCaptionDownloadFailuresUseCase()
                .catch { Timber.w(it, "자막 다운로드 실패 스트림이 끊겼다") }
                .collect { failure -> _effect.emit(StationPlayerEffect.CaptionDownloadFailed(failure.reason)) }
        }
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
                is StationPlayerAction.ToggleCaption -> toggleCaption()
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
            all = allStations.value,
            recents = getRecentStationsUseCase(RecentStationLimit).first(),
        )
        ring.neighbor(anchorId, delta)?.let { stationPlaybackController.play(it.id) }
    }

    // 받는 중이거나 caption 이 아직 이전 스테이션의 값이면 토글 판정이 어긋난다. 무시한다.
    private fun toggleCaption() {
        val active = state.value as? StationPlayerState.Active ?: return
        val current = caption.value
        if (active.isPending || current.stationId != active.station.id) return
        viewModelScope.launch {
            when (val result = toggleCaptionUseCase(current.state)) {
                is CaptionToggleResult.DownloadStarted ->
                    _effect.emit(StationPlayerEffect.CaptionDownloadStarted(result.sizeBytes))

                CaptionToggleResult.UnsupportedLanguage -> _effect.emit(StationPlayerEffect.CaptionUnsupported)

                CaptionToggleResult.Enabled,
                CaptionToggleResult.Disabled,
                CaptionToggleResult.DownloadCancelled -> {}
            }
        }
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

private class PendingLookup(val id: String, val station: Station?)

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

/** [details] 가 어느 스테이션의 것인지 함께 싣는다. null 은 받지 못했거나 쓸 만한 값이 없다는 뜻이다. */
data class StationDetailsOf(
    val stationId: String,
    val details: StationDetails?,
)

/** [program] 이 어느 스테이션의 것인지 함께 싣는다. null 은 프로그램 정보를 모르거나 받지 못했다는 뜻이다. */
data class OnAirOf(
    val stationId: String,
    val program: OnAirProgram?,
)

/** [schedule] 이 어느 스테이션의 것인지 함께 싣는다. 목록이 비어 있으면 편성표를 모르거나 받지 못했다는 뜻이다. */
data class ScheduleOf(
    val stationId: String,
    val schedule: StationSchedule,
)

sealed interface StationPlayerAction {
    data object PlayOrPause : StationPlayerAction

    /** 받는 중인 요청까지 취소하고 멈춘다. `stopStation` 이 아니라 컨트롤러의 stop 이다. */
    data object Stop : StationPlayerAction
    data object Previous : StationPlayerAction
    data object Next : StationPlayerAction
    data object ToggleFavorite : StationPlayerAction
    data class SetSleepTimer(val durationMs: Long) : StationPlayerAction
    data object ToggleCaption : StationPlayerAction
    data object CancelSleepTimer : StationPlayerAction
}

/** 시트의 스낵바로 알릴 일회성 이벤트. */
sealed interface StationPlayerEffect {
    data class CaptionDownloadStarted(val sizeBytes: Long) : StationPlayerEffect
    data object CaptionUnsupported : StationPlayerEffect
    data class CaptionDownloadFailed(val reason: CaptionDownloadFailure.Reason) : StationPlayerEffect
}
