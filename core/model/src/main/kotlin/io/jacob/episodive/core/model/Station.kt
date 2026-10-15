package io.jacob.episodive.core.model

/**
 * 화면에 보여 줄 방송국. 스트림 URL 과 외부 서비스 uuid 는 일부러 싣지 않는다 —
 * 재생 직전에 최신 주소를 따로 받아 온다.
 *
 * @property id 카탈로그 안에서 바뀌지 않는 slug
 * @property language 방송 언어(BCP-47). 라이브 자막의 인식 언어를 정한다. null 이면 자막 대상이 아니다
 * (음악 채널처럼 받아쓸 말이 없는 방송을 이것으로만 끈다).
 */
data class Station(
    val id: String,
    val name: String,
    val broadcaster: String,
    val kind: StationKind,
    val region: String?,
    val language: String?,
)
