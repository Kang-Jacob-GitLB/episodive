package io.jacob.episodive.core.model.caption

/**
 * 자막이 붙는 대상. 자막 세션과 [LiveCaption] 에 찍는 도장이다 — 엔진·presenter 는 이 값을
 * 해석하지 않고 그대로 실어 나르며, 화면이 지금 재생 중인 대상과 일치할 때만 그린다.
 */
sealed interface CaptionSubject {
    /** 에피소드. 화면은 `progress.episodeId` 와 비교한다. */
    data class Episode(val id: Long) : CaptionSubject

    /** 라디오 스테이션. */
    data class Station(val id: String) : CaptionSubject
}
