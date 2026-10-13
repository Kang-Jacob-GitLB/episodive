package io.jacob.episodive.core.data.player

import io.jacob.episodive.core.common.TimeProvider
import io.jacob.episodive.core.domain.player.SleepTimer
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.SLEEP_TIMER_FADE_OUT_MS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 플레이어는 메인에서만 만질 수 있어 [mainDispatcher] 에서 돈다. [job] 도 그 한 스레드에서만 만진다.
 */
class SleepTimerImpl(
    private val playerRepository: PlayerRepository,
    private val timeProvider: TimeProvider,
    private val scope: CoroutineScope,
    private val mainDispatcher: CoroutineDispatcher,
) : SleepTimer {

    private val _remainingMs = MutableStateFlow<Long?>(null)
    override val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    // replay 를 두지 않는다. 지난 만료가 다음 구독자에게 재생되면 이미 지나간 일을 알린다.
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val expired: Flow<Unit> = _expired

    private var job: Job? = null

    override fun start(durationMs: Long) {
        job?.cancel()
        job = scope.launch(mainDispatcher) {
            try {
                playerRepository.setVolume(1f)
                _remainingMs.value = durationMs
                val endTime = timeProvider.currentTimeMillis() + durationMs
                while (isActive) {
                    val remaining = endTime - timeProvider.currentTimeMillis()
                    if (remaining <= 0) {
                        playerRepository.setVolume(0f)
                        playerRepository.pause()
                        _expired.emit(Unit)
                        break
                    }
                    if (remaining <= SLEEP_TIMER_FADE_OUT_MS) {
                        val volume = remaining.toFloat() / SLEEP_TIMER_FADE_OUT_MS
                        playerRepository.setVolume(volume)
                    }
                    _remainingMs.value = remaining
                    delay(1000)
                }
            } finally {
                playerRepository.setVolume(1f)
                _remainingMs.value = null
            }
        }
    }

    override fun startEndOfEpisode() {
        job?.cancel()
        job = scope.launch(mainDispatcher) {
            try {
                playerRepository.setVolume(1f)
                // 여기서도 에피소드 판별을 progress 안의 episodeId 로 한다.
                // DB 파생 nowPlaying 과 섞으면 전환 시점에 타이머가 엉뚱한 에피소드를 기준으로 돈다.
                val current = playerRepository.progress.first()
                val startEpisodeId = current.episodeId ?: return@launch
                if (current.duration.inWholeMilliseconds <= 0) return@launch
                // HLS 라이브의 duration 은 DVR 창 길이라 양수다. 끝이 없으니 "에피소드 끝까지" 는 성립하지 않는다.
                if (playerRepository.liveEpisodeId.first() == startEpisodeId) return@launch

                var timerExpired = false
                playerRepository.progress.takeWhile { progress ->
                    val remaining = progress.duration.inWholeMilliseconds - progress.position.inWholeMilliseconds
                    if (progress.episodeId != startEpisodeId) return@takeWhile false
                    _remainingMs.value = remaining.coerceAtLeast(0)
                    if (remaining <= SLEEP_TIMER_FADE_OUT_MS) {
                        val volume = remaining.toFloat() / SLEEP_TIMER_FADE_OUT_MS
                        playerRepository.setVolume(volume.coerceIn(0f, 1f))
                    }
                    val shouldContinue = remaining > 500
                    if (!shouldContinue) timerExpired = true
                    shouldContinue
                }.collect {}

                if (timerExpired) {
                    playerRepository.setVolume(0f)
                    playerRepository.pause()
                    _expired.emit(Unit)
                }
            } finally {
                playerRepository.setVolume(1f)
                _remainingMs.value = null
            }
        }
    }

    override fun cancel() {
        job?.cancel()
        job = null
    }
}
