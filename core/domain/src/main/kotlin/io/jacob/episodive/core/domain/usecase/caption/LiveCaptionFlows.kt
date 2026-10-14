package io.jacob.episodive.core.domain.usecase.caption

import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionModelState
import io.jacob.episodive.core.model.caption.CaptionSession
import io.jacob.episodive.core.model.caption.CaptionSubject
import io.jacob.episodive.core.model.caption.LiveCaptionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

/** 꺼진 동안: 모델 상태만 비추고 인식 세션은 열지 않는다. */
internal fun CaptionRepository.observeModelOnly(language: CaptionLanguage): Flow<LiveCaptionState> =
    modelState(language).distinctUntilChanged().map { modelState ->
        val availability = when (modelState) {
            is CaptionModelState.NotInstalled -> CaptionAvailability.NotInstalled(language, modelState.sizeBytes)
            is CaptionModelState.Downloading -> CaptionAvailability.Downloading(language, modelState.progress)
            CaptionModelState.Installed -> CaptionAvailability.Ready(language)
        }
        LiveCaptionState(isEnabled = false, availability = availability, caption = null)
    }

/**
 * 모델 상태(`modelState`)가 바뀔 때만 세션을 새로 연다. `Downloading` 의 진행률 변화는
 * distinctUntilChanged 를 통과하지만 그 값이 세션을 여는 `Installed` 로 바뀌는 게 아니므로,
 * 이미 열려 있는 `liveCaptions` 수집을 재시작시키지 않는다.
 */
internal fun CaptionRepository.observeSession(subject: CaptionSubject, language: CaptionLanguage): Flow<LiveCaptionState> {
    // Unloadable 로 인식기가 죽으면 data 층이 modelState 를 NotInstalled 로 되돌리는 게
    // 정상 경로다. 그 재emit 이 올 때까지의 과도 상태를 채우기 위해 마지막으로 본 크기만 기억한다.
    var lastKnownSizeBytes = 0L

    // transformLatest 는 flatMapLatest 처럼 새 modelState 가 올 때 이전 collect(liveCaptions
    // 세션 포함)를 취소한다. 같은 receiver 안에서 emit 하므로(flow{} + collectLatest 조합과
    // 달리) 다른 코루틴에서 emit 하는 게 아니라 flow invariant 위반이 나지 않는다.
    return modelState(language).distinctUntilChanged().transformLatest { modelState ->
        when (modelState) {
            is CaptionModelState.NotInstalled -> {
                lastKnownSizeBytes = modelState.sizeBytes
                emit(LiveCaptionState(isEnabled = true, availability = CaptionAvailability.NotInstalled(language, modelState.sizeBytes), caption = null))
            }

            is CaptionModelState.Downloading -> {
                lastKnownSizeBytes = modelState.totalBytes
                emit(LiveCaptionState(isEnabled = true, availability = CaptionAvailability.Downloading(language, modelState.progress), caption = null))
            }

            CaptionModelState.Installed -> liveCaptions(subject, language).collect { session ->
                when (session) {
                    CaptionSession.Loading ->
                        emit(LiveCaptionState(isEnabled = true, availability = CaptionAvailability.Loading(language), caption = null))

                    is CaptionSession.Running ->
                        emit(LiveCaptionState(isEnabled = true, availability = CaptionAvailability.Ready(language), caption = session.caption))

                    CaptionSession.Unloadable ->
                        emit(LiveCaptionState(isEnabled = true, availability = CaptionAvailability.NotInstalled(language, lastKnownSizeBytes), caption = null))
                }
            }
        }
    }
}
