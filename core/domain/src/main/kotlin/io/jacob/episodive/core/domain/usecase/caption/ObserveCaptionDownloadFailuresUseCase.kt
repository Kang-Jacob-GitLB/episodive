package io.jacob.episodive.core.domain.usecase.caption

import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** STT 모델 다운로드/로드 실패를 일회성으로 흘려보낸다(스낵바 등). */
class ObserveCaptionDownloadFailuresUseCase @Inject constructor(
    private val captionRepository: CaptionRepository,
) {
    operator fun invoke(): Flow<CaptionDownloadFailure> = captionRepository.downloadFailures
}
