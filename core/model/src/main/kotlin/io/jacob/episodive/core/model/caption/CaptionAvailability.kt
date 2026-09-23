package io.jacob.episodive.core.model.caption

/**
 * 현재 재생 중인 에피소드에 자막을 붙일 수 있는지, 붙일 수 있다면 어떤 경로인지.
 */
sealed interface CaptionAvailability {
    /** 판정에 필요한 정보(progress/nowPlaying)가 아직 안 모였다. */
    data object Unknown : CaptionAvailability

    /** feedLanguage 가 지원 언어가 아니다. */
    data object Unsupported : CaptionAvailability

    /** VTT transcript 가 있는 에피소드. STT 없이 VTT cue 를 (번역해) 보여준다. */
    data object Transcript : CaptionAvailability

    /** STT 모델이 아직 없다. */
    data class NotInstalled(val language: CaptionLanguage, val sizeBytes: Long) : CaptionAvailability

    /** STT 모델을 받는 중. */
    data class Downloading(val language: CaptionLanguage, val progress: Float) : CaptionAvailability

    /** 모델은 설치됐지만 인식기를 아직 올리는 중. */
    data class Loading(val language: CaptionLanguage) : CaptionAvailability

    /** 인식기가 올라가 인식 중. */
    data class Ready(val language: CaptionLanguage) : CaptionAvailability
}
