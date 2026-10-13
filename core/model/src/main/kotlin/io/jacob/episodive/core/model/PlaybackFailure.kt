package io.jacob.episodive.core.model

/**
 * 플레이어가 재생을 이어 가지 못했다는 신호. 복구 쪽이 원인에 맞는 조치(주소 재요청·재시도·포기)를
 * 고르도록 원인만 갈라 싣는다. 직렬화하지 않는 값이라 enum value 규약 대신 sealed 로 둔다.
 *
 * @property stationId 실패한 항목이 스테이션이면 그 id, 에피소드면 null
 * @property streamUrl 실패한 항목의 주소. 복구가 "아직 그 주소인가" 를 가르는 근거다
 */
data class PlaybackFailure(
    val cause: Cause,
    val stationId: String?,
    val streamUrl: String?,
) {
    sealed interface Cause {
        /** HTTP 401/403/410. 토큰이 만료됐을 수 있어 주소를 다시 받아야 한다. */
        data object Expired : Cause

        /** 404·기타 4xx·파싱/미지원·cleartext 차단·파일 없음·content-type 불일치. 다시 해도 같다. */
        data object Gone : Cause

        /** 연결 실패·타임아웃·5xx. 잠시 뒤 다시 하면 될 수 있다. */
        data object Network : Cause

        data object BehindLiveWindow : Cause

        /** 스테이션 항목이 STATE_ENDED 가 된 것(Icecast 끊김). 라이브는 끝나지 않는다. */
        data object Ended : Cause

        data object Other : Cause
    }
}
