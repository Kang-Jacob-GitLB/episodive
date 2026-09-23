package io.jacob.episodive.core.model.caption

/**
 * 화면에 그릴 자막 한 줄. 같은 줄의 partial 갱신은 [id] 가 같다.
 *
 * @param id 줄 단위 식별자. 번역 줄은 원문 줄의 id 를 그대로 쓴다.
 * @param text 원문(정규화 완료) 또는 번역문.
 * @param isFinal 확정 줄이면 true, 아직 흘러가는 partial 이면 false. 번역 줄은 늘 true.
 */
data class CaptionLine(
    val id: Long,
    val text: String,
    val isFinal: Boolean,
)

/**
 * 화면에 그릴 자막 상태 전체. 롤링 표시를 위해 여러 줄을 한꺼번에 들고 있는다.
 *
 * @param episodeId 이 자막이 속한 에피소드. `progress.episodeId` 와 일치할 때만 화면에 그린다.
 * @param lines 원문 줄. 오래된 → 최신 순이고 마지막이 진행 중(partial)일 수 있다.
 * @param translations 번역 줄. 오래된 → 최신 순. 원문 줄보다 늦게 도착하며, 이미 받은 것보다
 * id 가 작은(늦게 온) 번역은 들어오지 않는다.
 * @param isTranslating 이 세션이 번역을 하는가(원문 언어와 기기 언어가 다르고 번역기가 있다).
 * 화면이 영역을 나눌지를 이것으로 정한다 — [translations] 가 비었는지로 정하면 첫 번역이
 * 도착하는 순간 원문 영역이 반으로 줄며 튄다.
 */
data class LiveCaption(
    val episodeId: Long,
    val lines: List<CaptionLine> = emptyList(),
    val translations: List<CaptionLine> = emptyList(),
    val isTranslating: Boolean = false,
)
