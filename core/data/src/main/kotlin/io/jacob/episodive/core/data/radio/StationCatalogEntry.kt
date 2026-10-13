package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.model.Station

/**
 * 카탈로그 한 항목. [station] 만 화면으로 나가고 나머지는 스트림 주소를 얻는 데만 쓴다.
 *
 * @property radioBrowserIds 시도할 Radio Browser uuid, 우선순위 순
 * @property isHls 스트림이 HLS 인지. [io.jacob.episodive.core.model.StationStream] 으로 밖에 실어 보낸다 —
 *   토큰이 붙은 URL 은 확장자로 추론하기 불안해 플레이어가 MIME 을 명시하는 근거가 된다.
 * @property fallbackUrl Radio Browser 가 모두 실패했을 때의 고정 주소. 토큰 URL 은 두지 않는다.
 */
internal data class StationCatalogEntry(
    val station: Station,
    val radioBrowserIds: List<String>,
    val isHls: Boolean,
    val fallbackUrl: String?,
)
