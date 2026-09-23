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
     * 번역 불가(같은 언어/미지원)면 번역 없이 원문만 흘린다. 빈 cue(cue 사이 공백)는 무시하고
     * 쌓아 둔 줄을 그대로 유지한다 — [seeks] 가 발행될 때만 롤링 history 를 통째로 비운다.
     */
    fun translatedCues(
        episodeId: Long,
        sourceLanguageTag: String,
        cues: Flow<String>,
        seeks: Flow<Unit>,
    ): Flow<LiveCaption?>
}
