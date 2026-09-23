package io.jacob.episodive.feature.player

import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.LiveCaptionState

/**
 * 컨트롤 바 자막 토글 버튼이 그릴 모양. 토글 상태표(설계 문서)를 그대로 따르는 순수 매핑이다.
 */
sealed interface CaptionButtonState {
    /** 자막 모델을 받는 중. `progress` 가 0 이하면 아직 크기를 모르는 무한 스피너로 그린다. */
    data class Progress(val progress: Float) : CaptionButtonState

    /** 켜져 있고 자막을 실제로 그릴(또는 그릴 예정인) 상태 — primary 틴트. */
    data object Active : CaptionButtonState

    /** 켜져 있지만 이 에피소드 언어를 지원하지 않는다 — primary alpha .38. */
    data object Dimmed : CaptionButtonState

    /** 꺼져 있거나, 켜져 있지만 모델을 아직 받지 않았다 — onSurfaceVariant. */
    data object Inactive : CaptionButtonState
}

fun LiveCaptionState.toCaptionButtonState(): CaptionButtonState {
    val availability = this.availability
    if (availability is CaptionAvailability.Downloading) {
        return CaptionButtonState.Progress(availability.progress)
    }
    if (!isEnabled) return CaptionButtonState.Inactive
    return when (availability) {
        is CaptionAvailability.Ready,
        is CaptionAvailability.Loading,
        CaptionAvailability.Unknown,
        CaptionAvailability.Transcript -> CaptionButtonState.Active
        CaptionAvailability.Unsupported -> CaptionButtonState.Dimmed
        is CaptionAvailability.NotInstalled -> CaptionButtonState.Inactive
        is CaptionAvailability.Downloading -> CaptionButtonState.Inactive // 위에서 이미 처리됨(도달 안 함)
    }
}
