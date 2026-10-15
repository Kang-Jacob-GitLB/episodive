package io.jacob.episodive.core.model

/**
 * 라디오 스테이션이 지금 방송 중인 프로그램. 방송사의 비공식 API 에서 받으므로 [title] 외에는 비어 있을 수 있다.
 *
 * @property host 진행자. 방송사가 주지 않으면 null.
 * @property imageUrl https 주소만.
 * @property startMillis 방송 시작(epoch millis). 시각을 모르면 null.
 * @property endMillis 방송 종료(epoch millis). 시각을 모르면 null.
 * @property song 지금 나오는 곡("제목 - 가수"). 곡 정보를 주는 방송사(MBC)만 채운다.
 */
data class OnAirProgram(
    val title: String,
    val host: String?,
    val imageUrl: String?,
    val startMillis: Long?,
    val endMillis: Long?,
    val song: String?,
)
