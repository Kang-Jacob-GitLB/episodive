package io.jacob.episodive.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.common.TimeProvider
import io.jacob.episodive.core.common.combine as combineTyped
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.caption.CaptionToggleResult
import io.jacob.episodive.core.domain.usecase.caption.ObserveCaptionDownloadFailuresUseCase
import io.jacob.episodive.core.domain.usecase.caption.ObserveLiveCaptionUseCase
import io.jacob.episodive.core.domain.usecase.caption.ToggleCaptionUseCase
import io.jacob.episodive.core.domain.usecase.episode.FetchEpisodeByIdUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetChaptersUseCase
import io.jacob.episodive.core.domain.usecase.episode.GetEpisodeByIdUseCase
import io.jacob.episodive.core.domain.usecase.episode.RefreshEpisodeDescriptionUseCase
import io.jacob.episodive.core.domain.usecase.episode.SaveEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.episode.UpdatePlayedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.GetNowPlayingUseCase
import io.jacob.episodive.core.domain.usecase.player.GetPlaylistUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.RestoreLastPlayStateUseCase
import io.jacob.episodive.core.domain.usecase.player.SaveLastPlayStateUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.domain.usecase.user.GetUserDataUseCase
import io.jacob.episodive.core.domain.usecase.user.SetSpeedUseCase
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.LiveCaptionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val toggleLikedEpisodeUseCase: ToggleLikedEpisodeUseCase,
    private val saveEpisodeUseCase: SaveEpisodeUseCase,
    private val updatePlayedEpisodeUseCase: UpdatePlayedEpisodeUseCase,
    private val refreshEpisodeDescriptionUseCase: RefreshEpisodeDescriptionUseCase,
    getPodcastUseCase: GetPodcastUseCase,
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    getNowPlayingUseCase: GetNowPlayingUseCase,
    getPlaylistUseCase: GetPlaylistUseCase,
    private val setSpeedUseCase: SetSpeedUseCase,
    getUserDataUseCase: GetUserDataUseCase,
    getChaptersUseCase: GetChaptersUseCase,
    private val toggleFollowedUseCase: ToggleFollowedUseCase,
    private val saveLastPlayStateUseCase: SaveLastPlayStateUseCase,
    private val restoreLastPlayStateUseCase: RestoreLastPlayStateUseCase,
    private val getEpisodeByIdUseCase: GetEpisodeByIdUseCase,
    private val fetchEpisodeByIdUseCase: FetchEpisodeByIdUseCase,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
    private val timeProvider: TimeProvider,
    observeLiveCaptionUseCase: ObserveLiveCaptionUseCase,
    private val toggleCaptionUseCase: ToggleCaptionUseCase,
    observeCaptionDownloadFailuresUseCase: ObserveCaptionDownloadFailuresUseCase,
) : ViewModel() {
    private val nowPlaying = getNowPlayingUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val podcast = nowPlaying
        .mapNotNull { it?.feedId }
        .distinctUntilChanged()
        .flatMapLatest { feedId -> getPodcastUseCase(feedId) }

    private val chapters = nowPlaying
        .map { it?.chaptersUrl }
        .distinctUntilChanged()
        .flatMapLatest { chaptersUrl ->
            val chapters = chaptersUrl?.let { getChaptersUseCase(it) } ?: emptyList()
            flowOf(chapters)
        }


    /**
     * 마지막 재생 복원 작업. 딥링크가 이것을 멈출 수 있어야 한다 — [openDeepLink] 참고.
     */
    private var restoreLastPlayJob: Job? = null

    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)

    val state: StateFlow<PlayerState> = combineTyped(
        podcast,
        nowPlaying,
        getPlaylistUseCase(),
        playerRepository.indexOfList,
        playerRepository.progress,
        playerRepository.isPlaying,
        playerRepository.speed,
        chapters,
        playerRepository.cue,
        _sleepTimerRemainingMs,
    ) { podcast, nowPlaying, playlist, indexOfList, progress, isPlaying, speed, chapters, cue, sleepTimerRemainingMs ->
        if (podcast != null && nowPlaying != null) {
            PlayerState.Success(
                podcast = podcast,
                nowPlaying = nowPlaying,
                playlist = playlist,
                indexOfList = indexOfList,
                progress = progress,
                isPlaying = isPlaying,
                speed = speed,
                chapters = chapters,
                cue = cue,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
            ) as PlayerState
        } else {
            PlayerState.Error("podcast or nowPlaying is null")
        }
    }.catch { e ->
        emit(PlayerState.Error(e.message ?: "Unknown error"))
        e.printStackTrace()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerState.Loading
    )

    // state(10-arity combine)에 끼우지 않는다 — PlayerBar 는 state 를 늘 수집하는데(미니바만
    // 보여도) 여기에 자막을 섞으면 시트가 닫혀 있어도 인식이 계속 돈다. PlayerBottomSheet 만
    // 이 StateFlow 를 수집한다.
    val caption: StateFlow<LiveCaptionState> = observeLiveCaptionUseCase()
        // catch 로 업스트림을 끝내면 토글이 죽는다: 저장값은 켜짐인데 화면은 Initial(꺼짐)로
        // 굳고, 탭해도 이미 true 인 setCaptionEnabled(true) 라 아무 일도 안 일어난다. 시트를
        // 닫았다 열어 재구독될 때까지(WhileSubscribed 5s) 이 상태가 이어진다. 대신 짧은
        // 백오프로 재구독한다 — stateIn 이 재시도 대기 중에도 마지막 값을 들고 있어 화면은
        // 끊기지 않는다.
        .retryWhen { cause, attempt ->
            Timber.w(cause, "자막 스트림이 끊겼다 - 재구독 시도 ${attempt + 1}")
            val backoffMs = (1_000L shl attempt.toInt().coerceIn(0, 5)).coerceAtMost(30_000L)
            delay(backoffMs)
            true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LiveCaptionState.Initial,
        )

    private val _action = MutableSharedFlow<PlayerAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<PlayerEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
        // 저장 키를 progress 자신이 들고 있는 episodeId 에서만 가져온다.
        // DB 파생 nowPlaying 과 combine 하면 에피소드 전환 순간 두 스트림의 지연 차 때문에
        // "이전 에피소드 + 새 위치" 쌍이 저장되어 이전 에피소드의 이어듣기 지점이 오염된다.
        // collectLatest 가 아니라 collect 인 이유: 전환 직전 tick 의 쓰기가 취소되면
        // 그 에피소드의 마지막 몇 초가 유실되어 이어듣기 지점이 뒤로 밀린다.
        // upsert 한 건짜리 저렴한 쓰기이므로 취소하지 않고 끝까지 보낸다.
        viewModelScope.launch {
            playerRepository.progress
                .distinctUntilChanged()
                .collect { progress ->
                    progress.episodeId?.let { episodeId ->
                        updatePlayedEpisodeUseCase(episodeId, progress)
                    }
                }
        }
        viewModelScope.launch {
            getUserDataUseCase()
                .mapNotNull { it.speed }
                .distinctUntilChanged()
                .first()
                .let { speed ->
                    playerRepository.setSpeed(speed)
                }
        }
        restoreLastPlayJob = viewModelScope.launch {
            val currentEpisode = playerRepository.nowPlaying.first()
            if (currentEpisode == null) {
                restoreLastPlayStateUseCase()
            }
        }
        viewModelScope.launch {
            var lastSavedTime = 0L
            var lastSavedEpisodeId: Long? = null
            var lastSavedPositionMs = 0L
            // 저장 위치와 마찬가지로 세션 스냅샷의 에피소드도 progress 에서 가져온다.
            // 별도 nowPlaying 과 묶으면 콜드스타트 직후 "직전 에피소드 + 위치 0" 이 저장되어
            // DataStore 의 마지막 재생 지점까지 0 으로 덮인다.
            combine(
                playerRepository.indexOfList,
                playerRepository.progress,
                playerRepository.isShuffle,
                playerRepository.repeat,
            ) { index: Int, progress: Progress, shuffle: Boolean, repeat: Repeat ->
                progress.episodeId?.let { episodeId ->
                    LastPlaySnapshot(
                        episodeId = episodeId,
                        index = index,
                        positionMs = progress.position.inWholeMilliseconds,
                        shuffle = shuffle,
                        repeat = repeat,
                    )
                }
            }
                .filterNotNull()
                .collectLatest { snapshot ->
                    val now = timeProvider.currentTimeMillis()
                    // 에피소드가 바뀌면 5초 창을 기다리지 않는다. 기다리면 전환 직후 한 번 저장된 값이
                    // 창이 닫힌 동안 갱신되지 않아, 그 사이 앱이 죽으면 엉뚱한 지점부터 복원된다.
                    val episodeChanged = snapshot.episodeId != lastSavedEpisodeId
                    // 위치가 뒤로 갔다면 사용자가 되감은 것이다. 재생 중에는 위치가 늘기만 하므로
                    // 재생 중에는 위치가 늘기만 하므로 이 조건은 탐색이나 반복 재생에서만 성립한다.
                    // 창을 기다리다 그 사이 일시정지하면 progress
                    // 방출이 멈춰 되감기 이전 값이 스냅샷에 남고, 다음 실행에서 되살아난다.
                    val rewound = snapshot.positionMs < lastSavedPositionMs
                    if (episodeChanged || rewound || now - lastSavedTime >= 5_000) {
                        saveLastPlayStateUseCase(
                            episodeId = snapshot.episodeId,
                            index = snapshot.index,
                            positionMs = snapshot.positionMs,
                            shuffle = snapshot.shuffle,
                            repeat = snapshot.repeat,
                        )
                        lastSavedTime = now
                        lastSavedEpisodeId = snapshot.episodeId
                        lastSavedPositionMs = snapshot.positionMs
                    }
                }
        }
        // 재생 중인 에피소드가 바뀔 때마다 description 을 fulltext 로 보강한다. 목록/재생 시작
        // 시점엔 잘린 설명만 있어 상세에 전체 설명을 보여주려면 이 시점에 다시 받아야 한다.
        // State 파이프라인(위 combine)에 끼워 넣지 않고 별도 launch 로 둔다 — 부작용이라 느려도
        // 재생 흐름(State 방출)을 막으면 안 된다.
        //
        // refreshEpisodeDescriptionUseCase 는 EpisodeRepositoryImpl 이 예외를 삼키는 데 기대고
        // 있지만, 여기서는 구현체가 아니라 EpisodeRepository 인터페이스에 의존하므로 그 규율이
        // 구현체 한 곳에만 걸려 있다. onEach 안에서 뭔가 던지면 collect 가 죽어 VM 수명 내내
        // 보강이 영구 중단되므로 이 스트림에도 에러 경계를 둔다. catch 는 collect{} 람다 안의
        // 예외는 못 잡고 업스트림(과 onEach) 예외만 잡으므로, 실제 작업은 onEach 에서 하고
        // collect 는 빈 람다로 스트림을 구동만 시킨다. 취소는 catch 가 재전파하므로 별도
        // 처리가 필요 없다.
        viewModelScope.launch {
            nowPlaying.mapNotNull { it?.id }
                .distinctUntilChanged()
                .onEach { refreshEpisodeDescriptionUseCase(it) }
                .catch { Timber.w(it, "설명 보강 스트림이 끊겼다") }
                .collect { }
        }
        // 자막 모델 다운로드/로드 실패는 caption StateFlow 에 담기지 않는 일회성 이벤트라
        // 여기서 직접 수집해 Effect 로 흘려보낸다.
        viewModelScope.launch {
            observeCaptionDownloadFailuresUseCase()
                .catch { Timber.w(it, "자막 다운로드 실패 스트림이 끊겼다") }
                .collect { failure ->
                    _effect.emit(PlayerEffect.CaptionDownloadFailed(failure.reason))
                }
        }
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collect { action ->
            when (action) {
                is PlayerAction.PlayOrPause -> playOrPause()
                is PlayerAction.Next -> next()
                is PlayerAction.Previous -> previous()
                is PlayerAction.Shuffle -> shuffle()
                is PlayerAction.Repeat -> repeat()
                is PlayerAction.PlayIndex -> playIndex(action.index)
                is PlayerAction.SeekTo -> seekTo(action.position)
                is PlayerAction.SeekBackward -> seekBackward()
                is PlayerAction.SeekForward -> seekForward()
                is PlayerAction.Speed -> speed(action.speed)
                is PlayerAction.ClickPodcast -> clickPodcast(action.podcast)
                is PlayerAction.ClickEpisode -> clickEpisode(action.episode)
                is PlayerAction.ToggleLike -> toggleCurrentLikedEpisode()
                is PlayerAction.ToggleLikedEpisode -> toggleLikedEpisode(action.episode)
                is PlayerAction.ToggleSave -> toggleCurrentSavedEpisode()
                is PlayerAction.ToggleSavedEpisode -> toggleSavedEpisode(action.episode)
                is PlayerAction.ToggleFollowedPodcast -> toggleFollowedPodcast(action.podcast)
                is PlayerAction.ExpandPlayer -> expandPlayer()
                is PlayerAction.CollapsePlayer -> collapsePlayer()
                is PlayerAction.SetSleepTimer -> startSleepTimer(action.durationMs)
                is PlayerAction.CancelSleepTimer -> cancelSleepTimer()
                is PlayerAction.SleepTimerEndOfEpisode -> startEndOfEpisodeTimer()
                is PlayerAction.OpenDeepLink ->
                    openDeepLink(action.episodeId, action.startPositionMs)
                is PlayerAction.ToggleCaption -> toggleCaption()
            }
        }
    }

    fun sendAction(action: PlayerAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun playOrPause() = viewModelScope.launch {
        playerRepository.playOrPause()
    }

    private fun next() {
        playerRepository.next()
    }

    private fun previous() {
        playerRepository.previous()
    }

    private fun shuffle() {
        playerRepository.shuffle()
    }

    private fun repeat() {
        playerRepository.changeRepeat()
    }

    private fun playIndex(index: Int) {
        playerRepository.playIndex(index)
    }

    private fun seekTo(position: Long) {
        playerRepository.seekTo(position)
    }

    private fun seekBackward() {
        playerRepository.seekBackward()
    }

    private fun seekForward() {
        playerRepository.seekForward()
    }

    private fun speed(speed: Float) = viewModelScope.launch {
        playerRepository.setSpeed(speed)
        setSpeedUseCase(speed)
    }

    private fun clickPodcast(podcast: Podcast) = viewModelScope.launch {
        _effect.emit(PlayerEffect.NavigateToPodcast(podcast.id))
    }

    private fun clickEpisode(episode: Episode) = viewModelScope.launch {
        val currentState = state.value
        if (currentState is PlayerState.Success) {
            val index = currentState.playlist.indexOf(episode)
            playerRepository.playIndex(index)
        }
    }

    /**
     * 공유받은 링크의 에피소드를 재생 큐에 올린다.
     *
     * 로컬을 먼저 보고, 없으면 원격에서 한 건 가져온다 — 남이 보낸 링크는 이 기기가 한 번도
     * 만난 적 없는 에피소드일 수 있는데 `getEpisodeById` 는 DB 만 보기 때문이다.
     *
     * 재생 중이던 재생목록은 이 호출로 갈린다(`PlayEpisodeUseCase` 가 PLAYLIST 그룹을
     * 갈아치운다). 링크를 눌러 그 에피소드를 듣겠다는 뜻이니 앱 안에서 다른 에피소드를 고른
     * 것과 같은 결과이고, 이어듣기 지점은 에피소드별로 남아 있어 잃는 것이 없다.
     */
    private fun openDeepLink(episodeId: Long, startPositionMs: Long?) = viewModelScope.launch {
        val episode = getEpisodeByIdUseCase(episodeId).first()
            ?: fetchEpisodeByIdUseCase(episodeId)

        if (episode == null) {
            // 복원은 건드리지 않은 채로 둔다. 올릴 것도 없는데 멈춰 버리면 링크 하나가
            // 잘못된 대가로 듣던 것까지 잃는다.
            _effect.emit(PlayerEffect.ShowDeepLinkError)
            return@launch
        }

        // 올릴 것이 확실해진 뒤에 마지막 재생 복원을 멈춘다. 콜드 스타트에서 둘이 나란히
        // 달리면 복원이 늦게 끝나면서 딥링크가 올린 것을 덮어쓰고, 이어서 우리 seekTo 만
        // 그 위에 적용되어 **엉뚱한 에피소드가 링크의 지점부터** 재생된다(실제로 겪은 버그다).
        // 링크는 사용자가 방금 누른 명시적 의도이므로 복원보다 우선한다.
        restoreLastPlayJob?.cancelAndJoin()

        playEpisodeUseCase(episode)
        // 0 도 뜻이 있는 값이라 그대로 태운다 — "맨 앞부터"다.
        startPositionMs?.let { playerRepository.seekTo(it) }

        // 올릴 것이 확정된 뒤에 시트를 연다. 먼저 열면 실패했을 때 빈 시트만 남는다.
        _effect.emit(PlayerEffect.ShowPlayerBottomSheet)
    }

    private fun toggleCurrentLikedEpisode() = viewModelScope.launch {
        val currentState = state.value
        if (currentState is PlayerState.Success) {
            toggleLikedEpisodeUseCase(currentState.nowPlaying)
        }
    }

    private fun toggleLikedEpisode(episode: Episode) = viewModelScope.launch {
        toggleLikedEpisodeUseCase(episode)
    }

    private fun toggleCurrentSavedEpisode() = viewModelScope.launch {
        val currentState = state.value
        if (currentState is PlayerState.Success) {
            val isSavedNow = saveEpisodeUseCase(currentState.nowPlaying)
            if (!isSavedNow) {
                _effect.emit(PlayerEffect.ShowUnsaveSnackbar(currentState.nowPlaying))
            }
        }
    }

    private fun toggleSavedEpisode(episode: Episode) = viewModelScope.launch {
        val isSavedNow = saveEpisodeUseCase(episode)
        if (!isSavedNow) {
            _effect.emit(PlayerEffect.ShowUnsaveSnackbar(episode))
        }
    }

    private fun toggleFollowedPodcast(podcast: Podcast) = viewModelScope.launch {
        toggleFollowedUseCase(podcast.id)
    }

    private fun expandPlayer() = viewModelScope.launch {
        _effect.emit(PlayerEffect.ShowPlayerBottomSheet)
    }

    private fun collapsePlayer() = viewModelScope.launch {
        _effect.emit(PlayerEffect.HidePlayerBottomSheet)
    }

    private fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            try {
                playerRepository.setVolume(1f)
                _sleepTimerRemainingMs.value = durationMs
                val endTime = timeProvider.currentTimeMillis() + durationMs
                while (isActive) {
                    val remaining = endTime - timeProvider.currentTimeMillis()
                    if (remaining <= 0) {
                        playerRepository.setVolume(0f)
                        playerRepository.pause()
                        _effect.emit(PlayerEffect.SleepTimerExpired)
                        break
                    }
                    if (remaining <= FADE_OUT_DURATION_MS) {
                        val volume = remaining.toFloat() / FADE_OUT_DURATION_MS
                        playerRepository.setVolume(volume)
                    }
                    _sleepTimerRemainingMs.value = remaining
                    delay(1000)
                }
            } finally {
                playerRepository.setVolume(1f)
                _sleepTimerRemainingMs.value = null
            }
        }
    }

    private fun startEndOfEpisodeTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            try {
                playerRepository.setVolume(1f)
                // 여기서도 에피소드 판별을 progress 안의 episodeId 로 한다.
                // DB 파생 nowPlaying 과 섞으면 전환 시점에 타이머가 엉뚱한 에피소드를 기준으로 돈다.
                val current = playerRepository.progress.first()
                val startEpisodeId = current.episodeId ?: return@launch
                if (current.duration.inWholeMilliseconds <= 0) return@launch

                var timerExpired = false
                playerRepository.progress.takeWhile { progress ->
                    val remaining = progress.duration.inWholeMilliseconds - progress.position.inWholeMilliseconds
                    if (progress.episodeId != startEpisodeId) return@takeWhile false
                    _sleepTimerRemainingMs.value = remaining.coerceAtLeast(0)
                    if (remaining <= FADE_OUT_DURATION_MS) {
                        val volume = remaining.toFloat() / FADE_OUT_DURATION_MS
                        playerRepository.setVolume(volume.coerceIn(0f, 1f))
                    }
                    val shouldContinue = remaining > 500
                    if (!shouldContinue) timerExpired = true
                    shouldContinue
                }.collect {}

                if (timerExpired) {
                    playerRepository.setVolume(0f)
                    playerRepository.pause()
                    _effect.emit(PlayerEffect.SleepTimerExpired)
                }
            } finally {
                playerRepository.setVolume(1f)
                _sleepTimerRemainingMs.value = null
            }
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    /** 컨트롤 바 자막 토글 버튼. 판정은 토글 상태표(설계 문서)를 그대로 따르는 유스케이스가 한다. */
    private fun toggleCaption() = viewModelScope.launch {
        when (val result = toggleCaptionUseCase(caption.value)) {
            is CaptionToggleResult.DownloadStarted ->
                _effect.emit(PlayerEffect.CaptionDownloadStarted(result.sizeBytes))

            CaptionToggleResult.UnsupportedLanguage ->
                _effect.emit(PlayerEffect.CaptionUnsupported)

            CaptionToggleResult.Enabled,
            CaptionToggleResult.Disabled,
            CaptionToggleResult.DownloadCancelled -> {}
        }
    }

    companion object {
        internal const val FADE_OUT_DURATION_MS = 15_000L
    }
}

sealed interface PlayerState {
    data object Loading : PlayerState
    data class Success(
        val podcast: Podcast,
        val nowPlaying: Episode,
        val playlist: List<Episode>,
        val indexOfList: Int,
        val progress: Progress,
        val isPlaying: Boolean,
        val speed: Float,
        val chapters: List<Chapter>,
        val cue: String,
        val sleepTimerRemainingMs: Long? = null,
    ) : PlayerState

    data class Error(val message: String) : PlayerState
}

sealed interface PlayerAction {
    data object PlayOrPause : PlayerAction
    data object Next : PlayerAction
    data object Previous : PlayerAction
    data object Shuffle : PlayerAction
    data object Repeat : PlayerAction
    data class PlayIndex(val index: Int) : PlayerAction
    data class SeekTo(val position: Long) : PlayerAction
    data object SeekBackward : PlayerAction
    data object SeekForward : PlayerAction
    data class Speed(val speed: Float) : PlayerAction
    data class ClickPodcast(val podcast: Podcast) : PlayerAction
    data class ClickEpisode(val episode: Episode) : PlayerAction
    data object ToggleLike : PlayerAction
    data class ToggleLikedEpisode(val episode: Episode) : PlayerAction
    data object ToggleSave : PlayerAction
    data class ToggleSavedEpisode(val episode: Episode) : PlayerAction
    data class ToggleFollowedPodcast(val podcast: Podcast) : PlayerAction
    data object ExpandPlayer : PlayerAction
    data object CollapsePlayer : PlayerAction
    data class SetSleepTimer(val durationMs: Long) : PlayerAction
    data object CancelSleepTimer : PlayerAction
    data object SleepTimerEndOfEpisode : PlayerAction

    /** 공유받은 링크로 들어온 에피소드를 재생 큐에 올린다. */
    data class OpenDeepLink(val episodeId: Long, val startPositionMs: Long?) : PlayerAction

    /** 컨트롤 바의 자막 토글 버튼. */
    data object ToggleCaption : PlayerAction
}

sealed interface PlayerEffect {
    data class NavigateToPodcast(val podcastId: Long) : PlayerEffect
    data object ShowPlayerBottomSheet : PlayerEffect
    data object HidePlayerBottomSheet : PlayerEffect
    data class ShowUnsaveSnackbar(val episode: Episode) : PlayerEffect
    data object SleepTimerExpired : PlayerEffect

    /** 공유받은 링크의 에피소드를 끝내 찾지 못했다. */
    data object ShowDeepLinkError : PlayerEffect

    /** 자막 모델 다운로드를 시작했다. 스낵바에 크기를 함께 보여준다. */
    data class CaptionDownloadStarted(val sizeBytes: Long) : PlayerEffect

    /** 켜진 에피소드의 언어가 자막을 지원하지 않는다. */
    data object CaptionUnsupported : PlayerEffect

    /** 자막 모델 다운로드/로드가 실패했다. */
    data class CaptionDownloadFailed(val reason: CaptionDownloadFailure.Reason) : PlayerEffect
}

private data class LastPlaySnapshot(
    val episodeId: Long,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeat: Repeat,
)
