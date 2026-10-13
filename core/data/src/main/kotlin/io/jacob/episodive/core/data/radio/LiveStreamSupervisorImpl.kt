package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.data.util.NetworkMonitor
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.domain.station.LiveStreamSupervisor
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.PlaybackFailure
import io.jacob.episodive.core.model.PlaybackFailure.Cause
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 모든 판단과 플레이어 조작은 [mainDispatcher] 에서 한다. 플레이어는 메인 스레드에서만 만질 수 있고,
 * 예산 필드도 그 한 스레드에서만 읽고 쓰므로 따로 동기화하지 않는다.
 */
class LiveStreamSupervisorImpl(
    private val playerRepository: PlayerRepository,
    private val stationRepository: StationRepository,
    private val networkMonitor: NetworkMonitor,
    private val scope: CoroutineScope,
    private val mainDispatcher: CoroutineDispatcher,
) : LiveStreamSupervisor {

    private val started = AtomicBoolean(false)
    private val _unplayable = MutableSharedFlow<Station>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val unplayable: Flow<Station> = _unplayable

    private var recoveryJob: Job? = null

    // 주소를 다시 받은 횟수(Expired·Gone 공유), 같은 주소 재연결(Network·Ended·Other), 라이브 창 이탈, 만료 재시도.
    private var resolveCount = 0
    private var reconnectCount = 0
    private var behindWindowCount = 0
    private var expiredCount = 0

    // 실패한 주소는 한 세션 동안 유지한다. 안정 재생으로도 비우지 않는다.
    private val excluded = mutableSetOf<String>()

    // 세션은 사용자가 그 스테이션을 튼 한 번이다. 같은 스테이션을 다시 눌러도 새 세션이므로, id 가 아니라
    // 사용자 재생 요청 수(playRequestCount)로 가른다. 플레이어에 올라 있다고 아는 주소는 첫 실패의
    // streamUrl 로 시작해 우리가 다시 올릴 때마다 갱신한다. 교체 뒤에 늦게 도착한 옛 주소의 실패를 거르는 근거다.
    private var sessionRequestCount = 0L
    private var currentUrl: String? = null

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        // 자식 셋 모두 UNDISPATCHED — 구독이 동기적으로 걸려야 start() 직후의 첫 실패를 놓치지 않는다.
        scope.launch(mainDispatcher, start = CoroutineStart.UNDISPATCHED) {
            sessionRequestCount = playerRepository.playRequestCount()
            launch(start = CoroutineStart.UNDISPATCHED) {
                playerRepository.nowPlayingStation
                    .map { it?.id }
                    .distinctUntilChanged()
                    .collect { beginSession() }
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                // 곧바로 다시 끊기는 스트림이 매번 예산을 되찾아 무한 재연결하지 않도록, 한동안 이어 들린
                // 뒤에만 되돌린다. READY 가 흔들리면 collectLatest 가 대기를 취소한다.
                playerRepository.playback.collectLatest {
                    if (it == Playback.READY) {
                        delay(STABLE_PLAYBACK_MILLIS)
                        resetBudget()
                    }
                }
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                playerRepository.failures.collect { failure ->
                    val current = playerRepository.nowPlayingStation.first() ?: return@collect
                    // 에피소드 실패(stationId null)나 이미 지나간 스테이션의 실패는 건드리지 않는다.
                    if (failure.stationId != current.id) return@collect
                    // playRequestCount 는 Flow 가 아니라, 새 세션인지는 실패를 받을 때와 다시 올리기 직전에 본다.
                    if (playerRepository.playRequestCount() != sessionRequestCount) beginSession()
                    // 우리가 이미 교체한 주소의 실패가 늦게 온 것이다. 새 주소를 건드리면 안 된다.
                    val known = currentUrl
                    if (known != null && failure.streamUrl != null && failure.streamUrl != known) return@collect
                    if (known == null) currentUrl = failure.streamUrl
                    recoveryJob?.cancel()
                    recoveryJob = launch { recover(current, failure) }
                }
            }
        }
    }

    /** 진행 중인 복구를 버리고 예산·제외 주소·기억한 주소를 비운다. */
    private fun beginSession() {
        recoveryJob?.cancel()
        resetBudget()
        excluded.clear()
        currentUrl = null
        sessionRequestCount = playerRepository.playRequestCount()
    }

    private fun resetBudget() {
        resolveCount = 0
        reconnectCount = 0
        behindWindowCount = 0
        expiredCount = 0
    }

    private suspend fun recover(station: Station, failure: PlaybackFailure) {
        val session = sessionRequestCount
        when (failure.cause) {
            Cause.Expired -> {
                if (expiredCount < MAX_EXPIRED) {
                    expiredCount++
                    reResolve(station, failure, session, excludeFailed = false)
                } else {
                    reResolve(station, failure, session, excludeFailed = true)
                }
            }

            Cause.Gone -> reResolve(station, failure, session, excludeFailed = true)

            // Other 는 AudioTrack·디코더 초기화처럼 기기 쪽 사정이라 주소 탓이 아니다. 주소를 제외하거나
            // 다시 받지 말고 같은 주소로 재연결한다.
            Cause.Network, Cause.Ended, Cause.Other -> {
                // 오프라인이면 횟수를 쓰지 않고 기다린다. 대기 중 취소되면(새 실패·새 세션) 아무것도 소모하지 않는다.
                networkMonitor.isOnline.first { it }
                if (reconnectCount >= MAX_RECONNECT) return giveUp(station)
                val attempt = reconnectCount++
                delay(BACKOFF_BASE_MILLIS shl attempt)
                restart(station, session)
            }

            Cause.BehindLiveWindow -> {
                if (behindWindowCount >= MAX_BEHIND_WINDOW) return giveUp(station)
                behindWindowCount++
                restart(station, session)
            }
        }
    }

    private suspend fun reResolve(
        station: Station,
        failure: PlaybackFailure,
        session: Long,
        excludeFailed: Boolean,
    ) {
        // 토큰이 붙은 주소는 매번 새 문자열이라 excluded 만으로는 루프가 안 끊긴다 — 이 상한이 끊는다.
        if (resolveCount >= MAX_RESOLVE) return giveUp(station)
        resolveCount++
        if (excludeFailed) failure.streamUrl?.let { excluded += it }
        val stream = stationRepository.resolveStreamUrl(
            stationId = station.id,
            excluding = if (excludeFailed) excluded.toSet() else emptySet(),
        ) ?: return giveUp(station)
        restart(station, session, stream)
    }

    /** 복구는 늦게 도착한다. 그 사이 사용자가 같은 스테이션을 다시 틀었다면 새로 올라온 것을 건드리지 않는다. */
    private fun restart(station: Station, session: Long, stream: StationStream? = null) {
        if (playerRepository.playRequestCount() != session) return
        if (playerRepository.restartStation(station.id, stream) && stream != null) {
            currentUrl = stream.url
        }
    }

    private fun giveUp(station: Station) {
        Timber.w("giving up live station. stationId: ${station.id}")
        // 항목을 비워야 hasMediaItem() 이 영구히 참으로 남아 마지막 에피소드 복원을 막지 않는다.
        if (playerRepository.stopStation(station.id)) {
            _unplayable.tryEmit(station)
        }
    }

    private companion object {
        const val MAX_RESOLVE = 3
        const val MAX_RECONNECT = 5
        const val MAX_BEHIND_WINDOW = 3
        const val MAX_EXPIRED = 1
        const val BACKOFF_BASE_MILLIS = 1_000L
        const val STABLE_PLAYBACK_MILLIS = 30_000L
    }
}
