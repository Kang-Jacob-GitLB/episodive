package io.jacob.episodive.core.model.caption

/**
 * 에피소드 하나에 대한 인식 세션의 진행 상태.
 */
sealed interface CaptionSession {
    /** 인식기를 올리는 중(RecognizerCache 미스). */
    data object Loading : CaptionSession

    /** 인식 중. caption 이 null 이면 아직 확정/부분 결과가 없다. */
    data class Running(val caption: LiveCaption?) : CaptionSession

    /** 모델 로드가 연속 2회 미완이라 더 이상 시도하지 않는다. */
    data object Unloadable : CaptionSession
}
