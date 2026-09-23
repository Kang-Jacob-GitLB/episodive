package io.jacob.episodive.core.model.caption

/**
 * 화면에 그릴 확정/부분 자막 한 줄.
 *
 * @param episodeId 이 자막이 속한 에피소드. `progress.episodeId` 와 일치할 때만 화면에 그린다.
 * @param lineId 줄 단위 식별자. 같은 줄의 partial 갱신은 lineId 가 같다.
 * @param text 원문(정규화 완료).
 * @param translation 기기 언어로 번역된 문장. 준비 전/불가하면 null.
 * @param isFinal 확정 줄이면 true, 아직 흘러가는 partial 이면 false.
 */
data class LiveCaption(
    val episodeId: Long,
    val lineId: Long,
    val text: String,
    val translation: String? = null,
    val isFinal: Boolean,
)
