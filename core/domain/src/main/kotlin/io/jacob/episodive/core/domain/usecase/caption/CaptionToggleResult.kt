package io.jacob.episodive.core.domain.usecase.caption

/** `ToggleCaptionUseCase` 가 자막 토글 탭 결과로 돌려주는 값. UI 는 이걸로 스낵바 등을 결정한다. */
sealed interface CaptionToggleResult {
    data object Enabled : CaptionToggleResult
    data object Disabled : CaptionToggleResult
    data object DownloadCancelled : CaptionToggleResult
    data object UnsupportedLanguage : CaptionToggleResult
    data class DownloadStarted(val sizeBytes: Long) : CaptionToggleResult
}
