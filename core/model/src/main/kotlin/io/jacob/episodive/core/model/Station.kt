package io.jacob.episodive.core.model

/**
 * 화면에 보여 줄 방송국. 스트림 URL 과 외부 서비스 uuid 는 일부러 싣지 않는다 —
 * 재생 직전에 최신 주소를 따로 받아 온다.
 *
 * @property id 카탈로그 안에서 바뀌지 않는 slug
 */
data class Station(
    val id: String,
    val name: String,
    val broadcaster: String,
    val kind: StationKind,
    val region: String?,
)
