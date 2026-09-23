package io.jacob.episodive.core.model.caption

/**
 * STT 모델 다운로드/로드 실패를 사용자에게 알리기 위한 일회성 이벤트.
 */
data class CaptionDownloadFailure(
    val language: CaptionLanguage,
    val reason: Reason,
) {
    enum class Reason(val value: String) {
        NETWORK("network"),
        STORAGE("storage"),
        CORRUPT("corrupt"),
        UNLOADABLE("unloadable"),
    }
}
