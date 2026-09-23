package io.jacob.episodive.core.domain.repository

import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionModelState
import io.jacob.episodive.core.model.caption.CaptionSession
import io.jacob.episodive.core.model.caption.LiveCaption
import kotlinx.coroutines.flow.Flow

interface CaptionRepository {
    fun modelState(language: CaptionLanguage): Flow<CaptionModelState>
    val downloadFailures: Flow<CaptionDownloadFailure>
    fun startDownload(language: CaptionLanguage)
    fun cancelDownload(language: CaptionLanguage)

    /** 수집하는 동안만 인식이 돈다(수집 한 번 = 세션 하나). 설치 안 됐으면 바로 끝. */
    fun liveCaptions(episodeId: Long, language: CaptionLanguage): Flow<CaptionSession>

    /**
     * VTT cue 를 기기 언어로 번역한다. sourceLanguageTag = feedLanguage.
     * 번역 불가(같은 언어/미지원)면 translation=null 로 그대로 흘린다. cue 빈 문자열이면 null 방출.
     */
    fun translatedCues(
        episodeId: Long,
        sourceLanguageTag: String,
        cues: Flow<String>,
    ): Flow<LiveCaption?>
}
