package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.domain.station.LiveStreamSupervisor
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

sealed interface PlayStationResult {
    data object Started : PlayStationResult

    /** 모르는 스테이션이거나 재생할 주소를 얻지 못했다. */
    data object Unavailable : PlayStationResult

    /** 주소를 받는 사이 사용자가 다른 것을 골라 이 요청은 버렸다. */
    data object Superseded : PlayStationResult
}

/**
 * 스테이션을 올려 튼다. 스테이션 재생의 유일한 진입점이다 — 감독자 시작·`Superseded` 판정·최근 청취 기록이
 * 이 순서로 묶여 있어야 하므로, 주소 해석이나 기록을 따로 부르는 유스케이스를 두지 않는다.
 *
 * **메인에서 호출할 것.** 주소 해석은 리포지토리가 IO 로 옮겨 하지만, `playRequestCount`·`playStation` 은
 * 플레이어 스레드에서만 부를 수 있다. 연타의 직렬화(이전 요청 Job 취소)는 호출 측의 책임이다 — 여기서는
 * 해석 중에 끼어든 다른 재생만 [PlayStationResult.Superseded] 로 가려낸다.
 */
class PlayStationUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val stationRepository: StationRepository,
    private val supervisor: LiveStreamSupervisor,
) {
    suspend operator fun invoke(stationId: String): PlayStationResult {
        // getStation 은 해외 스테이션이면 Room 을 기다릴 수 있다. 그 대기 중의 끼어듦도 잡도록 가장 먼저 읽는다.
        val requestCount = playerRepository.playRequestCount()
        val station = stationRepository.getStation(stationId) ?: return PlayStationResult.Unavailable

        // 감독자는 Application 이 아니라 여기서 처음 깨운다. Application.onCreate 에서 주입하면
        // WorkManager 로 깬 프로세스에서도 ExoPlayer 가 만들어진다.
        supervisor.start()

        val stream = stationRepository.resolveStreamUrl(stationId)
        // 알림 아트를 IO 에서 미리 그린다(메인에서 그리지 않게). 이 대기도 끼어듦 판정에 들어가도록 아래 확인보다 앞에 둔다. 실패해도 재생은 막지 않는다 —
        // playStation 이 메인에서 그리는 폴백을 가진다.
        if (stream != null) {
            try {
                playerRepository.prepareStationArtwork(station)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "prepareStationArtwork failed. stationId: $stationId")
            }
        }
        // 해석 결과가 null 이어도 먼저 본다. 사용자가 이미 다른 것을 골랐다면 이 요청의 실패는 보여 줄 가치가 없다.
        if (playerRepository.playRequestCount() != requestCount) return PlayStationResult.Superseded
        if (stream == null) return PlayStationResult.Unavailable

        playerRepository.playStation(station, stream)

        try {
            stationRepository.markPlayed(stationId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 최근 청취 기록 실패가 이미 시작한 재생을 실패로 만들면 안 된다.
            Timber.w(e, "markPlayed failed. stationId: $stationId")
        }
        return PlayStationResult.Started
    }
}
