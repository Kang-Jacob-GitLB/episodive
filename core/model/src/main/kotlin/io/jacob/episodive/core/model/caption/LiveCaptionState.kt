package io.jacob.episodive.core.model.caption

/**
 * 플레이어 화면이 그대로 그릴 자막 상태 전체.
 */
data class LiveCaptionState(
    val isEnabled: Boolean,
    val availability: CaptionAvailability,
    val caption: LiveCaption?,
) {
    companion object {
        val Initial = LiveCaptionState(
            isEnabled = false,
            availability = CaptionAvailability.Unknown,
            caption = null,
        )
    }
}
