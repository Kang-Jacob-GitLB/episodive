package io.jacob.episodive.core.network.model

/** 방송사 편성표 API 로 "지금 방송 중" 프로그램을 받을 때 필요한 식별자. */
sealed interface OnAirSource {
    /** KBS. [channelCode] 는 편성표 `channel_code` 값(예: 21). */
    data class Kbs(val channelCode: String) : OnAirSource

    /** MBC. [channel] 은 편성표 `Channel` 값(STFM·FM4U·CHAM). */
    data class Mbc(val channel: String) : OnAirSource

    /** SBS. [channelId] 는 온에어 목록의 `channelid` 값(S07·S08). */
    data class Sbs(val channelId: String) : OnAirSource

    /** CBS. [key] 는 편성표 응답의 채널 키(fm·musicfm·joy4u). */
    data class Cbs(val key: String) : OnAirSource
}
