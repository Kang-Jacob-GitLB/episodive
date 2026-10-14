package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.network.model.BroadcasterSource
import io.jacob.episodive.core.network.model.OnAirSource

/**
 * 카탈로그 한 항목. [station] 만 화면으로 나가고 나머지는 스트림 주소를 얻는 데만 쓴다.
 *
 * @property radioBrowserIds 시도할 Radio Browser uuid, 우선순위 순
 * @property isHls 스트림이 HLS 인지. [io.jacob.episodive.core.model.StationStream] 으로 밖에 실어 보낸다 —
 *   토큰이 붙은 URL 은 확장자로 추론하기 불안해 플레이어가 MIME 을 명시하는 근거가 된다.
 * @property broadcasterSource 방송사 자체 API 로 새 서명 URL 을 받을 수 있으면 그 식별자. 있으면 Radio Browser 보다
 *   먼저 시도한다. 방송사 URL 은 전부 HLS 이므로 값이 있으면 [isHls] 는 true 여야 한다.
 * @property onAirSource 방송사 편성표 API 로 "지금 방송 중" 프로그램을 받을 수 있으면 그 식별자. 없으면 프로그램 정보 없이
 *   스테이션 이름만 보인다.
 * @property fallbackUrl Radio Browser 가 모두 실패했을 때의 고정 주소. 토큰 URL 은 두지 않는다.
 */
internal data class StationCatalogEntry(
    val station: Station,
    val radioBrowserIds: List<String>,
    val isHls: Boolean,
    val fallbackUrl: String?,
    val broadcasterSource: BroadcasterSource? = null,
    val onAirSource: OnAirSource? = null,
)
