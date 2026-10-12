package io.jacob.episodive.core.domain.station

import io.jacob.episodive.core.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 스테이션 재생 요청의 유일한 창구. 앱에 하나뿐이라 어느 화면에서 눌러도 같은 요청 줄에 선다.
 *
 * 화면마다 요청을 직렬화하면 두 화면에 걸친 연타에서 옛 요청이 이기고, 정지가 다른 화면의 pending 을
 * 지우지 못하며, 화면이 컴포즈되지 않은 사이의 실패 안내가 유실된다. 메인에서 호출할 것.
 */
interface StationPlaybackController {
    /** 주소를 받는 중인 스테이션. 이 동안은 플레이어가 버퍼링하지 않아 카드가 이 값으로만 피드백을 준다. */
    val pendingStationId: StateFlow<String?>

    /**
     * 스테이션을 튼다.
     * - 지금 재생 중인 스테이션이고 받는 중인 것이 없으면 재생/일시정지만 바꾼다.
     * - 지금 재생 중인 스테이션인데 다른 스테이션을 받는 중이면, 받는 중인 요청만 취소한다(원래 것을 다시 고른 것).
     * - 이미 받는 중인 스테이션이면 무시한다.
     * - 그 외에는 앞 요청을 취소하고 새로 받는다.
     */
    fun play(stationId: String)

    /** 받는 중인 요청을 취소하고 재생을 멈춘다. 사용자 의도의 정지라 `PlayerRepository.stop()` 을 부른다. */
    fun stop()

    /** 재생할 주소를 얻지 못한 스테이션. 구독자가 없을 때의 발행은 버려진다. */
    val unavailable: Flow<Station>
}
