package io.jacob.episodive.core.model

/**
 * 지금 기준 편성표. 오늘 전체와 내일 새벽까지 이어지는 프로그램을 시작 시각순으로 담는다.
 *
 * @property dayStartMillis 오늘 00:00(Asia/Seoul)의 epoch millis. 화면이 "내일" 구분을 그을 기준이다.
 * @property programs 비어 있으면 편성표를 모르거나 받지 못했다는 뜻이다.
 */
data class StationSchedule(
    val dayStartMillis: Long,
    val programs: List<OnAirProgram>,
)
