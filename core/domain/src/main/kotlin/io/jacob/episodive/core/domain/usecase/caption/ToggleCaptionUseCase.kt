package io.jacob.episodive.core.domain.usecase.caption

import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.LiveCaptionState
import javax.inject.Inject

/** 컨트롤 바의 자막 토글 버튼을 눌렀을 때의 판정. 토글 상태표(설계 문서)를 그대로 따른다. */
class ToggleCaptionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val captionRepository: CaptionRepository,
) {
    suspend operator fun invoke(current: LiveCaptionState): CaptionToggleResult {
        val availability = current.availability

        // 다운로드 진행 중인 자리를 누르면 항상 취소한다(켜진 상태였더라도).
        if (availability is CaptionAvailability.Downloading) {
            captionRepository.cancelDownload(availability.language)
            userRepository.setCaptionEnabled(false)
            return CaptionToggleResult.DownloadCancelled
        }

        if (current.isEnabled) {
            // 켜져 있는데 모델이 없는 경우(예: 다른 언어 에피소드로 넘어감) — enabled 는 유지한 채 받기만 시작한다.
            if (availability is CaptionAvailability.NotInstalled) {
                captionRepository.startDownload(availability.language)
                return CaptionToggleResult.DownloadStarted(availability.sizeBytes)
            }
            userRepository.setCaptionEnabled(false)
            return CaptionToggleResult.Disabled
        }

        userRepository.setCaptionEnabled(true)
        return when (availability) {
            is CaptionAvailability.NotInstalled -> {
                captionRepository.startDownload(availability.language)
                CaptionToggleResult.DownloadStarted(availability.sizeBytes)
            }

            CaptionAvailability.Unsupported -> CaptionToggleResult.UnsupportedLanguage
            else -> CaptionToggleResult.Enabled
        }
    }
}
