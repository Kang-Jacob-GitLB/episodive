package io.jacob.episodive.core.model.caption

/**
 * 언어별 STT 모델의 설치 상태 (`CaptionModelManager` 가 언어마다 하나씩 든다).
 */
sealed interface CaptionModelState {
    data class NotInstalled(val sizeBytes: Long) : CaptionModelState

    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : CaptionModelState {
        val progress: Float
            get() = if (totalBytes <= 0L) 0f else downloadedBytes.toFloat() / totalBytes
    }

    data object Installed : CaptionModelState
}
